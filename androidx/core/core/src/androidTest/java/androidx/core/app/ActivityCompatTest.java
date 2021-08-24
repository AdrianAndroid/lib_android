/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertSame;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.Manifest;
import android.app.Activity;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;

import androidx.core.app.ActivityCompat.PermissionCompatDelegate;
import androidx.core.test.R;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ActivityCompatTest extends BaseInstrumentationTestCase<TestActivity> {

    public ActivityCompatTest() {
        super(TestActivity.class);
    }

    private Activity getActivity() {
        return mActivityTestRule.getActivity();
    }

    @Test
    public void testPermissionDelegate() {
        try (ActivityScenario<TestActivity> scenario =
                     ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(new ActivityScenario.ActivityAction<TestActivity>() {
                @Override
                public void perform(TestActivity activity) {
                    ActivityCompat.PermissionCompatDelegate delegate =
                            mock(PermissionCompatDelegate.class);

                    // First test setting the delegate
                    ActivityCompat.setPermissionCompatDelegate(delegate);

                    ActivityCompat.requestPermissions(activity, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION}, 42);
                    verify(delegate).requestPermissions(same(activity), aryEq(
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}), eq(42));

                    // Test clearing the delegate
                    ActivityCompat.setPermissionCompatDelegate(null);

                    ActivityCompat.requestPermissions(activity, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION}, 42);
                    verifyNoMoreInteractions(delegate);
                }
            });
        }
    }

    @Test
    public void testPermissionNull() {
        try (ActivityScenario<TestActivity> scenario =
                     ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(new ActivityScenario.ActivityAction<TestActivity>() {
                @Override
                public void perform(TestActivity activity) {
                    String[] permissions = new String[]{null};

                    try {
                        ActivityCompat.requestPermissions(activity, permissions, 42);
                    } catch (IllegalArgumentException e) {
                        assertThat(e).hasMessageThat().contains("Permission request for "
                                + "permissions " + Arrays.toString(permissions) + " must not "
                                + "contain null or empty values");
                    }
                }
            });
        }
    }

    @Test
    public void testPermissionEmpty() {
        try (ActivityScenario<TestActivity> scenario =
                     ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(new ActivityScenario.ActivityAction<TestActivity>() {
                @Override
                public void perform(TestActivity activity) {
                    String[] permissions = new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION, ""
                    };

                    try {
                        ActivityCompat.requestPermissions(activity, permissions, 42);
                    } catch (IllegalArgumentException e) {
                        assertThat(e).hasMessageThat().contains("Permission request for "
                                + "permissions " + Arrays.toString(permissions) + " must not "
                                + "contain null or empty values");
                    }
                }
            });
        }
    }

    @Test
    public void testRequireViewByIdFound() {
        View view = getActivity().findViewById(R.id.view);
        assertSame(view, ActivityCompat.requireViewById(getActivity(), R.id.view));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireViewByIdMissing() {
        // container isn't present inside activity
        ActivityCompat.requireViewById(getActivity(), R.id.container);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireViewByIdInvalid() {
        // NO_ID is always invalid
        ActivityCompat.requireViewById(getActivity(), View.NO_ID);
    }

}
