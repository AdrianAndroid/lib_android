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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

// TODO(shepshapard): Document.

internal fun down(
    id: Long,
    durationMillis: Long = 0L,
    x: Float = 0f,
    y: Float = 0f
): PointerInputChange =
    PointerInputChange(
        PointerId(id),
        durationMillis,
        Offset(x, y),
        true,
        durationMillis,
        Offset(x, y),
        false,
        ConsumedData()
    )

internal fun PointerInputChange.moveTo(durationMillis: Long, x: Float = 0f, y: Float = 0f) =
    copy(
        previousTime = uptimeMillis,
        previousPressed = pressed,
        previousPosition = position,
        currentTime = durationMillis,
        currentPressed = true,
        currentPosition = Offset(x, y),
        consumed = ConsumedData()
    )

internal fun PointerInputChange.moveBy(durationMillis: Long, dx: Float = 0f, dy: Float = 0f) =
    copy(
        previousTime = uptimeMillis,
        previousPressed = pressed,
        previousPosition = position,
        currentTime = uptimeMillis + durationMillis,
        currentPressed = true,
        currentPosition = Offset(position.x + dx, position.y + dy),
        consumed = ConsumedData()
    )

internal fun PointerInputChange.up(durationMillis: Long) =
    copy(
        previousTime = uptimeMillis,
        previousPressed = pressed,
        previousPosition = position,
        currentTime = durationMillis,
        currentPressed = false,
        currentPosition = position,
        consumed = ConsumedData()
    )

/**
 * A function used to react to and modify [PointerInputChange]s.
 */
internal typealias PointerInputHandler = (PointerEvent, PointerEventPass, IntSize) -> Unit

/**
 * Accepts:
 * 1. Single PointerEvent
 */
internal fun PointerInputHandler.invokeOverAllPasses(
    pointerEvent: PointerEvent,
    size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
) = invokeOverPasses(
    pointerEvent,
    listOf(
        PointerEventPass.Initial,
        PointerEventPass.Main,
        PointerEventPass.Final
    ),
    size = size
)

// TODO(shepshapard): Rename to invokeOverPass
/**
 * Accepts:
 * 1. Single PointerEvent
 * 2. Single PointerEventPass
 */
internal fun PointerInputHandler.invokeOverPass(
    pointerEvent: PointerEvent,
    pointerEventPass: PointerEventPass,
    size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
) = invokeOverPasses(pointerEvent, listOf(pointerEventPass), size)

/**
 * Accepts:
 * 1. Single PointerEvent
 * 2. vararg of PointerEventPass
 */
internal fun PointerInputHandler.invokeOverPasses(
    pointerEvent: PointerEvent,
    vararg pointerEventPasses: PointerEventPass,
    size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
) = invokeOverPasses(pointerEvent, pointerEventPasses.toList(), size)

/**
 * Accepts:
 * 1. Single PointerEvent
 * 2. List of PointerEventPass
 */
internal fun PointerInputHandler.invokeOverPasses(
    pointerEvent: PointerEvent,
    pointerEventPasses: List<PointerEventPass>,
    size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
) {
    require(pointerEvent.changes.isNotEmpty())
    require(pointerEventPasses.isNotEmpty())
    pointerEventPasses.fastForEach {
        this.invoke(pointerEvent, it, size)
    }
}
