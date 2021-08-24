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

package androidx.compose.animation.demos

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun SingleValueAnimationDemo() {
    val enabled = remember { mutableStateOf(true) }
    val alpha: Float by animateFloatAsState(if (enabled.value) 1f else 0.5f)
    val color = myAnimate(
        if (enabled.value) Color.Green else Color.Magenta,
        spring()
    ) {
        println("Finished at color $it")
    }
    Box(
        Modifier.fillMaxSize().clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            enabled
                .value = !enabled
                .value
        }
            .graphicsLayer(alpha = alpha)
            .background(color)
    )
}

@Composable
private fun myAnimate(
    targetValue: Color,
    animationSpec: AnimationSpec<Color>,
    onFinished: (Color) -> Unit
): Color {
    val color = remember { Animatable(targetValue) }
    val finishedListener = rememberUpdatedState(onFinished)
    LaunchedEffect(targetValue, animationSpec) {
        color.animateTo(targetValue, animationSpec)
        finishedListener.value(targetValue)
    }
    return color.value
}
