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

package androidx.media2.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link UriUtil}.
 */
@RunWith(AndroidJUnit4.class)
public class UriUtilTest {
    @Test
    @SmallTest
    public void isFromNetwork() {
        assertTrue(UriUtil.isFromNetwork(Uri.parse("http://localhost/video.mp4")));
        assertTrue(UriUtil.isFromNetwork(Uri.parse("https://localhost/video.mp4")));
        assertTrue(UriUtil.isFromNetwork(Uri.parse("rtsp://localhost/video.mp4")));
        assertFalse(UriUtil.isFromNetwork(Uri.parse("file:///video.mp4")));
    }
}
