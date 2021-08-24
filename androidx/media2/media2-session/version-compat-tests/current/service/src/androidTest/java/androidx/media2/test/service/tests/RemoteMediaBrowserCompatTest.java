/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2.test.service.tests;

import static androidx.media2.test.common.CommonConstants.MOCK_MEDIA2_LIBRARY_SERVICE;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.media2.test.service.RemoteMediaBrowserCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test {@link RemoteMediaBrowserCompat}. */
@RunWith(AndroidJUnit4.class)
public class RemoteMediaBrowserCompatTest extends MediaSessionTestBase {
    private Context mContext;
    private RemoteMediaBrowserCompat mRemoteBrowserCompat;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mRemoteBrowserCompat = new RemoteMediaBrowserCompat(mContext, MOCK_MEDIA2_LIBRARY_SERVICE);
    }

    @After
    public void cleanUp() {
        if (mRemoteBrowserCompat != null) {
            mRemoteBrowserCompat.cleanUp();
        }
    }

    @Test
    @SmallTest
    public void connect() throws Exception {
        mRemoteBrowserCompat.connect(true /* waitForConnection */);
        assertTrue(mRemoteBrowserCompat.isConnected());
    }
}
