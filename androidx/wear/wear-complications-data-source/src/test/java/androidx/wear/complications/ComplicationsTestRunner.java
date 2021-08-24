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

package androidx.wear.complications;

import androidx.annotation.NonNull;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;

/** Needed to prevent Robolectric from instrumenting various classes. */
public class ComplicationsTestRunner extends RobolectricTestRunner {
    public ComplicationsTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @NonNull
    @Override
    protected InstrumentationConfiguration createClassLoaderConfig(FrameworkMethod method) {
        return new InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
                .doNotInstrumentPackage("android.support.wearable.complications")
                .doNotInstrumentPackage("android.support.wearable.watchface")
                .doNotInstrumentPackage("androidx.wear.complications")
                .doNotInstrumentPackage("androidx.wear.watchface")
                .build();
    }
}
