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

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.ParcelImplListSlice;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.VideoSize;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Collections;
import java.util.List;

/**
 * Created by {@link MediaSession} to send its state to the {@link MediaController} when the
 * connection request is accepted. It's intentionally {@link VersionedParcelable} for future
 * extension.
 * <p>
 * All fields here are effectively final. Do not modify.
 */
@VersionedParcelize(isCustom = true)
class ConnectionResult extends CustomVersionedParcelable {
    @ParcelField(0)
    int mVersion;
    // Parceled via mSessionBinder.
    @NonParcelField
    IMediaSession mSessionStub;
    // For parceling mSessionStub. Should be only used by onPreParceling() and onPostParceling().
    @ParcelField(1)
    IBinder mSessionBinder;
    @ParcelField(2)
    PendingIntent mSessionActivity;
    @ParcelField(3)
    int mPlayerState;
    // Parceled via mParcelableCurrentMediaItem.
    @NonParcelField
    MediaItem mCurrentMediaItem;
    // For parceling mCurrentMediaItem. Should be only used by onPreParceling() and
    // onPostParceling().
    @ParcelField(4)
    MediaItem mParcelableCurrentMediaItem;
    @ParcelField(5)
    long mPositionEventTimeMs;
    @ParcelField(6)
    long mPositionMs;
    @ParcelField(7)
    float mPlaybackSpeed;
    @ParcelField(8)
    long mBufferedPositionMs;
    @ParcelField(9)
    MediaController.PlaybackInfo mPlaybackInfo;
    @ParcelField(10)
    int mRepeatMode;
    @ParcelField(11)
    int mShuffleMode;
    @ParcelField(12)
    ParcelImplListSlice mPlaylistSlice;
    @ParcelField(13)
    SessionCommandGroup mAllowedCommands;
    @ParcelField(14)
    int mCurrentMediaItemIndex;
    @ParcelField(15)
    int mPreviousMediaItemIndex;
    @ParcelField(16)
    int mNextMediaItemIndex;
    @ParcelField(17)
    Bundle mTokenExtras;
    @ParcelField(18)
    VideoSize mVideoSize;
    @ParcelField(19)
    List<TrackInfo> mTracks;
    @ParcelField(20)
    TrackInfo mSelectedVideoTrack;
    @ParcelField(21)
    TrackInfo mSelectedAudioTrack;
    @ParcelField(23)
    TrackInfo mSelectedSubtitleTrack;
    @ParcelField(24)
    TrackInfo mSelectedMetadataTrack;
    @ParcelField(25)
    MediaMetadata mPlaylistMetadata;
    @ParcelField(26)
    int mBufferingState;

    // For versioned parcelable
    ConnectionResult() {
        // no-op
    }

