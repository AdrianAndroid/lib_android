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

package androidx.compose.material.catalog.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.catalog.model.Theme
import androidx.compose.material.catalog.model.getColor
import androidx.compose.material.catalog.model.getFontFamily
import androidx.compose.material.catalog.model.getShape
import androidx.compose.material.catalog.util.isLightColor
import androidx.compose.material.catalog.util.onColor
import androidx.compose.material.catalog.util.variantColor
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat

@Composable
fun CatalogTheme(
    theme: Theme,
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val themePrimaryColor = theme.primaryColor.getColor(darkTheme)
    val themeSecondaryColor = theme.secondaryColor.getColor(darkTheme)
    val primaryColor = animateColorAsState(themePrimaryColor)
    val primaryVariantColor = animateColorAsState(themePrimaryColor.variantColor())
    val onPrimaryColor = animateColorAsState(themePrimaryColor.onColor())
    val secondaryColor = animateColorAsState(themeSecondaryColor)
    val secondaryVariantColor = animateColorAsState(themeSecondaryColor.variantColor())
    val onSecondaryColor = animateColorAsState(themeSecondaryColor.onColor())
    val smallShapeSize = animateDpAsState(theme.smallShapeCornerSize.dp)
    val mediumShapeSize = animateDpAsState(theme.mediumShapeCornerSize.dp)
    val largeShapeSize = animateDpAsState(theme.largeShapeCornerSize.dp)
    val colors = if (!darkTheme) {
        lightColors(
            primary = primaryColor.value,
            primaryVariant = primaryVariantColor.value,
            onPrimary = onPrimaryColor.value,
            secondary = secondaryColor.value,
            secondaryVariant = secondaryVariantColor.value,
            onSecondary = onSecondaryColor.value
        )
    } else {
        darkColors(
            primary = primaryColor.value,
            primaryVariant = primaryVariantColor.value,
            onPrimary = onPrimaryColor.value,
            secondary = secondaryColor.value,
            onSecondary = onSecondaryColor.value
        )
    }
    val view = LocalView.current
    SideEffect {
        ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars =
            colors.primarySurface.isLightColor()
    }
    MaterialTheme(
        colors = colors,
        typography = Typography(defaultFontFamily = theme.fontFamily.getFontFamily()),
        shapes = Shapes(
            small = theme.shapeCornerFamily.getShape(size = smallShapeSize.value),
            medium = theme.shapeCornerFamily.getShape(size = mediumShapeSize.value),
            large = theme.shapeCornerFamily.getShape(size = largeShapeSize.value),
        ),
        content = content
    )
}
