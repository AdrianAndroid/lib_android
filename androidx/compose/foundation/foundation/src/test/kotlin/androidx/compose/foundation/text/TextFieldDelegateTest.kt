/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalFoundationTextApi::class)
@RunWith(JUnit4::class)
class TextFieldDelegateTest {

    private lateinit var canvas: Canvas
    private lateinit var mDelegate: TextDelegate
    private lateinit var processor: EditProcessor
    private lateinit var onValueChange: (TextFieldValue) -> Unit
    private lateinit var onEditorActionPerformed: (Any) -> Unit
    private lateinit var textInputService: TextInputService
    private lateinit var layoutCoordinates: LayoutCoordinates
    private lateinit var multiParagraphIntrinsics: MultiParagraphIntrinsics
    private lateinit var textLayoutResultProxy: TextLayoutResultProxy
    private lateinit var textLayoutResult: TextLayoutResult

    /**
     * Test implementation of offset map which doubles the offset in transformed text.
     */
    private val skippingOffsetMap = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = offset * 2
        override fun transformedToOriginal(offset: Int): Int = offset / 2
    }

    @Before
    fun setup() {
        mDelegate = mock()
        canvas = mock()
        processor = mock()
        onValueChange = mock()
        onEditorActionPerformed = mock()
        textInputService = mock()
        layoutCoordinates = mock()
        multiParagraphIntrinsics = mock()
        textLayoutResult = mock()
        textLayoutResultProxy = mock()
        whenever(textLayoutResultProxy.value).thenReturn(textLayoutResult)
    }

    @Test
    fun test_setCursorOffset() {
        val position = Offset(100f, 200f)
        val offset = 10
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        whenever(processor.toTextFieldValue()).thenReturn(editorState)
        whenever(textLayoutResultProxy.getOffsetForPosition(position)).thenReturn(offset)

        TextFieldDelegate.setCursorOffset(
            position,
            textLayoutResultProxy,
            processor,
            OffsetMapping.Identity,
            onValueChange
        )

        verify(onValueChange, times(1)).invoke(
            eq(TextFieldValue(text = editorState.text, selection = TextRange(offset)))
        )
    }

    @Test
    fun on_focus() {
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        val imeOptions = ImeOptions(
            singleLine = true,
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Search
        )

        val textInputSession: TextInputSession = mock()
        whenever(
            textInputService.startInput(
                eq(editorState),
                eq(imeOptions),
                any(),
                eq(onEditorActionPerformed)
            )
        ).thenReturn(textInputSession)

        val actual = TextFieldDelegate.onFocus(
            textInputService = textInputService,
            value = editorState,
            editProcessor = processor,
            imeOptions = imeOptions,
            onValueChange = onValueChange,
            onImeActionPerformed = onEditorActionPerformed
        )
        verify(textInputService).startInput(
            eq(
                TextFieldValue(
                    text = editorState.text,
                    selection = editorState.selection
                )
            ),
            eq(imeOptions),
            any(),
            eq(onEditorActionPerformed)
        )

        verify(actual).showSoftwareKeyboard()
        assertThat(actual).isEqualTo(textInputSession)
    }

    @Test
    fun on_blur_with_hiding() {
        val editorState = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(1),
            composition = TextRange(3, 5)
        )
        whenever(processor.toTextFieldValue()).thenReturn(editorState)

        val textInputSession = mock<TextInputSession>()

        TextFieldDelegate.onBlur(textInputSession, processor, onValueChange)

        inOrder(textInputSession) {
            verify(textInputSession).hideSoftwareKeyboard()
            verify(textInputSession).dispose()
        }
        verify(onValueChange, times(1)).invoke(
            eq(editorState.copy(composition = null))
        )
    }

    @Test
    fun notify_focused_rect() {
        val rect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val point = Offset(5f, 6f)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        val textInputSession: TextInputSession = mock()

        val input = TextLayoutInput(
            text = AnnotatedString(editorState.text),
            style = TextStyle(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = mock(),
            constraints = mock()
        )
        whenever(textLayoutResult.layoutInput).thenReturn(input)

        TextFieldDelegate.notifyFocusedRect(
            editorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputSession,
            true /* hasFocus */,
            OffsetMapping.Identity
        )
        verify(textInputSession).notifyFocusedRect(any())
    }

    @Test
    fun notify_focused_rect_without_focus() {
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        val textInputSession: TextInputSession = mock()
        TextFieldDelegate.notifyFocusedRect(
            editorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputSession,
            false /* hasFocus */,
            OffsetMapping.Identity
        )
        verify(textInputSession, never()).notifyFocusedRect(any())
    }

    @Test
    fun notify_rect_tail() {
        val rect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val point = Offset(5f, 6f)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(12))
        val textInputSession: TextInputSession = mock()
        val input = TextLayoutInput(
            text = AnnotatedString(editorState.text),
            style = TextStyle(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = mock(),
            constraints = mock()
        )
        whenever(textLayoutResult.layoutInput).thenReturn(input)

        TextFieldDelegate.notifyFocusedRect(
            editorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputSession,
            true /* hasFocus */,
            OffsetMapping.Identity
        )
        verify(textInputSession).notifyFocusedRect(any())
    }

    @Test
    fun check_notify_rect_uses_offset_map() {
        val rect = Rect(0f, 1f, 2f, 3f)
        val point = Offset(5f, 6f)
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1, 3))

        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val input = TextLayoutInput(
            text = AnnotatedString(editorState.text),
            style = TextStyle(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = mock(),
            constraints = mock()
        )
        whenever(textLayoutResult.layoutInput).thenReturn(input)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )
        val textInputSession: TextInputSession = mock()

        TextFieldDelegate.notifyFocusedRect(
            editorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputSession,
            true /* hasFocus */,
            skippingOffsetMap
        )
        verify(textLayoutResult).getBoundingBox(6)
        verify(textInputSession).notifyFocusedRect(any())
    }

    @Test
    fun check_setCursorOffset_uses_offset_map() {
        val position = Offset(100f, 200f)
        val offset = 10
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        whenever(processor.toTextFieldValue()).thenReturn(editorState)
        whenever(textLayoutResultProxy.getOffsetForPosition(position)).thenReturn(offset)

        TextFieldDelegate.setCursorOffset(
            position,
            textLayoutResultProxy,
            processor,
            skippingOffsetMap,
            onValueChange
        )

        verify(onValueChange, times(1)).invoke(
            eq(TextFieldValue(text = editorState.text, selection = TextRange(offset / 2)))
        )
    }

    @Test
    fun use_identity_mapping_if_none_visual_transformation() {
        val transformedText = VisualTransformation.None.filter(
            AnnotatedString(text = "Hello, World")
        )
        val visualText = transformedText.text
        val offsetMapping = transformedText.offsetMapping

        assertThat(visualText.text).isEqualTo("Hello, World")
        for (i in 0..visualText.text.length) {
            // Identity mapping returns if no visual filter is provided.
            assertThat(offsetMapping.originalToTransformed(i)).isEqualTo(i)
            assertThat(offsetMapping.transformedToOriginal(i)).isEqualTo(i)
        }
    }

    @Test
    fun apply_composition_decoration() {
        val identityOffsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        }

        val input = TransformedText(
            text = AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(color = Color.Red))
                append("Hello, World")
            }.toAnnotatedString(),
            offsetMapping = identityOffsetMapping
        )

        val result = TextFieldDelegate.applyCompositionDecoration(
            compositionRange = TextRange(3, 6),
            transformed = input
        )

        assertThat(result.text.text).isEqualTo(input.text.text)
        assertThat(result.text.spanStyles.size).isEqualTo(2)
        assertThat(result.text.spanStyles).contains(
            AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 3, 6)
        )
    }

    @Test
    fun apply_composition_decoration_with_offsetmap() {
        val offsetAmount = 5
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offsetAmount + offset
            override fun transformedToOriginal(offset: Int): Int = offset - offsetAmount
        }

        val input = TransformedText(
            text = AnnotatedString.Builder().apply {
                append(" ".repeat(offsetAmount))
                append("Hello World")
            }.toAnnotatedString(),
            offsetMapping = offsetMapping
        )

        val range = TextRange(0, 2)
        val result = TextFieldDelegate.applyCompositionDecoration(
            compositionRange = range,
            transformed = input
        )

        assertThat(result.text.spanStyles.size).isEqualTo(1)
        assertThat(result.text.spanStyles).contains(
            AnnotatedString.Range(
                SpanStyle(textDecoration = TextDecoration.Underline),
                range.start + offsetAmount,
                range.end + offsetAmount
            )
        )
    }

    @Test
    fun notify_transformed_text() {
        val rect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val point = Offset(5f, 6f)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )

        val textInputSession: TextInputSession = mock()
        val input = TextLayoutInput(
            // In this test case, transform the text into double characters text.
            text = AnnotatedString("HHeelllloo,,  WWoorrlldd"),
            style = TextStyle(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = mock(),
            constraints = mock()
        )
        whenever(textLayoutResult.layoutInput).thenReturn(input)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset * 2
            override fun transformedToOriginal(offset: Int): Int = offset / 2
        }

        // The beginning of the text.
        TextFieldDelegate.notifyFocusedRect(
            TextFieldValue(text = "Hello, World", selection = TextRange(0)),
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputSession,
            true /* hasFocus */,
            offsetMapping
        )
        verify(textInputSession).notifyFocusedRect(any())

        // The tail of the transformed text.
        reset(textInputSession)
        TextFieldDelegate.notifyFocusedRect(
            TextFieldValue(text = "Hello, World", selection = TextRange(24)),
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputSession,
            true /* hasFocus */,
            offsetMapping
        )
        verify(textInputSession).notifyFocusedRect(any())

        // Beyond the tail of the transformed text.
        reset(textInputSession)
        TextFieldDelegate.notifyFocusedRect(
            TextFieldValue(text = "Hello, World", selection = TextRange(25)),
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputSession,
            true /* hasFocus */,
            offsetMapping
        )
        verify(textInputSession).notifyFocusedRect(any())
    }

    private class MockCoordinates(
        override val size: IntSize = IntSize.Zero,
        val localOffset: Offset = Offset.Zero,
        val globalOffset: Offset = Offset.Zero,
        val rootOffset: Offset = Offset.Zero
    ) : LayoutCoordinates {
        override val providedAlignmentLines: Set<AlignmentLine>
            get() = emptySet()
        override val parentLayoutCoordinates: LayoutCoordinates?
            get() = null
        override val parentCoordinates: LayoutCoordinates?
            get() = null
        override val isAttached: Boolean
            get() = true

        override fun windowToLocal(relativeToWindow: Offset): Offset = localOffset

        override fun localToWindow(relativeToLocal: Offset): Offset = globalOffset

        override fun localToRoot(relativeToLocal: Offset): Offset = rootOffset
        override fun localPositionOf(
            sourceCoordinates: LayoutCoordinates,
            relativeToSource: Offset
        ): Offset = Offset.Zero

        override fun localBoundingBoxOf(
            sourceCoordinates: LayoutCoordinates,
            clipBounds: Boolean
        ): Rect = Rect.Zero

        override fun get(alignmentLine: AlignmentLine): Int = 0
    }
}