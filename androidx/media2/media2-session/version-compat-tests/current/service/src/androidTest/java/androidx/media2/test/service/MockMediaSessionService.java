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

import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.MediaSessionService;
import androidx.media2.session.SessionCommandGroup;

import java.util.concurrent.Executors;

public class MockMediaSessionService extends MediaSessionService {
    /**
     * ID of the session that this service will create.
     */
    public static final String ID = "TestSession";
    public MediaSession mSession2;

    @Override
    public void onCreate() {
        TestServiceRegistry.getInstance().setServiceInstance(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TestServiceRegistry.getInstance().cleanUp();
    }

    @Override
    public MediaSession onGetSession(@NonNull ControllerInfo controllerInfo) {
        TestServiceRegistry registry = TestServiceRegistry.getInstance();
        TestServiceRegistry.OnGetSessionHandler onGetSessionHandler =
                registry.getOnGetSessionHandler();
        if (onGetSessionHandler != null) {
            return onGetSessionHandler.onGetSession(controllerInfo);
        }

        if (mSession2 == null) {
            MediaSession.SessionCallback callback = registry.getSessionCallback();
            mSession2 = new MediaSession.Builder(MockMediaSessionService.this, new MockPlayer(0))
                    .setId(ID)
                    .setSessionCallback(Executors.newSingleThreadExecutor(),
                            callback != null ? callback : new TestSessionCallback())
                    .build();
        }
        return mSession2;
    }

    private class TestSessionCallback extends MediaSession.SessionCallback {
        @Override
        public SessionCommandGroup onConnect(@NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller) {
            if (TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName())) {
                return super.onConnect(session, controller);
            }
            return null;
        }
    }
}
