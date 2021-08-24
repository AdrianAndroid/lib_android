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

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

/**
 * A density of the screen. Used for convert [Dp] to pixels.
 *
 * @param density The logical density of the display. This is a scaling factor for the [Dp] unit.
 * @param fontScale Current user preference for the scaling factor for fonts.
 */
@Stable
fun Density(density: Float, fontScale: Float = 1f): Density =
    DensityImpl(density, fontScale)

private data class DensityImpl(
    override val density: Float,
    override val fontScale: Float
) : Density

/**
 * A density of the screen. Used for the conversions between [Dp], [Px], [Int] and [TextUnit].
 *
 * @sample androidx.compose.ui.unit.samples.WithDensitySample
 */
@Immutable
interface Density {

    /**
     * The logical density of the display. This is a scaling factor for the [Dp] unit.
     */
    @Stable
    val density: Float

    /**
     * Current user preference for the scaling factor for fonts.
     */
    @Stable
    val fontScale: Float

    /**
     * Convert [Dp] to pixels. Pixels are used to paint to Canvas.
     */
    @Stable
    fun Dp.toPx(): Float = value * density

    /**
     * Convert [Dp] to [Int] by rounding
     */
    @Stable
    fun Dp.roundToPx(): Int {
        val px = toPx()
        return if (px.isInfinite()) Constraints.Infinity else px.roundToInt()
    }

    /**
     * Convert [Dp] to Sp. Sp is used for font size, etc.
     */
    @Stable
    fun Dp.toSp(): TextUnit = (value / fontScale).sp

    /**
     * Convert Sp to pixels. Pixels are used to paint to Canvas.
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    @Stable
    fun TextUnit.toPx(): Float {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return value * fontScale * density
    }

    /**
     * Convert Sp to [Int] by rounding
     */
    @Stable
    fun TextUnit.roundToPx(): Int = toPx().roundToInt()

    /**
     * Convert Sp to [Dp].
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    @Stable
    fun TextUnit.toDp(): Dp {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return Dp(value * fontScale)
    }

    /**
     * Convert an [Int] pixel value to [Dp].
     */
    @Stable
    fun Int.toDp(): Dp = (this / density).dp

    /**
     * Convert an [Int] pixel value to Sp.
     */
    @Stable
    fun Int.toSp(): TextUnit = (this / (fontScale * density)).sp

    /** Convert a [Float] pixel value to a Dp */
    @Stable
    fun Float.toDp(): Dp = (this / density).dp

    /** Convert a [Float] pixel value to a Sp */
    @Stable
    fun Float.toSp(): TextUnit = (this / (fontScale * density)).sp

    /**
     * Convert a [DpRect] to a [Rect].
     */
    @Stable
    fun DpRect.toRect(): Rect {
        return Rect(
            left.toPx(),
            top.toPx(),
            right.toPx(),
            bottom.toPx()
        )
    }
}
