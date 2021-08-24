/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.samples.DraggableSample
import androidx.compose.foundation.samples.FocusableSample
import androidx.compose.foundation.samples.ScrollableSample
import androidx.compose.foundation.samples.TransformableSample
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HighLevelGesturesDemo() {
    Column {
        DraggableSample()
        Spacer(Modifier.height(50.dp))
        ScrollableSample()
        Spacer(Modifier.height(50.dp))
        TransformableSample()
        Spacer(Modifier.height(50.dp))
        FocusableSample()
    }
}