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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangedIgnoreConsumed
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.sign

/**
 * Waits for drag motion to pass [touch slop][ViewConfiguration.touchSlop], using [pointerId] as
 * the pointer to examine. If [pointerId] is raised, another pointer from those that are down
 * will be chosen to lead the gesture, and if none are down, `null` is returned. If [pointerId]
 * is not down when [awaitTouchSlopOrCancellation] is called, then `null` is returned.

 * [onTouchSlopReached] is called after [ViewConfiguration.touchSlop] motion in the any direction
 * with the change that caused the motion beyond touch slop and the [Offset] beyond touch slop that
 * has passed. [onTouchSlopReached] should consume the position change if it accepts the motion.
 * If it does, then the method returns that [PointerInputChange]. If not, touch slop detection will
 * continue.
 *
 * @return The [PointerInputChange] that was consumed in [onTouchSlopReached] or `null` if all
 * pointers are raised before touch slop is detected or another gesture consumed the position
 * change.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.AwaitDragOrCancellationSample
 *
 * @see awaitHorizontalTouchSlopOrCancellation
 * @see awaitVerticalTouchSlopOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitTouchSlopOrCancellation(
    pointerId: PointerId,
    onTouchSlopReached: (change: PointerInputChange, overSlop: Offset) -> Unit
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    var offset = Offset.Zero
    val touchSlop = viewConfiguration.touchSlop

    var pointer = pointerId

    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer }!!
        if (dragEvent.positionChangeConsumed()) {
            return null
        } else if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return null
            } else {
                pointer = otherDown.id
            }
        } else {
            offset += dragEvent.positionChange()
            val distance = offset.getDistance()
            var acceptedDrag = false
            if (distance >= touchSlop) {
                val touchSlopOffset = offset / distance * touchSlop
                onTouchSlopReached(dragEvent, offset - touchSlopOffset)
                if (dragEvent.positionChangeConsumed()) {
                    acceptedDrag = true
                } else {
                    offset = Offset.Zero
                }
            }

            if (acceptedDrag) {
                return dragEvent
            } else {
                awaitPointerEvent(PointerEventPass.Final)
                if (dragEvent.positionChangeConsumed()) {
                    return null
                }
            }
        }
    }
}

/**
 * Reads position change events for [pointerId] and calls [onDrag] for every change in
 * position. If [pointerId] is raised, a new pointer is chosen from those that are down and if
 * none exist, the method returns. This does not wait for touch slop.
 *
 * @return `true` if the drag completed normally or `false` if the drag motion was
 * canceled by another gesture detector consuming position change events.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.DragSample
 *
 * @see awaitTouchSlopOrCancellation
 * @see awaitDragOrCancellation
 * @see horizontalDrag
 * @see verticalDrag
 */
suspend fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): Boolean {
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrCancellation(pointer) ?: return false

        if (change.changedToUpIgnoreConsumed()) {
            return true
        }

        onDrag(change)
        pointer = change.id
    }
}

/**
 * Reads pointer input events until a drag is detected or all pointers are up. When the  final
 * pointer is raised, the up event is returned. When a drag event is detected, the
 * drag change will be returned. Note that if [pointerId] has been raised, another pointer
 * that is down will be used, if available, so the returned [PointerInputChange.id] may
 * differ from [pointerId]. If the position change in the any direction has been
 * consumed by the [PointerEventPass.Main] pass, then the drag is considered canceled and `null`
 * is returned.  If [pointerId] is not down when [awaitDragOrCancellation] is called, then
 * `null` is returned.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.AwaitDragOrCancellationSample
 *
 * @see awaitVerticalDragOrCancellation
 * @see awaitHorizontalDragOrCancellation
 * @see drag
 */
suspend fun AwaitPointerEventScope.awaitDragOrCancellation(
    pointerId: PointerId,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val change = awaitDragOrUp(pointerId) { it.positionChangedIgnoreConsumed() }
    return if (change.positionChangeConsumed()) null else change
}

