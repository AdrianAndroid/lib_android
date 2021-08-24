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

package androidx.camera.integration.extensions.idlingresource;

import android.view.View;

/** Idling resource which waits for a view to be shown. */
public class WaitForViewToShow extends ViewIdlingResource {

    public WaitForViewToShow(int viewId) {
        super(viewId);
    }

    @Override
    protected boolean isViewIdle(View view) {
        return view.isShown();
    }
}
