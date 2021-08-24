/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.appcompat;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toolbar;

/**
 * No-op extension activity for the AppCompat Lint demo
 */
public class AppCompatLintDemoExt extends AppCompatLintDemo {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            // The following call should be flagged since we're extending AppCompatActivity
            setActionBar(new Toolbar(this));
        }
    }
}
