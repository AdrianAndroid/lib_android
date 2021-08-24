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

package androidx.ads.identifier;

import androidx.annotation.NonNull;

/**
 * Indicates an AndroidX Advertising ID is not available.
 *
 * @deprecated Use the
 * <a href="https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient">
 * Advertising ID API that's available as part of Google Play Services</a> instead of this library.
 */
@Deprecated
public class AdvertisingIdNotAvailableException extends Exception {
    public AdvertisingIdNotAvailableException(@NonNull String message) {
        super(message);
    }

    public AdvertisingIdNotAvailableException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}
