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
package androidx.window

import android.app.Activity
import androidx.core.util.Consumer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.Type.Companion.HINGE
import com.google.common.util.concurrent.MoreExecutors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor

/** Tests for [WindowBackend] class.  */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class WindowBackendTest : WindowTestBase() {

    /**
     * Verifies that [WindowManager] instance would use the assigned
     * [WindowBackend].
     */
    @Test
    public fun testFakeWindowBackend() {
        val windowLayoutInfo = newTestWindowLayout()
        val windowBackend: WindowBackend = FakeWindowBackend(windowLayoutInfo)
        activityTestRule.scenario.onActivity { activity ->
            val wm = WindowManager(activity, windowBackend)
            val layoutInfoConsumer = mock<Consumer<WindowLayoutInfo>>()
            wm.registerLayoutChangeCallback(MoreExecutors.directExecutor(), layoutInfoConsumer)
            verify(layoutInfoConsumer).accept(windowLayoutInfo)
        }
    }

    private fun newTestWindowLayout(): WindowLayoutInfo {
        val displayFeature = FoldingFeature(Bounds(10, 0, 10, 100), HINGE, FLAT)
        return WindowLayoutInfo(listOf(displayFeature))
    }

    private class FakeWindowBackend(private val windowLayoutInfo: WindowLayoutInfo) :
        WindowBackend {
        override fun registerLayoutChangeCallback(
            activity: Activity,
            executor: Executor,
            callback: Consumer<WindowLayoutInfo>
        ) {
            executor.execute { callback.accept(windowLayoutInfo) }
        }

        override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
            // Empty
        }
    }
}
