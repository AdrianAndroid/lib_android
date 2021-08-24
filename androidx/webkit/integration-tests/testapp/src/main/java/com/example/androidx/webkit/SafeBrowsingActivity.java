/*
 * Copyright 2018 The Android Open Source Project
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
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link Activity} to exercise Safe Browsing functionality.
 */
public class SafeBrowsingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(this.getApplicationContext(),
                    value -> {
                        if (value) {
                            setupLayout();
                        } else {
                            TextView t = WebkitHelpers.showMessageInActivity(
                                    SafeBrowsingActivity.this,
                                    R.string.cannot_start_safe_browsing);
                            t.setOnClickListener(v -> showSafeBrowsingRequirementsInBrowser());
                        }
                    });
        } else {
            WebkitHelpers.showMessageInActivity(SafeBrowsingActivity.this,
                    R.string.webkit_api_not_available);
        }
    }

    private void showSafeBrowsingRequirementsInBrowser() {
        // Open documentation for WebView Safe Browsing to help the user
        // debug what's wrong.
        Uri safeBrowsingRequirementsUri = new Uri.Builder()
                .scheme("https")
                .authority("chromium.googlesource.com")
                .path("/chromium/src/+/main/android_webview/browser/safe_browsing/README.md")
                .encodedFragment("opt_in_consent_requirements")
                .build();

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(safeBrowsingRequirementsUri);
        startActivity(i);
    }

    private void setupLayout() {
        setContentView(R.layout.activity_safe_browsing);
        setTitle(R.string.safebrowsing_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        final Context activityContext = this;
        MenuListView listView = findViewById(R.id.safe_browsing_list);
        MenuListView.MenuItem[] menuItems = new MenuListView.MenuItem[] {
                new MenuListView.MenuItem(
                        getResources().getString(R.string.small_interstitial_activity_title),
                        new Intent(activityContext, SmallInterstitialActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.medium_wide_interstitial_activity_title),
                        new Intent(activityContext, MediumInterstitialActivity.class)
                                .putExtra(MediumInterstitialActivity.LAYOUT_HORIZONTAL, false)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.medium_tall_interstitial_activity_title),
                        new Intent(activityContext, MediumInterstitialActivity.class)
                                .putExtra(MediumInterstitialActivity.LAYOUT_HORIZONTAL, true)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.loud_interstitial_activity_title),
                        new Intent(activityContext, LoudInterstitialActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.giant_interstitial_activity_title),
                        new Intent(activityContext, GiantInterstitialActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.per_web_view_enable_activity_title),
                        new Intent(activityContext, PerWebViewEnableActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.invisible_activity_title),
                        new Intent(activityContext, InvisibleActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.unattached_activity_title),
                        new Intent(activityContext, UnattachedActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.custom_interstitial_activity_title),
                        new Intent(activityContext, CustomInterstitialActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.allowlist_activity_title),
                        new Intent(activityContext, AllowlistActivity.class)),
        };
        listView.setItems(menuItems);
    }
}
