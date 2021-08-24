/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import android.app.Application
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.DEFAULT_KEY
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.defaultFactory
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.lang.UnsupportedOperationException
import java.lang.reflect.InvocationTargetException

/**
 * An utility class that provides `ViewModels` for a scope.
 *
 * Default `ViewModelProvider` for an `Activity` or a `Fragment` can be obtained
 * by passing it to the constructor: `ViewModelProvider(myFragment)`
 *
 * @param store  `ViewModelStore` where ViewModels will be stored.
 * @param factory factory a `Factory` which will be used to instantiate
 * new `ViewModels`
 */
public open class ViewModelProvider(
    private val store: ViewModelStore,
    private val factory: Factory
) {
    /**
     * Implementations of `Factory` interface are responsible to instantiate ViewModels.
     */
    public interface Factory {
        /**
         * Creates a new instance of the given `Class`.
         *
         * @param modelClass a `Class` whose instance is requested
         * @return a newly created ViewModel
         */
        public fun <T : ViewModel> create(modelClass: Class<T>): T
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open class OnRequeryFactory {
        public open fun onRequery(viewModel: ViewModel) {}
    }

    /**
     * Implementations of `Factory` interface are responsible to instantiate ViewModels.
     *
     *
     * This is more advanced version of [Factory] that receives a key specified for requested
     * [ViewModel].
     *
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract class KeyedFactory : OnRequeryFactory(), Factory {
        /**
         * Creates a new instance of the given `Class`.
         *
         * @param key a key associated with the requested ViewModel
         * @param modelClass a `Class` whose instance is requested
         * @return a newly created ViewModel
         */
        public abstract fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>
        ): T

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            throw UnsupportedOperationException(
                "create(String, Class<?>) must be called on implementations of KeyedFactory"
            )
        }
    }

    /**
     * Creates `ViewModelProvider`. This will create `ViewModels`
     * and retain them in a store of the given `ViewModelStoreOwner`.
     *
     *
     * This method will use the
     * [default factory][HasDefaultViewModelProviderFactory.getDefaultViewModelProviderFactory]
     * if the owner implements [HasDefaultViewModelProviderFactory]. Otherwise, a
     * [NewInstanceFactory] will be used.
     */
    public constructor(
        owner: ViewModelStoreOwner
    ) : this(owner.viewModelStore, defaultFactory(owner))

    /**
     * Creates `ViewModelProvider`, which will create `ViewModels` via the given
     * `Factory` and retain them in a store of the given `ViewModelStoreOwner`.
     *
     * @param owner   a `ViewModelStoreOwner` whose [ViewModelStore] will be used to
     * retain `ViewModels`
     * @param factory a `Factory` which will be used to instantiate
     * new `ViewModels`
     */
    public constructor(owner: ViewModelStoreOwner, factory: Factory) : this(
        owner.viewModelStore,
        factory
    )

    /**
     * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
     * an activity), associated with this `ViewModelProvider`.
     *
     *
     * The created ViewModel is associated with the given scope and will be retained
     * as long as the scope is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     * present.
     * @return A ViewModel that is an instance of the given type `T`.
     * @throws IllegalArgumentException if the given [modelClass] is local or anonymous class.
     */
    @MainThread
    public open operator fun <T : ViewModel> get(modelClass: Class<T>): T {
        val canonicalName = modelClass.canonicalName
            ?: throw IllegalArgumentException("Local and anonymous classes can not be ViewModels")
        return get("$DEFAULT_KEY:$canonicalName", modelClass)
    }

    /**
     * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
     * an activity), associated with this `ViewModelProvider`.
     *
     * The created ViewModel is associated with the given scope and will be retained
     * as long as the scope is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param key        The key to use to identify the ViewModel.
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     * present.
     * @return A ViewModel that is an instance of the given type `T`.
     */
    @Suppress("UNCHECKED_CAST")
    @MainThread
    public open operator fun <T : ViewModel> get(key: String, modelClass: Class<T>): T {
        var viewModel = store[key]
        if (modelClass.isInstance(viewModel)) {
            (factory as? OnRequeryFactory)?.onRequery(viewModel)
            return viewModel as T
        } else {
            @Suppress("ControlFlowWithEmptyBody")
            if (viewModel != null) {
                // TODO: log a warning.
            }
        }
        viewModel = if (factory is KeyedFactory) {
            factory.create(key, modelClass)
        } else {
            factory.create(modelClass)
        }
        store.put(key, viewModel)
        return viewModel
    }

    /**
     * Simple factory, which calls empty constructor on the give class.
     */
    // actually there is getInstance()
    @Suppress("SingletonConstructor")
    public open class NewInstanceFactory : Factory {
        @Suppress("DocumentExceptions")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return try {
                modelClass.newInstance()
            } catch (e: InstantiationException) {
                throw RuntimeException("Cannot create an instance of $modelClass", e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Cannot create an instance of $modelClass", e)
            }
        }

        public companion object {
            private var sInstance: NewInstanceFactory? = null

            /**
             * @suppress
             * Retrieve a singleton instance of NewInstanceFactory.
             *
             * @return A valid [NewInstanceFactory]
             */
            @JvmStatic
            public val instance: NewInstanceFactory
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                get() {
                    if (sInstance == null) {
                        sInstance = NewInstanceFactory()
                    }
                    return sInstance!!
                }
        }
    }

    /**
     * [Factory] which may create [AndroidViewModel] and
     * [ViewModel], which have an empty constructor.
     *
     * @param application an application to pass in [AndroidViewModel]
     */
    public open class AndroidViewModelFactory(
        private val application: Application
    ) : NewInstanceFactory() {
        @Suppress("DocumentExceptions")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (AndroidViewModel::class.java.isAssignableFrom(modelClass)) {
                try {
                    modelClass.getConstructor(Application::class.java).newInstance(application)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: InstantiationException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: InvocationTargetException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                }
            } else super.create(modelClass)
        }

        public companion object {
            internal fun defaultFactory(owner: ViewModelStoreOwner): Factory =
                if (owner is HasDefaultViewModelProviderFactory)
                    owner.defaultViewModelProviderFactory else instance

            internal const val DEFAULT_KEY = "androidx.lifecycle.ViewModelProvider.DefaultKey"

            private var sInstance: AndroidViewModelFactory? = null

            /**
             * Retrieve a singleton instance of AndroidViewModelFactory.
             *
             * @param application an application to pass in [AndroidViewModel]
             * @return A valid [AndroidViewModelFactory]
             */
            @JvmStatic
            public fun getInstance(application: Application): AndroidViewModelFactory {
                if (sInstance == null) {
                    sInstance = AndroidViewModelFactory(application)
                }
                return sInstance!!
            }
        }
    }
}
