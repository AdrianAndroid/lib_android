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

package androidx.car.app.managers;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;

/**
 * A map of {@link Manager}s. They are created lazily by invoking the corresponding
 * {@link ManagerFactory}
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ManagerCache {
    private final Map<Class<? extends Manager>, Manager> mValues = new HashMap<>();
    private final Map<Class<? extends Manager>, RuntimeException> mExceptions = new HashMap<>();
    private final Map<Class<? extends Manager>, ManagerFactory<?>> mFactories = new HashMap<>();
    private final Map<String, Class<? extends Manager>> mClassByName = new HashMap<>();
    private final Map<Class<? extends Manager>, String> mNameByClass = new HashMap<>();

    /**
     * Adds a {@link ManagerFactory} to this container
     *
     * @param clazz   {@link Manager} class this factory is able to produce
     * @param name    name of the given {@link Manager}, or {@code null} if this {@link Manager} is
     *                for internal library use only
     * @param factory a factory able to produce an instance of this type of manager
     * @param <T>     the type of {@link Manager} being added to the container
     */
    public <T extends Manager> void addFactory(@NonNull Class<T> clazz,
            @Nullable String name, @NonNull ManagerFactory<T> factory) {
        mFactories.put(clazz, factory);
        if (name != null) {
            mClassByName.put(name, clazz);
            mNameByClass.put(clazz, name);
        }
    }

    /**
     * Returns the name of a {@link Manager} identified by its class
     *
     * @throws IllegalArgumentException if the name is unknown
     */
    @NonNull
    public String getName(@NonNull Class<? extends Manager> clazz) {
        String name = mNameByClass.get(clazz);
        if (name == null) {
            throw new IllegalArgumentException("The class does not correspond to a car service");
        }
        return name;
    }

    /**
     * Returns an instance of a {@link Manager} identified by its name
     *
     * @throws IllegalArgumentException if the name is unknown
     * @throws IllegalStateException if the given manager can not be instantiated
     */
    @NonNull
    public Manager getOrCreate(@NonNull String name) throws IllegalArgumentException {
        Class<? extends Manager> clazz = mClassByName.get(name);
        if (clazz == null) {
            throw new IllegalArgumentException(
                    "The name '" + name + "' does not correspond to a car service");
        }
        return getOrCreate(clazz);
    }

    /**
     * Returns an instance of a {@link Manager} identified by its class
     *
     * @throws IllegalArgumentException if the class is unknown
     * @throws IllegalStateException if the given manager can not be instantiated
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <T extends Manager> T getOrCreate(@NonNull Class<T> clazz) {
        RuntimeException ex = mExceptions.get(clazz);
        if (ex != null) {
            throw ex;
        }
        T value = (T) mValues.get(clazz);
        if (value != null) {
            return (T) value;
        }
        ManagerFactory<T> factory = (ManagerFactory<T>) mFactories.get(clazz);
        if (factory == null) {
            throw new IllegalArgumentException("The class '" + clazz + "' does not correspond to a "
                    + "car service");
        }
        try {
            value = factory.create();
            mValues.put(clazz, value);
            return value;
        } catch (RuntimeException newEx) {
            mExceptions.put(clazz, newEx);
            throw newEx;
        }
    }
}
