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

package com.example.androidx.webkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link Activity} for exercising various WebView functionality. This Activity is a {@link
 * ListView} which starts other Activities, each of which may similarly be a ListView, or may
 * actually exercise specific {@link android.webkit.WebView} features.
 */
public class AssetLoaderListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_asset_loader_list);
        setTitle(R.string.asset_loader_list_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        final Context activityContext = this;
        MenuListView listView = findViewById(R.id.asset_loader_list);
        MenuListView.MenuItem[] menuItems = new MenuListView.MenuItem[] {
                new MenuListView.MenuItem(
                        getResources().getString(R.string.asset_loader_simple_activity_title),
                        new Intent(activityContext, AssetLoaderSimpleActivity.class)),
                new MenuListView.MenuItem(
                    getResources().getString(R.string.asset_loader_ajax_activity_title),
                    new Intent(activityContext, AssetLoaderAjaxActivity.class)),
                new MenuListView.MenuItem(
                    getResources().getString(R.string.asset_loader_internal_storage_activity_title),
                    new Intent(activityContext, AssetLoaderInternalStorageActivity.class)),
        };
        listView.setItems(menuItems);
    }
}
