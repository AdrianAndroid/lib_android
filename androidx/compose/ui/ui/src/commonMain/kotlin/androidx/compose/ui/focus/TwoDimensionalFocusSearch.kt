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

package androidx.compose.ui.focus

import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.util.fastForEach
import kotlin.math.absoluteValue
import kotlin.math.max

private const val invalidFocusDirection = "This function should only be used for 2-D focus search"
private const val noActiveChild = "ActiveParent must have a focusedChild"

/**
 *  Perform a search among the immediate children of this [node][ModifiedFocusNode] in the
 *  specified [direction][FocusDirection] and return the node that is to be focused next. If one
 *  of the children is currently focused, we start from that point and search in the specified
 *  [direction][FocusDirection]. If none of the children are currently focused, we pick the
 *  top-left or bottom right based on the specified [direction][FocusDirection].
 */
internal fun ModifiedFocusNode.twoDimensionalFocusSearch(
    direction: FocusDirection
): ModifiedFocusNode? {
    return when (focusState) {
        Inactive -> this
        Disabled -> null
        ActiveParent -> {
            // If the focusedChild is an intermediate parent, we continue searching among it's
            // children, and return a focus node if we find one.
            val focusedChild = focusedChild ?: error(noActiveChild)
            if (focusedChild.focusState == ActiveParent) {
                focusedChild.twoDimensionalFocusSearch(direction)?.let { return it }
            }

            // Use the focus rect of the active node as the starting point and pick one of our
            // children as the next focused item.
            val activeRect = findActiveFocusNode()?.focusRect() ?: error(noActiveChild)
            focusableChildren().findBestCandidate(activeRect, direction)
        }
        Active, Captured -> {
            // The 2-D focus search starts form the root. If we reached here, it means that there
            // was no intermediate node that was ActiveParent. This is an initial focus scenario.
            // We need to search among this node's children to find the best focus candidate.
            val focusableChildren = focusableChildren()

            // If there are aren't multiple children to choose from, return the first child.
            if (focusableChildren.size <= 1) {
                return focusableChildren.firstOrNull()
            }

            // To start the search, we pick one of the four corners of this node as the initially
            // focused rectangle.
            val initialFocusRect = when (direction) {
                Right, Down -> focusRect().topLeft()
                Left, Up -> focusRect().bottomRight()
                else -> error(invalidFocusDirection)
            }
            focusableChildren.findBestCandidate(initialFocusRect, direction)
        }
    }
}

// Iterate through this list of focus nodes and find best candidate in the specified direction.
// TODO(b/182319711): For Left/Right focus moves, Consider finding the first candidate in the beam
//  and then only comparing candidates in the beam. If nothing is in the beam, then consider all
//  valid candidates.
private fun List<ModifiedFocusNode>.findBestCandidate(
    focusRect: Rect,
    direction: FocusDirection
): ModifiedFocusNode? {
    // Pick an impossible rectangle as the initial best candidate Rect.
    var bestCandidate = when (direction) {
        Left -> focusRect.translate(focusRect.width + 1, 0f)
        Right -> focusRect.translate(-(focusRect.width + 1), 0f)
        Up -> focusRect.translate(0f, focusRect.height + 1)
        Down -> focusRect.translate(0f, -(focusRect.height + 1))
        else -> error(invalidFocusDirection)
    }

    var searchResult: ModifiedFocusNode? = null
    fastForEach { candidateNode ->
        val candidateRect = candidateNode.focusRect()
        if (isBetterCandidate(candidateRect, bestCandidate, focusRect, direction)) {
            bestCandidate = candidateRect
            searchResult = candidateNode
        }
    }
    return searchResult
}

