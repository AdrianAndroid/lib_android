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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class SettableImageProxyBundleTest {
    private static final int CAPTURE_ID_0 = 0;
    private static final int CAPTURE_ID_1 = 1;
    private static final int CAPTURE_ID_NONEXISTANT = 5;
    private static final long TIMESTAMP_0 = 10L;
    private static final long TIMESTAMP_1 = 20L;
    private final ImageInfo mImageInfo0 = new FakeImageInfo();
    private final ImageInfo mImageInfo1 = new FakeImageInfo();
    private ImageProxy mImageProxy0;
    private ImageProxy mImageProxy1;
    private List<Integer> mCaptureIdList;
    private SettableImageProxyBundle mImageProxyBundle;
    private String mTagBundleKey = "fakeTagBundleKey";
    @Before
    public void setup() {
        ((FakeImageInfo) mImageInfo0).setTimestamp(TIMESTAMP_0);
        ((FakeImageInfo) mImageInfo1).setTimestamp(TIMESTAMP_1);
        ((FakeImageInfo) mImageInfo0).setTag(mTagBundleKey, CAPTURE_ID_0);
        ((FakeImageInfo) mImageInfo1).setTag(mTagBundleKey, CAPTURE_ID_1);
        mImageProxy0 = new FakeImageProxy(mImageInfo0);
        mImageProxy1 = new FakeImageProxy(mImageInfo1);

        mCaptureIdList = new ArrayList<>();
        mCaptureIdList.add(CAPTURE_ID_0);
        mCaptureIdList.add(CAPTURE_ID_1);

        mImageProxyBundle = new SettableImageProxyBundle(mCaptureIdList, mTagBundleKey);
    }

    @Test
    public void canInvokeMatchedImageProxyFuture() throws InterruptedException,
            ExecutionException, TimeoutException {

        // Inputs two ImageProxy to SettableImageProxyBundle.
        mImageProxyBundle.addImageProxy(mImageProxy0);
        mImageProxyBundle.addImageProxy(mImageProxy1);

        // Tries to get the Images for the ListenableFutures got from SettableImageProxyBundle.
        ImageProxy result0 = mImageProxyBundle.getImageProxy(CAPTURE_ID_0).get(0, TimeUnit.SECONDS);
        ImageProxy result1 = mImageProxyBundle.getImageProxy(CAPTURE_ID_1).get(0, TimeUnit.SECONDS);

        // Checks if the results match what was input.
        assertThat(result0.getImageInfo()).isSameInstanceAs(mImageInfo0);
        assertThat(result0).isSameInstanceAs(mImageProxy0);
        assertThat(result1.getImageInfo()).isSameInstanceAs(mImageInfo1);
        assertThat(result1).isSameInstanceAs(mImageProxy1);
    }

    @Test
    public void canInvokeMatchedImageProxyFutureWithMultiTag() throws InterruptedException,
            ExecutionException, TimeoutException {
        FakeImageInfo imageInfo0 = new FakeImageInfo();
        imageInfo0.setTimestamp(TIMESTAMP_0);
        FakeImageInfo imageInfo1 = new FakeImageInfo();
        imageInfo1.setTimestamp(TIMESTAMP_1);

        imageInfo0.setTag(mTagBundleKey, CAPTURE_ID_0);
        imageInfo1.setTag(mTagBundleKey, CAPTURE_ID_1);

        ImageProxy imageProxy0;
        imageProxy0 = new FakeImageProxy(imageInfo0);
        ImageProxy imageProxy1;
        imageProxy1 = new FakeImageProxy(imageInfo1);

        mImageProxyBundle.addImageProxy(imageProxy0);
        mImageProxyBundle.addImageProxy(imageProxy1);

        // Tries to get the Images for the ListenableFutures got from SettableImageProxyBundle.
        ImageProxy result0 = mImageProxyBundle.getImageProxy(CAPTURE_ID_0).get(0, TimeUnit.SECONDS);
        ImageProxy result1 = mImageProxyBundle.getImageProxy(CAPTURE_ID_1).get(0, TimeUnit.SECONDS);

        // Checks if the results match what was input.
        assertThat(result0.getImageInfo()).isSameInstanceAs(imageInfo0);
        assertThat(result0).isSameInstanceAs(imageProxy0);
        assertThat(result1.getImageInfo()).isSameInstanceAs(imageInfo1);
        assertThat(result1).isSameInstanceAs(imageProxy1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionWhenAddingImageWithInvalidCaptureId() {
        ImageInfo imageInfo = new FakeImageInfo();
        ImageProxy imageProxy = new FakeImageProxy(imageInfo);

        // Adds an ImageProxy with a capture id which doesn't exist in the initial list.
        ((FakeImageInfo) imageInfo).setTag(mTagBundleKey, CAPTURE_ID_NONEXISTANT);
        ((FakeImageProxy) imageProxy).setImageInfo(imageInfo);

        // Expects to throw exception while adding ImageProxy.
        mImageProxyBundle.addImageProxy(imageProxy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionWhenRetrievingImageWithInvalidCaptureId() throws InterruptedException,
            ExecutionException, TimeoutException {
        // Tries to get a ImageProxy with non-existed capture id. Expects to throw exception
        // while getting ImageProxy.
        mImageProxyBundle.getImageProxy(CAPTURE_ID_NONEXISTANT).get(0, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionWhenAddingImageWithInvalidCaptureIdInMultiTagCase() {
        ImageInfo imageInfo = new FakeImageInfo();
        ImageProxy imageProxy = new FakeImageProxy(imageInfo);

        // Adds an ImageProxy with a capture id which doesn't exist in the initial list.
        ((FakeImageInfo) imageInfo).setTag(mTagBundleKey,
                CAPTURE_ID_NONEXISTANT);
        ((FakeImageProxy) imageProxy).setImageInfo(imageInfo);

        // Expects to throw exception while adding ImageProxy.
        mImageProxyBundle.addImageProxy(imageProxy);
    }
}
