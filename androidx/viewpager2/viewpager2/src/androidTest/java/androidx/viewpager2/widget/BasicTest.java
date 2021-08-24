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

package androidx.viewpager2.widget;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.viewpager2.test.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.UUID;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BasicTest {
    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void test_childrenNotAllowed() {
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("ViewPager2 does not support direct child views");

        Context context = ApplicationProvider.getApplicationContext();
        ViewPager2 viewPager = new ViewPager2(context);
        viewPager.addView(new View(context));
    }

    @Test
    public void test_saveStateParcel_createRestore() {
        // given
        Bundle superState = createIntBundle(42);
        ViewPager2.SavedState state = new ViewPager2.SavedState(superState);
        state.mRecyclerViewId = 700;

        Bundle adapterState = new Bundle(1);
        adapterState.putParcelableArray("adapterState",
                new Parcelable[]{createIntBundle(1), createIntBundle(2), createIntBundle(3)});
        state.mAdapterState = adapterState;

        // when
        Parcel parcel = Parcel.obtain();
        state.writeToParcel(parcel, 0);
        final String parcelSuffix = UUID.randomUUID().toString();
        parcel.writeString(parcelSuffix); // to verify parcel boundaries
        parcel.setDataPosition(0);
        ViewPager2.SavedState recreatedState = ViewPager2.SavedState.CREATOR.createFromParcel(
                parcel);

        // then
        assertThat("Parcel reading should not go out of bounds", parcel.readString(),
                equalTo(parcelSuffix));
        assertThat("All of the parcel should be read", parcel.dataAvail(), equalTo(0));
        assertThat(recreatedState.mRecyclerViewId, equalTo(700));
        Parcelable[] recreatedAdapterState =
                ((Bundle) recreatedState.mAdapterState).getParcelableArray("adapterState");
        assertThat(recreatedAdapterState, arrayWithSize(3));
        assertThat((int) ((Bundle) recreatedState.getSuperState()).get("key"), equalTo(42));
        //noinspection ConstantConditions
        assertThat((int) ((Bundle) recreatedAdapterState[0]).get("key"), equalTo(1));
        assertThat((int) ((Bundle) recreatedAdapterState[1]).get("key"), equalTo(2));
        assertThat((int) ((Bundle) recreatedAdapterState[2]).get("key"), equalTo(3));
    }

    @Test
    public void test_inflateWithScrollbars() {
        // when
        LayoutInflater.from(ApplicationProvider.getApplicationContext()).inflate(
                R.layout.vertical_scrollbars, null);
        // then it shouldn't crash
    }

    private Bundle createIntBundle(int value) {
        Bundle bundle = new Bundle(1);
        bundle.putInt("key", value);
        return bundle;
    }
}
