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

package androidx.wear.watchface

import android.content.Intent
import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BroadcastsObserver(
    private val watchState: WatchState,
    private val watchFaceHostApi: WatchFaceHostApi,
    private val deferredWatchFaceImpl: Deferred<WatchFaceImpl>,
    private val uiThreadCoroutineScope: CoroutineScope
) : BroadcastsReceiver.BroadcastEventObserver {

    override fun onActionTimeTick() {
        if (!watchState.isAmbient.value) {
            watchFaceHostApi.invalidate()
        }
    }

    override fun onActionTimeZoneChanged() {
        uiThreadCoroutineScope.launch {
            deferredWatchFaceImpl.await().onActionTimeZoneChanged()
        }
    }

    override fun onActionTimeChanged() {
        uiThreadCoroutineScope.launch {
            deferredWatchFaceImpl.await().onActionTimeChanged()
        }
    }

    override fun onActionBatteryLow() {
        updateBatteryLowAndNotChargingStatus(true)
    }

    override fun onActionBatteryOkay() {
        updateBatteryLowAndNotChargingStatus(false)
    }

    override fun onActionPowerConnected() {
        updateBatteryLowAndNotChargingStatus(false)
    }

    override fun onMockTime(intent: Intent) {
        uiThreadCoroutineScope.launch {
            deferredWatchFaceImpl.await().onMockTime(intent)
        }
    }

    private fun updateBatteryLowAndNotChargingStatus(value: Boolean) {
        val isBatteryLowAndNotCharging =
            watchState.isBatteryLowAndNotCharging as ObservableWatchData.MutableObservableWatchData
        if (!isBatteryLowAndNotCharging.hasValue() ||
            value != isBatteryLowAndNotCharging.value
        ) {
            isBatteryLowAndNotCharging.value = value
            watchFaceHostApi.invalidate()
        }
    }
}