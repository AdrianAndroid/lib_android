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

package androidx.media2.test.service;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.ListenableFuture;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * A mock implementation of {@link SessionPlayer} for testing.
 */
public class MockPlayer extends SessionPlayer {
    private static final int ITEM_NONE = -1;

    public final CountDownLatch mCountDownLatch;
    public final boolean mChangePlayerStateWithTransportControl;

    public boolean mPlayCalled;
    public boolean mPauseCalled;
    public boolean mPrepareCalled;
    public boolean mSeekToCalled;
    public boolean mSetPlaybackSpeedCalled;
    public long mSeekPosition;
    public long mCurrentPosition;
    public long mBufferedPosition;
    public float mPlaybackSpeed = 1.0f;
    @PlayerState
    public int mLastPlayerState;
    @BuffState
    public int mLastBufferingState;
    public long mDuration;
    public List<TrackInfo> mTrackInfos;

    public List<MediaItem> mPlaylist;
    public MediaMetadata mMetadata;
    public MediaItem mCurrentMediaItem;
    public MediaItem mItem;
    public int mIndex = -1;
    @RepeatMode
    public int mRepeatMode = -1;
    @ShuffleMode
    public int mShuffleMode = -1;
    public VideoSize mVideoSize = new VideoSize(0, 0);
    public Surface mSurface;

    public boolean mSetPlaylistCalled;
    public boolean mUpdatePlaylistMetadataCalled;
    public boolean mAddPlaylistItemCalled;
    public boolean mRemovePlaylistItemCalled;
    public boolean mReplacePlaylistItemCalled;
    public boolean mSkipToPlaylistItemCalled;
    public boolean mSkipToPreviousItemCalled;
    public boolean mSkipToNextItemCalled;
    public boolean mSetRepeatModeCalled;
    public boolean mSetShuffleModeCalled;

    private AudioAttributesCompat mAudioAttributes;

    public MockPlayer(int count) {
        this(count, false);
    }

    public MockPlayer(boolean changePlayerStateWithTransportControl) {
        this(0, changePlayerStateWithTransportControl);
    }

