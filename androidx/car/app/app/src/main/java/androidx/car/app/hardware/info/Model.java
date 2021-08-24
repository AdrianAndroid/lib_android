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
package androidx.car.app.hardware.info;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarValue;

import java.util.Objects;

/**
 * Information about the androidx.car.app.hardware model such as name, year and manufacturer.
 */
@CarProtocol
@RequiresCarApi(3)
public final class Model {
    @Keep
    @NonNull
    private final CarValue<String> mName;

    @Keep
    @NonNull
    private final CarValue<Integer> mYear;

    @Keep
    @NonNull
    private final CarValue<String> mManufacturer;

    /** Returns the car model name. */
    @NonNull
    public CarValue<String> getName() {
        return requireNonNull(mName);
    }

    /** Returns the car model year. */
    @NonNull
    public CarValue<Integer> getYear() {
        return requireNonNull(mYear);
    }

    /** Returns the car manufacturer. */
    @NonNull
    public CarValue<String> getManufacturer() {
        return requireNonNull(mManufacturer);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ name: " + mName + ", year: " + mYear + ", manufacturer: " + mManufacturer + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mName, mYear, mManufacturer);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Model)) {
            return false;
        }
        Model otherModel = (Model) other;

        return Objects.equals(mName, otherModel.mName)
                && Objects.equals(mYear, otherModel.mYear)
                && Objects.equals(mManufacturer, otherModel.mManufacturer);
    }

    Model(Builder builder) {
        mName = requireNonNull(builder.mName);
        mManufacturer = requireNonNull(builder.mManufacturer);
        mYear = requireNonNull(builder.mYear);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Model() {
        mName = CarValue.UNIMPLEMENTED_STRING;
        mManufacturer = CarValue.UNIMPLEMENTED_STRING;
        mYear = CarValue.UNIMPLEMENTED_INTEGER;
    }

    /** A builder of {@link Model}. */
    public static final class Builder {
        CarValue<String> mName = CarValue.UNIMPLEMENTED_STRING;
        CarValue<Integer> mYear = CarValue.UNIMPLEMENTED_INTEGER;
        CarValue<String> mManufacturer = CarValue.UNIMPLEMENTED_STRING;

        /**
         * Sets the car model name.
         *
         * @throws NullPointerException if {@code name} is {@code null}
         */
        @NonNull
        public Builder setName(@NonNull CarValue<String> name) {
            mName = requireNonNull(name);
            return this;
        }

        /**
         * Sets the car model year.
         *
         * @throws NullPointerException if {@code year} is {@code null}
         */
        @NonNull
        public Builder setYear(@NonNull CarValue<Integer> year) {
            mYear = requireNonNull(year);
            return this;
        }

        /**
         * Sets the car manufacturer.
         *
         * @throws NullPointerException if {@code manufacturer} is {@code null}
         */
        @NonNull
        public Builder setManufacturer(@NonNull CarValue<String> manufacturer) {
            mManufacturer = requireNonNull(manufacturer);
            return this;
        }

        /**
         * Constructs the {@link Model} defined by this builder.
         */
        @NonNull
        public Model build() {
            return new Model(this);
        }

    }
}
