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

package androidx.compose.ui.input.mouse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MouseHoverFilterTest {
    private val window = TestComposeWindow(width = 100, height = 100, density = Density(2f))

    @Test
    fun `inside window`() {
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        window.setContent {
            Box(
                modifier = Modifier
                    .pointerMoveFilter(
                        onMove = {
                            moveCount++
                            false
                        },
                        onEnter = {
                            enterCount++
                            false
                        },
                        onExit = {
                            exitCount++
                            false
                        }
                    )
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseMoved(
            x = 10,
            y = 20
        )
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(1)

        window.onMouseMoved(
            x = 10,
            y = 15
        )
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(2)

        window.onMouseMoved(
            x = 30,
            y = 30
        )
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(1)
        assertThat(moveCount).isEqualTo(2)
    }

    @Test
    fun `window enter`() {
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        window.setContent {
            Box(
                modifier = Modifier
                    .pointerMoveFilter(
                        onMove = {
                            moveCount++
                            false
                        },
                        onEnter = {
                            enterCount++
                            false
                        },
                        onExit = {
                            exitCount++
                            false
                        }
                    )
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseEntered(
            x = 10,
            y = 20
        )
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(0)

        window.onMouseExited()
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(1)
        assertThat(moveCount).isEqualTo(0)
    }
}