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

package androidx.car.app.model;

import static androidx.car.app.model.CarIcon.ALERT;
import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.TestUtils;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link Row}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RowTest {
    @Test
    public void create_defaultValues() {
        Row row = new Row.Builder().setTitle("Title").build();
        assertThat(row.getTitle().toString()).isEqualTo("Title");
        assertThat(row.getTexts()).isEmpty();
        assertThat(row.getImage()).isNull();
        assertThat(row.getOnClickDelegate()).isNull();
        assertThat(row.isBrowsable()).isFalse();
        assertThat(row.getMetadata()).isEqualTo(Metadata.EMPTY_METADATA);
        assertThat(row.getRowImageType()).isEqualTo(Row.IMAGE_TYPE_SMALL);
    }

    @Test
    public void title_charSequence() {
        String title = "foo";
        Row row = new Row.Builder().setTitle(title).build();
        assertThat(CarText.create(title)).isEqualTo(row.getTitle());
    }

    @Test
    public void text_charSequence() {
        String text1 = "foo";
        String text2 = "bar";
        Row row = new Row.Builder().setTitle("Title").addText(text1).addText(text2).build();
        assertThat(row.getTexts()).containsExactly(CarText.create(text1), CarText.create(text2));
    }

    @Test
    public void title_text_variants() {
        List<CharSequence> titleVariants = new ArrayList<>();
        titleVariants.add("foo");
        titleVariants.add("foo long");

        List<CharSequence> textVariants = new ArrayList<>();
        textVariants.add("bar");
        textVariants.add("bar long");

        CarText title =
                new CarText.Builder(titleVariants.get(0)).addVariant(titleVariants.get(1)).build();
        CarText text = new CarText.Builder(textVariants.get(0)).addVariant(
                textVariants.get(1)).build();
        Row row = new Row.Builder().setTitle(title).addText(text).build();
        assertThat(title).isEqualTo(row.getTitle());
        assertThat(row.getTexts()).containsExactly(text);
    }

    @Test
    public void setImage() {
        CarIcon image1 = BACK;
        Row row = new Row.Builder().setTitle("Title").setImage(image1).build();
        assertThat(image1).isEqualTo(row.getImage());
    }

    @Test
    public void setToggle() {
        Toggle toggle1 = new Toggle.Builder(isChecked -> {
        }).build();
        Row row = new Row.Builder().setTitle("Title").setToggle(toggle1).build();
        assertThat(toggle1).isEqualTo(row.getToggle());
    }

    @Test
    public void setOnClickListenerAndToggle_throws() {
        Toggle toggle1 = new Toggle.Builder(isChecked -> {
        }).build();
        assertThrows(
                IllegalStateException.class,
                () ->
                        new Row.Builder()
                                .setTitle("Title")
                                .setOnClickListener(() -> {
                                })
                                .setToggle(toggle1)
                                .build());
    }

    @Test
    public void clickListener() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Row row = new Row.Builder().setTitle("Title").setOnClickListener(onClickListener).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        row.getOnClickDelegate().sendClick(onDoneCallback);
        verify(onClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void setMetadata() {
        Metadata metadata =
                new Metadata.Builder().setPlace(
                        new Place.Builder(CarLocation.create(1, 1)).build()).build();
        Row row = new Row.Builder().setTitle("Title").setMetadata(metadata).build();
        assertThat(row.getMetadata()).isEqualTo(metadata);
    }

    @Test
    public void setIsBrowsable_noListener_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new Row.Builder().setTitle("Title").setBrowsable(true).build());

        // Positive case.
        new Row.Builder().setTitle("Title").setBrowsable(false).build();
    }

    @Test
    public void setIsBrowsable_notExclusivelyTextOrImage_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new Row.Builder()
                                .setTitle("Title")
                                .setBrowsable(true)
                                .setToggle(new Toggle.Builder(state -> {
                                }).build())
                                .build());

        // Positive case.
        new Row.Builder()
                .setBrowsable(true)
                .setOnClickListener(() -> {
                })
                .setTitle("Title")
                .addText("Text")
                .setImage(TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                        "ic_test_1"))
                .build();
    }

    @Test
    public void equals() {
        String title = "title";

        Row row =
                new Row.Builder()
                        .setTitle(title)
                        .setImage(BACK)
                        .setOnClickListener(() -> {
                        })
                        .setBrowsable(false)
                        .setMetadata(Metadata.EMPTY_METADATA)
                        .addText(title)
                        .build();

        assertThat(
                new Row.Builder()
                        .setTitle(title)
                        .setImage(BACK)
                        .setOnClickListener(() -> {
                        })
                        .setBrowsable(false)
                        .setMetadata(Metadata.EMPTY_METADATA)
                        .addText(title)
                        .build())
                .isEqualTo(row);
    }

    @Test
    public void notEquals_differentTitle() {
        String title = "title";

        Row row = new Row.Builder().setTitle(title).build();

        assertThat(new Row.Builder().setTitle("foo").build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentImage() {
        Row row = new Row.Builder().setTitle("Title").setImage(BACK).build();

        assertThat(new Row.Builder().setTitle("Title").setImage(ALERT).build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_oneHasNoCallback() {
        Row row = new Row.Builder().setTitle("Title").setOnClickListener(() -> {
        }).build();

        assertThat(new Row.Builder().setTitle("Title").build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentBrowsable() {
        Row row =
                new Row.Builder().setTitle("Title").setBrowsable(false).setOnClickListener(() -> {
                }).build();

        assertThat(
                new Row.Builder()
                        .setTitle("Title")
                        .setBrowsable(true)
                        .setOnClickListener(() -> {
                        })
                        .build())
                .isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentMetadata() {
        Row row = new Row.Builder().setTitle("Title").setMetadata(Metadata.EMPTY_METADATA).build();

        assertThat(
                new Row.Builder()
                        .setTitle("Title")
                        .setMetadata(
                                new Metadata.Builder()
                                        .setPlace(
                                                new Place.Builder(CarLocation.create(/* latitude= */
                                                        1f, /* longitude= */ 1f))
                                                        .build())
                                        .build())
                        .build())
                .isNotEqualTo(row);
    }

    @Test
    public void notEquals_differenText() {
        Row row = new Row.Builder().setTitle("Title").addText("foo").build();

        assertThat(new Row.Builder().setTitle("Title").addText("bar").build()).isNotEqualTo(row);
    }
}
