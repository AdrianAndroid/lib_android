/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2.widget

import android.widget.TextView
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.SwipeToLocation.flingToCenter
import androidx.viewpager2.widget.BaseTest.Context.SwipeMethod
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Tests what happens when a smooth scroll is interrupted by a drag
 */
@RunWith(Parameterized::class)
@LargeTest
class DragWhileSmoothScrollTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        val title: String,
        @ViewPager2.Orientation val orientation: Int,
        val startPage: Int = 0,
        val targetPage: Int,
        val dragInOppositeDirection: Boolean,
        val distanceToTargetWhenStartDrag: Float,
        val endInSnappedPosition: Boolean = false
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    private lateinit var test: Context

    @Test
    fun test() {
        // given
        assertThat(config.distanceToTargetWhenStartDrag, greaterThan(0f))
        val pageCount = max(config.startPage, config.targetPage) + 1
        test = setUpTest(config.orientation)
        test.setAdapterSync(viewAdapterProvider.provider(stringSequence(pageCount)))
        test.viewPager.setCurrentItemSync(config.startPage, false, 2, SECONDS)

        var recorder = test.viewPager.addNewRecordingCallback()
        val movingForward = config.targetPage > config.startPage

        tryNTimes(
            3,
            resetBlock = {
                test.resetViewPagerTo(config.startPage)
                test.viewPager.unregisterOnPageChangeCallback(recorder)
                recorder = test.viewPager.addNewRecordingCallback()
            }
        ) {
            // when we are close enough
            val waitTillCloseEnough = test.viewPager.addWaitForDistanceToTarget(
                config.targetPage,
                config.distanceToTargetWhenStartDrag
            )
            test.runOnUiThreadSync { test.viewPager.setCurrentItem(config.targetPage, true) }
            waitTillCloseEnough.await(2, SECONDS)

            // then perform a swipe
            val idleLatch = test.viewPager.addWaitForIdleLatch()
            if (config.endInSnappedPosition) {
                swipeExactlyToPage(config.pageToSnapTo(movingForward))
            } else if (config.dragInOppositeDirection == movingForward) {
                test.swipeBackward(SwipeMethod.MANUAL)
            } else {
                test.swipeForward(SwipeMethod.MANUAL)
            }
            idleLatch.await(2, SECONDS)

            if (!recorder.wasSettleInterrupted) {
                throw RetryException("Settling phase of first swipe was not interrupted in time")
            }
        }

        // and check the result
        recorder.apply {
            assertThat(
                "Unexpected sequence of state changes:" + dumpEvents(),
                stateEvents.map { it.state },
                equalTo(
                    if (expectIdleAfterDrag()) {
                        listOf(
                            SCROLL_STATE_SETTLING,
                            SCROLL_STATE_DRAGGING,
                            SCROLL_STATE_IDLE
                        )
                    } else {
                        listOf(
                            SCROLL_STATE_SETTLING,
                            SCROLL_STATE_DRAGGING,
                            SCROLL_STATE_SETTLING,
                            SCROLL_STATE_IDLE
                        )
                    }
                )
            )

            val currentlyVisible = test.viewPager.currentCompletelyVisibleItem
            if (currentlyVisible == config.targetPage) {
                // drag coincidentally landed us on the targetPage,
                // this slightly changes the assertions
                assertThat(
                    "viewPager.getCurrentItem() should be ${config.targetPage}",
                    test.viewPager.currentItem, equalTo(config.targetPage)
                )
                assertThat(
                    "Exactly 1 onPageSelected event should be fired",
                    selectEvents.size, equalTo(1)
                )
                assertThat(
                    "onPageSelected event should have reported ${config.targetPage}",
                    selectEvents.first().position, equalTo(config.targetPage)
                )
            } else {
                assertThat(
                    "viewPager.getCurrentItem() should not be ${config.targetPage}",
                    test.viewPager.currentItem, not(equalTo(config.targetPage))
                )
                assertThat(
                    "Exactly 2 onPageSelected events should be fired",
                    selectEvents.size, equalTo(2)
                )
                assertThat(
                    "First onPageSelected event should have reported ${config.targetPage}",
                    selectEvents.first().position, equalTo(config.targetPage)
                )
                assertThat(
                    "Second onPageSelected event should have reported " +
                        "$currentlyVisible, or visible page should be " +
                        "${selectEvents.last().position}",
                    selectEvents.last().position, equalTo(currentlyVisible)
                )
            }
        }
    }

    /**
     * Swipe to the next page, but don't stop the swipe until the [pageToSnapTo] is in snapped
     * position.
     *
     * @param pageToSnapTo The page to swipe and snap to
     */
    private fun swipeExactlyToPage(pageToSnapTo: Int) {
        // Don't let Espresso perform the fling, it can be delayed until after the smooth scroll
        val fling = flingToCenter()

        // Find the view on the UI thread, as RV may be in layout
        val pageText = "$pageToSnapTo"
        var viewFound = false
        test.runOnUiThreadSync {
            val llm = test.viewPager.linearLayoutManager
            var i = 0
            while (!viewFound && i < llm.childCount) {
                val view = llm.getChildAt(i++) as TextView
                if (view.text == pageText) {
                    viewFound = true
                    // Resolve start and end coordinates immediately, before
                    // RV gets the chance to detach the view from its parent
                    fling.initialize(view)
                }
            }
        }

        // Perform the fling
        if (viewFound) {
            fling.perform(InstrumentationRegistry.getInstrumentation())
        } else {
            fail("Page with text \"$pageText\" not found")
        }
    }

    private fun ViewPager2.addNewRecordingCallback(): RecordingCallback {
        return RecordingCallback().also { registerOnPageChangeCallback(it) }
    }

    private fun TestConfig.pageToSnapTo(movingForward: Boolean): Int {
        val positionToStartDragging = if (movingForward) {
            targetPage - distanceToTargetWhenStartDrag
        } else {
            targetPage + distanceToTargetWhenStartDrag
        }
        return if (movingForward == dragInOppositeDirection) {
            floor(positionToStartDragging).toInt()
        } else {
            ceil(positionToStartDragging).toInt()
        }
    }

    private sealed class Event {
        data class OnPageScrolledEvent(
            val position: Int,
            val positionOffset: Float,
            val positionOffsetPixels: Int
        ) : Event()
        data class OnPageSelectedEvent(val position: Int) : Event()
        data class OnPageScrollStateChangedEvent(val state: Int) : Event()
    }

    private class RecordingCallback : ViewPager2.OnPageChangeCallback() {
        private val events = mutableListOf<Event>()

        val stateEvents get() = eventsCopy.mapNotNull { it as? OnPageScrollStateChangedEvent }
        val selectEvents get() = eventsCopy.mapNotNull { it as? OnPageSelectedEvent }

        private fun addEvent(e: Event) {
            synchronized(events) {
                events.add(e)
            }
        }

        private val eventsCopy: List<Event>
            get() = synchronized(events) {
                return mutableListOf<Event>().apply {
                    addAll(events)
                }
            }

        val wasSettleInterrupted: Boolean
            get() {
                val changeToSettlingEvent = OnPageScrollStateChangedEvent(SCROLL_STATE_SETTLING)
                val lastScrollEvent = eventsCopy
                    .dropWhile { it != changeToSettlingEvent }
                    .dropWhile { it !is OnPageScrolledEvent }
                    .takeWhile { it is OnPageScrolledEvent }
                    .lastOrNull() as? OnPageScrolledEvent
                return lastScrollEvent?.let { it.positionOffsetPixels != 0 } ?: false
            }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            addEvent(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
        }

        override fun onPageSelected(position: Int) {
            addEvent(OnPageSelectedEvent(position))
        }

        override fun onPageScrollStateChanged(state: Int) {
            addEvent(OnPageScrollStateChangedEvent(state))
        }

        fun expectIdleAfterDrag(): Boolean {
            val lastScrollEvent = eventsCopy
                .dropWhile { it != OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING) }.drop(1)
                .takeWhile { it is OnPageScrolledEvent }
                .lastOrNull() as? OnPageScrolledEvent
            return lastScrollEvent?.let { it.positionOffsetPixels == 0 } ?: false
        }

        fun dumpEvents(): String {
            return eventsCopy.joinToString("\n- ", "\n(${scrollStateGlossary()})\n- ")
        }
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(true, false).flatMap { dragInOppositeDirection ->
            listOf(0.4f, 1.5f).flatMap { distanceToTarget ->
                listOf(true, false).flatMap { endInSnappedPosition ->
                    listOf(
                        TestConfig(
                            title = "forward",
                            orientation = orientation,
                            startPage = 0,
                            targetPage = 4,
                            dragInOppositeDirection = dragInOppositeDirection,
                            distanceToTargetWhenStartDrag = distanceToTarget,
                            endInSnappedPosition = endInSnappedPosition
                        ),
                        TestConfig(
                            title = "backward",
                            orientation = orientation,
                            startPage = 8,
                            targetPage = 4,
                            dragInOppositeDirection = dragInOppositeDirection,
                            distanceToTargetWhenStartDrag = distanceToTarget,
                            endInSnappedPosition = endInSnappedPosition
                        )
                    )
                }
            }
        }.plus(
            listOf(
                TestConfig(
                    title = "drag back to start",
                    orientation = orientation,
                    startPage = 0,
                    targetPage = 1,
                    dragInOppositeDirection = true,
                    distanceToTargetWhenStartDrag = .7f
                )
            )
        )
    }
}

// endregion
