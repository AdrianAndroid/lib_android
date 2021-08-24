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
package androidx.navigation

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import java.util.UUID

/**
 * Representation of an entry in the back stack of a [androidx.navigation.NavController]. The
 * [Lifecycle], [ViewModelStore], and [SavedStateRegistry] provided via
 * this object are valid for the lifetime of this destination on the back stack: when this
 * destination is popped off the back stack, the lifecycle will be destroyed, state
 * will no longer be saved, and ViewModels will be cleared.
 */
public class NavBackStackEntry private constructor(
    private val context: Context?,
    /**
     * The destination associated with this entry
     * @return The destination that is currently visible to users
     */
    public val destination: NavDestination,
    /**
     * The arguments used for this entry
     * @return The arguments used when this entry was created
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var arguments: Bundle? = null,
    navControllerLifecycleOwner: LifecycleOwner? = null,
    private val viewModelStoreProvider: NavViewModelStoreProvider? = null,
    /**
     * The unique ID that serves as the identity of this entry
     * @return the unique ID of this entry
     */
    public val id: String = UUID.randomUUID().toString(),
    private val savedState: Bundle? = null
) : LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun create(
            context: Context?,
            destination: NavDestination,
            arguments: Bundle? = null,
            navControllerLifecycleOwner: LifecycleOwner? = null,
            viewModelStoreProvider: NavViewModelStoreProvider? = null,
            id: String = UUID.randomUUID().toString(),
            savedState: Bundle? = null
        ): NavBackStackEntry = NavBackStackEntry(
            context, destination, arguments,
            navControllerLifecycleOwner, viewModelStoreProvider, id, savedState
        )
    }

    private var lifecycle = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var hostLifecycleState = Lifecycle.State.CREATED
    private val defaultFactory by lazy {
        SavedStateViewModelFactory((context?.applicationContext as? Application), this, arguments)
    }

    /**
     * The [SavedStateHandle] for this entry.
     */
    public val savedStateHandle: SavedStateHandle by lazy {
        check(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            "You cannot access the NavBackStackEntry's SavedStateHandle until it is added to " +
                "the NavController's back stack (i.e., the Lifecycle of the NavBackStackEntry " +
                "reaches the CREATED state)."
        }
        ViewModelProvider(
            this, NavResultSavedStateFactory(this, null)
        ).get(SavedStateViewModel::class.java).handle
    }

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun replaceArguments(newArgs: Bundle?) {
        arguments = newArgs
    }

    /**
     * {@inheritDoc}
     *
     * If the [androidx.navigation.NavHost] has not called
     * [androidx.navigation.NavHostController.setLifecycleOwner], the
     * Lifecycle will be capped at [Lifecycle.State.CREATED].
     */
    public override fun getLifecycle(): Lifecycle {
        return lifecycle
    }

    /** @suppress */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var maxLifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(maxState) {
            if (field == Lifecycle.State.INITIALIZED) {
                // Perform the restore just when moving from the INITIALIZED state
                savedStateRegistryController.performRestore(savedState)
            }
            field = maxState
            updateState()
        }

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun handleLifecycleEvent(event: Lifecycle.Event) {
        hostLifecycleState = event.targetState
        updateState()
    }

    /**
     * Update the state to be the lower of the two constraints:
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun updateState() {
        if (hostLifecycleState.ordinal < maxLifecycle.ordinal) {
            lifecycle.currentState = hostLifecycleState
        } else {
            lifecycle.currentState = maxLifecycle
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if called before the [lifecycle] has moved to
     * [Lifecycle.State.CREATED] or before the [androidx.navigation.NavHost] has called
     * [androidx.navigation.NavHostController.setViewModelStore].
     */
    public override fun getViewModelStore(): ViewModelStore {
        check(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            "You cannot access the NavBackStackEntry's ViewModels until it is added to " +
                "the NavController's back stack (i.e., the Lifecycle of the NavBackStackEntry " +
                "reaches the CREATED state)."
        }
        checkNotNull(viewModelStoreProvider) {
            "You must call setViewModelStore() on your NavHostController before accessing the " +
                "ViewModelStore of a navigation graph."
        }
        return viewModelStoreProvider.getViewModelStore(id)
    }

    public override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return defaultFactory
    }

    public override fun getSavedStateRegistry(): SavedStateRegistry {
        return savedStateRegistryController.savedStateRegistry
    }

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun saveState(outBundle: Bundle) {
        savedStateRegistryController.performSave(outBundle)
    }

    /**
     * Used to create the {SavedStateViewModel}
     */
    private class NavResultSavedStateFactory(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return SavedStateViewModel(handle) as T
        }
    }

    private class SavedStateViewModel(val handle: SavedStateHandle) : ViewModel()

    init {
        if (navControllerLifecycleOwner != null) {
            hostLifecycleState = navControllerLifecycleOwner.lifecycle.currentState
        }
    }
}
