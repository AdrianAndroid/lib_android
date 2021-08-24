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

package androidx.compose.ui.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class GraphicsLayerModifierTest {

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testInspectable() {
        val modifier = Modifier.graphicsLayer(rotationX = 2.0f) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("graphicsLayer")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("scaleX", 1.0f),
            ValueElement("scaleY", 1.0f),
            ValueElement("alpha", 1.0f),
            ValueElement("translationX", 0.0f),
            ValueElement("translationY", 0.0f),
            ValueElement("shadowElevation", 0.0f),
            ValueElement("rotationX", 2.0f),
            ValueElement("rotationY", 0.0f),
            ValueElement("rotationZ", 0.0f),
            ValueElement("cameraDistance", DefaultCameraDistance),
            ValueElement("transformOrigin", TransformOrigin.Center),
            ValueElement("shape", RectangleShape),
            ValueElement("clip", false)
        )
    }

    @Test
    fun testEquals() {
        assertThat(
            Modifier.graphicsLayer(
                scaleX = 1.0f,
                scaleY = 2.0f,
                alpha = 0.75f,
                translationX = 3.0f,
                translationY = 4.0f,
                shadowElevation = 5.0f,
                rotationX = 6.0f,
                rotationY = 7.0f,
                rotationZ = 8.0f,
                transformOrigin = TransformOrigin.Center,
                shape = RectangleShape,
                clip = true
            )
        )
            .isEqualTo(
                Modifier.graphicsLayer(
                    scaleX = 1.0f,
                    scaleY = 2.0f,
                    alpha = 0.75f,
                    translationX = 3.0f,
                    translationY = 4.0f,
                    shadowElevation = 5.0f,
                    rotationX = 6.0f,
                    rotationY = 7.0f,
                    rotationZ = 8.0f,
                    transformOrigin = TransformOrigin.Center,
                    shape = RectangleShape,
                    clip = true
                )
            )
    }
}