/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.drawerlayout.widget;

import static org.junit.Assert.assertEquals;

import android.os.Build;

import androidx.drawerlayout.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DrawerCustomThemeTest {
    @SuppressWarnings("deprecation")
    @Rule
    public final androidx.test.rule.ActivityTestRule<DrawerCustomThemeActivity> mActivityTestRule =
            new androidx.test.rule.ActivityTestRule<>(DrawerCustomThemeActivity.class);

    @Test
    @SmallTest
    public void testCustomElevation() {
        final DrawerCustomThemeActivity activity = mActivityTestRule.getActivity();
        DrawerLayout drawerLayout = activity.findViewById(
                R.id.drawer_custom_layout);

        if (Build.VERSION.SDK_INT < 21) {
            float expectedElevation = 0;
            assertEquals("Expected elevation and actual elevation", expectedElevation,
                    drawerLayout.getDrawerElevation(), 0.0);
        } else {
            float expectedElevation = activity.getResources()
                    .getDimension(R.dimen.custom_drawer_elevation);
            assertEquals("Expected elevation and actual elevation", expectedElevation,
                    drawerLayout.getDrawerElevation(), 0.0);
        }
    }
}