    private MockPlayer(int count, boolean changePlayerStateWithTransportControl) {
        mCountDownLatch = (count > 0) ? new CountDownLatch(count) : null;
        mChangePlayerStateWithTransportControl = changePlayerStateWithTransportControl;
        // This prevents MS2#play() from triggering SessionPlayer#prepare().
        mLastPlayerState = PLAYER_STATE_PAUSED;

        // Sets default audio attributes to prevent setVolume() from being called with the play().
        mAudioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA).build();
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public ListenableFuture<PlayerResult> play() {
        mPlayCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PLAYING);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<PlayerResult> pause() {
        mPauseCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PAUSED);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<PlayerResult> prepare() {
        mPrepareCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PAUSED);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<PlayerResult> seekTo(long pos) {
        mSeekToCalled = true;
        mSeekPosition = pos;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getPlayerState() {
        return mLastPlayerState;
    }

    @Override
    public long getCurrentPosition() {
        return mCurrentPosition;
    }

    @Override
    public long getBufferedPosition() {
        return mBufferedPosition;
    }

    @Override
    public float getPlaybackSpeed() {
        return mPlaybackSpeed;
    }

    @Override
    public int getBufferingState() {
        return mLastBufferingState;
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    public void notifyPlayerStateChanged(final int state) {
        mLastPlayerState = state;

        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlayerStateChanged(MockPlayer.this, state);
                }
            });
        }
    }

    public void notifyCurrentMediaItemChanged(final MediaItem item) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onCurrentMediaItemChanged(MockPlayer.this, item);
                }
            });
        }
    }

    public void notifyBufferingStateChanged(final MediaItem item,
            final @BuffState int buffState) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onBufferingStateChanged(MockPlayer.this, item, buffState);
                }
            });
        }
    }

    public void notifyPlaybackSpeedChanged(final float speed) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaybackSpeedChanged(MockPlayer.this, speed);
                }
            });
        }
    }

    public void notifySeekCompleted(final long position) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onSeekCompleted(MockPlayer.this, position);
                }
            });
        }
    }

    public void notifyAudioAttributesChanged(final AudioAttributesCompat attrs) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onAudioAttributesChanged(MockPlayer.this, attrs);
                }
            });
        }
    }

    public void notifyTrackInfoChanged(final List<TrackInfo> trackInfos) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onTrackInfoChanged(MockPlayer.this, trackInfos);
                }
            });
        }
    }

    public void notifyTrackSelected(final TrackInfo trackInfo) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onTrackSelected(MockPlayer.this, trackInfo);
                }
            });
        }
    }

    public void notifyTrackDeselected(final TrackInfo trackInfo) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onTrackDeselected(MockPlayer.this, trackInfo);
                }
            });
        }
    }

    @Override
    public ListenableFuture<PlayerResult> setAudioAttributes(AudioAttributesCompat attributes) {
        mAudioAttributes = attributes;
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return mAudioAttributes;
    }

    @Override
    public ListenableFuture<PlayerResult> setPlaybackSpeed(float speed) {
        mSetPlaybackSpeedCalled = true;
        mPlaybackSpeed = speed;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Playlist APIs
    /////////////////////////////////////////////////////////////////////////////////

    @Override
    public List<MediaItem> getPlaylist() {
        return mPlaylist;
    }

    @Override
    public ListenableFuture<PlayerResult> setMediaItem(MediaItem item) {
        mItem = item;
        ArrayList<MediaItem> list = new ArrayList<>();
        list.add(item);
        return setPlaylist(list, null);
    }

    @Override
    public ListenableFuture<PlayerResult> setPlaylist(
            List<MediaItem> list, MediaMetadata metadata) {
        mSetPlaylistCalled = true;
        mPlaylist = list;
        mMetadata = metadata;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public MediaMetadata getPlaylistMetadata() {
        return mMetadata;
    }

    @Override
    public ListenableFuture<PlayerResult> updatePlaylistMetadata(MediaMetadata metadata) {
        mUpdatePlaylistMetadataCalled = true;
        mMetadata = metadata;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        return mCurrentMediaItem;
    }

    @Override
    public int getCurrentMediaItemIndex() {
        if (mPlaylist == null) {
            return ITEM_NONE;
        }
        return mPlaylist.indexOf(mCurrentMediaItem);
    }

    @Override
    public int getPreviousMediaItemIndex() {
        // TODO: reflect repeat & shuffle modes
        int currentIdx = getCurrentMediaItemIndex();
        if (currentIdx == ITEM_NONE || currentIdx == 0) {
            return ITEM_NONE;
        }
        return currentIdx--;
    }

    @Override
    public int getNextMediaItemIndex() {
        // TODO: reflect repeat & shuffle modes
        int currentIdx = getCurrentMediaItemIndex();
        if (currentIdx == ITEM_NONE || currentIdx == mPlaylist.size() - 1) {
            return ITEM_NONE;
        }
        return currentIdx++;
    }

    @Override
    public ListenableFuture<PlayerResult> addPlaylistItem(int index, MediaItem item) {
        // TODO: check for invalid index
        mAddPlaylistItemCalled = true;
        mIndex = index;
        mItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<PlayerResult> removePlaylistItem(int index) {
        // TODO: check for invalid index
        mRemovePlaylistItemCalled = true;
        mIndex = index;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<PlayerResult> replacePlaylistItem(int index, MediaItem item) {
        // TODO: check for invalid index
        mReplacePlaylistItemCalled = true;
        mIndex = index;
        mItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<PlayerResult> skipToPlaylistItem(int index) {
        // TODO: check for invalid index
        mSkipToPlaylistItemCalled = true;
        mIndex = index;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<PlayerResult> skipToPreviousPlaylistItem() {
        // TODO: reflect repeat & shuffle modes
        mSkipToPreviousItemCalled = true;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<PlayerResult> skipToNextPlaylistItem() {
        // TODO: reflect repeat & shuffle modes
        mSkipToNextItemCalled = true;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getRepeatMode() {
        return mRepeatMode;
    }

    @Override
    public ListenableFuture<PlayerResult> setRepeatMode(int repeatMode) {
        mSetRepeatModeCalled = true;
        mRepeatMode = repeatMode;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getShuffleMode() {
        return mShuffleMode;
    }

    @Override
    public ListenableFuture<PlayerResult> setShuffleMode(int shuffleMode) {
        mSetShuffleModeCalled = true;
        mShuffleMode = shuffleMode;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @NonNull
    @Override
    public List<TrackInfo> getTrackInfoInternal() {
        if (mTrackInfos == null) {
            return new ArrayList<TrackInfo>();
        }
        return mTrackInfos;
    }

    @NonNull
    @Override
    public ListenableFuture<PlayerResult> selectTrackInternal(@NonNull TrackInfo trackInfo) {
        notifyTrackSelected(trackInfo);
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @NonNull
    @Override
    public ListenableFuture<PlayerResult> deselectTrackInternal(@NonNull TrackInfo trackInfo) {
        notifyTrackDeselected(trackInfo);
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    public void notifyShuffleModeChanged() {
        final int shuffleMode = mShuffleMode;
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onShuffleModeChanged(MockPlayer.this, shuffleMode);
                }
            });
        }
    }

    public void notifyRepeatModeChanged() {
        final int repeatMode = mRepeatMode;
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onRepeatModeChanged(MockPlayer.this, repeatMode);
                }
            });
        }
    }

    public void notifyPlaybackCompleted() {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaybackCompleted(MockPlayer.this);
                }
            });
        }
    }

    public void notifyPlaylistChanged() {
        final List<MediaItem> list = mPlaylist;
        final MediaMetadata metadata = mMetadata;
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistChanged(MockPlayer.this, list, metadata);
                }
            });
        }
    }

    public void notifyPlaylistMetadataChanged() {
        final MediaMetadata metadata = mMetadata;
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistMetadataChanged(MockPlayer.this, metadata);
                }
            });
        }
    }

    @Override
    @NonNull
    public VideoSize getVideoSizeInternal() {
        if (mVideoSize == null) {
            mVideoSize = new VideoSize(0, 0);
        }
        return mVideoSize;
    }

    public void notifyVideoSizeChanged(@NonNull final VideoSize videoSize) {
        MediaItem fakeItem = new MediaItem.Builder().build();

        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onVideoSizeChangedInternal(MockPlayer.this, fakeItem, videoSize);
                }
            });
        }
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setSurfaceInternal(@Nullable Surface surface) {
        mSurface = surface;
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    public boolean surfaceExists() {
        return mSurface != null;
    }

    public void notifySubtitleData(@NonNull final MediaItem item, @NonNull final TrackInfo track,
            @NonNull final SubtitleData data) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onSubtitleData(MockPlayer.this, item, track, data);
                }
            });
        }
    }
}