/**
 * Gesture detector that waits for pointer down and touch slop in any direction and then
 * calls [onDrag] for each drag event. It follows the touch slop detection of
 * [awaitTouchSlopOrCancellation] but will consume the position change automatically
 * once the touch slop has been crossed. [onDragStart] will be called when touch slop in passed
 * with the last known pointer position provided. [onDragEnd] is called after all pointers are up
 * and [onDragCancel] is called if another gesture has consumed pointer input, canceling this
 * gesture.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.DetectDragGesturesSample
 *
 * @see detectVerticalDragGestures
 * @see detectHorizontalDragGestures
 * @see detectDragGesturesAfterLongPress to detect gestures after long press
 */
suspend fun PointerInputScope.detectDragGestures(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false)
            var drag: PointerInputChange?
            var overSlop = Offset.Zero
            do {
                drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                    change.consumePositionChange()
                    overSlop = over
                }
            } while (drag != null && !drag.positionChangeConsumed())
            if (drag != null) {
                onDragStart.invoke(drag.position)
                onDrag(drag, overSlop)
                if (
                    !drag(drag.id) {
                        onDrag(it, it.positionChange())
                        it.consumePositionChange()
                    }
                ) {
                    onDragCancel()
                } else {
                    onDragEnd()
                }
            }
        }
    }
}

/**
 * Gesture detector that waits for pointer down and long press, after which it calls [onDrag] for
 * each drag event. [onDragStart] will be called when long press in detected with the last known
 * pointer position provided. [onDragEnd] is called after all pointers are up and [onDragCancel] is
 * called if another gesture has consumed pointer input, canceling this gesture. This function
 * will automatically consume all the position change after the long press.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.DetectDragWithLongPressGesturesSample
 *
 * @see detectVerticalDragGestures
 * @see detectHorizontalDragGestures
 * @see detectDragGestures
 */
suspend fun PointerInputScope.detectDragGesturesAfterLongPress(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    forEachGesture {
        val down = awaitPointerEventScope {
            awaitFirstDown(requireUnconsumed = false)
        }
        try {
            val drag = awaitLongPressOrCancellation(down)
            if (drag != null) {
                onDragStart.invoke(drag.position)

                awaitPointerEventScope {
                    if (
                        drag(drag.id) {
                            onDrag(it, it.positionChange())
                            it.consumePositionChange()
                        }
                    ) {
                        // consume up if we quit drag gracefully with the up
                        currentEvent.changes.fastForEach {
                            if (it.changedToUp()) {
                                it.consumeDownChange()
                            }
                        }
                        onDragEnd()
                    } else {
                        onDragCancel()
                    }
                }
            }
        } catch (c: CancellationException) {
            onDragCancel()
            throw c
        }
    }
}

/**
 * Waits for vertical drag motion to pass [touch slop][ViewConfiguration.touchSlop], using
 * [pointerId] as the pointer to examine. If [pointerId] is raised, another pointer from
 * those that are down will be chosen to lead the gesture, and if none are down, `null` is returned.
 * If [pointerId] is not down when [awaitVerticalTouchSlopOrCancellation] is called, then `null`
 * is returned.
 *
 * [onTouchSlopReached] is called after [ViewConfiguration.touchSlop] motion in the vertical
 * direction with the change that caused the motion beyond touch slop and the pixels beyond touch
 * slop. [onTouchSlopReached] should consume the position change if it accepts the motion.
 * If it does, then the method returns that [PointerInputChange]. If not, touch slop detection will
 * continue.
 *
 * @return The [PointerInputChange] that was consumed in [onTouchSlopReached] or `null` if all
 * pointers are raised before touch slop is detected or another gesture consumed the position
 * change.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.AwaitVerticalDragOrCancellationSample
 *
 * @see awaitHorizontalTouchSlopOrCancellation
 * @see awaitTouchSlopOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitVerticalTouchSlopOrCancellation(
    pointerId: PointerId,
    onTouchSlopReached: (change: PointerInputChange, overSlop: Float) -> Unit
) = awaitTouchSlopOrCancellation(
    pointerId = pointerId,
    onTouchSlopReached = onTouchSlopReached,
    getDragDirectionValue = { it.y }
)

/**
 * Reads vertical position change events for [pointerId] and calls [onDrag] for every change in
 * position. If [pointerId] is raised, a new pointer is chosen from those that are down and if
 * none exist, the method returns. This does not wait for touch slop
 *
 * @return `true` if the vertical drag completed normally or `false` if the drag motion was
 * canceled by another gesture detector consuming position change events.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.VerticalDragSample
 *
 * @see awaitVerticalTouchSlopOrCancellation
 * @see awaitVerticalDragOrCancellation
 * @see horizontalDrag
 * @see drag
 */
