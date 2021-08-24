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

package androidx.core.content;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

import static androidx.core.content.IntentCompat.APP_HIBERNATION_DISABLED;
import static androidx.core.content.IntentCompat.APP_HIBERNATION_ENABLED;
import static androidx.core.content.IntentCompat.PERMISSION_REVOCATION_DISABLED;
import static androidx.core.content.IntentCompat.PERMISSION_REVOCATION_ENABLED;
import static androidx.core.content.IntentCompat.UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE;
import static androidx.core.content.IntentCompat.UNUSED_APP_RESTRICTION_STATUS_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
/** Tests for {@link IntentCompat}. */
public class IntentCompatTest {

    private Context mContext;
    private PackageManager mPackageManager = mock(PackageManager.class);
    private static final String PACKAGE_NAME = "package.name";
    private static final String VERIFIER_PACKAGE_NAME = "verifier.package.name";
    private static final String VERIFIER_PACKAGE_NAME2 = "verifier.package.name.2";
    private static final String NON_VERIFIER_PACKAGE_NAME = "non.verifier.package.name";

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    @SdkSuppress(maxSdkVersion = ICE_CREAM_SANDWICH)
    public void makeMainSelectorActivity_preApi14() {
        String selectorAction = Intent.ACTION_MAIN;
        String selectorCategory = Intent.CATEGORY_APP_BROWSER;

        Intent activityIntent = IntentCompat.makeMainSelectorActivity(selectorAction,
                selectorCategory);

        assertThat(activityIntent.getAction()).isEqualTo(selectorAction);
        assertThat(activityIntent.getCategories()).containsExactly(selectorCategory);
    }

    @Test
    @SdkSuppress(minSdkVersion = ICE_CREAM_SANDWICH_MR1)
    public void makeMainSelectorActivity() {
        String selectorAction = Intent.ACTION_MAIN;
        String selectorCategory = Intent.CATEGORY_APP_BROWSER;

        Intent activityIntent = IntentCompat.makeMainSelectorActivity(selectorAction,
                selectorCategory);

        Intent expectedIntent = Intent.makeMainSelectorActivity(selectorAction,
                selectorCategory);
        assertThat(activityIntent.filterEquals(expectedIntent)).isTrue();
    }

