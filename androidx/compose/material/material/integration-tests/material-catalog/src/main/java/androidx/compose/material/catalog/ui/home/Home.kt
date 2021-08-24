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

package androidx.compose.material.catalog.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.catalog.R
import androidx.compose.material.catalog.insets.LocalWindowInsets
import androidx.compose.material.catalog.insets.toPaddingValues
import androidx.compose.material.catalog.model.Component
import androidx.compose.material.catalog.model.Theme
import androidx.compose.material.catalog.ui.common.CatalogScaffold
import androidx.compose.material.catalog.ui.component.ComponentItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun Home(
    components: List<Component>,
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit,
    onComponentClick: (component: Component) -> Unit
) {
    CatalogScaffold(
        topBarTitle = stringResource(id = R.string.compose_material),
        theme = theme,
        onThemeChange = onThemeChange
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues)) {
            val cellsCount = maxOf((maxWidth / HomeCellMinSize).toInt(), 1)
            LazyVerticalGrid(
                // LazyGridScope doesn't expose nColumns from LazyVerticalGrid
                // https://issuetracker.google.com/issues/183187002
                cells = GridCells.Fixed(count = cellsCount),
                content = {
                    itemsIndexed(components) { index, component ->
                        ComponentItem(
                            component = component,
                            onClick = onComponentClick,
                            index = index,
                            cellsCount = cellsCount
                        )
                    }
                },
                contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()
            )
        }
    }
}

private val HomeCellMinSize = 180.dp
