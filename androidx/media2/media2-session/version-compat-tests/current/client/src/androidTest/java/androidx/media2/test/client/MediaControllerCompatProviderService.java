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

package androidx.media2.test.client;

import static androidx.media2.test.common.CommonConstants.ACTION_MEDIA_CONTROLLER_COMPAT;
import static androidx.media2.test.common.CommonConstants.KEY_ARGUMENTS;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.media2.test.common.IRemoteMediaControllerCompat;
import androidx.media2.test.common.TestUtils.SyncHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A Service that creates {@link MediaControllerCompat} and calls its methods
 * according to the service app's requests.
 */
public class MediaControllerCompatProviderService extends Service {
    private static final String TAG = "MediaControllerCompatProviderService";

    Map<String, MediaControllerCompat> mMediaControllerCompatMap = new HashMap<>();
    RemoteMediaControllerCompatStub mBinder;

    SyncHandler mHandler;
    Executor mExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new RemoteMediaControllerCompatStub();

        mHandler = new SyncHandler(getMainLooper());
        mExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                mHandler.post(command);
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (ACTION_MEDIA_CONTROLLER_COMPAT.equals(intent.getAction())) {
            return mBinder;
        }
        return null;
    }

    private class RemoteMediaControllerCompatStub extends IRemoteMediaControllerCompat.Stub {

        @Override
        public void create(String controllerId, Bundle tokenBundle, boolean waitForConnection) {
            MediaSessionCompat.Token token = (MediaSessionCompat.Token) getParcelable(tokenBundle);
            MediaControllerCompat controller = new MediaControllerCompat(
                    MediaControllerCompatProviderService.this, token);

            final TestControllerCallback callback = new TestControllerCallback();
            controller.registerCallback(callback, mHandler);

            mMediaControllerCompatMap.put(controllerId, controller);

            if (!waitForConnection) {
                return;
            }

            boolean connected = false;
            try {
                connected = callback.mConnectionLatch.await(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException occurred while waiting for connection", ex);
            }

            if (!connected) {
                Log.e(TAG, "Could not connect to the given session.");
            }
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaControllerCompat methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void addQueueItem(String controllerId, Bundle descriptionBundle)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            MediaDescriptionCompat desc = (MediaDescriptionCompat) getParcelable(descriptionBundle);
            controller.addQueueItem(desc);
        }

        @Override
        public void addQueueItemWithIndex(String controllerId, Bundle descriptionBundle, int index)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            MediaDescriptionCompat desc = (MediaDescriptionCompat) getParcelable(descriptionBundle);
            controller.addQueueItem(desc, index);
        }

        @Override
        public void removeQueueItem(String controllerId, Bundle descriptionBundle)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            MediaDescriptionCompat desc = (MediaDescriptionCompat) getParcelable(descriptionBundle);
            controller.removeQueueItem(desc);
        }

        @Override
        public void setVolumeTo(String controllerId, int value, int flags)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.setVolumeTo(value, flags);
        }

        @Override
        public void adjustVolume(String controllerId, int direction, int flags)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.adjustVolume(direction, flags);
        }

        @Override
        public void sendCommand(String controllerId, String command, Bundle params,
                ResultReceiver cb) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.sendCommand(command, params, cb);
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaControllerCompat.TransportControls methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void prepare(String controllerId) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().prepare();
        }

        @Override
        public void prepareFromMediaId(String controllerId, String mediaId, Bundle extras)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().prepareFromMediaId(mediaId, extras);
        }

        @Override
        public void prepareFromSearch(String controllerId, String query, Bundle extras)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().prepareFromSearch(query, extras);
        }

        @Override
        public void prepareFromUri(String controllerId, Uri uri, Bundle extras)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().prepareFromUri(uri, extras);
        }

        @Override
        public void play(String controllerId) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().play();
        }

        @Override
        public void playFromMediaId(String controllerId, String mediaId, Bundle extras)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().playFromMediaId(mediaId, extras);
        }

        @Override
        public void playFromSearch(String controllerId, String query, Bundle extras)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().playFromSearch(query, extras);
        }

        @Override
        public void playFromUri(String controllerId, Uri uri, Bundle extras)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().playFromUri(uri, extras);
        }

        @Override
        public void skipToQueueItem(String controllerId, long id) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().skipToQueueItem(id);
        }

        @Override
        public void pause(String controllerId) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().pause();
        }

        @Override
        public void stop(String controllerId) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().stop();
        }

        @Override
        public void seekTo(String controllerId, long pos) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().seekTo(pos);
        }

        @Override
        public void setPlaybackSpeed(String controllerId, float speed) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().setPlaybackSpeed(speed);
        }

        @Override
        public void fastForward(String controllerId) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().fastForward();
        }

        @Override
        public void skipToNext(String controllerId) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().skipToNext();
        }

        @Override
        public void rewind(String controllerId) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().rewind();
        }

        @Override
        public void skipToPrevious(String controllerId) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().skipToPrevious();
        }

        @Override
        public void setRating(String controllerId, Bundle ratingBundle) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            RatingCompat rating = (RatingCompat) getParcelable(ratingBundle);
            controller.getTransportControls().setRating(rating);
        }

        @Override
        public void setRatingWithExtras(String controllerId, Bundle ratingBundle, Bundle extras)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            RatingCompat rating = (RatingCompat) getParcelable(ratingBundle);
            controller.getTransportControls().setRating(rating, extras);
        }

        @Override
        public void setCaptioningEnabled(String controllerId, boolean enabled)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().setCaptioningEnabled(enabled);
        }

        @Override
        public void setRepeatMode(String controllerId, int repeatMode) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().setRepeatMode(repeatMode);
        }

        @Override
        public void setShuffleMode(String controllerId, int shuffleMode) throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().setShuffleMode(shuffleMode);
        }

        @Override
        public void sendCustomAction(String controllerId, Bundle customActionBundle, Bundle args)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            PlaybackStateCompat.CustomAction customAction =
                    (PlaybackStateCompat.CustomAction) getParcelable(customActionBundle);
            controller.getTransportControls().sendCustomAction(customAction, args);
        }

        @Override
        public void sendCustomActionWithName(String controllerId, String action, Bundle args)
                throws RemoteException {
            MediaControllerCompat controller = mMediaControllerCompatMap.get(controllerId);
            controller.getTransportControls().sendCustomAction(action, args);
        }

        private Parcelable getParcelable(Bundle bundle) {
            bundle.setClassLoader(MediaSessionCompat.class.getClassLoader());
            return bundle.getParcelable(KEY_ARGUMENTS);
        }
    }

    private class TestControllerCallback extends MediaControllerCompat.Callback {
        private CountDownLatch mConnectionLatch = new CountDownLatch(1);

        @Override
        public void onSessionReady() {
            super.onSessionReady();
            mConnectionLatch.countDown();
        }
    }
}
