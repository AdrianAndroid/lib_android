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

package androidx.core.os;

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.UiAutomation;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@LargeTest
@SdkSuppress(minSdkVersion = 21) // Required for UiAutomation#executeShellCommand()
public final class TraceCompatTest {

    private static final int TRACE_BUFFER_SIZE = 8192;
    private ByteArrayOutputStream mByteArrayOutputStream;

    @Before
    public void setUp() {
        mByteArrayOutputStream = new ByteArrayOutputStream();
    }

    @After
    public void stopAtrace() throws IOException {
        // Since API 23, 'async_stop' will work. On lower API levels it was broken (see aosp/157142)
        if (Build.VERSION.SDK_INT >= 23) {
            executeCommand("atrace --async_stop");
        } else {
            // Ensure tracing is not currently running by performing a short synchronous trace.
            executeCommand("atrace -t 0");
        }
    }

    @Test
    public void beginAndEndSection() throws IOException {
        startTrace();
        TraceCompat.beginSection("beginAndEndSection");
        TraceCompat.endSection();
        dumpTrace();

        assertTraceContains("tracing_mark_write:\\ B\\|.*\\|beginAndEndSection");
        assertTraceContains("tracing_mark_write:\\ E");
    }

    @Test
    public void beginAndEndSectionAsync() throws IOException {
        startTrace();
        TraceCompat.beginAsyncSection("beginAndEndSectionAsync", /*cookie=*/5099);
        TraceCompat.endAsyncSection("beginAndEndSectionAsync", /*cookie=*/5099);
        dumpTrace();

        assertTraceContains("tracing_mark_write:\\ S\\|.*\\|beginAndEndSectionAsync\\|5099");
        assertTraceContains("tracing_mark_write:\\ F\\|.*\\|beginAndEndSectionAsync\\|5099");
    }

    @Test
    public void setCounter() throws IOException {
        startTrace();
        TraceCompat.setCounter("counterName", 42);
        TraceCompat.setCounter("counterName", 47);
        TraceCompat.setCounter("counterName", 9787);
        dumpTrace();

        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|42");
        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|47");
        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|9787");
    }

    @Test
    public void isEnabledDuringTrace() throws IOException {
        startTrace();
        boolean enabled = TraceCompat.isEnabled();
        dumpTrace();

        assertThat(enabled).isTrue();
    }

    @SmallTest
    @Test
    public void isNotEnabledWhenNotTracing() {
        assertThat(TraceCompat.isEnabled()).isFalse();
    }

    private void startTrace() throws IOException {
        String processName =
                ApplicationProvider.getApplicationContext().getApplicationInfo().processName;

        // Write the "async_start" status to the byte array to ensure atrace has fully started
        // before issuing any trace commands. This will also capture any errors that occur during
        // start so they can be added to the assertion error's message.
        executeCommand(
                String.format("atrace --async_start -b %d -a %s", TRACE_BUFFER_SIZE, processName));
    }

    private void dumpTrace() throws IOException {
        // On older versions of atrace, the -b option is required when dumping the trace so the
        // trace buffer doesn't get cleared before being dumped.
        executeCommand(
                String.format("atrace --async_dump -b %d", TRACE_BUFFER_SIZE),
                mByteArrayOutputStream);
    }

    private static void executeCommand(@NonNull String command) throws IOException {
        executeCommand(command, null);
    }

    private static void executeCommand(@NonNull String command,
            @Nullable ByteArrayOutputStream outputStream) throws IOException {
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        try (ParcelFileDescriptor pfDescriptor = automation.executeShellCommand(command);
             ParcelFileDescriptor.AutoCloseInputStream inputStream =
                     new ParcelFileDescriptor.AutoCloseInputStream(
                             pfDescriptor)) {
            byte[] buffer = new byte[1024];

            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                if (outputStream != null) {
                    outputStream.write(buffer, 0, length);
                }
            }
        }
    }

    private void assertTraceContains(@NonNull String contentRegex) {
        String traceString = new String(mByteArrayOutputStream.toByteArray(), UTF_8);

        Pattern pattern = Pattern.compile(contentRegex);
        Matcher matcher = pattern.matcher(traceString);

        if (!matcher.find()) {
            throw new AssertionError(
                    String.format("Trace does not contain requested regex: %s\n%s", contentRegex,
                            traceString));
        }
    }

}

