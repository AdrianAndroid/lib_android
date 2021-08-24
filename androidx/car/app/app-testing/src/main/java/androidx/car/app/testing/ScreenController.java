/*
 * Copyright 2020 The Android Open Source Project
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

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.Screen;
import androidx.car.app.model.Template;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A controller that allows testing of a {@link Screen}.
 *
 * <p>This controller will allows:
 *
 * <ul>
 *   <li>Moving a {@link Screen} through its different {@link State}s.
 *   <li>Retrieving all {@link Template}s returned from {@link Screen#onGetTemplate}. The values can
 *       be reset with {@link #reset}.
 * </ul>
 */
@SuppressWarnings("NotCloseable")
public class ScreenController {
    private final TestCarContext mTestCarContext;
    private final Screen mScreen;

    /**
     * Creates a ScreenController to control a {@link Screen} for testing.
     *
     * @throws NullPointerException if either {@code testCarContext} or {@code screen} are null
     */
    @NonNull
    public static ScreenController of(
            @NonNull TestCarContext testCarContext, @NonNull Screen screen) {
        return new ScreenController(requireNonNull(screen), requireNonNull(testCarContext));
    }

    /** Resets values tracked by this {@link ScreenController}. */
    public void reset() {
        mTestCarContext.getCarService(TestAppManager.class).resetTemplatesStoredForScreen(get());
    }

    /**
     * Returns all the {@link Template}s returned from {@link Screen#onGetTemplate} for the {@link
     * Screen} being controlled.
     *
     * <p>The templates are stored in the order in which they were returned from
     * {@link Screen#onGetTemplate}, where the first template in the list, is the first template
     * returned.
     *
     * <p>The templates will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<Template> getTemplatesReturned() {
        List<Template> templates = new ArrayList<>();
        for (Pair<Screen, Template> pair :
                mTestCarContext.getCarService(TestAppManager.class).getTemplatesReturned()) {
            if (pair.first == get()) {
                templates.add(pair.second);
            }
        }
        return templates;
    }

    /**
     * Returns the result that was set via {@link Screen#setResult(Object)}, or {@code null} if
     * one was not set.
     */
    @Nullable
    public Object getScreenResult() {
        try {
            Field result = Screen.class.getDeclaredField("mResult");
            result.setAccessible(true);
            return result.get(mScreen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access result from screen being "
                    + "controlled", e);
        }
    }

    /**
     * Creates the {@link Screen} being controlled.
     *
     * <p>This method will also push the {@link Screen} onto the {@link
     * androidx.car.app.ScreenManager}'s screen stack if it isn't the current top.
     */
    @NonNull
    public ScreenController create() {
        putScreenOnStackIfNotTop();

        dispatchLifecycleEvent(Event.ON_CREATE);
        return this;
    }

    /**
     * Starts the {@link Screen} being controlled.
     *
     * <p>This method will also push the {@link Screen} onto the {@link
     * androidx.car.app.ScreenManager}'s screen stack if it isn't the current top.
     *
     * @see Screen#getLifecycle
     */
    @NonNull
    public ScreenController start() {
        putScreenOnStackIfNotTop();

        dispatchLifecycleEvent(Event.ON_START);
        return this;
    }

    /**
     * Resumes the {@link Screen} being controlled.
     *
     * <p>This method will also push the {@link Screen} onto the {@link
     * androidx.car.app.ScreenManager}'s screen stack if it isn't the current top.
     *
     * @see Screen#getLifecycle
     */
    @NonNull
    public ScreenController resume() {
        putScreenOnStackIfNotTop();

        dispatchLifecycleEvent(Event.ON_RESUME);
        return this;
    }

    /**
     * Pauses the {@link Screen} being controlled.
     *
     * @see Screen#getLifecycle
     */
    @NonNull
    public ScreenController pause() {
        dispatchLifecycleEvent(Event.ON_PAUSE);
        return this;
    }

    /**
     * Stops to the {@link Screen} being controlled.
     *
     * @see Screen#getLifecycle
     */
    @NonNull
    public ScreenController stop() {
        dispatchLifecycleEvent(Event.ON_STOP);
        return this;
    }

    /**
     * Destroys to the {@link Screen} being controlled.
     *
     * @see Screen#getLifecycle
     */
    @NonNull
    public ScreenController destroy() {
        dispatchLifecycleEvent(Event.ON_DESTROY);
        return this;
    }

    /** Returns the {@link Screen} being controlled. */
    @NonNull
    public Screen get() {
        return mScreen;
    }

    private void putScreenOnStackIfNotTop() {
        TestScreenManager testScreenManager = mTestCarContext.getCarService(
                TestScreenManager.class);
        if (!testScreenManager.hasScreens() || !mScreen.equals(testScreenManager.getTop())) {
            testScreenManager.push(mScreen);
        }
    }

    private ScreenController(Screen screen, TestCarContext testCarContext) {
        this.mScreen = screen;
        this.mTestCarContext = testCarContext;

        // Use reflection to inject the TestCarContext into the Screen.
        try {
            Field field = Screen.class.getDeclaredField("mCarContext");
            field.setAccessible(true);
            field.set(screen, testCarContext);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set a test car context for testing", e);
        }
    }

    @SuppressLint("BanUncheckedReflection")
    private void dispatchLifecycleEvent(Event event) {
        // Use reflection to call internal APIs for testing purposes.
        try {
            Method method = Screen.class.getDeclaredMethod("dispatchLifecycleEvent", Event.class);
            method.setAccessible(true);
            method.invoke(mScreen, event);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed dispatching lifecycle event", e);
        }
    }
}