suspend fun AwaitPointerEventScope.verticalDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): Boolean = drag(
    pointerId = pointerId,
    onDrag = onDrag,
    motionFromChange = { it.positionChangeIgnoreConsumed().y },
    motionConsumed = { it.positionChangeConsumed() }
)

/**
 * Reads pointer input events until a vertical drag is detected or all pointers are up. When the
 * final pointer is raised, the up event is returned. When a drag event is detected, the
 * drag change will be returned. Note that if [pointerId] has been raised, another pointer
 * that is down will be used, if available, so the returned [PointerInputChange.id] may
 * differ from [pointerId]. If the position change  has been consumed by the
 * [PointerEventPass.Main] pass, then the drag is considered canceled and `null` is returned. If
 * [pointerId] is not down when [awaitVerticalDragOrCancellation] is called, then `null` is
 * returned.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.AwaitVerticalDragOrCancellationSample
 *
 * @see awaitHorizontalDragOrCancellation
 * @see awaitDragOrCancellation
 * @see verticalDrag
 */
suspend fun AwaitPointerEventScope.awaitVerticalDragOrCancellation(
    pointerId: PointerId,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val change = awaitDragOrUp(pointerId) { it.positionChangeIgnoreConsumed().y != 0f }
    return if (change.positionChangeConsumed()) null else change
}

/**
 * Gesture detector that waits for pointer down and touch slop in the vertical direction and then
 * calls [onVerticalDrag] for each vertical drag event. It follows the touch slop detection of
 * [awaitVerticalTouchSlopOrCancellation], but will consume the position change automatically
 * once the touch slop has been crossed. [onDragStart] will be called when
 * touch slop in passed with the last known pointer position provided. [onDragEnd] is called
 * after all pointers are up and [onDragCancel] is called if another gesture has consumed pointer
 * input, canceling this gesture.
 *
 * This gesture detector will coordinate with [detectHorizontalDragGestures] and
 * [awaitHorizontalTouchSlopOrCancellation] to ensure only vertical or horizontal dragging
 * is locked, but not both.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.DetectVerticalDragGesturesSample
 *
 * @see detectDragGestures
 * @see detectHorizontalDragGestures
 */
suspend fun PointerInputScope.detectVerticalDragGestures(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onVerticalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false)
            var overSlop = 0f
            val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                change.consumePositionChange()
                overSlop = over
            }
            if (drag != null) {
                onDragStart.invoke(drag.position)
                onVerticalDrag.invoke(drag, overSlop)
                if (
                    verticalDrag(drag.id) {
                        onVerticalDrag(it, it.positionChange().y)
                        it.consumePositionChange()
                    }
                ) {
                    onDragEnd()
                } else {
                    onDragCancel()
                }
            }
        }
    }
}

/**
 * Waits for horizontal drag motion to pass [touch slop][ViewConfiguration.touchSlop], using
 * [pointerId] as the pointer to examine. If [pointerId] is raised, another pointer from
 * those that are down will be chosen to lead the gesture, and if none are down, `null` is returned.

 * [onTouchSlopReached] is called after [ViewConfiguration.touchSlop] motion in the horizontal
 * direction with the change that caused the motion beyond touch slop and the pixels beyond touch
 * slop. [onTouchSlopReached] should consume the position change if it accepts the motion.
 * If it does, then the method returns that [PointerInputChange]. If not, touch slop detection will
 * continue. If [pointerId] is not down when [awaitHorizontalTouchSlopOrCancellation] is called,
 * then `null` is returned.
 *
 * @return The [PointerInputChange] that was consumed in [onTouchSlopReached] or `null` if all
 * pointers are raised before touch slop is detected or another gesture consumed the position
 * change.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.AwaitHorizontalDragOrCancellationSample
 *
 * @see awaitVerticalTouchSlopOrCancellation
 * @see awaitTouchSlopOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitHorizontalTouchSlopOrCancellation(
    pointerId: PointerId,
    onTouchSlopReached: (change: PointerInputChange, overSlop: Float) -> Unit
) = awaitTouchSlopOrCancellation(
    pointerId = pointerId,
    onTouchSlopReached = onTouchSlopReached,
    getDragDirectionValue = { it.x }
)

/**
 * Reads horizontal position change events for [pointerId] and calls [onDrag] for every change in
 * position. If [pointerId] is raised, a new pointer is chosen from those that are down and if
 * none exist, the method returns. This does not wait for touch slop.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.HorizontalDragSample
 *
 * @see awaitHorizontalTouchSlopOrCancellation
 * @see awaitDragOrCancellation
 * @see verticalDrag
 * @see drag
 */
