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

package androidx.compose.ui.text.platform

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LocaleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TestFontResourceLoader
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidAccessibilitySpannableStringTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val density = Density(context)
    private val resourceLoader = TestFontResourceLoader(context)

    @Test
    fun toAccessibilitySpannableString_with_locale() {
        val languageTag = "en-GB"
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(localeList = LocaleList(languageTag))) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            LocaleSpan::class, 5, 10
        ) {
            it.locale?.language == languageTag
            true
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_color() {
        val color = Color.Black
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(color = color)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            ForegroundColorSpan::class, 5, 10
        ) {
            it.foregroundColor == color.toArgb()
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_fontSizeInSp() {
        val fontSize = 12.sp
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontSize = fontSize)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            AbsoluteSizeSpan::class, 5, 10
        ) {
            it.size == with(density) { fontSize.roundToPx() }
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_fontSizeInEm() {
        val fontSize = 2.em
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontSize = fontSize)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            RelativeSizeSpan::class, 5, 10
        ) {
            it.sizeChange == fontSize.value
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_fontWeightBold() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            StyleSpan::class, 5, 10
        ) {
            it.style == Typeface.BOLD
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_italic() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            StyleSpan::class, 5, 10
        ) {
            it.style == Typeface.ITALIC
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_fontFamily() {
        val fontFamily = FontFamily.Monospace
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontFamily = fontFamily)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            TypefaceSpan::class, 5, 10
        ) {
            it.family == "monospace"
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_underline() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            UnderlineSpan::class, 5, 10
        )
    }

    @Test
    fun toAccessibilitySpannableString_with_lineThrough() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            StrikethroughSpan::class, 5, 10
        )
    }

    @Test
    fun toAccessibilitySpannableString_with_scaleX() {
        val scaleX = 1.2f
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(
                style = SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX = scaleX))
            ) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            ScaleXSpan::class, 5, 10
        ) {
            it.scaleX == scaleX
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_background() {
        val backgroundColor = Color.Red
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(background = backgroundColor)) {
                append("world")
            }
        }

        val spannableString =
            annotatedString.toAccessibilitySpannableString(density, resourceLoader)

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            BackgroundColorSpan::class, 5, 10
        ) {
            it.backgroundColor == backgroundColor.toArgb()
        }
    }
}