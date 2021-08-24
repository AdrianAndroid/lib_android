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

package androidx.media2.widget;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.fragment.app.FragmentActivity;
import androidx.media2.widget.test.R;

/**
 * A minimal application for testing {@link MediaControlView}.
 */
public class MediaControlViewTestActivity extends FragmentActivity {
    private SurfaceView mSurfaceView;

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TestUtils.setKeepScreenOn(this);
        setContentView(R.layout.mediacontrolviewtest_layout);
        mSurfaceView = findViewById(R.id.surfaceview);
    }

    public SurfaceHolder getSurfaceHolder() {
        if (mSurfaceView == null) return null;
        return mSurfaceView.getHolder();
    }
}
