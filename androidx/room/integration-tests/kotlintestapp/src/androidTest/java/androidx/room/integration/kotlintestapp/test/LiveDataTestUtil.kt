/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.test

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object LiveDataTestUtil {

    @Throws(InterruptedException::class)
    fun <T> awaitValue(liveData: LiveData<T>): T {
        val latch = CountDownLatch(1)
        var data: T? = null
        val observer = object : Observer<T> {
            override fun onChanged(o: T?) {
                data = o
                liveData.removeObserver(this)
                latch.countDown()
            }
        }
        ArchTaskExecutor.getMainThreadExecutor().execute {
            liveData.observeForever(observer)
        }
        latch.await(10, TimeUnit.SECONDS)
        return data!!
    }
}
