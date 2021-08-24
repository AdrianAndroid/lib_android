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

package androidx.compose.ui.test.inputdispatcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.testutils.expectError
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BatchingTest : InputDispatcherTest() {

    companion object {
        private const val cannotEnqueueError = "Can't enqueue event \\(.*\\), " +
            "events have already been \\(or are being\\) dispatched or disposed"
        private const val cannotSendError = "Events have already " +
            "been \\(or are being\\) dispatched or disposed"
    }

    /**
     * Tests that enqueue doesn't send, send sends and dispose doesn't send anything else
     *
     * Happy path
     */
    @Test
    fun enqueueSendDispose() {
        subject.enqueueDown(0, Offset.Zero)
        subject.enqueueMove()
        subject.enqueueMove()
        assertThat(recorder.events).isEmpty()

        subject.sendAllSynchronous()
        assertThat(recorder.events).hasSize(3)

        subject.dispose()
        assertThat(recorder.events).hasSize(3)
    }

    /**
     * Tests that enqueue doesn't send, send sends and subsequent enqueue fails
     */
    @Test
    fun enqueueSendEnqueue() {
        subject.enqueueDown(0, Offset.Zero)
        subject.enqueueMove()
        subject.enqueueMove()
        assertThat(recorder.events).isEmpty()

        subject.sendAllSynchronous()
        assertThat(recorder.events).hasSize(3)

        expectError<IllegalStateException>(expectedMessage = cannotEnqueueError) {
            subject.enqueueMove()
        }
        assertThat(recorder.events).hasSize(3)

        // Do final check to see if the failed enqueue really didn't enqueue an event
        expectError<IllegalStateException>(expectedMessage = cannotSendError) {
            subject.sendAllSynchronous()
        }
        assertThat(recorder.events).hasSize(3)
    }

    /**
     * Tests that enqueue doesn't send, send sends and subsequent send fails
     */
    @Test
    fun enqueueSendSend() {
        subject.enqueueDown(0, Offset.Zero)
        subject.enqueueMove()
        subject.enqueueMove()
        assertThat(recorder.events).isEmpty()

        subject.sendAllSynchronous()
        assertThat(recorder.events).hasSize(3)

        expectError<IllegalStateException>(expectedMessage = cannotSendError) {
            subject.sendAllSynchronous()
        }
        assertThat(recorder.events).hasSize(3)
    }

    /**
     * Tests that enqueue doesn't send, dispose doesn't send anything and subsequent enqueue fails
     */
    @Test
    fun enqueueDisposeEnqueue() {
        subject.enqueueDown(0, Offset.Zero)
        subject.enqueueMove()
        subject.enqueueMove()
        assertThat(recorder.events).isEmpty()

        subject.dispose()
        assertThat(recorder.events).isEmpty()

        expectError<IllegalStateException>(expectedMessage = cannotEnqueueError) {
            subject.enqueueMove()
        }
        assertThat(recorder.events).isEmpty()

        // Do final check to see if the failed enqueue really didn't enqueue an event
        expectError<IllegalStateException>(expectedMessage = cannotSendError) {
            subject.sendAllSynchronous()
        }
        assertThat(recorder.events).isEmpty()
    }

    /**
     * Tests that enqueue doesn't send, dispose doesn't send anything and subsequent send fails
     */
    @Test
    fun enqueueDisposeSend() {
        subject.enqueueDown(0, Offset.Zero)
        subject.enqueueMove()
        subject.enqueueMove()
        assertThat(recorder.events).isEmpty()

        subject.dispose()
        assertThat(recorder.events).isEmpty()

        expectError<IllegalStateException>(expectedMessage = cannotSendError) {
            subject.sendAllSynchronous()
        }
        assertThat(recorder.events).isEmpty()
    }

    /**
     * Tests that enqueue doesn't send, dispose doesn't send anything and subsequent dispose
     * doesn't do anything either
     */
    @Test
    fun enqueueDisposeDispose() {
        subject.enqueueDown(0, Offset.Zero)
        subject.enqueueMove()
        subject.enqueueMove()
        assertThat(recorder.events).isEmpty()

        subject.dispose()
        assertThat(recorder.events).isEmpty()

        subject.dispose()
        assertThat(recorder.events).isEmpty()
    }
}
