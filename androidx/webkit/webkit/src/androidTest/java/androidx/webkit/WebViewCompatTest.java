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

package androidx.webkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Looper;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewCompatTest {
    WebViewOnUiThread mWebViewOnUiThread;

    @Before
    public void setUp() {
        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testVisualStateCallbackCalled. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testVisualStateCallbackCalled() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.VISUAL_STATE_CALLBACK);

        final long kRequest = 100;

        mWebViewOnUiThread.loadUrl("about:blank");

        final ResolvableFuture<Long> visualStateFuture = ResolvableFuture.create();
        mWebViewOnUiThread.postVisualStateCallbackCompat(kRequest, visualStateFuture::set);

        assertEquals(kRequest, (long) WebkitUtils.waitForFuture(visualStateFuture));
    }

    @Test
    public void testCheckThread() {
        // Skip this test if VisualStateCallback is not supported.
        WebkitUtils.checkFeature(WebViewFeature.VISUAL_STATE_CALLBACK);
        try {
            WebViewCompat.postVisualStateCallback(
                    mWebViewOnUiThread.getWebViewOnCurrentThread(), 5, requestId -> {});
        } catch (RuntimeException e) {
            return;
        }
        fail("Calling a WebViewCompat method on the wrong thread must cause a run-time exception");
    }

    private static class MockContext extends ContextWrapper {
        private boolean mGetApplicationContextWasCalled;

        MockContext(Context context) {
            super(context);
        }

        public Context getApplicationContext() {
            mGetApplicationContextWasCalled = true;
            return super.getApplicationContext();
        }

        public boolean wasGetApplicationContextCalled() {
            return mGetApplicationContextWasCalled;
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingUseApplicationContext. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingUseApplicationContext() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        final MockContext ctx =
                new MockContext(
                        ApplicationProvider.getApplicationContext().getApplicationContext());
        final ResolvableFuture<Boolean> startSafeBrowsingFuture = ResolvableFuture.create();
        WebViewCompat.startSafeBrowsing(ctx,
                value -> startSafeBrowsingFuture.set(ctx.wasGetApplicationContextCalled()));
        assertTrue(WebkitUtils.waitForFuture(startSafeBrowsingFuture));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingWithNullCallbackDoesntCrash.
     * Modifications to this test should be reflected in that test as necessary. See
     * http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingWithNullCallbackDoesntCrash() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        WebViewCompat.startSafeBrowsing(ApplicationProvider.getApplicationContext(), null);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingInvokesCallback. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingInvokesCallback() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        final ResolvableFuture<Boolean> startSafeBrowsingFuture = ResolvableFuture.create();
        WebViewCompat.startSafeBrowsing(
                ApplicationProvider.getApplicationContext().getApplicationContext(),
                value -> startSafeBrowsingFuture.set(Looper.getMainLooper().isCurrentThread()));
        assertTrue(WebkitUtils.waitForFuture(startSafeBrowsingFuture));
    }

    private static boolean setSafeBrowsingAllowlistSync(Set<String> allowlist) {
        final ResolvableFuture<Boolean> safeBrowsingAllowlistFuture = ResolvableFuture.create();
        WebViewCompat.setSafeBrowsingAllowlist(allowlist,
                safeBrowsingAllowlistFuture::set);
        return WebkitUtils.waitForFuture(safeBrowsingAllowlistFuture);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testSetSafeBrowsingAllowlistWithMalformedList. Modifications
     * to this test should be reflected in that test as necessary. See
     * http://go/modifying-webview-cts.
     */
    @Test
    public void testSetSafeBrowsingAllowlistWithMalformedList() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_ALLOWLIST);

        Set<String> allowlist = new HashSet<>();
        // Protocols are not supported in the allowlist
        allowlist.add("http://google.com");
        assertFalse("Malformed list entry should fail", setSafeBrowsingAllowlistSync(allowlist));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testSetSafeBrowsingAllowlistWithValidList. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testSetSafeBrowsingAllowlistWithValidList() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_ALLOWLIST);
        // This test relies on the onSafeBrowsingHit callback to verify correctness.
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_HIT);

        Set<String> allowlist = new HashSet<>();
        allowlist.add("safe-browsing");
        assertTrue("Valid allowlist should be successful", setSafeBrowsingAllowlistSync(allowlist));

        final ResolvableFuture<Void> pageFinishedFuture = ResolvableFuture.create();
        mWebViewOnUiThread.setWebViewClient(new WebViewClientCompat() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageFinishedFuture.set(null);
            }

            @Override
            public void onSafeBrowsingHit(@NonNull WebView view,
                    @NonNull WebResourceRequest request, int threatType,
                    @NonNull SafeBrowsingResponseCompat callback) {
                pageFinishedFuture.setException(new IllegalStateException(
                        "Should not invoke onSafeBrowsingHit"));
            }
        });

        mWebViewOnUiThread.loadUrl("chrome://safe-browsing/match?type=malware");

        // Wait until page load has completed
        WebkitUtils.waitForFuture(pageFinishedFuture);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetSafeBrowsingPrivacyPolicyUrl. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetSafeBrowsingPrivacyPolicyUrl() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL);

        assertNotNull(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl());
        try {
            new URL(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl().toString());
        } catch (MalformedURLException e) {
            Assert.fail("The privacy policy URL should be a well-formed URL");
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetWebViewClient. Modifications to this test should be
     * reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetWebViewClient() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_CLIENT);

        // Create a new WebView because WebViewOnUiThread sets a WebViewClient during
        // construction.
        WebView webView = WebViewOnUiThread.createWebView();

        // getWebViewClient should return a default WebViewClient if it hasn't been set yet
        WebViewClient client = WebViewOnUiThread.getWebViewClient(webView);
        assertNotNull(client);
        assertTrue(client instanceof WebViewClient);

        // getWebViewClient should return the client after it has been set
        WebViewClient client2 = new WebViewClient();
        assertNotSame(client, client2);
        WebViewOnUiThread.setWebViewClient(webView, client2);
        assertSame(client2, WebViewOnUiThread.getWebViewClient(webView));

        WebViewOnUiThread.destroy(webView);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetWebChromeClient. Modifications to this test should be
     * reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetWebChromeClient() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_CHROME_CLIENT);

        // Create a new WebView because WebViewOnUiThread sets a WebChromeClient during
        // construction.
        WebView webView = WebViewOnUiThread.createWebView();

        // getWebChromeClient should return null if the client hasn't been set yet
        WebChromeClient client = WebViewOnUiThread.getWebChromeClient(webView);
        assertNull(client);

        // getWebChromeClient should return the client after it has been set
        WebChromeClient client2 = new WebChromeClient();
        assertNotSame(client, client2);
        WebViewOnUiThread.setWebChromeClient(webView, client2);
        assertSame(client2, WebViewOnUiThread.getWebChromeClient(webView));

        WebViewOnUiThread.destroy(webView);
    }

    /**
     * This test should have an equivalent in CTS when this is implemented in the framework.
     */
    @Test
    public void testMultiProcess() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROCESS);
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_RENDERER);

        // Creates a new WebView for non static getWebViewRenderProcess method
        WebView webView = WebViewOnUiThread.createWebView();

        // Asserts that if WebView is running in multi process, render process is not null
        WebViewRenderProcess renderer = WebkitUtils.onMainThreadSync(
                () -> WebViewCompat.getWebViewRenderProcess(webView));
        assertEquals(WebViewCompat.isMultiProcessEnabled(), renderer != null);

        WebViewOnUiThread.destroy(webView);
    }

    /**
     * WebViewCompat.getCurrentWebViewPackage should be null on pre-L devices.
     * On L+ devices WebViewCompat.getCurrentWebViewPackage should be null only in exceptional
     * circumstances - like when the WebView APK is being updated, or for Wear devices. The L+
     * devices used in support library testing should have a non-null WebView package.
     */
    @Test
    public void testGetCurrentWebViewPackage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            assertNull(WebViewCompat.getCurrentWebViewPackage(
                    ApplicationProvider.getApplicationContext()));
        } else {
            assertNotNull(
                    WebViewCompat.getCurrentWebViewPackage(
                            ApplicationProvider.getApplicationContext()));
        }
    }
}
