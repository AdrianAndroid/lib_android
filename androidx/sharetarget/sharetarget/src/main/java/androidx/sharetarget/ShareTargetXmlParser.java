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

package androidx.sharetarget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to parse the list of {@link ShareTargetCompat} from app's Xml resource.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
class ShareTargetXmlParser {

    static final String TAG = "ShareTargetXmlParser";

    private static final String META_DATA_APP_SHORTCUTS = "android.app.shortcuts";

    private static final String TAG_SHARE_TARGET = "share-target";
    private static final String ATTR_TARGET_CLASS = "targetClass";

    private static final String TAG_DATA = "data";
    private static final String ATTR_SCHEME = "scheme";
    private static final String ATTR_HOST = "host";
    private static final String ATTR_PORT = "port";
    private static final String ATTR_PATH = "path";
    private static final String ATTR_PATH_PATTERN = "pathPattern";
    private static final String ATTR_PATH_PREFIX = "pathPrefix";
    private static final String ATTR_MIME_TYPE = "mimeType";

    private static final String TAG_CATEGORY = "category";
    private static final String ATTR_NAME = "name";

    // List of share targets loaded from app's manifest. Will not change while the app is running.
    private static volatile ArrayList<ShareTargetCompat> sShareTargets;
    private static final Object GET_INSTANCE_LOCK = new Object();

    @WorkerThread
    static ArrayList<ShareTargetCompat> getShareTargets(Context context) {
        if (sShareTargets == null) {
            synchronized (GET_INSTANCE_LOCK) {
                if (sShareTargets == null) {
                    sShareTargets = parseShareTargets(context);
                }
            }
        }
        return sShareTargets;
    }

    private ShareTargetXmlParser() {
        /* Hide the constructor */
    }

    private static ArrayList<ShareTargetCompat> parseShareTargets(Context context) {
        ArrayList<ShareTargetCompat> targets = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(context.getPackageName());

        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(
                mainIntent, PackageManager.GET_META_DATA);
        if (resolveInfos == null) {
            return targets;
        }

        for (ResolveInfo info : resolveInfos) {
            ActivityInfo activityInfo = info.activityInfo;
            Bundle metaData = activityInfo.metaData;
            if (metaData != null && metaData.containsKey(META_DATA_APP_SHORTCUTS)) {
                List<ShareTargetCompat> shareTargets = parseShareTargets(context, activityInfo);
                targets.addAll(shareTargets);
            }
        }

        return targets;
    }

    private static ArrayList<ShareTargetCompat> parseShareTargets(Context context,
            ActivityInfo activityInfo) {
        ArrayList<ShareTargetCompat> targets = new ArrayList<>();
        XmlResourceParser parser = getXmlResourceParser(context, activityInfo);

        try {
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && parser.getName().equals(TAG_SHARE_TARGET)) {
                    ShareTargetCompat target = parseShareTarget(parser);
                    if (target != null) {
                        targets.add(target);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse the Xml resource: ", e);
        }

        parser.close();
        return targets;
    }

    private static XmlResourceParser getXmlResourceParser(Context context, ActivityInfo info) {
        XmlResourceParser parser = info.loadXmlMetaData(context.getPackageManager(),
                META_DATA_APP_SHORTCUTS);
        if (parser == null) {
            throw new IllegalArgumentException("Failed to open " + META_DATA_APP_SHORTCUTS
                    + " meta-data resource of " + info.name);
        }

        return parser;
    }

    private static ShareTargetCompat parseShareTarget(XmlResourceParser parser) throws Exception {
        String targetClass = getAttributeValue(parser, ATTR_TARGET_CLASS);
        ArrayList<ShareTargetCompat.TargetData> targetData = new ArrayList<>();
        ArrayList<String> categories = new ArrayList<>();

        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case TAG_DATA:
                        targetData.add(parseTargetData(parser));
                        break;
                    case TAG_CATEGORY:
                        categories.add(getAttributeValue(parser, ATTR_NAME));
                        break;
                }
            } else if (type == XmlPullParser.END_TAG && parser.getName().equals(TAG_SHARE_TARGET)) {
                break;
            }
        }
        if (targetData.isEmpty() || targetClass == null || categories.isEmpty()) {
            return null;
        }
        return new ShareTargetCompat(
                targetData.toArray(new ShareTargetCompat.TargetData[targetData.size()]),
                targetClass, categories.toArray(new String[categories.size()]));
    }

    private static ShareTargetCompat.TargetData parseTargetData(XmlResourceParser parser) {
        String scheme = getAttributeValue(parser, ATTR_SCHEME);
        String host = getAttributeValue(parser, ATTR_HOST);
        String port = getAttributeValue(parser, ATTR_PORT);
        String path = getAttributeValue(parser, ATTR_PATH);
        String pathPattern = getAttributeValue(parser, ATTR_PATH_PATTERN);
        String pathPrefix = getAttributeValue(parser, ATTR_PATH_PREFIX);
        String mimeType = getAttributeValue(parser, ATTR_MIME_TYPE);

        return new ShareTargetCompat.TargetData(scheme, host, port, path, pathPattern, pathPrefix,
                mimeType);
    }

    private static String getAttributeValue(XmlResourceParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res/android",
                attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }
}
