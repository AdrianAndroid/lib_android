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

import static android.support.v4.media.MediaBrowserCompat.EXTRA_PAGE;
import static android.support.v4.media.MediaBrowserCompat.EXTRA_PAGE_SIZE;

import static androidx.media2.session.LibraryResult.RESULT_SUCCESS;
import static androidx.media2.session.MediaUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES;

import android.content.Context;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.PlayerResult;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController.PlaybackInfo;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionImpl;
import androidx.media2.session.MediaSession.CommandButton;
import androidx.media2.session.MediaSession.ControllerCb;
import androidx.media2.session.MediaSession.ControllerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link MediaBrowserServiceCompat} for interoperability between
 * {@link MediaLibraryService} and {@link MediaBrowserCompat}.
 */
class MediaLibraryServiceLegacyStub extends MediaSessionServiceLegacyStub {
    private static final String TAG = "MLS2LegacyStub";
    private static final boolean DEBUG = false;

    private final ControllerCb mBrowserLegacyCbForBroadcast;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaLibrarySessionImpl mLibrarySessionImpl;

    // Note: We'd better not obtain token from the session because it's called inside of the
    // session's constructor and session's token may not be initialized here.
    MediaLibraryServiceLegacyStub(Context context, MediaLibrarySessionImpl session,
            MediaSessionCompat.Token token) {
        super(context, session, token);
        mLibrarySessionImpl = session;
        mBrowserLegacyCbForBroadcast = new BrowserLegacyCbForBroadcast(this);
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, final Bundle rootHints) {
        BrowserRoot browserRoot = super.onGetRoot(clientPackageName, clientUid, rootHints);
        if (browserRoot == null) {
            return null;
        }
        final ControllerInfo controller = getCurrentController();
        if (controller == null) {
            return null;
        }
        if (getConnectedControllersManager().isAllowedCommand(controller,
                SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT)) {
            // Call callbacks directly instead of execute on the executor. Here's the reason.
            // We need to return browser root here. So if we run the callback on the executor, we
            // should wait for the completion.
            // However, we cannot wait if the callback executor is the main executor, which posts
            // the runnable to the main thread's. In that case, since this onGetRoot() always runs
            // on the main thread, the posted runnable for calling onGetLibraryRoot() wouldn't run
            // in here. Even worse, we cannot know whether it would be run on the main thread or
            // not.
            // Because of the reason, just call onGetLibraryRoot() directly here. onGetLibraryRoot()
            // has documentation that it may be called on the main thread.
            LibraryParams params = MediaUtils.convertToLibraryParams(
                    mLibrarySessionImpl.getContext(), rootHints);
            LibraryResult result = mLibrarySessionImpl.getCallback().onGetLibraryRoot(
                    mLibrarySessionImpl.getInstance(), controller, params);
            if (result != null && result.getResultCode() == RESULT_SUCCESS
                    && result.getMediaItem() != null) {
                MediaMetadata metadata = result.getMediaItem().getMetadata();
                String id = metadata != null
                        ? metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) : "";
                return new BrowserRoot(id,
                        MediaUtils.convertToRootHints(result.getLibraryParams()));
            } else if (DEBUG) {
                Log.d(TAG, "Unexpected LibraryResult for getting the root from the legacy browser."
                        + " Will return stub root to allow getting session.");
            }
        } else if (DEBUG) {
            Log.d(TAG, "Command MBC.connect from " + controller + " was rejected by "
                    + mLibrarySessionImpl);
        }
        // No library root, but keep browser compat connected to allow getting session.
        return MediaUtils.sDefaultBrowserRoot;
    }

    @Override
    public void onSubscribe(final String id, final Bundle option) {
        final ControllerInfo controller = getCurrentController();
        if (TextUtils.isEmpty(id)) {
            Log.w(TAG, "onSubscribe(): Ignoring empty id from " + controller);
            return;
        }
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Note: If a developer calls notifyChildrenChanged inside, onLoadChildren will be
                // called twice for a single subscription event.
                // TODO(post 1.0): Fix the issue above.
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.subscribe() from " + controller + " was rejected"
                                + " by " + mLibrarySessionImpl);
                    }
                    return;
                }
                LibraryParams params = MediaUtils.convertToLibraryParams(
                        mLibrarySessionImpl.getContext(), option);
                mLibrarySessionImpl.getCallback().onSubscribe(mLibrarySessionImpl.getInstance(),
                        controller, id, params);
            }
        });
    }

    @Override
    public void onUnsubscribe(final String id) {
        final ControllerInfo controller = getCurrentController();
        if (TextUtils.isEmpty(id)) {
            Log.w(TAG, "onUnsubscribe(): Ignoring empty id from " + controller);
            return;
        }
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.unsubscribe() from " + controller + " was rejected"
                                + " by " + mLibrarySessionImpl);
                    }
                    return;
                }
                mLibrarySessionImpl.getCallback().onUnsubscribe(mLibrarySessionImpl.getInstance(),
                                controller, id);
            }
        });
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
        onLoadChildren(parentId, result, null);
    }

    @Override
    public void onLoadChildren(final String parentId,
            final Result<List<MediaBrowserCompat.MediaItem>> result, final Bundle options) {
        final ControllerInfo controller = getCurrentController();
        if (TextUtils.isEmpty(parentId)) {
            Log.w(TAG, "onLoadChildren(): Ignoring empty parentId from " + controller);
            result.sendError(null);
            return;
        }
        result.detach();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.subscribe() from " + controller + " was rejected"
                                + " by " + mLibrarySessionImpl);
                    }
                    result.sendError(null);
                    return;
                }
                if (options != null) {
                    options.setClassLoader(mLibrarySessionImpl.getContext().getClassLoader());
                    try {
                        int page = options.getInt(EXTRA_PAGE);
                        int pageSize = options.getInt(EXTRA_PAGE_SIZE);
                        if (page > 0 && pageSize > 0) {
                            // Requesting the list of children through pagination.
                            LibraryParams params = MediaUtils.convertToLibraryParams(
                                    mLibrarySessionImpl.getContext(), options);
                            LibraryResult libraryResult = mLibrarySessionImpl.getCallback()
                                    .onGetChildren(mLibrarySessionImpl.getInstance(), controller,
                                            parentId, page, pageSize, params);
                            if (libraryResult == null
                                    || libraryResult.getResultCode() != RESULT_SUCCESS) {
                                result.sendResult(null);
                            } else {
                                result.sendResult(MediaUtils.truncateListBySize(
                                        MediaUtils.convertToMediaItemList(
                                                libraryResult.getMediaItems()),
                                        TRANSACTION_SIZE_LIMIT_IN_BYTES));
                            }
                            return;
                        }
                        // Cannot distinguish onLoadChildren() why it's called either by
                        // {@link MediaBrowserCompat#subscribe()} or
                        // {@link MediaBrowserServiceCompat#notifyChildrenChanged}.
                    } catch (BadParcelableException e) {
                        // pass-through.
                    }
                }
                // A MediaBrowserCompat called loadChildren with no pagination option.
                LibraryResult libraryResult = mLibrarySessionImpl.getCallback()
                        .onGetChildren(mLibrarySessionImpl.getInstance(), controller, parentId,
                                0 /* page */, Integer.MAX_VALUE /* pageSize*/,
                                null /* extras */);
                if (libraryResult == null
                        || libraryResult.getResultCode() != RESULT_SUCCESS) {
                    result.sendResult(null);
                } else {
                    result.sendResult(MediaUtils.truncateListBySize(
                            MediaUtils.convertToMediaItemList(libraryResult.getMediaItems()),
                            TRANSACTION_SIZE_LIMIT_IN_BYTES));
                }
            }
        });
    }

    @Override
    public void onLoadItem(final String itemId, final Result<MediaBrowserCompat.MediaItem> result) {
        final ControllerInfo controller = getCurrentController();
        if (TextUtils.isEmpty(itemId)) {
            Log.w(TAG, "Ignoring empty itemId from " + controller);
            result.sendError(null);
            return;
        }
        result.detach();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.getItem() from " + controller + " was rejected by "
                                + mLibrarySessionImpl);
                    }
                    result.sendError(null);
                    return;
                }
                LibraryResult libraryResult = mLibrarySessionImpl.getCallback().onGetItem(
                        mLibrarySessionImpl.getInstance(), controller, itemId);
                if (libraryResult == null || libraryResult.getResultCode() != RESULT_SUCCESS) {
                    result.sendResult(null);
                } else {
                    result.sendResult(MediaUtils.convertToMediaItem(libraryResult.getMediaItem()));
                }
            }
        });
    }

    @Override
    public void onSearch(final String query, final Bundle extras,
            final Result<List<MediaBrowserCompat.MediaItem>> result) {
        final ControllerInfo controller = getCurrentController();
        if (TextUtils.isEmpty(query)) {
            Log.w(TAG, "Ignoring empty query from " + controller);
            result.sendError(null);
            return;
        }
        if (!(controller.getControllerCb() instanceof BrowserLegacyCb)) {
            if (DEBUG) {
                throw new IllegalStateException("Callback hasn't registered. Must be a bug");
            }
            return;
        }
        result.detach();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.search() from " + controller + " was rejected by "
                                + mLibrarySessionImpl);
                    }
                    result.sendError(null);
                    return;
                }
                BrowserLegacyCb cb = (BrowserLegacyCb) controller.getControllerCb();
                cb.registerSearchRequest(controller, query, extras, result);
                LibraryParams params = MediaUtils.convertToLibraryParams(
                        mLibrarySessionImpl.getContext(), extras);
                mLibrarySessionImpl.getCallback().onSearch(mLibrarySessionImpl.getInstance(),
                        controller, query, params);
                // Actual search result will be sent by notifySearchResultChanged().
            }
        });
    }

    @Override
    public void onCustomAction(final String action, final Bundle extras,
            final Result<Bundle> result) {
        if (result != null) {
            result.detach();
        }
        final ControllerInfo controller = getCurrentController();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            @SuppressWarnings("ObjectToString")
            public void run() {
                SessionCommand command = new SessionCommand(action, null);
                if (!getConnectedControllersManager().isAllowedCommand(controller, command)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.sendCustomAction(" + command + ") from "
                                + controller + " was rejected by " + mLibrarySessionImpl);
                    }
                    if (result != null) {
                        result.sendError(null);
                    }
                    return;
                }
                SessionResult sessionResult = mLibrarySessionImpl.getCallback().onCustomCommand(
                        mLibrarySessionImpl.getInstance(), controller, command, extras);
                if (sessionResult != null) {
                    result.sendResult(sessionResult.getCustomCommandResult());
                }
            }
        });
    }

    @Override
    ControllerInfo createControllerInfo(RemoteUserInfo remoteUserInfo) {
        return new ControllerInfo(remoteUserInfo, MediaUtils.VERSION_UNKNOWN,
                mManager.isTrustedForMediaControl(remoteUserInfo),
                new BrowserLegacyCb(remoteUserInfo), null /* connectionHints */);
    }

    ControllerCb getBrowserLegacyCbForBroadcast() {
        return mBrowserLegacyCbForBroadcast;
    }

    private ControllerInfo getCurrentController() {
        return getConnectedControllersManager().getController(getCurrentBrowserInfo());
    }

    private static class SearchRequest {
        public final ControllerInfo mController;
        public final RemoteUserInfo mRemoteUserInfo;
        public final String mQuery;
        public final Bundle mExtras;
        public final Result<List<MediaBrowserCompat.MediaItem>> mResult;

        SearchRequest(ControllerInfo controller, RemoteUserInfo remoteUserInfo, String query,
                Bundle extras, Result<List<MediaBrowserCompat.MediaItem>> result) {
            mController = controller;
            mRemoteUserInfo = remoteUserInfo;
            mQuery = query;
            mExtras = extras;
            mResult = result;
        }
    }

    // Base class for MediaBrowserCompat's ControllerCb.
    // This documents
    //   1) Why some APIs does nothing
    //   2) Why some APIs should throw exception when DEBUG is {@code true}.
    private abstract static class BaseBrowserLegacyCb extends MediaSession.ControllerCb {
        @Override
        void onPlayerResult(int seq, PlayerResult result) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Session features.
        }

        @Override
        void onSessionResult(int seq, SessionResult result) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Session features.
        }

        @Override
        void onLibraryResult(int seq, LibraryResult result) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Browser features.
        }

        @Override
        void onPlayerChanged(int seq,
                @Nullable SessionPlayer oldPlayer,
                @Nullable MediaController.PlaybackInfo oldPlaybackInfo,
                @NonNull SessionPlayer player,
                @NonNull MediaController.PlaybackInfo playbackInfo)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void setCustomLayout(int seq, @NonNull List<CommandButton> layout)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaybackInfoChanged(int seq, @NonNull PlaybackInfo info)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onAllowedCommandsChanged(int seq, @NonNull SessionCommandGroup commands)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void sendCustomCommand(int seq, @NonNull SessionCommand command, Bundle args)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlayerStateChanged(int seq, long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaybackSpeedChanged(int seq, long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onBufferingStateChanged(int seq, @NonNull MediaItem item, int bufferingState,
                long bufferedPositionMs, long eventTimeMs, long positionMs) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onSeekCompleted(int seq, long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onCurrentMediaItemChanged(int seq, MediaItem item, int currentIdx,
                int previousIdx, int nextIdx) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaylistChanged(int seq, @NonNull List<MediaItem> playlist,
                MediaMetadata metadata, int currentIdx, int previousIdx, int nextIdx)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaylistMetadataChanged(int seq, MediaMetadata metadata)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onShuffleModeChanged(int seq, int shuffleMode, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onRepeatModeChanged(int seq, int repeatMode, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaybackCompleted(int seq) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onDisconnected(int seq) throws RemoteException {
            // No-op. BrowserCompat doesn't have concept of receiving release of a session.
        }

        @Override
        void onVideoSizeChanged(int seq, @NonNull VideoSize videoSize) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        void onTracksChanged(int seq, List<TrackInfo> tracks,
                TrackInfo selectedVideoTrack, TrackInfo selectedAudioTrack,
                TrackInfo selectedSubtitleTrack, TrackInfo selectedMetadataTrack)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onTrackSelected(int seq, TrackInfo trackInfo)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onTrackDeselected(int seq, TrackInfo trackInfo)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onSubtitleData(int seq, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
            // No-op. BrowserCompat doesn't understand Controller features.
        }
    }

    private final class BrowserLegacyCb extends BaseBrowserLegacyCb {
        private final Object mLock = new Object();
        private final RemoteUserInfo mRemoteUserInfo;

        @GuardedBy("mLock")
        private final List<SearchRequest> mSearchRequests = new ArrayList<>();

        BrowserLegacyCb(RemoteUserInfo remoteUserInfo) {
            mRemoteUserInfo = remoteUserInfo;
        }

        @Override
        void onChildrenChanged(int seq, @NonNull String parentId, int itemCount,
                LibraryParams params) throws RemoteException {
            Bundle extras = params != null ? params.getExtras() : null;
            notifyChildrenChanged(mRemoteUserInfo, parentId, extras);
        }

        @Override
        @SuppressWarnings("ObjectToString")
        void onSearchResultChanged(int seq, @NonNull String query, int itemCount,
                LibraryParams params) throws RemoteException {
            // In MediaLibrarySession/MediaBrowser, we have two different APIs for getting size of
            // search result (and also starting search) and getting result.
            // However, MediaBrowserService/MediaBrowserCompat only have one search API for getting
            // search result.
            final List<SearchRequest> searchRequests = new ArrayList<>();
            synchronized (mLock) {
                for (int i = mSearchRequests.size() - 1; i >= 0; i--) {
                    SearchRequest iter = mSearchRequests.get(i);
                    if (ObjectsCompat.equals(mRemoteUserInfo, iter.mRemoteUserInfo)
                            && iter.mQuery.equals(query)) {
                        searchRequests.add(iter);
                        mSearchRequests.remove(i);
                    }
                }
                if (searchRequests.size() == 0) {
                    if (DEBUG) {
                        Log.d(TAG, "search() hasn't called by " + mRemoteUserInfo
                                + " with query=" + query);
                    }
                    return;
                }
            }

            mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < searchRequests.size(); i++) {
                        SearchRequest request = searchRequests.get(i);
                        int page = 0;
                        int pageSize = Integer.MAX_VALUE;
                        if (request.mExtras != null) {
                            try {
                                request.mExtras.setClassLoader(
                                        mLibrarySessionImpl.getContext().getClassLoader());
                                page = request.mExtras.getInt(MediaBrowserCompat.EXTRA_PAGE, -1);
                                pageSize = request.mExtras
                                        .getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1);
                            } catch (BadParcelableException e) {
                                request.mResult.sendResult(null);
                                return;
                            }
                        }
                        if (page < 0 || pageSize < 1) {
                            page = 0;
                            pageSize = Integer.MAX_VALUE;
                        }
                        LibraryParams params = MediaUtils.convertToLibraryParams(
                                mLibrarySessionImpl.getContext(), request.mExtras);
                        LibraryResult libraryResult  = mLibrarySessionImpl.getCallback()
                                .onGetSearchResult(mLibrarySessionImpl.getInstance(),
                                        request.mController, request.mQuery, page, pageSize,
                                        params);
                        if (libraryResult == null
                                || libraryResult.getResultCode() != RESULT_SUCCESS) {
                            request.mResult.sendResult(null);
                        } else {
                            request.mResult.sendResult(
                                    MediaUtils.truncateListBySize(
                                            MediaUtils.convertToMediaItemList(
                                                    libraryResult.getMediaItems()),
                                    TRANSACTION_SIZE_LIMIT_IN_BYTES));
                        }
                    }
                }
            });
        }

        void registerSearchRequest(ControllerInfo controller, String query, Bundle extras,
                Result<List<MediaBrowserCompat.MediaItem>> result) {
            synchronized (mLock) {
                mSearchRequests.add(new SearchRequest(controller, controller.getRemoteUserInfo(),
                        query, extras, result));
            }
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mRemoteUserInfo);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BrowserLegacyCb)) {
                return false;
            }
            BrowserLegacyCb other = (BrowserLegacyCb) obj;
            return ObjectsCompat.equals(mRemoteUserInfo, other.mRemoteUserInfo);
        }
    }

    // Intentionally static class to prevent lint warning 'SyntheticAccessor' in constructor.
    private static class BrowserLegacyCbForBroadcast extends BaseBrowserLegacyCb {
        private final MediaBrowserServiceCompat mService;

        BrowserLegacyCbForBroadcast(MediaBrowserServiceCompat service) {
            mService = service;
        }

        @Override
        void onChildrenChanged(int seq, @NonNull String parentId, int itemCount,
                LibraryParams libraryParams) throws RemoteException {
            // This will trigger {@link MediaLibraryServiceLegacyStub#onLoadChildren}.
            if (libraryParams == null || libraryParams.getExtras() == null) {
                mService.notifyChildrenChanged(parentId);
            } else {
                mService.notifyChildrenChanged(parentId, libraryParams.getExtras());
            }
        }

        @Override
        void onSearchResultChanged(int seq, @NonNull String query, int itemCount,
                LibraryParams params) throws RemoteException {
            // Shouldn't be called. If it's called, it's bug.
            // This method in the base class is introduced to internally send return of
            // {@link MediaLibrarySessionCallback#onSearchResultChanged}. However, for
            // BrowserCompat, it should be done by {@link Result#sendResult} from
            // {@link MediaLibraryServiceLegacyStub#onSearch} instead.
            if (DEBUG) {
                throw new RuntimeException("Unexpected API call. Use result.sendResult() for"
                        + " sending onSearchResultChanged() result instead of this");
            }
        }
    }
}
