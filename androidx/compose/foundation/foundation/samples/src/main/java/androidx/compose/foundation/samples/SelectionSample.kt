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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer

@Sampled
@Composable
fun SelectionSample() {
    SelectionContainer {
        Column {
            Text("Text 1")
            Text("Text 2")
            Text("טקסט 3")
        }
    }
}

@Sampled
@Composable
fun DisableSelectionSample() {
    SelectionContainer {
        Column {
            Text("Text 1")

            DisableSelection {
                Text("Text 2")
                Text("טקסט 3")
            }

            Text("Text 3")
        }
    }
}