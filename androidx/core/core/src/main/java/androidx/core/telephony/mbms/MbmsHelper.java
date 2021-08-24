/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.telephony.mbms;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.telephony.mbms.ServiceInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Helper methods for working with the {@link android.telephony.mbms} cell-broadcast APIs.
 */
public final class MbmsHelper {
    // Do not instantiate
    private MbmsHelper() {}

    /**
     * Finds the best name for an eMBMS streaming or file-download service, given a
     * {@link Context} that specifies the user's preferred languages. If no language supported by
     * the {@link ServiceInfo} is preferred by the user, this method will return {@code null}.
     * If called while running on an SDK version prior to {@link Build.VERSION_CODES#P}, this method
     * will return {@code null}.
     *
     * @param context An instance of {@link Context} from your user-facing app
     * @param serviceInfo An instance of {@link android.telephony.mbms.StreamingServiceInfo} or
     * {@link android.telephony.mbms.FileServiceInfo} provided by the middleware.
     * @return The best name to display to the user for the service, or {@code null} if nothing
     * matches.
     */
    // Remove BanTargetApiAnnotation suppression once b/120625123 is addressed.
    @TargetApi(Build.VERSION_CODES.P)
    @SuppressLint("BanTargetApiAnnotation")
    @Nullable
    public static CharSequence getBestNameForService(@NonNull Context context,
            @NonNull ServiceInfo serviceInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }
        LocaleList localeList = context.getResources().getConfiguration().getLocales();

        int numLanguagesSupportedByService = serviceInfo.getNamedContentLocales().size();
        if (numLanguagesSupportedByService == 0) {
            return null;
        }
        String[] supportedLanguages = new String[numLanguagesSupportedByService];

        int i = 0;
        for (Locale l : serviceInfo.getNamedContentLocales()) {
            supportedLanguages[i] = l.toLanguageTag();
            i++;
        }

        Locale bestLocale = localeList.getFirstMatch(supportedLanguages);
        return bestLocale == null ? null : serviceInfo.getNameForLocale(bestLocale);
    }
}