    @Test
    // TODO: replace with VERSION_CODES.S once it's defined
    @SdkSuppress(minSdkVersion = 31)
    public void createManageUnusedAppRestrictionsIntent_api31Plus() {
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction()).isEqualTo(ACTION_APPLICATION_DETAILS_SETTINGS);
        assertThat(activityIntent.getData()).isEqualTo(
                Uri.fromParts("package", PACKAGE_NAME, /* fragment= */ null));
    }

    @Test
    @SdkSuppress(minSdkVersion = R, maxSdkVersion = R)
    public void createManageUnusedAppRestrictionsIntent_api30() {
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction())
                .isEqualTo("android.intent.action.AUTO_REVOKE_PERMISSIONS");
        assertThat(activityIntent.getData()).isEqualTo(
                Uri.fromParts("package", PACKAGE_NAME, /* fragment= */ null));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_noRevocationApp() {
        // Don't install an app that can resolve the permission auto-revocation intent

        assertThrows(UnsupportedOperationException.class,
                () -> IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, NON_VERIFIER_PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_noVerifierRevocationApp() {
        setupPermissionRevocationApps(Arrays.asList(NON_VERIFIER_PACKAGE_NAME));
        // Do not set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                NON_VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_DENIED);

        assertThrows(UnsupportedOperationException.class,
                () -> IntentCompat.createManageUnusedAppRestrictionsIntent(
                        mContext, PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_verifierRevocationApp() {
        setupPermissionRevocationApps(Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction()).isEqualTo(
                "android.intent.action.AUTO_REVOKE_PERMISSIONS");
        assertThat(activityIntent.getPackage()).isEqualTo(VERIFIER_PACKAGE_NAME);
        assertThat(activityIntent.getData()).isEqualTo(
                Uri.fromParts("package", PACKAGE_NAME, /* fragment= */ null));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void createManageUnusedAppRestrictionsIntent_preApi30_manyVerifierRevocationApps() {
        setupPermissionRevocationApps(Arrays.asList(VERIFIER_PACKAGE_NAME, VERIFIER_PACKAGE_NAME2));
        // Set both apps as the Verifier on the device, but we should fail gracefully.
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME2)).thenReturn(PERMISSION_GRANTED);
        Intent activityIntent = IntentCompat.createManageUnusedAppRestrictionsIntent(
                mContext, PACKAGE_NAME);

        assertThat(activityIntent.getAction()).isEqualTo(
                "android.intent.action.AUTO_REVOKE_PERMISSIONS");
        // Verify that we use the first Verifier's package name.
        assertThat(activityIntent.getPackage()).isEqualTo(VERIFIER_PACKAGE_NAME);
        assertThat(activityIntent.getData()).isEqualTo(
                Uri.fromParts("package", PACKAGE_NAME, /* fragment= */ null));
    }

    @Test
    @SdkSuppress(maxSdkVersion = LOLLIPOP)
    public void createManageUnusedAppRestrictionsIntent_preApi23() {
        assertThrows(UnsupportedOperationException.class,
                () -> IntentCompat.createManageUnusedAppRestrictionsIntent(
                        mContext, PACKAGE_NAME));
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void getUnusedAppRestrictionsStatus_api31Plus_disabled_returnsAppHibernationDisabled() {
        // Mark the application as exempt from app hibernation, so the feature is disabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(true);

        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                APP_HIBERNATION_DISABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void getUnusedAppRestrictionsStatus_api31Plus_enabled_returnsAppHibernationEnabled() {
        // Mark the application as _not_ exempt from app hibernation, so the feature is enabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(false);

        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                APP_HIBERNATION_ENABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = R, maxSdkVersion = R)
    public void getUnusedAppRestrictionsStatus_api30_disabled_returnsPermRevocationDisabled() {
        // Mark the application as exempt from permission revocation, so the feature is disabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(true);

        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                PERMISSION_REVOCATION_DISABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = R)
    public void getUnusedAppRestrictionsStatus_api30Plus_enabled_returnsPermRevocationEnabled() {
        // Mark the application as _not_ exempt from permission revocation, so the feature is
        // enabled
        when(mPackageManager.isAutoRevokeWhitelisted()).thenReturn(false);

        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                PERMISSION_REVOCATION_ENABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_noRevocationApp_returnsNotAvailable() {
        // Don't install an app that can resolve the permission auto-revocation intent

        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_noVerifierRevokeApp_returnsNotAvailable() {
        setupPermissionRevocationApps(Arrays.asList(NON_VERIFIER_PACKAGE_NAME));
        // Do not set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                NON_VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_DENIED);

        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_verifierRevocationApp_returnsUnknown() {
        setupPermissionRevocationApps(Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Set this app as the Verifier on the device
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);

        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                UNUSED_APP_RESTRICTION_STATUS_UNKNOWN);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void getUnusedAppRestrictionsStatus_preApi30_manyVerifierRevocationApps_doesNotThrow() {
        setupPermissionRevocationApps(Arrays.asList(VERIFIER_PACKAGE_NAME, VERIFIER_PACKAGE_NAME2));
        // Set both apps as the Verifier on the device, but we should have a graceful failure.
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME)).thenReturn(PERMISSION_GRANTED);
        when(mPackageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                VERIFIER_PACKAGE_NAME2)).thenReturn(PERMISSION_GRANTED);

        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                UNUSED_APP_RESTRICTION_STATUS_UNKNOWN);
    }

    @Test
    @SdkSuppress(maxSdkVersion = LOLLIPOP)
    public void getUnusedAppRestrictionsStatus_preApi23_returnsFeatureNotAvailable() {
        assertThat(IntentCompat.getUnusedAppRestrictionsStatus(mContext)).isEqualTo(
                UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE);
    }

    /**
     * Setup applications that can handle unused app restriction features. In this case,
     * they are permission revocation apps.
     */
    private void setupPermissionRevocationApps(List<String> packageNames) {
        List<ResolveInfo> resolveInfos = new ArrayList<>();

        for (String packageName : packageNames) {
            ApplicationInfo appInfo = new ApplicationInfo();
            appInfo.uid = 12345;
            appInfo.packageName = packageName;

            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = packageName;
            activityInfo.name = "Name needed to keep toString() happy :)";
            activityInfo.applicationInfo = appInfo;

            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            resolveInfo.providerInfo = new ProviderInfo();
            resolveInfo.providerInfo.name = "Name needed to keep toString() happy :)";

            resolveInfos.add(resolveInfo);
        }

        // Mark the applications as being able to resolve the AUTO_REVOKE_PERMISSIONS intent
        when(mPackageManager.queryIntentActivities(
                nullable(Intent.class), eq(0))).thenReturn(resolveInfos);
    }
}
