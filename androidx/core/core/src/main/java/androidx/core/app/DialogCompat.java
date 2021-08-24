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

package androidx.core.app;

import android.app.Dialog;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Helper for accessing features in {@link android.app.Dialog} in a backwards compatible
 * fashion.
 */

public class DialogCompat {

    private DialogCompat() {
    }

    /**
     * Finds the first descendant view with the given ID or throws an IllegalArgumentException if
     * the ID is invalid (< 0), there is no matching view in the hierarchy, or the dialog has not
     * yet been fully created (for example, via {@link android.app.Dialog#show()} or
     * {@link android.app.Dialog#create()}).
     * <p>
     * <strong>Note:</strong> In most cases -- depending on compiler support --
     * the resulting view is automatically cast to the target class type. If
     * the target class type is unconstrained, an explicit cast may be
     * necessary.
     *
     * @param dialog the Dialog to search the View in
     * @param id     the ID to search for
     * @return a view with given ID
     * @see View#requireViewById(int)
     * @see Dialog#requireViewById(int)
     * @see Dialog#findViewById(int)
     */
    @NonNull
    public static View requireViewById(@NonNull Dialog dialog, int id) {
        if (Build.VERSION.SDK_INT >= 28) {
            return dialog.requireViewById(id);
        } else {
            View view = dialog.findViewById(id);
            if (view == null) {
                throw new
                        IllegalArgumentException("ID does not reference a View inside this Dialog");
            }
            return view;
        }
    }
}
