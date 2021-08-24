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
package androidx.navigation.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.transition.TransitionManager
import com.google.android.material.appbar.CollapsingToolbarLayout
import java.lang.ref.WeakReference

/**
 * The OnDestinationChangedListener specifically for keeping a
 * CollapsingToolbarLayout+Toolbar updated.
 * This handles both updating the title and updating the Up Indicator, transitioning between
 * the drawer icon and up arrow as needed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class CollapsingToolbarOnDestinationChangedListener(
    collapsingToolbarLayout: CollapsingToolbarLayout,
    toolbar: Toolbar,
    configuration: AppBarConfiguration
) : AbstractAppBarOnDestinationChangedListener(collapsingToolbarLayout.context, configuration) {

    private val mCollapsingToolbarLayoutWeakReference: WeakReference<CollapsingToolbarLayout> =
        WeakReference(collapsingToolbarLayout)
    private val mToolbarWeakReference: WeakReference<Toolbar> = WeakReference(toolbar)

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val collapsingToolbarLayout = mCollapsingToolbarLayoutWeakReference.get()
        val toolbar = mToolbarWeakReference.get()
        if (collapsingToolbarLayout == null || toolbar == null) {
            controller.removeOnDestinationChangedListener(this)
            return
        }
        super.onDestinationChanged(controller, destination, arguments)
    }

    override fun setTitle(title: CharSequence?) {
        val collapsingToolbarLayout = mCollapsingToolbarLayoutWeakReference.get()
        if (collapsingToolbarLayout != null) {
            collapsingToolbarLayout.title = title
        }
    }

    override fun setNavigationIcon(icon: Drawable?, @StringRes contentDescription: Int) {
        val toolbar = mToolbarWeakReference.get()
        if (toolbar != null) {
            val useTransition = icon == null && toolbar.navigationIcon != null
            toolbar.navigationIcon = icon
            toolbar.setNavigationContentDescription(contentDescription)
            if (useTransition) {
                TransitionManager.beginDelayedTransition(toolbar)
            }
        }
    }
}
