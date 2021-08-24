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

package androidx.media2.test.service.tests;

import static androidx.media2.session.SessionResult.RESULT_ERROR_INVALID_STATE;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;
import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.Rating;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionResult;
import androidx.media2.session.StarRating;
import androidx.media2.test.common.TestUtils;
import androidx.media2.test.service.MediaTestUtils;
import androidx.media2.test.service.MockPlayer;
import androidx.media2.test.service.RemoteMediaController;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaSession.SessionCallback}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaSessionCallbackTest extends MediaSessionTestBase {
    private static final String TAG = "MediaSessionCallbackTest";

    MockPlayer mPlayer;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(1);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Test
    public void onPostConnect_afterConnected() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public void onPostConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                latch.countDown();
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPostConnect_afterConnected").build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onPostConnect_afterConnectionRejected() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                return null;
            }

            @Override
            public void onPostConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                latch.countDown();
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPostConnect_afterConnectionRejected").build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());
            assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onCommandRequest() throws InterruptedException {
        mPlayer = new MockPlayer(1);

        final MockOnCommandCallback callback = new MockOnCommandCallback();
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnCommandRequest")
                .build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());

            controller.pause();
            assertFalse(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertFalse(mPlayer.mPauseCalled);
            assertEquals(1, callback.commands.size());
            assertEquals(SessionCommand.COMMAND_CODE_PLAYER_PAUSE,
                    (long) callback.commands.get(0).getCommandCode());

            controller.play();
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mPlayCalled);
            assertFalse(mPlayer.mPauseCalled);
            assertEquals(2, callback.commands.size());
            assertEquals(SessionCommand.COMMAND_CODE_PLAYER_PLAY,
                    (long) callback.commands.get(1).getCommandCode());
        }
    }

    @Test
    public void onCreateMediaItem() throws InterruptedException {
        mPlayer = new MockPlayer(1);

        final List<String> list = MediaTestUtils.createMediaIds(3);
        final List<MediaItem> convertedList = MediaTestUtils.createPlaylist(list.size());

        final MockOnCommandCallback callback = new MockOnCommandCallback() {
            @Override
            public MediaItem onCreateMediaItem(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull String mediaId) {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(mediaId, list.get(i))) {
                        return convertedList.get(i);
                    }
                }
                fail();
                return null;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnCreateMediaItem")
                .build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());

            controller.setPlaylist(list, null);
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            List<MediaItem> playerList = mPlayer.getPlaylist();
            assertEquals(convertedList.size(), playerList.size());
            for (int i = 0; i < playerList.size(); i++) {
                String expected = convertedList.get(i).getMetadata().getString(
                        MediaMetadata.METADATA_KEY_MEDIA_ID);
                String actual = playerList.get(i).getMetadata().getString(
                        MediaMetadata.METADATA_KEY_MEDIA_ID);
                assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void onCustomCommand() throws InterruptedException {
        // TODO(jaewan): Need to revisit with the permission.
        final SessionCommand testCommand = new SessionCommand("testCustomCommand", null);
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "testOnCustomCommand");

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                SessionCommandGroup commands = new SessionCommandGroup.Builder()
                        .addAllPredefinedCommands(SessionCommand.COMMAND_VERSION_1)
                        .addCommand(testCommand)
                        .build();
                return commands;
            }

            @NonNull
            @Override
            public SessionResult onCustomCommand(@NonNull MediaSession session,
                    @NonNull MediaSession.ControllerInfo controller,
                    @NonNull SessionCommand sessionCommand, Bundle args) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testCommand, sessionCommand);
                assertTrue(TestUtils.equals(testArgs, args));
                latch.countDown();
                return new SessionResult(RESULT_SUCCESS, null);
            }
        };

        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnCustomCommand")
                .build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());
            controller.sendCustomCommand(testCommand, testArgs);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onFastForward() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public int onFastForward(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnFastForward").build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());
            controller.fastForward();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onRewind() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public int onRewind(@NonNull MediaSession session, @NonNull ControllerInfo controller) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnRewind").build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());
            controller.rewind();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onSkipForward() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public int onSkipForward(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnSkipForward").build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());
            controller.skipForward();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onSkipBackward() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public int onSkipBackward(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnSkipBackward").build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());
            controller.skipBackward();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onSetMediaUri() throws InterruptedException {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }

        final Uri testUri = Uri.parse("foo://boo");
        final Bundle testExtras = TestUtils.createTestBundle();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public int onSetMediaUri(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull Uri uri, @Nullable Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testUri, uri);
                assertTrue(TestUtils.equals(testExtras, extras));
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnSetMediaUri")
                .build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());

            controller.setMediaUri(testUri, testExtras);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onSetRating() throws InterruptedException {
        final float ratingValue = 3.5f;
        final Rating testRating = new StarRating(5, ratingValue);
        final String testMediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession.SessionCallback callback = new MediaSession.SessionCallback() {
            @Override
            public int onSetRating(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull String mediaId,
                    @NonNull Rating rating) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testMediaId, mediaId);
                assertEquals(testRating, rating);
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };

        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnSetRating").build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());

            controller.setRating(testMediaId, testRating);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onConnect() throws InterruptedException {
        final AtomicReference<Bundle> connectionHints = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("testOnConnect")
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        // TODO: Get uid of client app's and compare.
                        if (!CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return null;
                        }
                        connectionHints.set(controller.getConnectionHints());
                        latch.countDown();
                        return super.onConnect(session, controller);
                    }
                }).build()) {
            Bundle testConnectionHints = new Bundle();
            testConnectionHints.putString("test_key", "test_value");

            RemoteMediaController controller = createRemoteController(
                    session.getToken(), false  /* waitForConnection */, testConnectionHints);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(TestUtils.equals(testConnectionHints, connectionHints.get()));
        }
    }

    @Test
    public void onDisconnected() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("testOnDisconnected")
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() {
                    @Override
                    public void onDisconnected(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                        // TODO: Get uid of client app's and compare.
                        latch.countDown();
                    }
                }).build()) {
            RemoteMediaController controller = createRemoteController(session.getToken());
            controller.close();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }


    // TODO(jaewan): Add test for service connect rejection, when we differentiate session
    //               active/inactive and connection accept/refuse
    class TestSessionCallback extends MediaSession.SessionCallback {
        CountDownLatch mLatch;

        void resetLatchCount(int count) {
            mLatch = new CountDownLatch(count);
        }
    }

    public class MockOnCommandCallback extends MediaSession.SessionCallback {
        public final ArrayList<SessionCommand> commands = new ArrayList<>();

        @Override
        public int onCommandRequest(@NonNull MediaSession session,
                @NonNull ControllerInfo controllerInfo, @NonNull SessionCommand command) {
            // TODO: Get uid of client app's and compare.
            assertEquals(CLIENT_PACKAGE_NAME, controllerInfo.getPackageName());
            assertFalse(controllerInfo.isTrusted());
            commands.add(command);
            if (command.getCommandCode() == SessionCommand.COMMAND_CODE_PLAYER_PAUSE) {
                return RESULT_ERROR_INVALID_STATE;
            }
            return RESULT_SUCCESS;
        }
    }
}
