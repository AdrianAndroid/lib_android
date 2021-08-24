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

package androidx.savedstate;


import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

/**
 * A scope that owns {@link SavedStateRegistry}
 *
 * @see ViewTreeSavedStateRegistryOwner
 */
public interface SavedStateRegistryOwner extends LifecycleOwner {
    /**
     * Returns owned {@link SavedStateRegistry}
     *
     * @return a {@link SavedStateRegistry}
     */
    @NonNull
    SavedStateRegistry getSavedStateRegistry();
}