// Is this Rect a better candidate than currentCandidateRect for a focus search in a particular
// direction from a source rect? This is the core routine that determines the order of focus
// searching.
private fun isBetterCandidate(
    proposedCandidate: Rect,
    currentCandidate: Rect,
    focusedRect: Rect,
    direction: FocusDirection
): Boolean {

    // Is this Rect a candidate for the next focus given the direction? This checks whether the
    // rect is at least partially to the direction of (e.g left of) from source. Includes an edge
    // case for an empty rect (which is used in some cases when searching from a point on the
    // screen).
    fun Rect.isCandidate() = when (direction) {
        Left -> (focusedRect.right > right || focusedRect.left >= right) && focusedRect.left > left
        Right -> (focusedRect.left < left || focusedRect.right <= left) && focusedRect.right < right
        Up -> (focusedRect.bottom > bottom || focusedRect.top >= bottom) && focusedRect.top > top
        Down -> (focusedRect.top < top || focusedRect.bottom <= top) && focusedRect.bottom < bottom
        else -> error(invalidFocusDirection)
    }

    // The distance from the edge furthest in the given direction of source to the edge nearest
    // in the given direction of dest. If the dest is not in the direction from source, return 0.
    fun Rect.majorAxisDistance(): Float {
        val majorAxisDistance = when (direction) {
            Left -> focusedRect.left - right
            Right -> left - focusedRect.right
            Up -> focusedRect.top - bottom
            Down -> top - focusedRect.bottom
            else -> error(invalidFocusDirection)
        }
        return max(0.0f, majorAxisDistance)
    }

    // Find the distance on the minor axis w.r.t the direction to the nearest edge of the
    // destination rectangle.
    fun Rect.minorAxisDistance() = when (direction) {
        // the distance between the center verticals
        Left, Right -> (focusedRect.top + focusedRect.height / 2) - (top + height / 2)
        // the distance between the center horizontals
        Up, Down -> (focusedRect.left + focusedRect.width / 2) - (left + width / 2)
        else -> error(invalidFocusDirection)
    }

    // Fudge-factor opportunity: how to calculate distance given major and minor axis distances.
    // Warning: This fudge factor is finely tuned, run all focus tests if you dare tweak it.
    fun weightedDistance(candidate: Rect): Long {
        val majorAxisDistance = candidate.majorAxisDistance().absoluteValue.toLong()
        val minorAxisDistance = candidate.minorAxisDistance().absoluteValue.toLong()
        return 13 * majorAxisDistance * majorAxisDistance + minorAxisDistance * minorAxisDistance
    }

    return when {
        // to be a better candidate, need to at least be a candidate in the first place.
        !proposedCandidate.isCandidate() -> false

        // If the currentCandidate is not a candidate, proposedCandidate is better.
        !currentCandidate.isCandidate() -> true

        // if proposedCandidate is better by beam, it wins.
        beamBeats(focusedRect, proposedCandidate, currentCandidate, direction) -> true

        // if currentCandidate is better, then the proposedCandidate can't be.
        beamBeats(focusedRect, currentCandidate, proposedCandidate, direction) -> false

        else -> weightedDistance(proposedCandidate) < weightedDistance(currentCandidate)
    }
}

/**
 * A rectangle may be a better candidate by virtue of being exclusively in the beam of the source
 * rect.
 * @return Whether rect1 is a better candidate than rect2 by virtue of it being in the source's
 * beam.
 */
private fun beamBeats(
    source: Rect,
    rect1: Rect,
    rect2: Rect,
    direction: FocusDirection
): Boolean {
    // Do the "beams" w.r.t the given direction's axis of rect1 and rect2 overlap?
    fun Rect.inSourceBeam() = when (direction) {
        Left, Right -> this.bottom > source.top && this.top < source.bottom
        Up, Down -> this.right > source.left && this.left < source.right
        else -> error(invalidFocusDirection)
    }

    // Whether the rect is in the direction of search.
    fun Rect.isInDirectionOfSearch() = when (direction) {
        Left -> source.left >= right
        Right -> source.right <= left
        Up -> source.top >= bottom
        Down -> source.bottom <= top
        else -> error(invalidFocusDirection)
    }

    // The distance from the edge furthest in the given direction of source to the edge nearest
    // in the given direction of dest. If the dest is not in the direction from source, return 0.
    fun Rect.majorAxisDistance(): Float {
        val majorAxisDistance = when (direction) {
            Left -> source.left - right
            Right -> left - source.right
            Up -> source.top - bottom
            Down -> top - source.bottom
            else -> error(invalidFocusDirection)
        }
        return max(0.0f, majorAxisDistance)
    }

    // The distance along the major axis w.r.t the direction from the edge of source to the far
    // edge of dest. If the dest is not in the direction from source, return 1 (to break ties
    // with Rect.majorAxisDistance).
    fun Rect.majorAxisDistanceToFarEdge(): Float {
        val majorAxisDistance = when (direction) {
            Left -> source.left - left
            Right -> right - source.right
            Up -> source.top - top
            Down -> bottom - source.bottom
            else -> error(invalidFocusDirection)
        }
        return max(1.0f, majorAxisDistance)
    }

    return when {
        // if rect1 isn't exclusively in the src beam, it doesn't win
        rect2.inSourceBeam() || !rect1.inSourceBeam() -> false

        // We know rect1 is in the beam, and rect2 is not. If Rect2 is not in the direction of
        // search, rect1 wins (since rect2 could be reached by going in another direction).
        !rect2.isInDirectionOfSearch() -> true

        // for horizontal directions, being exclusively in beam always wins
        direction == Left || direction == Right -> true

        // for vertical directions, beams only beat up to a point:
        // now, as long as rect2 isn't completely closer, rect1 wins
        // e.g for direction down, completely closer means for rect2's top
        // edge to be closer to the source's top edge than rect1's bottom edge.
        else -> rect1.majorAxisDistance() < rect2.majorAxisDistanceToFarEdge()
    }
}

private fun Rect.topLeft() = Rect(left, top, left, top)
private fun Rect.bottomRight() = Rect(right, bottom, right, bottom)
