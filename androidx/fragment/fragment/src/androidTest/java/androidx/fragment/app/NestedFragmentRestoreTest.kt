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

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.app.test.FragmentTestActivity.ParentFragment
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class NestedFragmentRestoreTest {

    @Suppress("DEPRECATION")
    @Test
    fun recreateActivity() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val activity = withActivity {
                val parent = ParentFragment()
                parent.retainChildInstance = true

                supportFragmentManager.beginTransaction()
                    .add(parent, "parent")
                    .commitNow()

                this
            }

            val fm = withActivity { supportFragmentManager }
            val parent = fm.findFragmentByTag("parent") as ParentFragment
            val child = parent.childFragment

            var attachedTo: Context? = null
            val latch = CountDownLatch(1)
            child.onAttachListener = { context ->
                attachedTo = context
                latch.countDown()
            }

            recreate()

            assertWithMessage("timeout waiting for recreate")
                .that(latch.await(10, TimeUnit.SECONDS))
                .isTrue()

            assertWithMessage("attached as part of recreate").that(attachedTo).isNotNull()
            assertWithMessage("attached to new context")
                .that(attachedTo)
                .isNotSameInstanceAs(activity)
            assertWithMessage("attached to new parent fragment")
                .that(child)
                .isNotSameInstanceAs(parent)
        }
    }

    @Test
    fun restoreViewStateTest() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val parent = RestoreViewParentFragment()

                supportFragmentManager.beginTransaction()
                    .add(parent, "parent")
                    .commitNow()
            }

            recreate()

            val fm = withActivity { supportFragmentManager }
            val parent = fm.findFragmentByTag("parent") as RestoreViewParentFragment
            val child = parent.childFragment

            assertWithMessage("parent view was restored before child")
                .that(child.viewRestoredAfterParent)
                .isTrue()
        }
    }
}

class RestoreViewParentFragment : Fragment(R.layout.fragment_a) {
    var viewRestored = false

    val childFragment: RestoreViewChildFragment
        get() = childFragmentManager.findFragmentByTag("childFragment") as RestoreViewChildFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (childFragmentManager.findFragmentByTag("childFragment") == null) {
            childFragmentManager.beginTransaction()
                .add(RestoreViewChildFragment(), "childFragment")
                .commitNow()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        childFragment.requireView()
        viewRestored = true
    }
}

class RestoreViewChildFragment : Fragment(R.layout.fragment_a) {
    var viewRestoredAfterParent = false

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val parentFragment = parentFragment
        if (parentFragment is RestoreViewParentFragment) {
            viewRestoredAfterParent = parentFragment.viewRestored
        }
    }
}
