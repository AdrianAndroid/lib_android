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

package androidx.compose.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class ClearFocusTest(private val forced: Boolean) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "forcedClear = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun active_isCleared() {
        // Arrange.
        val modifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun active_isClearedAndRemovedFromParentsFocusedChild() {
        // Arrange.
        val parent = FocusModifier(ActiveParent)
        val modifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = parent) {
                Box(modifier = modifier)
            }
            SideEffect {
                parent.focusedChild = modifier.focusNode
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_noFocusedChild_throwsException() {
        // Arrange.
        val modifier = FocusModifier(ActiveParent)
        rule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }
    }

    @Test
    fun activeParent_isClearedAndRemovedFromParentsFocusedChild() {
        // Arrange.
        val parent = FocusModifier(ActiveParent)
        val modifier = FocusModifier(ActiveParent)
        val child = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = parent) {
                Box(modifier = modifier) {
                    Box(modifier = child)
                }
            }
            SideEffect {
                parent.focusedChild = modifier.focusNode
                modifier.focusedChild = child.focusNode
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(modifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun activeParent_clearsEntireHierarchy() {
        // Arrange.
        val modifier = FocusModifier(ActiveParent)
        val child = FocusModifier(ActiveParent)
        val grandchild = FocusModifier(ActiveParent)
        val greatGrandchild = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = modifier) {
                Box(modifier = child) {
                    Box(modifier = grandchild) {
                        Box(modifier = greatGrandchild)
                    }
                }
            }
            SideEffect {
                modifier.focusedChild = child.focusNode
                child.focusedChild = grandchild.focusNode
                grandchild.focusedChild = greatGrandchild.focusNode
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(child.focusedChild).isNull()
            assertThat(grandchild.focusedChild).isNull()
            assertThat(modifier.focusState).isEqualTo(Inactive)
            assertThat(child.focusState).isEqualTo(Inactive)
            assertThat(grandchild.focusState).isEqualTo(Inactive)
            assertThat(greatGrandchild.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun captured_isCleared_whenForced() {
        // Arrange.
        val modifier = FocusModifier(Captured)
        rule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            when (forced) {
                true -> {
                    assertThat(cleared).isTrue()
                    assertThat(modifier.focusState).isEqualTo(Inactive)
                }
                false -> {
                    assertThat(cleared).isFalse()
                    assertThat(modifier.focusState).isEqualTo(Captured)
                }
            }
        }
    }

    @Test
    fun active_isClearedAndRemovedFromParentsFocusedChild_whenForced() {
        // Arrange.
        val parent = FocusModifier(ActiveParent)
        val modifier = FocusModifier(Captured)
        rule.setFocusableContent {
            Box(modifier = parent) {
                Box(modifier = modifier)
            }
            SideEffect {
                parent.focusedChild = modifier.focusNode
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            when (forced) {
                true -> {
                    assertThat(cleared).isTrue()
                    assertThat(modifier.focusState).isEqualTo(Inactive)
                }
                false -> {
                    assertThat(cleared).isFalse()
                    assertThat(modifier.focusState).isEqualTo(Captured)
                }
            }
        }
    }

    @Test
    fun Inactive_isUnchanged() {
        // Arrange.
        val modifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun Disabled_isUnchanged() {
        // Arrange.
        val modifier = FocusModifier(Disabled)
        rule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.focusNode.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusState).isEqualTo(Disabled)
        }
    }
}