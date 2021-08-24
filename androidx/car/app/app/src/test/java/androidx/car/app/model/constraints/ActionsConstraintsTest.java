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

package androidx.car.app.model.constraints;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;

/** Tests for {@link ActionsConstraints}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ActionsConstraintsTest {
    @Test
    public void createEmpty() {
        ActionsConstraints constraints = new ActionsConstraints.Builder().build();

        assertThat(constraints.getMaxActions()).isEqualTo(Integer.MAX_VALUE);
        assertThat(constraints.getRequiredActionTypes()).isEmpty();
    }

    @Test
    public void create_requiredExceedsMaxAllowedActions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionsConstraints.Builder()
                        .setMaxActions(1)
                        .addRequiredActionType(Action.TYPE_BACK)
                        .addRequiredActionType(Action.TYPE_CUSTOM)
                        .build());
    }

    @Test
    public void create_requiredAlsoDisallowed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionsConstraints.Builder()
                        .addRequiredActionType(Action.TYPE_BACK)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .build());
    }

    @Test
    public void createConstraints() {
        ActionsConstraints constraints =
                new ActionsConstraints.Builder()
                        .setMaxActions(2)
                        .addRequiredActionType(Action.TYPE_CUSTOM)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .build();

        assertThat(constraints.getMaxActions()).isEqualTo(2);
        assertThat(constraints.getRequiredActionTypes()).containsExactly(Action.TYPE_CUSTOM);
        assertThat(constraints.getDisallowedActionTypes()).containsExactly(Action.TYPE_BACK);
    }

    @Test
    public void validateActions() {
        ActionsConstraints constraints =
                new ActionsConstraints.Builder()
                        .setMaxActions(2)
                        .setMaxCustomTitles(1)
                        .addRequiredActionType(Action.TYPE_CUSTOM)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .build();

        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action actionWithIcon = TestUtils.createAction(null, carIcon);
        Action actionWithTitle = TestUtils.createAction("Title", carIcon);

        // Positive case: instance that fits the 2-max-actions, only-1-has-title constraint.
        constraints.validateOrThrow(
                new ActionStrip.Builder()
                        .addAction(actionWithIcon)
                        .addAction(actionWithTitle)
                        .build()
                        .getActions());
        // Positive case: empty list is okay when there are no required types
        new ActionsConstraints.Builder().setMaxActions(2).build().validateOrThrow(
                Collections.emptyList());

        // Missing required type.
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder().addAction(
                                Action.APP_ICON).build().getActions()));

        // Disallowed type
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder().addAction(Action.BACK).build().getActions()));

        // Over max allowed actions
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(Action.APP_ICON)
                                .addAction(actionWithIcon)
                                .addAction(actionWithTitle)
                                .build()
                                .getActions()));

        // Over max allowed actions with title
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(actionWithTitle)
                                .addAction(actionWithTitle)
                                .build()
                                .getActions()));
    }
}
