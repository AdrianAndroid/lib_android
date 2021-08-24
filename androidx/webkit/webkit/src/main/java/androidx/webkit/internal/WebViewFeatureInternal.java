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

package androidx.webkit.internal;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.SafeBrowsingResponseCompat;
import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.TracingConfig;
import androidx.webkit.TracingController;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebResourceErrorCompat;
import androidx.webkit.WebResourceRequestCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.chromium.support_lib_boundary.util.Features;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Enum representing a WebView feature, this provides functionality for determining whether a
 * feature is supported by the current framework and/or WebView APK.
 */
public enum WebViewFeatureInternal implements ConditionallySupportedFeature {
    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#postVisualStateCallback(android.webkit.WebView, long,
     * androidx.webkit.WebViewCompat.VisualStateCallback)}, and
     * {@link WebViewClientCompat#onPageCommitVisible(android.webkit.WebView, String)}.
     */
    VISUAL_STATE_CALLBACK(WebViewFeature.VISUAL_STATE_CALLBACK,
            Features.VISUAL_STATE_CALLBACK, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getOffscreenPreRaster(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setOffscreenPreRaster(WebSettings, boolean)}.
     */
    OFF_SCREEN_PRERASTER(WebViewFeature.OFF_SCREEN_PRERASTER, Features.OFF_SCREEN_PRERASTER,
            Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getSafeBrowsingEnabled(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setSafeBrowsingEnabled(WebSettings, boolean)}.
     */
    SAFE_BROWSING_ENABLE(WebViewFeature.SAFE_BROWSING_ENABLE, Features.SAFE_BROWSING_ENABLE,
            Build.VERSION_CODES.O),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getDisabledActionModeMenuItems(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setDisabledActionModeMenuItems(WebSettings, int)}.
     */
    DISABLED_ACTION_MODE_MENU_ITEMS(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
            Features.DISABLED_ACTION_MODE_MENU_ITEMS, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#startSafeBrowsing(Context, ValueCallback)}.
     */
    START_SAFE_BROWSING(WebViewFeature.START_SAFE_BROWSING, Features.START_SAFE_BROWSING,
            Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(Set,
     * ValueCallback)}, plumbing through the deprecated boundary interface.
     *
     * <p>Don't use this value directly. This exists only so {@link WebViewFeature#isSupported}
     * supports the <b>deprecated</b> public feature when running against <b>old</b> WebView
     * versions.
     *
     * @deprecated use {@link #SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_DEPRECATED} to test for the
     * <b>old</b> boundary interface
     */
    @Deprecated
    SAFE_BROWSING_ALLOWLIST_DEPRECATED_TO_DEPRECATED(WebViewFeature.SAFE_BROWSING_WHITELIST,
            Features.SAFE_BROWSING_WHITELIST, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(Set,
     * ValueCallback)}, plumbing through the new boundary interface.
     *
     * <p>Don't use this value directly. This exists only so {@link WebViewFeature#isSupported}
     * supports the <b>deprecated</b> public feature when running against <b>new</b> WebView
     * versions.
     *
     * @deprecated use {@link #SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_PREFERRED} to test for the
     * <b>new</b> boundary interface.
     */
    @Deprecated
    SAFE_BROWSING_ALLOWLIST_DEPRECATED_TO_PREFERRED(WebViewFeature.SAFE_BROWSING_WHITELIST,
            Features.SAFE_BROWSING_ALLOWLIST, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingAllowlist(Set,
     * ValueCallback)}, plumbing through the deprecated boundary interface.
     */
    SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_DEPRECATED(WebViewFeature.SAFE_BROWSING_ALLOWLIST,
            Features.SAFE_BROWSING_WHITELIST, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingAllowlist(Set,
     * ValueCallback)}, plumbing through the new boundary interface.
     */
    SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_PREFERRED(WebViewFeature.SAFE_BROWSING_ALLOWLIST,
            Features.SAFE_BROWSING_ALLOWLIST, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link WebViewCompat#getSafeBrowsingPrivacyPolicyUrl()}.
     */
    SAFE_BROWSING_PRIVACY_POLICY_URL(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL,
            Features.SAFE_BROWSING_PRIVACY_POLICY_URL, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerControllerCompat#getInstance()}.
     */
    SERVICE_WORKER_BASIC_USAGE(WebViewFeature.SERVICE_WORKER_BASIC_USAGE,
            Features.SERVICE_WORKER_BASIC_USAGE, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getCacheMode()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setCacheMode(int)}.
     */
    SERVICE_WORKER_CACHE_MODE(WebViewFeature.SERVICE_WORKER_CACHE_MODE,
            Features.SERVICE_WORKER_CACHE_MODE, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getAllowContentAccess()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setAllowContentAccess(boolean)}.
     */
    SERVICE_WORKER_CONTENT_ACCESS(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS,
            Features.SERVICE_WORKER_CONTENT_ACCESS, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getAllowFileAccess()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setAllowFileAccess(boolean)}.
     */
    SERVICE_WORKER_FILE_ACCESS(WebViewFeature.SERVICE_WORKER_FILE_ACCESS,
            Features.SERVICE_WORKER_FILE_ACCESS, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getBlockNetworkLoads()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setBlockNetworkLoads(boolean)}.
     */
    SERVICE_WORKER_BLOCK_NETWORK_LOADS(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS,
            Features.SERVICE_WORKER_BLOCK_NETWORK_LOADS, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link ServiceWorkerClientCompat#shouldInterceptRequest(WebResourceRequest)}.
     */
    SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST,
            Features.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link WebViewClientCompat#onReceivedError(android.webkit.WebView, WebResourceRequest,
     * WebResourceErrorCompat)}.
     */
    RECEIVE_WEB_RESOURCE_ERROR(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR,
            Features.RECEIVE_WEB_RESOURCE_ERROR, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebViewClientCompat#onReceivedHttpError(android.webkit.WebView, WebResourceRequest,
     * WebResourceResponse)}.
     */
    RECEIVE_HTTP_ERROR(WebViewFeature.RECEIVE_HTTP_ERROR, Features.RECEIVE_HTTP_ERROR,
            Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebViewClientCompat#shouldOverrideUrlLoading(android.webkit.WebView,
     * WebResourceRequest)}.
     */
    SHOULD_OVERRIDE_WITH_REDIRECTS(WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS,
            Features.SHOULD_OVERRIDE_WITH_REDIRECTS, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link WebViewClientCompat#onSafeBrowsingHit(android.webkit.WebView,
     * WebResourceRequest, int, SafeBrowsingResponseCompat)}.
     */
    SAFE_BROWSING_HIT(WebViewFeature.SAFE_BROWSING_HIT, Features.SAFE_BROWSING_HIT,
            Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link WebResourceRequestCompat#isRedirect(WebResourceRequest)}.
     */
    WEB_RESOURCE_REQUEST_IS_REDIRECT(WebViewFeature.WEB_RESOURCE_REQUEST_IS_REDIRECT,
            Features.WEB_RESOURCE_REQUEST_IS_REDIRECT, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link WebResourceErrorCompat#getDescription()}.
     */
    WEB_RESOURCE_ERROR_GET_DESCRIPTION(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION,
            Features.WEB_RESOURCE_ERROR_GET_DESCRIPTION, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebResourceErrorCompat#getErrorCode()}.
     */
    WEB_RESOURCE_ERROR_GET_CODE(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE,
            Features.WEB_RESOURCE_ERROR_GET_CODE, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#backToSafety(boolean)}.
     */
    SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
            Features.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#proceed(boolean)}.
     */
    SAFE_BROWSING_RESPONSE_PROCEED(WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED,
            Features.SAFE_BROWSING_RESPONSE_PROCEED, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#showInterstitial(boolean)}.
     */
    SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL(
            WebViewFeature.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL,
            Features.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link WebMessagePortCompat#postMessage(WebMessageCompat)}.
     */
    WEB_MESSAGE_PORT_POST_MESSAGE(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE,
            Features.WEB_MESSAGE_PORT_POST_MESSAGE, Build.VERSION_CODES.M),

    /**
     * * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#close()}.
     */
    WEB_MESSAGE_PORT_CLOSE(WebViewFeature.WEB_MESSAGE_PORT_CLOSE, Features.WEB_MESSAGE_PORT_CLOSE,
            Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebMessagePortCompat#setWebMessageCallback(
     * WebMessagePortCompat.WebMessageCallbackCompat)}, and
     * {@link WebMessagePortCompat#setWebMessageCallback(Handler,
     * WebMessagePortCompat.WebMessageCallbackCompat)}.
     */
     WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK(WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK,
            Features.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebViewCompat#createWebMessageChannel(WebView)}.
     */
    CREATE_WEB_MESSAGE_CHANNEL(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL,
            Features.CREATE_WEB_MESSAGE_CHANNEL, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    POST_WEB_MESSAGE(WebViewFeature.POST_WEB_MESSAGE, Features.POST_WEB_MESSAGE,
            Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    WEB_MESSAGE_CALLBACK_ON_MESSAGE(WebViewFeature.WEB_MESSAGE_CALLBACK_ON_MESSAGE,
            Features.WEB_MESSAGE_CALLBACK_ON_MESSAGE, Build.VERSION_CODES.M),

    /**
     * This feature covers {@link WebViewCompat#getWebViewClient(WebView)}.
     */
    GET_WEB_VIEW_CLIENT(WebViewFeature.GET_WEB_VIEW_CLIENT, Features.GET_WEB_VIEW_CLIENT,
            Build.VERSION_CODES.O),

    /**
     * This feature covers {@link WebViewCompat#getWebChromeClient(WebView)}.
     */
    GET_WEB_CHROME_CLIENT(WebViewFeature.GET_WEB_CHROME_CLIENT, Features.GET_WEB_CHROME_CLIENT,
            Build.VERSION_CODES.O),

    GET_WEB_VIEW_RENDERER(WebViewFeature.GET_WEB_VIEW_RENDERER, Features.GET_WEB_VIEW_RENDERER,
            Build.VERSION_CODES.Q),
    WEB_VIEW_RENDERER_TERMINATE(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE,
            Features.WEB_VIEW_RENDERER_TERMINATE, Build.VERSION_CODES.Q),

    /**
     * This feature covers
     * {@link TracingController#getInstance()},
     * {@link TracingController#isTracing()},
     * {@link TracingController#start(TracingConfig)},
     * {@link TracingController#stop(OutputStream, Executor)}.
     */
    TRACING_CONTROLLER_BASIC_USAGE(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE,
            Features.TRACING_CONTROLLER_BASIC_USAGE, Build.VERSION_CODES.P),

    /**
     * This feature covers
     * {@link WebViewCompat#getWebViewRenderProcessClient()},
     * {@link WebViewCompat#setWebViewRenderProcessClient(WebViewRenderProcessClient)},
     * {@link WebViewRenderProcessClient#onRenderProcessUnresponsive(WebView,WebViewRenderProcess)},
     * {@link WebViewRenderProcessClient#onRenderProcessResponsive(WebView,WebViewRenderProcess)}
     */
    WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
            Features.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE, Build.VERSION_CODES.Q),

    /**
     * This feature covers
     * {@link ProxyController#setProxyOverride(ProxyConfig, Executor, Runnable)},
     * {@link ProxyController#setProxyOverride(ProxyConfig, Runnable)},
     * {@link ProxyController#clearProxyOverride(Executor, Runnable)}, and
     * {@link ProxyController#clearProxyOverride(Runnable)}.
     */
    PROXY_OVERRIDE(WebViewFeature.PROXY_OVERRIDE, Features.PROXY_OVERRIDE),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#willSuppressErrorPage(WebSettings)} and
     * {@link androidx.webkit.WebSettingsCompat#setWillSuppressErrorPage(WebSettings, boolean)}.
     */
    SUPPRESS_ERROR_PAGE(WebViewFeature.SUPPRESS_ERROR_PAGE, Features.SUPPRESS_ERROR_PAGE),

    /**
     * This feature covers {@link WebViewCompat#isMultiProcessEnabled()}.
     */
    MULTI_PROCESS(WebViewFeature.MULTI_PROCESS, Features.MULTI_PROCESS_QUERY),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setForceDark(WebSettings, int)} and
     * {@link androidx.webkit.WebSettingsCompat#getForceDark(WebSettings)}.
     */
    FORCE_DARK(WebViewFeature.FORCE_DARK, Features.FORCE_DARK),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setForceDarkStrategy(WebSettings, int)} and
     * {@link androidx.webkit.WebSettingsCompat#getForceDarkStrategy(WebSettings)}.
     */
    FORCE_DARK_STRATEGY(WebViewFeature.FORCE_DARK_STRATEGY, Features.FORCE_DARK_BEHAVIOR),

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#setWebMessageListener(android.webkit.WebView,
     * androidx.webkit.WebViewCompat.WebMessageListener, String, String[])} and
     * {@link androidx.webkit.WebViewCompat#removeWebMessageListener()}
     */
    WEB_MESSAGE_LISTENER(WebViewFeature.WEB_MESSAGE_LISTENER, Features.WEB_MESSAGE_LISTENER),

    /**
     * This feature covers
     * {@link
     * androidx.webkit.WebViewCompat#addDocumentStartJavaScript(android.webkit.WebView, String,
     * Set)}
     */
    DOCUMENT_START_SCRIPT(WebViewFeature.DOCUMENT_START_SCRIPT, Features.DOCUMENT_START_SCRIPT),

    /**
     * This feature covers {@link androidx.webkit.ProxyConfig.Builder.setReverseBypass(boolean)}
     */
    PROXY_OVERRIDE_REVERSE_BYPASS(WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS,
            Features.PROXY_OVERRIDE_REVERSE_BYPASS),

    ;  // This semicolon ends the enum. Add new features with a trailing comma above this line.

    private static final int NOT_SUPPORTED_BY_FRAMEWORK = -1;
    private final String mPublicFeatureValue;
    private final String mInternalFeatureValue;
    private final int mOsVersion;

    /**
     * Creates a WebViewFeatureInternal that does not correspond to a framework API.
     *
     * <p>Features constructed with this constructor can be later converted to use the
     * other constructor if framework support is added.
     *
     * @param publicFeatureValue   The public facing feature string denoting this feature
     * @param internalFeatureValue The internal feature string denoting this feature
     */
    WebViewFeatureInternal(@NonNull @WebViewFeature.WebViewSupportFeature String publicFeatureValue,
            @NonNull String internalFeatureValue) {
        this(publicFeatureValue, internalFeatureValue, NOT_SUPPORTED_BY_FRAMEWORK);
    }

    /**
     * Creates a WebViewFeatureInternal that is implemented in the framework.
     *
     * @param publicFeatureValue   The public facing feature string denoting this feature
     * @param internalFeatureValue The internal feature string denoting this feature
     * @param osVersion            The Android SDK level after which this feature is implemented
     *                             in the framework.
     */
    WebViewFeatureInternal(@NonNull @WebViewFeature.WebViewSupportFeature String publicFeatureValue,
            @NonNull String internalFeatureValue, int osVersion) {
        mPublicFeatureValue = publicFeatureValue;
        mInternalFeatureValue = internalFeatureValue;
        mOsVersion = osVersion;
    }

    /**
     * Return whether a public feature is supported by any internal features defined in this enum.
     */
    public static boolean isSupported(
            @NonNull @WebViewFeature.WebViewSupportFeature String publicFeatureValue) {
        Set<ConditionallySupportedFeature> features = new HashSet<>();
        for (WebViewFeatureInternal feature : WebViewFeatureInternal.values()) {
            features.add(feature);
        }
        return isSupported(publicFeatureValue, features);
    }

    /**
     * Return whether a public feature is supported by any {@link ConditionallySupportedFeature}s
     * defined in {@code internalFeatures}.
     *
     * @throws RuntimeException if {@code publicFeatureValue} is not matched in
     *      {@code internalFeatures}
     */
    @VisibleForTesting
    public static boolean isSupported(
            @NonNull @WebViewFeature.WebViewSupportFeature String publicFeatureValue,
            @NonNull Collection<ConditionallySupportedFeature> internalFeatures) {
        Set<ConditionallySupportedFeature> matchingFeatures = new HashSet<>();
        for (ConditionallySupportedFeature feature : internalFeatures) {
            if (feature.getPublicFeatureName().equals(publicFeatureValue)) {
                matchingFeatures.add(feature);
            }
        }
        if (matchingFeatures.isEmpty()) {
            throw new RuntimeException("Unknown feature " + publicFeatureValue);
        }
        for (ConditionallySupportedFeature feature : matchingFeatures) {
            if (feature.isSupported()) return true;
        }
        return false;
    }

    /**
     * Return whether this {@link WebViewFeatureInternal} is supported by the framework of the
     * current device.
     */
    public boolean isSupportedByFramework() {
        if (mOsVersion == NOT_SUPPORTED_BY_FRAMEWORK) {
            return false;
        }
        return Build.VERSION.SDK_INT >= mOsVersion;
    }

    /**
     * Return whether this {@link WebViewFeatureInternal} is supported by the current WebView APK.
     */
    public boolean isSupportedByWebView() {
        return BoundaryInterfaceReflectionUtil.containsFeature(
                LAZY_HOLDER.WEBVIEW_APK_FEATURES, mInternalFeatureValue);
    }

    @Override
    @NonNull
    public String getPublicFeatureName() {
        return mPublicFeatureValue;
    }

    @Override
    public boolean isSupported() {
        return isSupportedByFramework() || isSupportedByWebView();
    }

    private static class LAZY_HOLDER {
        static final Set<String> WEBVIEW_APK_FEATURES =
                new HashSet<>(
                        Arrays.asList(WebViewGlueCommunicator.getFactory().getWebViewFeatures()));
    }

    @NonNull
    public static Set<String> getWebViewApkFeaturesForTesting() {
        return LAZY_HOLDER.WEBVIEW_APK_FEATURES;
    }

    /**
     * Utility method for throwing an exception explaining that the feature the app trying to use
     * isn't supported.
     */
    @NonNull
    public static UnsupportedOperationException getUnsupportedOperationException() {
        return new UnsupportedOperationException("This method is not supported by the current "
                + "version of the framework and the current WebView APK");
    }
}
