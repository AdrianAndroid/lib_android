/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.testing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link SessionController}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class SessionControllerTest {
    private final Screen mScreen = new TestScreen();

    @Mock
    private DefaultLifecycleObserver mMockObserver;

    private SessionController mSessionController;
    private TestCarContext mCarContext;
    private Intent mIntent;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mCarContext = TestCarContext.createCarContext(
                ApplicationProvider.getApplicationContext());
        mIntent = new Intent().setComponent(new ComponentName(mCarContext,
                this.getClass()));

        Session session = new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return mScreen;
            }

        };

        mSessionController = SessionController.of(session, mCarContext);
        session.getLifecycle().addObserver(mMockObserver);
    }

    @Test
    public void create() {
        mSessionController.create(mIntent);

        verify(mMockObserver).onCreate(any());
    }

    @Test
    public void start() {
        mSessionController.create(mIntent).start();

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
    }

    @Test
    public void resume() {
        mSessionController.create(mIntent).resume();

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
        verify(mMockObserver).onResume(any());
    }

    @Test
    public void pause() {
        mSessionController.create(mIntent).resume().pause();

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
        verify(mMockObserver).onResume(any());
        verify(mMockObserver).onPause(any());
    }

    @Test
    public void stop() {
        mSessionController.create(mIntent).resume().stop();

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
        verify(mMockObserver).onResume(any());
        verify(mMockObserver).onPause(any());
        verify(mMockObserver).onStop(any());
    }

    @Test
    public void destroy() {
        mSessionController.create(mIntent).resume().destroy();

        verify(mMockObserver).onCreate(any());
        verify(mMockObserver).onStart(any());
        verify(mMockObserver).onResume(any());
        verify(mMockObserver).onPause(any());
        verify(mMockObserver).onStop(any());
        verify(mMockObserver).onDestroy(any());
    }


    /** A no-op screen for testing. */
    private static class TestScreen extends Screen {
        private TestScreen() {
            super(TestCarContext.createCarContext(ApplicationProvider.getApplicationContext()));
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            return new Template() {
            };
        }
    }
}
