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

package androidx.startup.second_library

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.startup.first_library.WorkManagerInitializer

/**
 * A [Initializer] that depends on [WorkManagerInitializer].
 */
class DependentInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Log.i(TAG, "Created.")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(WorkManagerInitializer::class.java)

    companion object {
        private const val TAG = "DepComponentInit"
    }
}
