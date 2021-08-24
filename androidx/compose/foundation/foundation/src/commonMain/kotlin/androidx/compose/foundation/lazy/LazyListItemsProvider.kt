/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.compose.runtime.Composable

internal interface LazyListItemsProvider {
    /** The total size of the list */
    val itemsCount: Int

    /** The list of indexes of the sticky header items */
    val headerIndexes: List<Int>

    /** Returns the key for the item on this index */
    fun getKey(index: Int): Any

    /** Returns the content lambda for the given index and scope object */
    fun getContent(index: Int, scope: LazyItemScope): @Composable() () -> Unit
}
