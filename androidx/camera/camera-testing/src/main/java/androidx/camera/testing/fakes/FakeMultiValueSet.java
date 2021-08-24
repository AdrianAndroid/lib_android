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

package androidx.camera.testing.fakes;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.MultiValueSet;

/**
 * A fake implementation for {@link MultiValueSet}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class FakeMultiValueSet extends MultiValueSet<FakeMultiValueSet> {

    /** Clone a {@link FakeMultiValueSet}. */
    @Override
    @NonNull
    public MultiValueSet<FakeMultiValueSet> clone() {
        FakeMultiValueSet set = new FakeMultiValueSet();
        set.addAll(getAllItems());
        return set;
    }
}
