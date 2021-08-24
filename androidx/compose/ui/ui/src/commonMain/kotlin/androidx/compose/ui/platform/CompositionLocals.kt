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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.node.Owner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The CompositionLocal to provide communication with platform accessibility service.
 */
val LocalAccessibilityManager = staticCompositionLocalOf<AccessibilityManager?> { null }

/**
 * The CompositionLocal that can be used to trigger autofill actions.
 * Eg. [Autofill.requestAutofillForNode].
 */
@get:ExperimentalComposeUiApi
@ExperimentalComposeUiApi
val LocalAutofill = staticCompositionLocalOf<Autofill?> { null }

/**
 * The CompositionLocal that can be used to add
 * [AutofillNode][import androidx.compose.ui.autofill.AutofillNode]s to the autofill tree. The
 * [AutofillTree] is a temporary data structure that will be replaced by Autofill Semantics
 * (b/138604305).
 */
@get:ExperimentalComposeUiApi
@ExperimentalComposeUiApi
val LocalAutofillTree = staticCompositionLocalOf<AutofillTree> {
    noLocalProvidedFor("LocalAutofillTree")
}

/**
 * The CompositionLocal to provide communication with platform clipboard service.
 */
val LocalClipboardManager = staticCompositionLocalOf<ClipboardManager> {
    noLocalProvidedFor("LocalClipboardManager")
}

/**
 * Provides the [Density] to be used to transform between [density-independent pixel
 * units (DP)][androidx.compose.ui.unit.Dp] and [pixel units][androidx.compose.ui.unit.Px] or
 * [scale-independent pixel units (SP)][androidx.compose.ui.unit.TextUnit] and
 * [pixel units][androidx.compose.ui.unit.Px]. This is typically used when a
 * [DP][androidx.compose.ui.unit.Dp] is provided and it must be converted in the body of [Layout]
 * or [DrawModifier].
 */
val LocalDensity = staticCompositionLocalOf<Density> {
    noLocalProvidedFor("LocalDensity")
}

/**
 * The CompositionLocal that can be used to control focus within Compose.
 */
val LocalFocusManager = staticCompositionLocalOf<FocusManager> {
    noLocalProvidedFor("LocalFocusManager")
}

/**
 * The CompositionLocal to provide platform font loading methods.
 *
 * Use [androidx.compose.ui.res.fontResource] instead.
 * @suppress
 */
val LocalFontLoader = staticCompositionLocalOf<Font.ResourceLoader> {
    noLocalProvidedFor("LocalFontLoader")
}

/**
 * The CompositionLocal to provide haptic feedback to the user.
 */
val LocalHapticFeedback = staticCompositionLocalOf<HapticFeedback> {
    noLocalProvidedFor("LocalHapticFeedback")
}

/**
 * The CompositionLocal to provide the layout direction.
 */
val LocalLayoutDirection = staticCompositionLocalOf<LayoutDirection> {
    noLocalProvidedFor("LocalLayoutDirection")
}

/**
 * The CompositionLocal to provide communication with platform text input service.
 */
val LocalTextInputService = staticCompositionLocalOf<TextInputService?> { null }

/**
 * The CompositionLocal to provide text-related toolbar.
 */
val LocalTextToolbar = staticCompositionLocalOf<TextToolbar> {
    noLocalProvidedFor("LocalTextToolbar")
}

/**
 * The CompositionLocal to provide functionality related to URL, e.g. open URI.
 */
val LocalUriHandler = staticCompositionLocalOf<UriHandler> {
    noLocalProvidedFor("LocalUriHandler")
}

/**
 * The CompositionLocal that provides the ViewConfiguration.
 */
val LocalViewConfiguration = staticCompositionLocalOf<ViewConfiguration> {
    noLocalProvidedFor("LocalViewConfiguration")
}

/**
 * The CompositionLocal that provides information about the window that hosts the current [Owner].
 */
val LocalWindowInfo = staticCompositionLocalOf<WindowInfo> {
    noLocalProvidedFor("LocalWindowInfo")
}

@ExperimentalComposeUiApi
@Composable
internal fun ProvideCommonCompositionLocals(
    owner: Owner,
    uriHandler: UriHandler,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAccessibilityManager provides owner.accessibilityManager,
        LocalAutofill provides owner.autofill,
        LocalAutofillTree provides owner.autofillTree,
        LocalClipboardManager provides owner.clipboardManager,
        LocalDensity provides owner.density,
        LocalFocusManager provides owner.focusManager,
        LocalFontLoader provides owner.fontLoader,
        LocalHapticFeedback provides owner.hapticFeedBack,
        LocalLayoutDirection provides owner.layoutDirection,
        LocalTextInputService provides owner.textInputService,
        LocalTextToolbar provides owner.textToolbar,
        LocalUriHandler provides uriHandler,
        LocalViewConfiguration provides owner.viewConfiguration,
        LocalWindowInfo provides owner.windowInfo,
        content = content
    )
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
