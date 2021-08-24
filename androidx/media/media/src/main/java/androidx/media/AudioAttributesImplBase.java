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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MOVIE;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_SONIFICATION;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_SPEECH;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_UNKNOWN;
import static androidx.media.AudioAttributesCompat.INVALID_STREAM_TYPE;
import static androidx.media.AudioAttributesCompat.TAG;
import static androidx.media.AudioAttributesCompat.USAGE_ALARM;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANCE_ACCESSIBILITY;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANCE_SONIFICATION;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANT;
import static androidx.media.AudioAttributesCompat.USAGE_GAME;
import static androidx.media.AudioAttributesCompat.USAGE_MEDIA;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_DELAYED;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_INSTANT;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_REQUEST;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_EVENT;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_RINGTONE;
import static androidx.media.AudioAttributesCompat.USAGE_UNKNOWN;
import static androidx.media.AudioAttributesCompat.USAGE_VIRTUAL_SOURCE;
import static androidx.media.AudioAttributesCompat.USAGE_VOICE_COMMUNICATION;
import static androidx.media.AudioAttributesCompat.USAGE_VOICE_COMMUNICATION_SIGNALLING;

import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat.AudioManagerHidden;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Arrays;

/** @hide */
@VersionedParcelize(jetifyAs = "android.support.v4.media.AudioAttributesImplBase")
@RestrictTo(LIBRARY)
public class AudioAttributesImplBase implements AudioAttributesImpl {
    /** @hide */
    // It should be public to allow Parcelizers which never be de/jetified can access the field.
    @RestrictTo(LIBRARY)
    @ParcelField(1)
    public int mUsage = USAGE_UNKNOWN;

    /** @hide */
    // It should be public to allow Parcelizers which never be de/jetified can access the field.
    @RestrictTo(LIBRARY)
    @ParcelField(2)
    public int mContentType = CONTENT_TYPE_UNKNOWN;

    /** @hide */
    // It should be public to allow Parcelizers which never be de/jetified can access the field.
    @RestrictTo(LIBRARY)
    @ParcelField(3)
    public int mFlags = 0x0;

    /** @hide */
    // It should be public to allow Parcelizers which never be de/jetified can access the field.
    @RestrictTo(LIBRARY)
    @ParcelField(4)
    public int mLegacyStream = INVALID_STREAM_TYPE;

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    /** @hide */
    // It should be public to allow Parcelizers which never be de/jetified can access the
    // constructor.
    @RestrictTo(LIBRARY)
    public AudioAttributesImplBase() { }

    AudioAttributesImplBase(int contentType, int flags, int usage, int legacyStream) {
        mContentType = contentType;
        mFlags = flags;
        mUsage = usage;
        mLegacyStream = legacyStream;
    }

    @Override
    @Nullable
    public Object getAudioAttributes() {
        return null;
    }

    @Override
    public int getVolumeControlStream() {
        return AudioAttributesCompat.toVolumeStreamType(true, mFlags, mUsage);
    }

    @Override
    public int getLegacyStreamType() {
        if (mLegacyStream != INVALID_STREAM_TYPE) {
            return mLegacyStream;
        }
        return AudioAttributesCompat.toVolumeStreamType(false, mFlags, mUsage);
    }

    @Override
    public int getRawLegacyStreamType() {
        return mLegacyStream;
    }

    @Override
    public int getContentType() {
        return mContentType;
    }

    @Override
    public @AudioAttributesCompat.AttributeUsage int getUsage() {
        return mUsage;
    }

    @Override
    public int getFlags() {
        int flags = mFlags;
        int legacyStream = getLegacyStreamType();
        if (legacyStream == AudioManagerHidden.STREAM_BLUETOOTH_SCO) {
            flags |= AudioAttributesCompat.FLAG_SCO;
        } else if (legacyStream == AudioManagerHidden.STREAM_SYSTEM_ENFORCED) {
            flags |= AudioAttributesCompat.FLAG_AUDIBILITY_ENFORCED;
        }
        return flags & AudioAttributesCompat.FLAG_ALL_PUBLIC;
    }

