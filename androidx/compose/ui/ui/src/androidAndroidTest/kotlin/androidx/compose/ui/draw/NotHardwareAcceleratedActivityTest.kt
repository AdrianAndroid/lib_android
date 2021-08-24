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

package androidx.compose.ui.draw

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.GOLDEN_UI
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class NotHardwareAcceleratedActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<NotHardwareAcceleratedActivity>()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    @Test
    fun layerTransformationsApplied() {
        composeTestRule.setContent {
            Box(Modifier.size(200.dp).background(Color.White).testTag("box")) {
                Box(
                    Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                val offset = 50.dp.roundToPx()
                                placeable.placeWithLayer(offset, offset) {
                                    alpha = 0.5f
                                    scaleX = 0.5f
                                    scaleY = 0.5f
                                }
                            }
                        }
                        .size(100.dp)
                        .background(Color.Red)
                )
            }
        }

        composeTestRule.onNodeWithTag("box")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "not_hardware_accelerated_activity")
    }
}

class NotHardwareAcceleratedActivity : ComponentActivity()
