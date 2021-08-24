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

package androidx.benchmark.macro.perfetto

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.device
import androidx.test.platform.app.InstrumentationRegistry
import org.jetbrains.annotations.TestOnly
import java.io.File

/**
 * Enables parsing perfetto traces on-device on Q+ devices.
 */
@RequiresApi(29)
internal object PerfettoTraceProcessor {
    private const val TAG = "PerfettoTraceProcessor"

    /**
     * The actual [File] path to the `trace_processor_shell`.
     *
     * Lazily copies the `trace_processor_shell` and enables parsing of the perfetto trace files.
     */
    @get:TestOnly
    val shellPath: String by lazy {
        // Checks for ABI support
        PerfettoHelper.createExecutable("trace_processor_shell")
    }

    fun getJsonMetrics(absoluteTracePath: String, metric: String): String {
        require(!absoluteTracePath.contains(" ")) {
            "Trace path must not contain spaces: $absoluteTracePath"
        }
        require(!metric.contains(" ")) {
            "Metric must not contain spaces: $metric"
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = instrumentation.device()

        val command = "$shellPath --run-metric $metric $absoluteTracePath --metrics-output=json"
        Log.d(TAG, "Executing command $command")
        val json = device.executeShellCommand(command)
            .trim() // trim to enable empty check below
        Log.d(TAG, "Trace Processor result: \n\n $json")
        if (json.isEmpty()) {
            throw IllegalStateException(
                "Empty json result from Trace Processor - " +
                    "possibly malformed command? Command: $command"
            )
        }
        return json
    }
}
