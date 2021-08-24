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
package androidx.fragment.app

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.fragment.app.test.FragmentResultActivity
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for Fragment startActivityForResult and startIntentSenderForResult.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentReceiveResultTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    private lateinit var activity: FragmentTestActivity
    private lateinit var fragment: TestFragment

    @Before
    fun setup() {
        activity = activityRule.activity
        fragment = attachTestFragment()
    }

    @Suppress("DEPRECATION")
    @Test
    @UiThreadTest
    fun testNoFragmentOnActivityResult() {
        activity.supportFragmentManager.saveAllStateInternal()

        // 0xffff is the request code for the startActivityResult launcher in FragmentManager
        activity.onActivityResult(0xffff, Activity.RESULT_OK, Intent())
    }

    @Suppress("DEPRECATION")
    @Test
    fun testNoFragmentOnRequestPermissionsResult() {
        // 0xffff + 2 is the request code for the requestPermissions launcher in FragmentManager
        activity.onRequestPermissionsResult(0xffff + 2, arrayOf("permission"), intArrayOf(1))
    }

    @Test
    fun testStartActivityForResultOk() {
        startActivityForResult(10, Activity.RESULT_OK, "content 10")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult[0]).isTrue()
        assertThat(fragment.requestCode[0]).isEqualTo(10)
        assertThat(fragment.resultCode[0]).isEqualTo(Activity.RESULT_OK)
        assertThat(fragment.resultContent[0]).isEqualTo("content 10")
    }

    @Test
    fun testMultipleStartActivityForResultOk() {
        startActivityForResult(10, Activity.RESULT_OK, "content 10")
        startActivityForResult(20, Activity.RESULT_OK, "content 20")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult[0]).isTrue()
        assertThat(fragment.requestCode[0]).isEqualTo(10)
        assertThat(fragment.resultCode[0]).isEqualTo(Activity.RESULT_OK)
        assertThat(fragment.resultContent[0]).isEqualTo("content 10")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult[1]).isTrue()
        assertThat(fragment.requestCode[1]).isEqualTo(20)
        assertThat(fragment.resultCode[1]).isEqualTo(Activity.RESULT_OK)
        assertThat(fragment.resultContent[1]).isEqualTo("content 20")
    }

    @Test
    fun testStartActivityForResultCanceled() {
        startActivityForResult(20, Activity.RESULT_CANCELED, "content 20")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult[0]).isTrue()
        assertThat(fragment.requestCode[0]).isEqualTo(20)
        assertThat(fragment.resultCode[0]).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(fragment.resultContent[0]).isEqualTo("content 20")
    }

    @Test
    fun testStartIntentSenderForResultOk() {
        startIntentSenderForResult(30, Activity.RESULT_OK, "content 30")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult[0]).isTrue()
        assertThat(fragment.requestCode[0]).isEqualTo(30)
        assertThat(fragment.resultCode[0]).isEqualTo(Activity.RESULT_OK)
        assertThat(fragment.resultContent[0]).isEqualTo("content 30")
    }

    @Test
    fun testStartIntentSenderForResultWithOptionsOk() {
        startIntentSenderForResult(30, Activity.RESULT_OK, "content 30", Bundle())

        assertWithMessage("Fragment should receive result").that(fragment.hasResult[0]).isTrue()
        assertThat(fragment.requestCode[0]).isEqualTo(30)
        assertThat(fragment.resultCode[0]).isEqualTo(Activity.RESULT_OK)
        assertThat(fragment.resultContent[0]).isEqualTo("content 30")
    }

    @Test
    fun testStartIntentSenderForResultCanceled() {
        startIntentSenderForResult(40, Activity.RESULT_CANCELED, "content 40")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult[0]).isTrue()
        assertThat(fragment.requestCode[0]).isEqualTo(40)
        assertThat(fragment.resultCode[0]).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(fragment.resultContent[0]).isEqualTo("content 40")
    }

    private fun attachTestFragment(): TestFragment {
        val fragment = TestFragment()
        activityRule.runOnUiThread {
            activity.supportFragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
            activity.supportFragmentManager.executePendingTransactions()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return fragment
    }

    @Suppress("DEPRECATION")
    private fun startActivityForResult(
        requestCode: Int,
        resultCode: Int,
        content: String
    ) {
        activityRule.runOnUiThread {
            val intent = Intent(activity, FragmentResultActivity::class.java)
            intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT, content)

            fragment.startActivityForResult(intent, requestCode)
        }
        assertThat(
            fragment.resultReceiveLatch[fragment.onActivityResultCount]
                .await(1, TimeUnit.SECONDS)
        ).isTrue()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Suppress("DEPRECATION")
    private fun startIntentSenderForResult(
        requestCode: Int,
        resultCode: Int,
        content: String,
        options: Bundle? = null
    ) {
        activityRule.runOnUiThread {
            val intent = Intent(activity, FragmentResultActivity::class.java)
            intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT, content)

            val pendingIntent = PendingIntent.getActivity(activity, requestCode, intent, 0)

            try {
                fragment.startIntentSenderForResult(
                    pendingIntent.intentSender,
                    requestCode, null, 0, 0, 0, options
                )
            } catch (e: IntentSender.SendIntentException) {
                fail("IntentSender failed")
            }
        }
        assertThat(fragment.resultReceiveLatch[0].await(1, TimeUnit.SECONDS)).isTrue()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    class TestFragment : Fragment() {
        internal val hasResult = ArrayList<Boolean>()
        internal val requestCode = ArrayList<Int>()
        internal val resultCode = ArrayList<Int>()
        internal val resultContent = ArrayList<String>()
        internal val resultReceiveLatch = arrayListOf(CountDownLatch(1), CountDownLatch(1))
        internal var onActivityResultCount = 0

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            hasResult.add(true)
            this.requestCode.add(requestCode)
            this.resultCode.add(resultCode)
            resultContent.add(data!!.getStringExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT)!!)
            resultReceiveLatch[onActivityResultCount].countDown()
            onActivityResultCount++
        }
    }
}