    ConnectionResult(@NonNull MediaSessionStub sessionStub,
            @NonNull MediaSession.MediaSessionImpl sessionImpl,
            @NonNull SessionCommandGroup allowedCommands) {
        mSessionStub = sessionStub;
        mPlayerState = sessionImpl.getPlayerState();
        mCurrentMediaItem = sessionImpl.getCurrentMediaItem();
        mPositionEventTimeMs = SystemClock.elapsedRealtime();
        mPositionMs = sessionImpl.getCurrentPosition();
        mPlaybackSpeed = sessionImpl.getPlaybackSpeed();
        mBufferedPositionMs = sessionImpl.getBufferedPosition();
        mPlaybackInfo = sessionImpl.getPlaybackInfo();
        mRepeatMode = sessionImpl.getRepeatMode();
        mShuffleMode = sessionImpl.getShuffleMode();
        mSessionActivity = sessionImpl.getSessionActivity();
        mCurrentMediaItemIndex = sessionImpl.getCurrentMediaItemIndex();
        mPreviousMediaItemIndex = sessionImpl.getPreviousMediaItemIndex();
        mNextMediaItemIndex = sessionImpl.getNextMediaItemIndex();
        mTokenExtras = sessionImpl.getToken().getExtras();
        mVideoSize = sessionImpl.getVideoSize();
        mTracks = sessionImpl.getTracks();
        mSelectedVideoTrack = sessionImpl.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_VIDEO);
        mSelectedAudioTrack = sessionImpl.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_AUDIO);
        mSelectedSubtitleTrack = sessionImpl.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE);
        mSelectedMetadataTrack = sessionImpl.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_METADATA);
        if (allowedCommands.hasCommand(SessionCommand.COMMAND_CODE_PLAYER_GET_PLAYLIST)) {
            List<MediaItem> playlist = sessionImpl.getPlaylist();
            mPlaylistSlice = MediaUtils.convertMediaItemListToParcelImplListSlice(playlist);
        } else {
            mPlaylistSlice = null;
        }
        if (allowedCommands.hasCommand(SessionCommand.COMMAND_CODE_PLAYER_GET_PLAYLIST)
                || allowedCommands.hasCommand(
                        SessionCommand.COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA)) {
            mPlaylistMetadata = sessionImpl.getPlaylistMetadata();
        } else {
            mPlaylistMetadata = null;
        }
        mBufferingState = sessionImpl.getBufferingState();
        mAllowedCommands = allowedCommands;
        mVersion = MediaUtils.CURRENT_VERSION;
    }

    public IMediaSession getSessionStub() {
        return mSessionStub;
    }

    public PendingIntent getSessionActivity() {
        return mSessionActivity;
    }

    public int getPlayerState() {
        return mPlayerState;
    }

    public MediaItem getCurrentMediaItem() {
        return mCurrentMediaItem;
    }

    public long getPositionEventTimeMs() {
        return mPositionEventTimeMs;
    }

    public long getPositionMs() {
        return mPositionMs;
    }

    public float getPlaybackSpeed() {
        return mPlaybackSpeed;
    }

    public long getBufferedPositionMs() {
        return mBufferedPositionMs;
    }

    public MediaController.PlaybackInfo getPlaybackInfo() {
        return mPlaybackInfo;
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public int getShuffleMode() {
        return mShuffleMode;
    }

    public ParcelImplListSlice getPlaylistSlice() {
        return mPlaylistSlice;
    }

    public SessionCommandGroup getAllowedCommands() {
        return mAllowedCommands;
    }

    public int getVersion() {
        return mVersion;
    }

    public int getCurrentMediaItemIndex() {
        return mCurrentMediaItemIndex;
    }

    public int getPreviousMediaItemIndex() {
        return mPreviousMediaItemIndex;
    }

    public int getNextMediaItemIndex() {
        return mNextMediaItemIndex;
    }

    public Bundle getTokenExtras() {
        return mTokenExtras;
    }

    public VideoSize getVideoSize() {
        return mVideoSize;
    }

    @NonNull
    public List<TrackInfo> getTracks() {
        return (mTracks == null) ? Collections.emptyList() : mTracks;
    }

    public TrackInfo getSelectedVideoTrack() {
        return mSelectedVideoTrack;
    }

    public TrackInfo getSelectedAudioTrack() {
        return mSelectedAudioTrack;
    }

    public TrackInfo getSelectedSubtitleTrack() {
        return mSelectedSubtitleTrack;
    }

    public TrackInfo getSelectedMetadataTrack() {
        return mSelectedMetadataTrack;
    }

    @Nullable
    public MediaMetadata getPlaylistMetadata() {
        return mPlaylistMetadata;
    }

    public int getBufferingState() {
        return mBufferingState;
    }

    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField") // mSessionStub is effectively final.
    public void onPreParceling(boolean isStream) {
        synchronized (mSessionStub) {
            if (mSessionBinder == null) {
                mSessionBinder = (IBinder) mSessionStub;
                mParcelableCurrentMediaItem =
                        MediaUtils.upcastForPreparceling(mCurrentMediaItem);
            }
        }
    }

    @Override
    public void onPostParceling() {
        mSessionStub = IMediaSession.Stub.asInterface(mSessionBinder);
        mCurrentMediaItem = mParcelableCurrentMediaItem;
    }
}
