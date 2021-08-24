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

package androidx.preference.tests

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [PreferenceFragmentCompat] interfaces that can be implemented via both [Context] and
 * [android.app.Activity], to ensure that they are called, and only once.
 *
 * This test doesn't test the paths including [PreferenceFragmentCompat.getCallbackFragment], as
 * this API is @RestrictTo and we don't expect developers to be using it.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PreferenceFragmentCompatInterfaceTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val noInterfaceActivityRule = androidx.test.rule.ActivityTestRule(
        NoInterfaceTestActivity::class.java,
        false,
        false
    )

    @Suppress("DEPRECATION")
    @get:Rule
    val withInterfaceActivityRule = androidx.test.rule.ActivityTestRule(
        WithInterfaceTestActivity::class.java,
        false,
        false
    )

    @Test
    fun onPreferenceStartFragmentTest_contextCallback() {
        verifyCallsWithContext {
            Preference(context).apply {
                fragment = "dummy.fragment"
            }
        }
    }

    @Test
    fun onPreferenceStartFragmentTest_activityCallback() {
        verifyCallsWithActivity {
            Preference(context).apply {
                fragment = "dummy.fragment"
            }
        }
    }

    @Test
    fun onPreferenceStartFragmentTest_contextAndActivityCallback_contextHandlesCall() {
        verifyCallsWithContextAndActivity(contextHandlesCall = true) {
            Preference(context).apply {
                fragment = "dummy.fragment"
            }
        }
    }

    @Test
    fun onPreferenceStartFragmentTest_contextAndActivityCallback_contextDoesNotHandleCall() {
        verifyCallsWithContextAndActivity(contextHandlesCall = false) {
            Preference(context).apply {
                fragment = "dummy.fragment"
            }
        }
    }

    @Test
    fun onPreferenceStartScreenTest_contextCallback() {
        verifyCallsWithContext {
            preferenceManager.createPreferenceScreen(context).apply {
                // Add a preference as PreferenceScreen will only dispatch
                // onPreferenceStartScreen if its count != 0
                addPreference(Preference(context))
            }
        }
    }

    @Test
    fun onPreferenceStartScreenTest_activityCallback() {
        verifyCallsWithActivity {
            preferenceManager.createPreferenceScreen(context).apply {
                // Add a preference as PreferenceScreen will only dispatch
                // onPreferenceStartScreen if its count != 0
                addPreference(Preference(context))
            }
        }
    }

    @Test
    fun onPreferenceStartScreenTest_contextAndActivityCallback_contextHandlesCall() {
        verifyCallsWithContextAndActivity(contextHandlesCall = true) {
            preferenceManager.createPreferenceScreen(context).apply {
                // Add a preference as PreferenceScreen will only dispatch
                // onPreferenceStartScreen if its count != 0
                addPreference(Preference(context))
            }
        }
    }

    @Test
    fun onPreferenceStartScreenTest_contextAndActivityCallback_contextDoesNotHandleCall() {
        verifyCallsWithContextAndActivity(contextHandlesCall = false) {
            preferenceManager.createPreferenceScreen(context).apply {
                // Add a preference as PreferenceScreen will only dispatch
                // onPreferenceStartScreen if its count != 0
                addPreference(Preference(context))
            }
        }
    }

    @Test
    fun onPreferenceDisplayDialogTest_contextCallback() {
        verifyCallsWithContext {
            EditTextPreference(context)
        }
    }

    @Test
    fun onPreferenceDisplayDialogTest_activityCallback() {
        verifyCallsWithActivity {
            EditTextPreference(context)
        }
    }

    @Test
    fun onPreferenceDisplayDialogTest_contextAndActivityCallback_contextHandlesCall() {
        verifyCallsWithContextAndActivity(contextHandlesCall = true) {
            EditTextPreference(context)
        }
    }

    @Test
    fun onPreferenceDisplayDialogTest_contextAndActivityCallback_contextDoesNotHandleCall() {
        verifyCallsWithContextAndActivity(contextHandlesCall = false) {
            EditTextPreference(context)
        }
    }

    private fun verifyCallsWithContext(
        testPreferenceFactory: PreferenceFragmentCompat.() -> Preference
    ) {
        var count = 0
        val incrementCount: () -> Boolean = {
            count++
            true
        }

        noInterfaceActivityRule.launchActivity(Intent())

        noInterfaceActivityRule.run {
            runOnUiThread {
                activity.displayPreferenceFragment(
                    TestFragment(
                        testPreferenceFactory,
                        contextCallback = incrementCount
                    )
                )
            }
        }

        TestFragment.assertPreferenceIsDisplayed()

        noInterfaceActivityRule.runOnUiThread {
            assertEquals(0, count)
        }

        TestFragment.clickOnPreference()

        noInterfaceActivityRule.runOnUiThread {
            assertEquals(1, count)
        }
    }

    private fun verifyCallsWithActivity(
        testPreferenceFactory: PreferenceFragmentCompat.() -> Preference
    ) {
        var count = 0
        val incrementCount: () -> Boolean = {
            count++
            true
        }

        withInterfaceActivityRule.launchActivity(Intent())

        withInterfaceActivityRule.run {
            runOnUiThread {
                activity.setTestCallback(incrementCount)
                activity.displayPreferenceFragment(
                    TestFragment(
                        testPreferenceFactory,
                        contextCallback = null
                    )
                )
            }
        }

        TestFragment.assertPreferenceIsDisplayed()

        withInterfaceActivityRule.runOnUiThread {
            assertEquals(0, count)
        }

        TestFragment.clickOnPreference()

        withInterfaceActivityRule.runOnUiThread {
            assertEquals(1, count)
        }
    }

    /**
     * @param contextHandlesCall whether the context implementation 'handles' the interface call,
     * by returning true. If it does, then the activity implementation should not be called.
     */
    private fun verifyCallsWithContextAndActivity(
        contextHandlesCall: Boolean,
        testPreferenceFactory: PreferenceFragmentCompat.() -> Preference
    ) {
        var contextCount = 0
        val incrementContextCount: () -> Boolean = {
            contextCount++
            contextHandlesCall
        }

        var activityCount = 0
        val incrementActivityCount: () -> Boolean = {
            activityCount++
            true
        }

        withInterfaceActivityRule.launchActivity(Intent())

        withInterfaceActivityRule.run {
            runOnUiThread {
                activity.setTestCallback(incrementActivityCount)
                activity.displayPreferenceFragment(
                    TestFragment(
                        testPreferenceFactory,
                        contextCallback = incrementContextCount
                    )
                )
            }
        }

        TestFragment.assertPreferenceIsDisplayed()

        withInterfaceActivityRule.runOnUiThread {
            assertEquals(0, contextCount)
            assertEquals(0, activityCount)
        }

        TestFragment.clickOnPreference()

        withInterfaceActivityRule.runOnUiThread {
            // Context should be checked before activity, so this will always be called
            assertEquals(1, contextCount)

            val expectedActivityCount = if (contextHandlesCall) {
                // If context returns true, then we should never call the activity implementation
                0
            } else {
                // If context returns false, it has not handled the call, so we should call the
                // activity implementation
                1
            }
            assertEquals(expectedActivityCount, activityCount)
        }
    }
}

