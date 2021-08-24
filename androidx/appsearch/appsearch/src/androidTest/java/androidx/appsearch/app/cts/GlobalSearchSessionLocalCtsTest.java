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
// @exportToFramework:skipFile()
package androidx.appsearch.app.cts;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

// TODO(b/175801531): Support this test for the platform backend once the global search API is
//  public.
public class GlobalSearchSessionLocalCtsTest extends GlobalSearchSessionCtsTestBase {
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSession(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context).setDatabaseName(dbName).build());
    }

    @Override
    protected ListenableFuture<GlobalSearchSession> createGlobalSearchSession() {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createGlobalSearchSession(
                new LocalStorage.GlobalSearchContext.Builder(context).build());
    }
}
