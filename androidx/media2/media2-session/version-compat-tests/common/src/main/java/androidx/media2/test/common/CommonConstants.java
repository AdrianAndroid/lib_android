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

package androidx.media2.test.common;

import android.content.ComponentName;

public class CommonConstants {

    public static final String SERVICE_PACKAGE_NAME = "androidx.media2.test.service.test";
    public static final String CLIENT_PACKAGE_NAME = "androidx.media2.test.client.test";

    public static final ComponentName MEDIA2_SESSION_PROVIDER_SERVICE = new ComponentName(
            SERVICE_PACKAGE_NAME, "androidx.media2.test.service.MediaSessionProviderService");
    public static final ComponentName MEDIA2_CONTROLLER_PROVIDER_SERVICE = new ComponentName(
            CLIENT_PACKAGE_NAME, "androidx.media2.test.client.MediaControllerProviderService");

    public static final ComponentName MEDIA_SESSION_COMPAT_PROVIDER_SERVICE = new ComponentName(
            SERVICE_PACKAGE_NAME, "androidx.media2.test.service.MediaSessionCompatProviderService");
    public static final ComponentName MEDIA_CONTROLLER_COMPAT_PROVIDER_SERVICE = new ComponentName(
            CLIENT_PACKAGE_NAME,
            "androidx.media2.test.client.MediaControllerCompatProviderService");
    public static final ComponentName MEDIA_BROWSER_COMPAT_PROVIDER_SERVICE = new ComponentName(
            CLIENT_PACKAGE_NAME, "androidx.media2.test.client.MediaBrowserCompatProviderService");


    public static final ComponentName MOCK_MEDIA2_SESSION_SERVICE = new ComponentName(
            SERVICE_PACKAGE_NAME, "androidx.media2.test.service.MockMediaSessionService");
    public static final ComponentName MOCK_MEDIA2_LIBRARY_SERVICE = new ComponentName(
            SERVICE_PACKAGE_NAME, "androidx.media2.test.service.MockMediaLibraryService");

    public static final String ACTION_MEDIA2_SESSION = "androidx.media2.test.action.MEDIA2_SESSION";
    public static final String ACTION_MEDIA2_CONTROLLER =
            "androidx.media2.test.action.MEDIA2_CONTROLLER";
    public static final String ACTION_MEDIA_SESSION_COMPAT =
            "androidx.media2.test.action.MEDIA_SESSION_COMPAT";
    public static final String ACTION_MEDIA_CONTROLLER_COMPAT =
            "androidx.media2.test.action.MEDIA_CONTROLLER_COMPAT";
    public static final String ACTION_MEDIA_BROWSER_COMPAT =
            "androidx.media2.test.action.MEDIA_BROWSER_COMPAT";

    public static final String VERSION_TOT = "tot";
    public static final String VERSION_PREVIOUS = "previous";

    // Keys for arguments.
    public static final String KEY_CLIENT_VERSION = "client_version";
    public static final String KEY_SERVICE_VERSION = "service_version";
    public static final String KEY_AUDIO_ATTRIBUTES = "audioAttributes";
    public static final String KEY_PLAYER_STATE = "playerState";
    public static final String KEY_PLAYLIST = "playlist";
    public static final String KEY_CURRENT_POSITION = "currentPosition";
    public static final String KEY_BUFFERED_POSITION = "bufferedPosition";
    public static final String KEY_BUFFERING_STATE = "bufferingState";
    public static final String KEY_PLAYBACK_SPEED = "playbackSpeed";
    public static final String KEY_MEDIA_ITEM = "mediaItem";
    public static final String KEY_PLAYLIST_METADATA = "playlistMetadata";
    public static final String KEY_ARGUMENTS = "arguments";
    public static final String KEY_RESULT_RECEIVER = "resultReceiver";
    public static final String KEY_MAX_VOLUME = "maxVolume";
    public static final String KEY_CURRENT_VOLUME = "currentVolume";
    public static final String KEY_VOLUME_CONTROL_TYPE = "volumeControlType";
    public static final String KEY_VIDEO_SIZE = "videoSize";
    public static final String KEY_TRACK_INFO = "trackInfo";
    public static final String KEY_SHUFFLE_MODE = "shuffleMode";
    public static final String KEY_REPEAT_MODE = "repeatMode";

    // SessionCompat arguments
    public static final String KEY_SESSION_COMPAT_TOKEN = "sessionCompatToken";
    public static final String KEY_PLAYBACK_STATE_COMPAT = "playbackStateCompat";
    public static final String KEY_METADATA_COMPAT = "metadataCompat";
    public static final String KEY_QUEUE = "queue";

    public static final int INDEX_FOR_UNKONWN_ITEM = -1;
    public static final int INDEX_FOR_NULL_ITEM = -2;

    // Default test name
    public static final String DEFAULT_TEST_NAME = "defaultTestName";

    private CommonConstants() {
    }
}
