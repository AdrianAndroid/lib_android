/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.emoji2.widget;

import android.content.Context;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.viewsintegration.EmojiEditTextHelper;

/**
 * EditText widget enhanced with emoji capability by using {@link EmojiEditTextHelper}. When used
 * on devices running API 18 or below, this widget acts as a regular {@link EditText}.
 *
 * {@link androidx.emoji.R.attr#maxEmojiCount}
 */
public class EmojiEditText extends EditText {
    private EmojiEditTextHelper mEmojiEditTextHelper;

    /**
     * Prevent calling {@link #init(AttributeSet, int, int)} multiple times in case super()
     * constructors call other constructors.
     */
    private boolean mInitialized;

    public EmojiEditText(@NonNull Context context) {
        super(context);
        init(null /*attrs*/, 0 /*defStyleAttr*/, 0 /*defStyleRes*/);
    }

    public EmojiEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, android.R.attr.editTextStyle, 0 /*defStyleRes*/);
    }

    public EmojiEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0 /*defStyleRes*/);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (!mInitialized) {
            mInitialized = true;
            final EditTextAttributeHelper attrHelper = new EditTextAttributeHelper(this, attrs,
                    defStyleAttr, defStyleRes);
            setMaxEmojiCount(attrHelper.getMaxEmojiCount());
            setKeyListener(super.getKeyListener());
        }
    }

    @Override
    public void setKeyListener(@Nullable KeyListener keyListener) {
        if (keyListener != null) {
            keyListener = getEmojiEditTextHelper().getKeyListener(keyListener);
        }
        super.setKeyListener(keyListener);
    }

    @Nullable
    @Override
    public InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
        final InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        return getEmojiEditTextHelper().onCreateInputConnection(inputConnection, outAttrs);
    }

    /**
     * Set the maximum number of EmojiSpans to be added to a CharSequence. The number of spans in a
     * CharSequence affects the performance of the EditText insert/delete operations. Insert/delete
     * operations slow down as the number of spans increases.
     *
     * @param maxEmojiCount maximum number of EmojiSpans to be added to a single CharSequence,
     *                      should be equal or greater than 0
     *
     * @see EmojiCompat#process(CharSequence, int, int, int)
     *
     * {@link androidx.emoji.R.attr#maxEmojiCount}
     */
    public void setMaxEmojiCount(@IntRange(from = 0) int maxEmojiCount) {
        getEmojiEditTextHelper().setMaxEmojiCount(maxEmojiCount);
    }

    /**
     * Returns the maximum number of EmojiSpans to be added to a CharSequence.
     *
     * @see #setMaxEmojiCount(int)
     * @see EmojiCompat#process(CharSequence, int, int, int)
     *
     * {@link androidx.emoji.R.attr#maxEmojiCount}
     */
    public int getMaxEmojiCount() {
        return getEmojiEditTextHelper().getMaxEmojiCount();
    }

    private EmojiEditTextHelper getEmojiEditTextHelper() {
        if (mEmojiEditTextHelper == null) {
            mEmojiEditTextHelper = new EmojiEditTextHelper(this);
        }
        return mEmojiEditTextHelper;
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(
            @NonNull ActionMode.Callback actionModeCallback
    ) {
        super.setCustomSelectionActionModeCallback(TextViewCompat
                .wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }
}
