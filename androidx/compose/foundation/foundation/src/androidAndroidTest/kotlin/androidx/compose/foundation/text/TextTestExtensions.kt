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
package androidx.compose.foundation.text

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.test.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.font.toFontFamily
import androidx.core.content.res.ResourcesCompat
import kotlin.math.ceil
import kotlin.math.roundToInt

fun Float.toIntPx(): Int = ceil(this).roundToInt()

internal class TestFontResourceLoader(val context: Context) : Font.ResourceLoader {
    override fun load(font: Font): Typeface {
        return when (font) {
            is ResourceFont -> ResourcesCompat.getFont(context, font.resId)!!
            else -> throw IllegalArgumentException("Unknown font type: $font")
        }
    }
}

val TEST_FONT = Font(
    resId = R.font.sample_font,
    weight = FontWeight.Normal,
    style = FontStyle.Normal
)

val TEST_FONT_FAMILY = TEST_FONT.toFontFamily()