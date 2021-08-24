/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.core.view

import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat.Type
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("DEPRECATION") // Testing our deprecated methods
@RunWith(AndroidJUnit4::class)
@SmallTest
class WindowInsetsCompatTest {
    @Test
    public fun consumeDisplayCutout_returnsNonNull() {
        val insets = WindowInsetsCompat.Builder().build()
        assertThat(insets.consumeDisplayCutout(), notNullValue())
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public fun inset_systemWindow() {
        val start = Insets.of(12, 34, 35, 31)
        val insets = WindowInsetsCompat.Builder().setSystemWindowInsets(start).build()

        val result = insets.inset(10, 10, 10, 10).systemWindowInsets
        assertEquals(start.left - 10, result.left)
        assertEquals(start.top - 10, result.top)
        assertEquals(start.right - 10, result.right)
        assertEquals(start.bottom - 10, result.bottom)
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public fun inset_systemWindow_largeValues() {
        val start = Insets.of(12, 34, 35, 31)
        val insets = WindowInsetsCompat.Builder().setSystemWindowInsets(start).build()

        // We inset with values larger than the inset values
        val result = insets.inset(100, 100, 100, 100).systemWindowInsets
        // And assert that we have 0 inset values
        assertEquals(0, result.left)
        assertEquals(0, result.top)
        assertEquals(0, result.right)
        assertEquals(0, result.bottom)
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public fun inset_systemBars() {
        val start = Insets.of(12, 34, 35, 31)
        val insets = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), start)
            .build()

        val result = insets.inset(10, 10, 10, 10).getInsets(Type.systemBars())
        assertEquals(start.left - 10, result.left)
        assertEquals(start.top - 10, result.top)
        assertEquals(start.right - 10, result.right)
        assertEquals(start.bottom - 10, result.bottom)
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public fun inset_systemBars_largeValues() {
        val start = Insets.of(12, 34, 35, 31)
        val insets = WindowInsetsCompat.Builder().setInsets(Type.systemBars(), start).build()
        // We inset with values larger than the inset values
        val result = insets.inset(100, 100, 100, 100).getInsets(Type.systemBars())
        // And assert that we have 0 inset values
        assertEquals(0, result.left)
        assertEquals(0, result.top)
        assertEquals(0, result.right)
        assertEquals(0, result.bottom)
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public fun inset_set_ime_insets() {
        val start = Insets.of(10, 11, 12, 13)
        val insets = WindowInsetsCompat.Builder()
            .setInsets(Type.ime(), start)
            .build()
            .getInsets(Type.ime())

        // And assert that we have 0 inset values
        assertEquals(10, insets.left)
        assertEquals(11, insets.top)
        assertEquals(12, insets.right)
        assertEquals(13, insets.bottom)
    }

    /**
     * On API 29+ we can test more types.
     */
    @Test
    @SdkSuppress(minSdkVersion = 29)
    public fun builder_min29_types() {
        val sysBars = Insets.of(12, 34, 35, 31)
        val sysGesture = Insets.of(74, 26, 79, 21)
        val mandSysGesture = Insets.of(20, 10, 57, 1)
        val tappable = Insets.of(65, 82, 32, 62)

        val result = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), sysBars)
            .setInsets(Type.systemGestures(), sysGesture)
            .setInsets(Type.mandatorySystemGestures(), mandSysGesture)
            .setInsets(Type.tappableElement(), tappable)
            .build()

        assertEquals(sysBars, result.getInsets(Type.systemBars()))
        assertEquals(sysBars, result.systemWindowInsets)
        assertEquals(sysGesture, result.getInsets(Type.systemGestures()))
        assertEquals(sysGesture, result.systemGestureInsets)
        assertEquals(mandSysGesture, result.getInsets(Type.mandatorySystemGestures()))
        assertEquals(mandSysGesture, result.mandatorySystemGestureInsets)
        assertEquals(tappable, result.getInsets(Type.tappableElement()))
        assertEquals(tappable, result.tappableElementInsets)
    }

    /**
     * On API 29+ we can test more types.
     */
    @Test
    @SdkSuppress(minSdkVersion = 29)
    public fun builder_min29_deprecated() {
        val sysBars = Insets.of(12, 34, 35, 31)
        val sysGesture = Insets.of(74, 26, 79, 21)
        val mandSysGesture = Insets.of(20, 10, 57, 1)
        val tappable = Insets.of(65, 82, 32, 62)

        val result = WindowInsetsCompat.Builder()
            .setSystemWindowInsets(sysBars)
            .setSystemGestureInsets(sysGesture)
            .setMandatorySystemGestureInsets(mandSysGesture)
            .setTappableElementInsets(tappable)
            .build()

        assertEquals(sysBars, result.getInsets(Type.systemBars()))
        assertEquals(sysBars, result.systemWindowInsets)
        assertEquals(sysGesture, result.getInsets(Type.systemGestures()))
        assertEquals(sysGesture, result.systemGestureInsets)
        assertEquals(mandSysGesture, result.getInsets(Type.mandatorySystemGestures()))
        assertEquals(mandSysGesture, result.mandatorySystemGestureInsets)
        assertEquals(tappable, result.getInsets(Type.tappableElement()))
        assertEquals(tappable, result.tappableElementInsets)
    }

    /**
     * Only API 20-28, only `setSystemWindowInsets` and `systemBars()` works.
     */
    @Test
    @SdkSuppress(minSdkVersion = 20)
    public fun builder_min20_types() {
        val sysBars = Insets.of(12, 34, 35, 31)
        val result = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), sysBars)
            .build()
        assertEquals(sysBars, result.systemWindowInsets)
        assertEquals(sysBars, result.getInsets(Type.systemBars()))
    }

    /**
     * Only API 20-28, only `setSystemWindowInsets` and `systemBars()` works.
     */
    @Test
    @SdkSuppress(minSdkVersion = 20)
    public fun builder_min20_deprecated() {
        val sysBars = Insets.of(12, 34, 35, 31)
        val result = WindowInsetsCompat.Builder()
            .setSystemWindowInsets(sysBars)
            .build()
        assertEquals(sysBars, result.systemWindowInsets)
        assertEquals(sysBars, result.getInsets(Type.systemBars()))
    }

    /**
     * Only API min-19, none of the setters work. Instead we just make sure we return non-null.
     */
    @Test
    public fun builder_minapi() {
        val sysBars = Insets.of(12, 34, 35, 31)
        val result = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), sysBars)
            .build()
        assertNotNull(result)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 19)
    public fun consumed_exists() {
        assertNotNull(WindowInsetsCompat.CONSUMED)
    }

    @Test
    @SdkSuppress(minSdkVersion = 20)
    public fun consumed_exists_api20() {
        assertNotNull(WindowInsetsCompat.CONSUMED)
        assertNotNull(WindowInsetsCompat.CONSUMED.toWindowInsets())
        assertTrue(WindowInsetsCompat.CONSUMED.isConsumed)
    }

    @Suppress("DEPRECATION")
    @Test
    @SdkSuppress(minSdkVersion = 20)
    public fun consumed_returnsNoneInsets() {
        val sysBars = Insets.of(12, 34, 35, 31)
        val original = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), sysBars)
            .build()
        val consumed = original.consumeSystemWindowInsets()
        assertEquals(Insets.NONE, consumed.systemWindowInsets)
    }

    @Test
    public fun test_equals() {
        val result = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), Insets.of(1, 2, 3, 4))
            .setInsetsIgnoringVisibility(Type.systemBars(), Insets.of(11, 12, 13, 14))
            .build()
        result.setRootViewData(Insets.of(0, 0, 0, 15))
        val result2 = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), Insets.of(1, 2, 3, 4))
            .setInsetsIgnoringVisibility(Type.systemBars(), Insets.of(11, 12, 13, 14))
            .build()
        result2.setRootViewData(Insets.of(0, 0, 0, 15))
        assertEquals(result, result2)
    }

    @Test
    @SdkSuppress(minSdkVersion = 20)
    public fun test_not_equals_root_visible_insets() {
        val result = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), Insets.of(1, 2, 3, 4))
            .setInsetsIgnoringVisibility(Type.systemBars(), Insets.of(11, 12, 13, 14))
            .build()
        result.setRootViewData(Insets.of(0, 0, 0, 15))
        val result2 = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), Insets.of(1, 2, 3, 4))
            .setInsetsIgnoringVisibility(Type.systemBars(), Insets.of(11, 12, 13, 14))
            .build()
        result2.setRootViewData(Insets.of(0, 0, 0, 16))
        assertNotEquals(result, result2)
    }

    @Test
    public fun test_hashCode() {
        val result = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), Insets.of(1, 2, 3, 4))
            .setInsetsIgnoringVisibility(Type.systemBars(), Insets.of(11, 12, 13, 14))
            .build()
        val result2 = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), Insets.of(1, 2, 3, 4))
            .setInsetsIgnoringVisibility(Type.systemBars(), Insets.of(11, 12, 13, 14))
            .build()
        assertEquals(result.hashCode(), result2.hashCode())
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492236
    @Test
    public fun set_only_navigation_bar_insets() {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(Type.statusBars(), Insets.of(0, 100, 0, 0))
            .setInsets(Type.navigationBars(), Insets.of(0, 0, 0, 200))
            .build()

        val removeStatusBarInsets = WindowInsetsCompat.Builder(insets)
            .setInsets(Type.statusBars(), Insets.NONE)
            .build()

        assertEquals(0, removeStatusBarInsets.getInsets(Type.statusBars()).top)
        assertEquals(200, removeStatusBarInsets.getInsets(Type.navigationBars()).bottom)

        val removeNavBarInsets = WindowInsetsCompat.Builder(insets)
            .setInsets(Type.navigationBars(), Insets.NONE)
            .build()

        assertEquals(100, removeNavBarInsets.getInsets(Type.statusBars()).top)
        assertEquals(0, removeNavBarInsets.getInsets(Type.navigationBars()).bottom)
    }
}