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

package androidx.camera.camera2.internal.compat;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper for {@link CameraCharacteristics} which caches the retrieved values to optimize
 * the latency and might contain backward compatible fixes for certain parameters.
 */
public class CameraCharacteristicsCompat {
    @NonNull
    @GuardedBy("this")
    private final Map<CameraCharacteristics.Key<?>, Object> mValuesCache = new HashMap<>();
    @NonNull
    private final CameraCharacteristics mCameraCharacteristics;

    private CameraCharacteristicsCompat(@NonNull CameraCharacteristics cameraCharacteristics) {
        mCameraCharacteristics = cameraCharacteristics;
    }

    /**
     * Tests might need to create CameraCharacteristicsCompat directly for convenience. Elsewhere
     * we should get the CameraCharacteristicsCompat instance from {@link CameraManagerCompat}.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @NonNull
    public static CameraCharacteristicsCompat toCameraCharacteristicsCompat(
            @NonNull CameraCharacteristics characteristics) {
        return new CameraCharacteristicsCompat(characteristics);
    }

    /**
     * Gets a camera characteristics field value and caches the value for later use.
     *
     * <p>It will cache the value once get() is called. If get() is called more than once using
     * the same key, it will return instantly.
     *
     * @param key The characteristics field to read.
     * @return The value of that key, or null if the field is not set.
     */
    @Nullable
    public <T> T get(@NonNull CameraCharacteristics.Key<T> key) {
        synchronized (this) {
            @SuppressWarnings("unchecked") // The value type always matches the key type.
            T value = (T) mValuesCache.get(key);
            if (value != null) {
                return value;
            }

            value = mCameraCharacteristics.get(key);
            if (value != null) {
                mValuesCache.put(key, value);
            }
            return value;
        }
    }

    /**
     * Returns the {@link CameraCharacteristics} represented by this object.
     */
    @NonNull
    public CameraCharacteristics toCameraCharacteristics() {
        return mCameraCharacteristics;
    }
}
