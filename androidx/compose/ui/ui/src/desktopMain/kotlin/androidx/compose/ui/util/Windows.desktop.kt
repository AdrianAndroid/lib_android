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

package androidx.compose.ui.util

import androidx.compose.desktop.ComposeWindow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import java.awt.Dialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.Window
import kotlin.math.roundToInt

/**
 * Ignore size updating if window is maximized or in fullscreen.
 * Otherwise we will reset maximized / fullscreen state.
 */
internal fun ComposeWindow.setSizeSafely(size: WindowSize) {
    if (placement == WindowPlacement.Floating) {
        (this as Window).setSizeSafely(size)
    }
}

/**
 * Ignore position updating if window is maximized or in fullscreen.
 * Otherwise we will reset maximized / fullscreen state.
 */
internal fun ComposeWindow.setPositionSafely(
    position: WindowPosition
) {
    if (placement == WindowPlacement.Floating) {
        (this as Window).setPositionSafely(position)
    }
}

/**
 * Limit the width and the height to a minimum of 0
 */
internal fun Window.setSizeSafely(size: WindowSize) {
    val width = size.width.value.roundToInt().coerceAtLeast(0)
    val height = size.height.value.roundToInt().coerceAtLeast(0)
    setSize(width, height)
}

internal fun Window.setPositionSafely(
    position: WindowPosition
) = when (position) {
    WindowPosition.PlatformDefault -> setLocationByPlatformSafely(true)
    is WindowPosition.Aligned -> align(position.alignment)
    is WindowPosition.Absolute -> setLocation(
        position.x.value.roundToInt(),
        position.y.value.roundToInt()
    )
}

internal fun Window.align(alignment: Alignment) {
    val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration)
    val screenBounds = graphicsConfiguration.bounds
    val size = IntSize(size.width, size.height)
    val screenSize = IntSize(screenBounds.width, screenBounds.height)
    val location = alignment.align(size, screenSize, LayoutDirection.Ltr)

    setLocation(
        screenInsets.left + location.x,
        screenInsets.top + location.y
    )
}

/**
 * We cannot call [Frame.setLocation] if window is showing - AWT will throw an
 * exception.
 * But we can call [Frame.setLocationByPlatform] if isLocationByPlatform isn't changed.
 */
internal fun Window.setLocationByPlatformSafely(isLocationByPlatform: Boolean) {
    if (this.isLocationByPlatform != isLocationByPlatform) {
        this.isLocationByPlatform = isLocationByPlatform
    }
}

/**
 * We cannot call [Frame.setUndecorated] if window is showing - AWT will throw an exception.
 * But we can call [Frame.setUndecoratedSafely] if isUndecorated isn't changed.
 */
internal fun Frame.setUndecoratedSafely(value: Boolean) {
    if (this.isUndecorated != value) {
        this.isUndecorated = value
    }
}

/**
 * We cannot change call [Dialog.setUndecorated] if window is showing - AWT will throw an exception.
 * But we can call [Dialog.setUndecoratedSafely] if isUndecorated isn't changed.
 */
internal fun Dialog.setUndecoratedSafely(value: Boolean) {
    if (this.isUndecorated != value) {
        this.isUndecorated = value
    }
}