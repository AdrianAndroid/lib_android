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

package androidx.versionedparcelable;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public class ParcelImpl implements Parcelable {

    @Nullable
    private final VersionedParcelable mParcel;

    public ParcelImpl(@Nullable VersionedParcelable parcel) {
        mParcel = parcel;
    }

    protected ParcelImpl(@NonNull Parcel in) {
        mParcel = new VersionedParcelParcel(in).readVersionedParcelable();
    }

    /**
     */
    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    @Nullable
    public <T extends VersionedParcelable> T getVersionedParcel() {
        return (T) mParcel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VersionedParcelParcel parcel = new VersionedParcelParcel(dest);
        parcel.writeVersionedParcelable(mParcel);
    }

    public static final Creator<ParcelImpl> CREATOR = new Creator<ParcelImpl>() {
        @Override
        @NonNull
        public ParcelImpl createFromParcel(@NonNull Parcel in) {
            return new ParcelImpl(in);
        }

        @Override
        @NonNull
        public ParcelImpl[] newArray(int size) {
            return new ParcelImpl[size];
        }
    };
}
