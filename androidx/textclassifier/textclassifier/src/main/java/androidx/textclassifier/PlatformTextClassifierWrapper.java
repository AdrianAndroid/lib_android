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

package androidx.textclassifier;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

/**
 * Provides a {@link androidx.textclassifier.TextClassifier} interface for a
 * {@link android.view.textclassifier.TextClassifier} object.
 */
@RequiresApi(Build.VERSION_CODES.O)
final class PlatformTextClassifierWrapper extends TextClassifier {
    private final android.view.textclassifier.TextClassifier mPlatformTextClassifier;
    private final Context mContext;
    private final TextClassifier mFallback;

    @VisibleForTesting
    PlatformTextClassifierWrapper(
            @NonNull Context context,
            @NonNull android.view.textclassifier.TextClassifier platformTextClassifier) {
        mContext = Preconditions.checkNotNull(context);
        mPlatformTextClassifier = Preconditions.checkNotNull(platformTextClassifier);
        mFallback = LegacyTextClassifier.of(context);
    }

    /**
     * Returns a newly create instance of PlatformTextClassifierWrapper.
     */
    @NonNull
    static PlatformTextClassifierWrapper create(@NonNull Context context) {
        android.view.textclassifier.TextClassificationManager textClassificationManager =
                (android.view.textclassifier.TextClassificationManager)
                        context.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE);
        android.view.textclassifier.TextClassifier platformTextClassifier =
                textClassificationManager.getTextClassifier();

        return new PlatformTextClassifierWrapper(context, platformTextClassifier);
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextSelection suggestSelection(@NonNull TextSelection.Request request) {
        Preconditions.checkNotNull(request);
        ensureNotOnMainThread();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return TextSelection.fromPlatform(
                    mPlatformTextClassifier.suggestSelection(
                            (android.view.textclassifier.TextSelection.Request)
                                    request.toPlatform()));
        }
        return TextSelection.fromPlatform(
                mPlatformTextClassifier.suggestSelection(
                        request.getText(),
                        request.getStartIndex(),
                        request.getEndIndex(),
                        ConvertUtils.unwrapLocalListCompat(request.getDefaultLocales())));
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextClassification classifyText(@NonNull TextClassification.Request request) {
        Preconditions.checkNotNull(request);
        ensureNotOnMainThread();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return TextClassification.fromPlatform(mContext,
                    mPlatformTextClassifier.classifyText(
                            (android.view.textclassifier.TextClassification.Request)
                                    request.toPlatform()));
        }
        TextClassification textClassification = TextClassification.fromPlatform(mContext,
                mPlatformTextClassifier.classifyText(
                        request.getText(),
                        request.getStartIndex(),
                        request.getEndIndex(),
                        ConvertUtils.unwrapLocalListCompat(request.getDefaultLocales())));
        return textClassification;
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextLinks generateLinks(@NonNull TextLinks.Request request) {
        Preconditions.checkNotNull(request);
        ensureNotOnMainThread();
        if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.P) {
            return TextLinks.fromPlatform(mPlatformTextClassifier.generateLinks(
                    request.toPlatform()), request.getText());
        }
        return mFallback.generateLinks(request);
    }

    @NonNull
    @Override
    public ConversationActions suggestConversationActions(
            @NonNull ConversationActions.Request request) {
        Preconditions.checkNotNull(request);
        ensureNotOnMainThread();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ConversationActions.fromPlatform(
                    mPlatformTextClassifier.suggestConversationActions(
                            request.toPlatform()));
        }
        return mFallback.suggestConversationActions(request);
    }
}
