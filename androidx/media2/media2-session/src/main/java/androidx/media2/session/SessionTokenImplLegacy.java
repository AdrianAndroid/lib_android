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

package androidx.media2.session;

import static androidx.media2.session.SessionToken.TYPE_BROWSER_SERVICE_LEGACY;
import static androidx.media2.session.SessionToken.TYPE_LIBRARY_SERVICE;
import static androidx.media2.session.SessionToken.TYPE_SESSION;
import static androidx.media2.session.SessionToken.TYPE_SESSION_LEGACY;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.media2.session.SessionToken.SessionTokenImpl;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

@VersionedParcelize(isCustom = true)
final class SessionTokenImplLegacy extends CustomVersionedParcelable implements SessionTokenImpl {
    // Don't mark mLegacyToken @ParcelField, because we need to use toBundle()/fromBundle() instead
    // of the writeToParcel()/Parcelable.Creator for sending extra binder.
    @NonParcelField
    private MediaSessionCompat.Token mLegacyToken;
    // For parceling mLegacyToken. Should be only used by onPreParceling() and onPostParceling().
    @ParcelField(1)
    Bundle mLegacyTokenBundle;
    @ParcelField(2)
    int mUid;
    @ParcelField(3)
    int mType;
    @ParcelField(4)
    ComponentName mComponentName;
    @ParcelField(5)
    String mPackageName;
    @ParcelField(6)
    Bundle mExtras;

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    SessionTokenImplLegacy(MediaSessionCompat.Token token, String packageName, int uid,
            Bundle sessionInfo) {
        if (token == null) {
            throw new NullPointerException("token shouldn't be null");
        }
        if (packageName == null) {
            throw new NullPointerException("packageName shouldn't be null");
        } else if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName shouldn't be empty");
        }

        mLegacyToken = token;
        mUid = uid;
        mPackageName = packageName;
        mComponentName = null;
        mType = TYPE_SESSION_LEGACY;
        mExtras = sessionInfo;
    }

    SessionTokenImplLegacy(ComponentName serviceComponent, int uid) {
        if (serviceComponent == null) {
            throw new NullPointerException("serviceComponent shouldn't be null");
        }

        mLegacyToken = null;
        mUid = uid;
        mType = TYPE_BROWSER_SERVICE_LEGACY;
        mPackageName = serviceComponent.getPackageName();
        mComponentName = serviceComponent;
        mExtras = null;
    }

    /**
     * Used for {@link VersionedParcelable}
     */
    SessionTokenImplLegacy() {
        // Do nothing.
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mType, mComponentName, mLegacyToken);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionTokenImplLegacy)) {
            return false;
        }
        SessionTokenImplLegacy other = (SessionTokenImplLegacy) obj;
        if (mType != other.mType) {
            return false;
        }
        switch (mType) {
            case TYPE_SESSION_LEGACY:
                return ObjectsCompat.equals(mLegacyToken, other.mLegacyToken);
            case TYPE_BROWSER_SERVICE_LEGACY:
                return ObjectsCompat.equals(mComponentName, other.mComponentName);
        }
        return false;
    }

    @Override
    public boolean isLegacySession() {
        return true;
    }

    @Override
    @SuppressWarnings("ObjectToString")
    public String toString() {
        return "SessionToken {legacyToken=" + mLegacyToken + "}";
    }

    @Override
    public int getUid() {
        return mUid;
    }

    @Override
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    @Override
    @Nullable
    public String getServiceName() {
        return mComponentName == null ? null : mComponentName.getClassName();
    }

    @Override
    public ComponentName getComponentName() {
        return mComponentName;
    }

    @Override
    @SessionToken.TokenType
    public int getType() {
        switch (mType) {
            case TYPE_SESSION_LEGACY:
                return TYPE_SESSION;
            case TYPE_BROWSER_SERVICE_LEGACY:
                return TYPE_LIBRARY_SERVICE;
        }
        return TYPE_SESSION;
    }

    @Override
    @Nullable
    public Bundle getExtras() {
        return mExtras;
    }

    @Override
    public Object getBinder() {
        return mLegacyToken;
    }

    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField") // mLegacyToken is effectively final.
    public void onPreParceling(boolean isStream) {
        if (mLegacyToken != null) {
            synchronized (mLegacyToken) {
                // Note: mLegacyTokenBundle should always be recreated, because mLegacyToken is
                // mutable.

                // Note: token should be null or SessionToken whose impl equals to this object.
                VersionedParcelable token = mLegacyToken.getSession2Token();

                // Temporarily sets the SessionToken to null to prevent infinite loop when
                // parceling. Otherwise, this will be called again when mLegacyToken parcelize
                // SessionToken in it and it never ends.
                mLegacyToken.setSession2Token(null);

                // Although mLegacyToken is Parcelable, we should use toBundle() instead here
                // because extra binder inside of the mLegacyToken are shared only through the
                // toBundle().
                mLegacyTokenBundle = mLegacyToken.toBundle();

                // Resets the SessionToken.
                mLegacyToken.setSession2Token(token);
            }
        } else {
            mLegacyTokenBundle = null;
        }
    }

    @Override
    public void onPostParceling() {
        // Although mLegacyToken is Parcelable, we should use fromBundle() instead here because
        // extra binder inside of the mLegacyToken are shared only through the fromBundle().
        mLegacyToken = MediaSessionCompat.Token.fromBundle(mLegacyTokenBundle);
    }
}