suspend fun AwaitPointerEventScope.horizontalDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): Boolean = drag(
    pointerId = pointerId,
    onDrag = onDrag,
    motionFromChange = { it.positionChangeIgnoreConsumed().x },
    motionConsumed = { it.positionChangeConsumed() }
)

/**
 * Reads pointer input events until a horizontal drag is detected or all pointers are up. When the
 * final pointer is raised, the up event is returned. When a drag event is detected, the
 * drag change will be returned. Note that if [pointerId] has been raised, another pointer
 * that is down will be used, if available, so the returned [PointerInputChange.id] may
 * differ from [pointerId]. If the position change has been consumed by the
 * [PointerEventPass.Main] pass, then the drag is considered canceled and `null` is returned. If
 * [pointerId] is not down when [awaitHorizontalDragOrCancellation] is called, then `null` is
 * returned.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.AwaitHorizontalDragOrCancellationSample
 *
 * @see horizontalDrag
 * @see awaitVerticalDragOrCancellation
 * @see awaitDragOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitHorizontalDragOrCancellation(
    pointerId: PointerId,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val change = awaitDragOrUp(pointerId) { it.positionChangeIgnoreConsumed().x != 0f }
    return if (change.positionChangeConsumed()) null else change
}

/**
 * Gesture detector that waits for pointer down and touch slop in the horizontal direction and
 * then calls [onHorizontalDrag] for each horizontal drag event. It follows the touch slop
 * detection of [awaitHorizontalTouchSlopOrCancellation], but will consume the position change
 * automatically once the touch slop has been crossed. [onDragStart] will be called when touch
 * slop in passed with the last known pointer position provided. [onDragEnd] is called after all
 * pointers are up and [onDragCancel] is called if another gesture has consumed pointer input,
 * canceling this gesture.
 *
 * This gesture detector will coordinate with [detectVerticalDragGestures] and
 * [awaitVerticalTouchSlopOrCancellation] to ensure only vertical or horizontal dragging is locked,
 * but not both.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.DetectHorizontalDragGesturesSample
 *
 * @see detectVerticalDragGestures
 * @see detectDragGestures
 */
suspend fun PointerInputScope.detectHorizontalDragGestures(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onHorizontalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false)
            var overSlop = 0f
            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                change.consumePositionChange()
                overSlop = over
            }
            if (drag != null) {
                onDragStart.invoke(drag.position)
                onHorizontalDrag(drag, overSlop)
                if (
                    horizontalDrag(drag.id) {
                        onHorizontalDrag(it, it.positionChange().x)
                        it.consumePositionChange()
                    }
                ) {
                    onDragEnd()
                } else {
                    onDragCancel()
                }
            }
        }
    }
}

/**
 * Continues to read drag events until all pointers are up or the drag event is canceled.
 * The initial pointer to use for driving the drag is [pointerId]. [motionFromChange]
 * converts the [PointerInputChange] to the pixel change in the direction that this
 * drag should detect. [onDrag] is called whenever the pointer moves and [motionFromChange]
 * returns non-zero.
 *
 * @return `true` when the gesture ended with all pointers up and `false` when the gesture
 * was canceled.
 */
private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
    motionFromChange: (PointerInputChange) -> Float,
    motionConsumed: (PointerInputChange) -> Boolean
): Boolean {
    if (currentEvent.isPointerUp(pointerId)) {
        return false // The pointer has already been lifted, so the gesture is canceled
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) { motionFromChange(it) != 0f }

        if (motionConsumed(change)) {
            return false
        }

        if (change.changedToUpIgnoreConsumed()) {
            return true
        }

        onDrag(change)
        pointer = change.id
    }
}

/**
 * Waits for a single drag in one axis, final pointer up, or all pointers are up.
 * When [pointerId] has lifted, another pointer that is down is chosen to be the finger
 * governing the drag. When the final pointer is lifted, that [PointerInputChange] is
 * returned. When a drag is detected, that [PointerInputChange] is returned. A drag is
 * only detected when [hasDragged] returns `true`.
 */
