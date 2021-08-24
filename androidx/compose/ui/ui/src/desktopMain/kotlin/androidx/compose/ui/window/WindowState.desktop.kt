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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param placement the initial value for [WindowState.placement]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]
 */
@Composable
fun rememberWindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: WindowSize = WindowSize(800.dp, 600.dp),
): WindowState = rememberSaveable(saver = WindowStateImpl.Saver(position)) {
    WindowStateImpl(
        placement,
        isMinimized,
        position,
        size
    )
}

/**
 * A state object that can be hoisted to control and observe window attributes
 * (size/position/state).
 *
 * In most cases, this will be created via [rememberWindowState].
 *
 * @param placement the initial value for [WindowState.placement]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]
 */
fun WindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: WindowSize = WindowSize(800.dp, 600.dp)
): WindowState = WindowStateImpl(
    placement, isMinimized, position, size
)

/**
 * A state object that can be hoisted to control and observe window attributes
 * (size/position/state).
 *
 * In most cases, this will be created via [rememberWindowState].
 */
interface WindowState {
    /**
     * Describes how the window is placed on the screen.
     */
    var placement: WindowPlacement

    /**
     * `true` if the window is minimized.
     */
    var isMinimized: Boolean

    /**
     * Current position of the window. If position is not specified ([WindowPosition.isSpecified]
     * is false) then once the window shows on the screen the position will be set to
     * absolute values [WindowPosition.Absolute].
     */
    var position: WindowPosition

    /**
     * Current size of the window.
     */
    var size: WindowSize
}

private class WindowStateImpl(
    placement: WindowPlacement,
    isMinimized: Boolean,
    position: WindowPosition,
    size: WindowSize
) : WindowState {
    override var placement by mutableStateOf(placement)
    override var isMinimized by mutableStateOf(isMinimized)
    override var position by mutableStateOf(position)
    override var size by mutableStateOf(size)

    companion object {
        /**
         * The default [Saver] implementation for [WindowStateImpl].
         */
        fun Saver(unspecifiedPosition: WindowPosition) = listSaver<WindowState, Any>(
            save = {
                listOf(
                    it.placement.ordinal,
                    it.isMinimized,
                    it.position.isSpecified,
                    it.position.x.value,
                    it.position.y.value,
                    it.size.width.value,
                    it.size.height.value,
                )
            },
            restore = { state ->
                WindowStateImpl(
                    placement = WindowPlacement.values()[state[0] as Int],
                    isMinimized = state[1] as Boolean,
                    position = if (state[2] as Boolean) {
                        WindowPosition((state[3] as Float).dp, (state[4] as Float).dp)
                    } else {
                        unspecifiedPosition
                    },
                    size = WindowSize((state[5] as Float).dp, (state[6] as Float).dp),
                )
            }
        )
    }
}