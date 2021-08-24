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

package androidx.paging

import androidx.annotation.VisibleForTesting

/**
 * Wrapper class for [PagingSource] factory intended for usage in [Pager] construction.
 * Stores reference to the [PagingSource] factory and the [PagingSource]s it produces for
 * invalidation when the backing dataset is updated.
 *
 * Calling [invalidate] on this [InvalidatingPagingSourceFactory] will automatically forward
 * invalidate signals to all active [PagingSource]s.
 *
 * @param pagingSourceFactory The [PagingSource] factory that returns a PagingSource when called
 */
public class InvalidatingPagingSourceFactory<Key : Any, Value : Any>(
    private val pagingSourceFactory: () -> PagingSource<Key, Value>
) : () -> PagingSource<Key, Value> {

    @VisibleForTesting
    internal val pagingSources = mutableListOf<PagingSource<Key, Value>>()

    /**
     * @return [PagingSource] which will be invalidated when this factory's [invalidate] method
     * is called
     */
    override fun invoke(): PagingSource<Key, Value> {
        return pagingSourceFactory().also { pagingSources.add(it) }
    }

    /**
     * Calls [PagingSource.invalidate] on each [PagingSource] that was produced by this
     * [InvalidatingPagingSourceFactory]
     */
    public fun invalidate() {
        while (pagingSources.isNotEmpty()) {
            pagingSources.removeFirst().also {
                if (!it.invalid) {
                    it.invalidate()
                }
            }
        }
    }
}