    //////////////////////////////////////////////////////////////////////
    // Override Object methods

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {mContentType, mFlags, mUsage, mLegacyStream});
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AudioAttributesImplBase)) {
            return false;
        }
        final AudioAttributesImplBase that = (AudioAttributesImplBase) o;
        return ((mContentType == that.getContentType())
                && (mFlags == that.getFlags())
                && (mUsage == that.getUsage())
                && (mLegacyStream  == that.mLegacyStream)); // query the slot directly, don't guess
    }

    @Override
    @NonNull
    public String toString() {
        final StringBuilder sb = new StringBuilder("AudioAttributesCompat:");
        if (mLegacyStream != INVALID_STREAM_TYPE) {
            sb.append(" stream=").append(mLegacyStream);
            sb.append(" derived");
        }
        sb.append(" usage=")
                .append(AudioAttributesCompat.usageToString(mUsage))
                .append(" content=")
                .append(mContentType)
                .append(" flags=0x")
                .append(Integer.toHexString(mFlags).toUpperCase());
        return sb.toString();
    }

    static class Builder implements AudioAttributesImpl.Builder {
        private int mUsage = USAGE_UNKNOWN;
        private int mContentType = CONTENT_TYPE_UNKNOWN;
        private int mFlags = 0x0;
        private int mLegacyStream = INVALID_STREAM_TYPE;

        Builder() {
        }

        Builder(AudioAttributesCompat aa) {
            mUsage = aa.getUsage();
            mContentType = aa.getContentType();
            mFlags = aa.getFlags();
            mLegacyStream = aa.getRawLegacyStreamType();
        }

        @Override
        @NonNull
        public AudioAttributesImpl build() {
            return new AudioAttributesImplBase(mContentType, mFlags, mUsage, mLegacyStream);
        }

        @Override
        @NonNull
        public Builder setUsage(@AudioAttributesCompat.AttributeUsage int usage) {
            switch (usage) {
                case USAGE_UNKNOWN:
                case USAGE_MEDIA:
                case USAGE_VOICE_COMMUNICATION:
                case USAGE_VOICE_COMMUNICATION_SIGNALLING:
                case USAGE_ALARM:
                case USAGE_NOTIFICATION:
                case USAGE_NOTIFICATION_RINGTONE:
                case USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
                case USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
                case USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
                case USAGE_NOTIFICATION_EVENT:
                case USAGE_ASSISTANCE_ACCESSIBILITY:
                case USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
                case USAGE_ASSISTANCE_SONIFICATION:
                case USAGE_GAME:
                case USAGE_VIRTUAL_SOURCE:
                    mUsage = usage;
                    break;
                // TODO: shouldn't it be USAGE_ASSISTANT?
                case USAGE_ASSISTANT:
                    mUsage = USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
                    break;
                default:
                    mUsage = USAGE_UNKNOWN;
            }
            return this;
        }

        @Override
        @NonNull
        public Builder setContentType(@AudioAttributesCompat.AttributeContentType int contentType) {
            switch (contentType) {
                case CONTENT_TYPE_UNKNOWN:
                case CONTENT_TYPE_MOVIE:
                case CONTENT_TYPE_MUSIC:
                case CONTENT_TYPE_SONIFICATION:
                case CONTENT_TYPE_SPEECH:
                    mContentType = contentType;
                    break;
                default:
                    mContentType = CONTENT_TYPE_UNKNOWN;
            }
            return this;
        }

        @Override
        @NonNull
        public Builder setFlags(int flags) {
            flags &= AudioAttributesCompat.FLAG_ALL;
            mFlags |= flags;
            return this;
        }

        @Override
        @NonNull
        public Builder setLegacyStreamType(int streamType) {
            if (streamType == AudioManagerHidden.STREAM_ACCESSIBILITY) {
                throw new IllegalArgumentException(
                        "STREAM_ACCESSIBILITY is not a legacy stream "
                                + "type that was used for audio playback");
            }
            mLegacyStream = streamType;
            return setInternalLegacyStreamType(streamType);
        }

        private Builder setInternalLegacyStreamType(int streamType) {
            switch (streamType) {
                case AudioManager.STREAM_VOICE_CALL:
                    mContentType = CONTENT_TYPE_SPEECH;
                    break;
                case AudioManagerHidden.STREAM_SYSTEM_ENFORCED:
                    mFlags |= AudioAttributesCompat.FLAG_AUDIBILITY_ENFORCED;
                    // intended fall through, attributes in common with STREAM_SYSTEM
                case AudioManager.STREAM_SYSTEM:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    break;
                case AudioManager.STREAM_RING:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    break;
                case AudioManager.STREAM_MUSIC:
                    mContentType = CONTENT_TYPE_MUSIC;
                    break;
                case AudioManager.STREAM_ALARM:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    break;
                case AudioManager.STREAM_NOTIFICATION:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    break;
                case AudioManagerHidden.STREAM_BLUETOOTH_SCO:
                    mContentType = CONTENT_TYPE_SPEECH;
                    mFlags |= AudioAttributesCompat.FLAG_SCO;
                    break;
                case AudioManager.STREAM_DTMF:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    break;
                case AudioManagerHidden.STREAM_TTS:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    break;
                case AudioManager.STREAM_ACCESSIBILITY:
                    mContentType = CONTENT_TYPE_SPEECH;
                    break;
                default:
                    Log.e(TAG, "Invalid stream type " + streamType + " for AudioAttributesCompat");
            }
            mUsage = usageForStreamType(streamType);
            return this;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static int usageForStreamType(int streamType) {
        switch (streamType) {
            case AudioManager.STREAM_VOICE_CALL:
                return USAGE_VOICE_COMMUNICATION;
            case AudioManagerHidden.STREAM_SYSTEM_ENFORCED:
            case AudioManager.STREAM_SYSTEM:
                return USAGE_ASSISTANCE_SONIFICATION;
            case AudioManager.STREAM_RING:
                return USAGE_NOTIFICATION_RINGTONE;
            case AudioManager.STREAM_MUSIC:
                return USAGE_MEDIA;
            case AudioManager.STREAM_ALARM:
                return USAGE_ALARM;
            case AudioManager.STREAM_NOTIFICATION:
                return USAGE_NOTIFICATION;
            case AudioManagerHidden.STREAM_BLUETOOTH_SCO:
                return USAGE_VOICE_COMMUNICATION;
            case AudioManager.STREAM_DTMF:
                return USAGE_VOICE_COMMUNICATION_SIGNALLING;
            case AudioManager.STREAM_ACCESSIBILITY:
                return USAGE_ASSISTANCE_ACCESSIBILITY;
            case AudioManagerHidden.STREAM_TTS:
            default:
                return USAGE_UNKNOWN;
        }
    }
}
