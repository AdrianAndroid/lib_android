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

package androidx.compose.ui.text

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParagraphStyleTest {

    @Test(expected = IllegalStateException::class)
    fun `negative lineHeight throws IllegalStateException`() {
        ParagraphStyle(lineHeight = (-1).sp)
    }

    @Test
    fun `merge textAlign uses other's textAlign`() {
        val style = ParagraphStyle(textAlign = TextAlign.Justify)
        val otherStyle = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isEqualTo(otherStyle.textAlign)
    }

    @Test
    fun `merge textAlign other null, return original`() {
        val style = ParagraphStyle(textAlign = TextAlign.Justify)
        val otherStyle = ParagraphStyle(textAlign = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isEqualTo(style.textAlign)
    }

    @Test
    fun `merge textAlign both null returns null`() {
        val style = ParagraphStyle(textAlign = null)
        val otherStyle = ParagraphStyle(textAlign = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isNull()
    }

    @Test
    fun `merge textDirection uses other's textDirection`() {
        val style = ParagraphStyle(textDirection = TextDirection.Rtl)
        val otherStyle = ParagraphStyle(textDirection = TextDirection.Ltr)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isEqualTo(
            otherStyle.textDirection
        )
    }

    @Test
    fun `merge textDirection other null, returns original`() {
        val style = ParagraphStyle(textDirection = TextDirection.Rtl)
        val otherStyle = ParagraphStyle(textDirection = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isEqualTo(style.textDirection)
    }

    @Test
    fun `merge textDirection both null returns null`() {
        val style = ParagraphStyle(textDirection = null)
        val otherStyle = ParagraphStyle(textDirection = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isNull()
    }

    @Test
    fun `merge lineHeight uses other's lineHeight`() {
        val style = ParagraphStyle(lineHeight = 12.sp)
        val otherStyle = ParagraphStyle(lineHeight = 20.sp)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(otherStyle.lineHeight)
    }

    @Test
    fun `merge lineHeight other unspecified, return original`() {
        val style = ParagraphStyle(lineHeight = 12.sp)
        val otherStyle = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(style.lineHeight)
    }

    @Test
    fun `merge lineHeight both unspecified returns unspecified`() {
        val style = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val otherStyle = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
    }

    @Test
    fun `merge textIndent uses other's textIndent`() {
        val style = ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp))
        val otherStyle = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isEqualTo(otherStyle.textIndent)
    }

    @Test
    fun `merge textIndent other null, return original`() {
        val style = ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp))
        val otherStyle = ParagraphStyle(textIndent = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isEqualTo(style.textIndent)
    }

    @Test
    fun `merge textIndent both null returns null`() {
        val style = ParagraphStyle(textIndent = null)
        val otherStyle = ParagraphStyle(textIndent = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isNull()
    }

    @Test
    fun `plus operator merges`() {
        val style = ParagraphStyle(
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Rtl
        ) + ParagraphStyle(
            textAlign = TextAlign.Justify,
            lineHeight = 12.sp
        )

        assertThat(style).isEqualTo(
            ParagraphStyle(
                textAlign = TextAlign.Justify, // overridden by RHS
                textDirection = TextDirection.Rtl, // from LHS,
                lineHeight = 12.sp // from RHS
            )
        )
    }

    @Test
    fun `lerp textAlign with a null, b not null and t is smaller than half`() {
        val style1 = ParagraphStyle(textAlign = null)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textAlign).isNull()
    }

    @Test
    fun `lerp textAlign with a and b are not Null and t is smaller than half`() {
        val style1 = ParagraphStyle(textAlign = TextAlign.Left)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textAlign).isEqualTo(style1.textAlign)
    }

    @Test
    fun `lerp textAlign with a and b are not Null and t is larger than half`() {
        val style1 = ParagraphStyle(textAlign = TextAlign.Left)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.textAlign).isEqualTo(style2.textAlign)
    }

    @Test
    fun `lerp textDirection with a null, b not null and t is smaller than half`() {
        val style1 = ParagraphStyle(textDirection = null)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textDirection).isNull()
    }

    @Test
    fun `lerp textDirection with a and b are not Null and t is smaller than half`() {
        val style1 = ParagraphStyle(textDirection = TextDirection.Ltr)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textDirection).isEqualTo(style1.textDirection)
    }

    @Test
    fun `lerp textDirection with a and b are not Null and t is larger than half`() {
        val style1 = ParagraphStyle(textDirection = TextDirection.Ltr)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.textDirection).isEqualTo(style2.textDirection)
    }

    @Test
    fun `lerp textIndent with a null, b not null and t is smaller than half returns null`() {
        val style1 = ParagraphStyle(textIndent = null)
        val style2 = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))
        val fraction = 0.4f

        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.textIndent).isEqualTo(
            lerp(TextIndent(), style2.textIndent!!, fraction)
        )
    }

    @Test
    fun `lerp textIndent with a and b are not Null`() {
        val style1 = ParagraphStyle(textIndent = TextIndent(firstLine = 10.sp))
        val style2 = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))
        val fraction = 0.6f
        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.textIndent).isEqualTo(
            lerp(style1.textIndent!!, style2.textIndent!!, fraction)
        )
    }

    @Test
    fun `lerp lineHeight with a and b are specified`() {
        val style1 = ParagraphStyle(lineHeight = 10.sp)
        val style2 = ParagraphStyle(lineHeight = 20.sp)
        val fraction = 0.4f

        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.lineHeight).isEqualTo(
            lerp(style1.lineHeight, style2.lineHeight, fraction)
        )
    }

    @Test
    fun `lerp lineHeight with a and b are unspecified`() {
        val style1 = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val style2 = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
    }

    @Test
    fun `lerp lineHeight with either a or b is unspecified`() {
        val style1 = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val style2 = ParagraphStyle(lineHeight = 22.sp)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)
        val anotherNewStyle = lerp(start = style1, stop = style2, fraction = 0.8f)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
        assertThat(anotherNewStyle.lineHeight).isEqualTo(22.sp)
    }
}