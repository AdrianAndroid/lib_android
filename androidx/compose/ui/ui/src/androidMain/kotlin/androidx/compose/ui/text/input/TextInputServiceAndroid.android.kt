/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.text.input

import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.core.view.inputmethod.EditorInfoCompat
import kotlinx.coroutines.channels.Channel
import kotlin.math.roundToInt

private const val DEBUG_CLASS = "TextInputServiceAndroid"

/**
 * Provide Android specific input service with the Operating System.
 */
internal class TextInputServiceAndroid(
    val view: View,
    private val inputMethodManager: InputMethodManager
) : PlatformTextInputService {
    /** True if the currently editable composable has connected */
    private var editorHasFocus = false

    /**
     *  The following three observers are set when the editable composable has initiated the input
     *  session
     */
    private var onEditCommand: (List<EditCommand>) -> Unit = {}
    private var onImeActionPerformed: (ImeAction) -> Unit = {}

    // Visible for testing
    internal var state = TextFieldValue(text = "", selection = TextRange.Zero)
        private set
    private var imeOptions = ImeOptions.Default
    private var ic: RecordingInputConnection? = null
    // used for sendKeyEvent delegation
    private val baseInputConnection by lazy(LazyThreadSafetyMode.NONE) {
        BaseInputConnection(view, false)
    }

    private var focusedRect: android.graphics.Rect? = null

    /**
     * A channel that is used to send ShowKeyboard/HideKeyboard commands. Send 'true' for
     * show Keyboard and 'false' to hide keyboard.
     */
    private val showKeyboardChannel = Channel<Boolean>(Channel.CONFLATED)

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        // focusedRect is null if there is not ongoing text input session. So safe to request
        // latest focused rectangle whenever global layout has changed.
        focusedRect?.let {
            // Notice that view.requestRectangleOnScreen may modify the input Rect, we have to
            // create another Rect and then pass it.
            view.requestRectangleOnScreen(android.graphics.Rect(it))
        }
    }

    internal constructor(view: View) : this(view, InputMethodManagerImpl(view.context))

    init {
        if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.create") }

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                v?.rootView?.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener)
            }

            override fun onViewAttachedToWindow(v: View?) {
                v?.rootView?.viewTreeObserver?.addOnGlobalLayoutListener(layoutListener)
            }
        })
    }

    /**
     * Creates new input connection.
     */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (!editorHasFocus) {
            return null
        }

        outAttrs.update(imeOptions, state)

        return RecordingInputConnection(
            initState = state,
            autoCorrect = imeOptions.autoCorrect,
            eventCallback = object : InputEventCallback2 {
                override fun onEditCommands(editCommands: List<EditCommand>) {
                    onEditCommand(editCommands)
                }

                override fun onImeAction(imeAction: ImeAction) {
                    onImeActionPerformed(imeAction)
                }

                override fun onKeyEvent(event: KeyEvent) {
                    baseInputConnection.sendKeyEvent(event)
                }
            }
        ).also {
            ic = it
            if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.createInputConnection: $ic") }
        }
    }

    /**
     * Returns true if some editable component is focused.
     */
    fun isEditorFocused(): Boolean = editorHasFocus

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.startInput") }

        editorHasFocus = true
        state = value
        this.imeOptions = imeOptions
        this.onEditCommand = onEditCommand
        this.onImeActionPerformed = onImeActionPerformed

        view.post {
            restartInput()
            showSoftwareKeyboard()
        }
    }

    override fun stopInput() {
        if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.stopInput") }

        editorHasFocus = false
        onEditCommand = {}
        onImeActionPerformed = {}
        focusedRect = null

        restartInput()
        editorHasFocus = false
    }

    private fun restartInput() {
        if (DEBUG) Log.d(TAG, "$DEBUG_CLASS.restartInput")
        inputMethodManager.restartInput(view)
    }

    override fun showSoftwareKeyboard() {
        if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.showSoftwareKeyboard") }
        showKeyboardChannel.trySend(true)
    }

    override fun hideSoftwareKeyboard() {
        if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.hideSoftwareKeyboard") }
        showKeyboardChannel.trySend(false)
    }

    suspend fun keyboardVisibilityEventLoop() {
        // TODO(b/180071033): Allow for more IMPLICIT flag to be passed.
        for (showKeyboard in showKeyboardChannel) {
            // Even though we are using a conflated channel, and the producers and consumers are
            // on the same thread, there is a possibility that we have a stale value in the channel
            // because we start consuming from it before we finish producing all the values. We poll
            // to make sure that we use the most recent value.
            if (showKeyboardChannel.tryReceive().getOrNull() ?: showKeyboard) {
                if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.keyboardVisibilityEventLoop.showSoftInput") }
                inputMethodManager.showSoftInput(view)
            } else {
                if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.keyboardVisibilityEventLoop.hideSoftInput") }
                inputMethodManager.hideSoftInputFromWindow(view.windowToken)
            }
        }
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.updateState called: $oldValue -> $newValue")
        }

        this.state = newValue
        // update the latest TextFieldValue in InputConnection
        ic?.mTextFieldValue = newValue

        if (oldValue == newValue) {
            if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.updateState early return") }
            return
        }

        val restartInput = oldValue?.let {
            it.text != newValue.text ||
                // when selection is the same but composition has changed, need to reset the input.
                (it.selection == newValue.selection && it.composition != newValue.composition)
        } ?: false

        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.updateState: restart($restartInput), state: $state")
        }

        if (restartInput) {
            restartInput()
        } else {
            ic?.updateInputState(this.state, inputMethodManager, view)
        }
    }

    override fun notifyFocusedRect(rect: Rect) {
        focusedRect = android.graphics.Rect(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt()
        )

        // Requesting rectangle too early after obtaining focus may bring view into wrong place
        // probably due to transient IME inset change. We don't know the correct timing of calling
        // requestRectangleOnScreen API, so try to call this API only after the IME is ready to
        // use, i.e. InputConnection has created.
        // Even if we miss all the timing of requesting rectangle during initial text field focus,
        // focused rectangle will be requested when software keyboard has shown.
        if (ic == null) {
            focusedRect?.let {
                // Notice that view.requestRectangleOnScreen may modify the input Rect, we have to
                // create another Rect and then pass it.
                view.requestRectangleOnScreen(android.graphics.Rect(it))
            }
        }
    }
}