private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
    hasDragged: (PointerInputChange) -> Boolean
): PointerInputChange {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer }!!
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else if (hasDragged(dragEvent)) {
            return dragEvent
        }
    }
}

/**
 * Waits for drag motion along one axis based on [getDragDirectionValue] to pass touch slop,
 * using [pointerId] as the pointer to examine. If [pointerId] is raised, another pointer
 * from those that are down will be chosen to lead the gesture, and if none are down,
 * `null` is returned. If [pointerId] is not down when [awaitTouchSlopOrCancellation] is called,
 * then `null` is returned.
 *
 * When touch slop is detected, [onTouchSlopReached] is called with the change and the distance
 * beyond the touch slop. [getDragDirectionValue] should return the position change in the direction
 * of the drag axis. If [onTouchSlopReached] does not consume the position change, touch slop
 * will not have been considered detected and the detection will continue or, if it is consumed,
 * the [PointerInputChange] that was consumed will be returned.
 *
 * This works with [awaitTouchSlopOrCancellation] for the other axis to ensure that only horizontal
 * or vertical dragging is done, but not both.
 *
 * @return The [PointerInputChange] of the event that was consumed in [onTouchSlopReached] or
 * `null` if all pointers are raised or the position change was consumed by another gesture
 * detector.
 */
private suspend inline fun AwaitPointerEventScope.awaitTouchSlopOrCancellation(
    pointerId: PointerId,
    onTouchSlopReached: (PointerInputChange, Float) -> Unit,
    getDragDirectionValue: (Offset) -> Float
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val touchSlop = viewConfiguration.touchSlop
    var pointer: PointerId = pointerId
    var totalPositionChange = 0f

    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer }!!
        if (dragEvent.positionChangeConsumed()) {
            return null
        } else if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return null
            } else {
                pointer = otherDown.id
            }
        } else {
            val currentPosition = dragEvent.position
            val previousPosition = dragEvent.previousPosition
            val positionChange = getDragDirectionValue(currentPosition) -
                getDragDirectionValue(previousPosition)
            totalPositionChange += positionChange

            val inDirection = abs(totalPositionChange)
            if (inDirection < touchSlop) {
                // verify that nothing else consumed the drag event
                awaitPointerEvent(PointerEventPass.Final)
                if (dragEvent.positionChangeConsumed()) {
                    return null
                }
            } else {
                onTouchSlopReached(
                    dragEvent,
                    totalPositionChange - (sign(totalPositionChange) * touchSlop)
                )
                if (dragEvent.positionChangeConsumed()) {
                    return dragEvent
                } else {
                    totalPositionChange = 0f
                }
            }
        }
    }
}

private suspend fun PointerInputScope.awaitLongPressOrCancellation(
    initialDown: PointerInputChange
): PointerInputChange? {
    var longPress: PointerInputChange? = null
    var currentDown = initialDown
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    return try {
        // wait for first tap up or long press
        withTimeout(longPressTimeout) {
            awaitPointerEventScope {
                var finished = false
                while (!finished) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
                        // All pointers are up
                        finished = true
                    }

                    if (
                        event.changes.fastAny { it.consumed.downChange || it.isOutOfBounds(size) }
                    ) {
                        finished = true // Canceled
                    }

                    // Check for cancel by position consumption. We can look on the Final pass of
                    // the existing pointer event because it comes after the Main pass we checked
                    // above.
                    val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
                    if (consumeCheck.changes.fastAny { it.positionChangeConsumed() }) {
                        finished = true
                    }
                    if (!event.isPointerUp(currentDown.id)) {
                        longPress = event.changes.fastFirstOrNull { it.id == currentDown.id }
                    } else {
                        val newPressed = event.changes.fastFirstOrNull { it.pressed }
                        if (newPressed != null) {
                            currentDown = newPressed
                            longPress = currentDown
                        } else {
                            // should technically never happen as we checked it above
                            finished = true
                        }
                    }
                }
            }
        }
        null
    } catch (_: TimeoutCancellationException) {
        longPress ?: initialDown
    }
}

private fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.fastFirstOrNull { it.id == pointerId }?.pressed != true