open class NoInterfaceTestActivity : AppCompatActivity() {
    /**
     * Displays the given [fragment] by adding it to a FragmentTransaction
     */
    fun displayPreferenceFragment(fragment: PreferenceFragmentCompat) {
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, fragment)
            .commitNow()
    }
}

class WithInterfaceTestActivity :
    NoInterfaceTestActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
    PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    private lateinit var testCallback: () -> Boolean

    /**
     * Sets the [callback] to be invoked when an interface method from [PreferenceFragmentCompat]
     * is invoked on this Activity.
     *
     * @param callback returns true if it handles the event, false if not
     */
    fun setTestCallback(callback: () -> Boolean) {
        testCallback = callback
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ) = testCallback()

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat?,
        pref: PreferenceScreen?
    ) = testCallback()

    override fun onPreferenceDisplayDialog(
        caller: PreferenceFragmentCompat,
        pref: Preference?
    ) = testCallback()
}

/**
 * [Context] that implements interface methods so that we can return it from inside getContext().
 *
 * @property testCallback invoked when an interface method from [PreferenceFragmentCompat] is
 * invoked on this object. Returns true if it handles the event, false if not.
 */
private class TestContext(baseContext: Context, private val testCallback: () -> Boolean) :
    ContextWrapper(baseContext),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
    PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ) = testCallback()

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat?,
        pref: PreferenceScreen?
    ) = testCallback()

    override fun onPreferenceDisplayDialog(
        caller: PreferenceFragmentCompat,
        pref: Preference?
    ) = testCallback()
}

/**
 * Testing fragment that will add the [Preference] created by [testPreferenceFactory] to the
 * hierarchy, and set its title so it can be clicked on in tests.
 *
 * @property testPreferenceFactory factory that creates the [Preference] to be tested
 * @property contextCallback optional callback that will be used in the interface methods
 * implemented by a wrapped [TestContext] returned by [getContext] if not null. This simulates the
 * case where a non-Activity object is returned by [getContext], with the interface methods
 * implemented, for non-Activity based Fragment hosts.
 */
class TestFragment(
    private val testPreferenceFactory: PreferenceFragmentCompat.() -> Preference,
    private val contextCallback: (() -> Boolean)? = null
) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(context)
        val testPreference = testPreferenceFactory()
        testPreference.title = preferenceTitle
        preferenceScreen.addPreference(testPreference)
    }

    override fun getContext(): Context? {
        val superContext = super.getContext()!!
        return if (contextCallback != null) {
            TestContext(superContext, contextCallback)
        } else {
            superContext
        }
    }

    companion object {
        private const val preferenceTitle = "preference"
        /**
         * Asserts the preference created by [TestFragment.testPreferenceFactory] is displayed.
         */
        fun assertPreferenceIsDisplayed() {
            onView(withText(preferenceTitle)).check(matches(isDisplayed()))
        }
        /**
         * Clicks on the preference created by [TestFragment.testPreferenceFactory].
         */
        fun clickOnPreference() {
            onView(withText(preferenceTitle)).perform(click())
        }
    }
}