/**
 * Fills necessary info of EditorInfo.
 */
internal fun EditorInfo.update(imeOptions: ImeOptions, textFieldValue: TextFieldValue) {
    this.imeOptions = when (imeOptions.imeAction) {
        ImeAction.Default -> {
            if (imeOptions.singleLine) {
                // this is the last resort to enable single line
                // Android IME still show return key even if multi line is not send
                // TextView.java#onCreateInputConnection
                EditorInfo.IME_ACTION_DONE
            } else {
                EditorInfo.IME_ACTION_UNSPECIFIED
            }
        }
        ImeAction.None -> EditorInfo.IME_ACTION_NONE
        ImeAction.Go -> EditorInfo.IME_ACTION_GO
        ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
        ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
        ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
        ImeAction.Send -> EditorInfo.IME_ACTION_SEND
        ImeAction.Done -> EditorInfo.IME_ACTION_DONE
        else -> error("invalid ImeAction")
    }
    when (imeOptions.keyboardType) {
        KeyboardType.Text -> this.inputType = InputType.TYPE_CLASS_TEXT
        KeyboardType.Ascii -> {
            this.inputType = InputType.TYPE_CLASS_TEXT
            this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_FORCE_ASCII
        }
        KeyboardType.Number -> this.inputType = InputType.TYPE_CLASS_NUMBER
        KeyboardType.Phone -> this.inputType = InputType.TYPE_CLASS_PHONE
        KeyboardType.Uri ->
            this.inputType = InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
        KeyboardType.Email ->
            this.inputType =
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        KeyboardType.Password -> {
            this.inputType =
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        }
        KeyboardType.NumberPassword -> {
            this.inputType =
                InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
        }
        else -> error("Invalid Keyboard Type")
    }

    if (!imeOptions.singleLine) {
        if (hasFlag(this.inputType, InputType.TYPE_CLASS_TEXT)) {
            // TextView.java#setInputTypeSingleLine
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE

            if (imeOptions.imeAction == ImeAction.Default) {
                this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
            }
        }
    }

    if (hasFlag(this.inputType, InputType.TYPE_CLASS_TEXT)) {
        when (imeOptions.capitalization) {
            KeyboardCapitalization.Characters -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            }
            KeyboardCapitalization.Words -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }
            KeyboardCapitalization.Sentences -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            else -> {
                /* do nothing */
            }
        }

        if (imeOptions.autoCorrect) {
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        }
    }

    this.initialSelStart = textFieldValue.selection.start
    this.initialSelEnd = textFieldValue.selection.end

    EditorInfoCompat.setInitialSurroundingText(this, textFieldValue.text)

    this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
}

private fun hasFlag(bits: Int, flag: Int): Boolean = (bits and flag) == flag