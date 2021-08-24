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
package androidx.remotecallback;

import static androidx.remotecallback.BroadcastReceiverWithCallbacks.ACTION_BROADCAST_CALLBACK;
import static androidx.remotecallback.RemoteCallback.EXTRA_METHOD;
import static androidx.remotecallback.RemoteCallback.TYPE_RECEIVER;

import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.RestrictTo;

/**
 * Version of {@link AppWidgetProvider} that implements a {@link CallbackReceiver}.
 *
 * @param <T> Should be specified as the root class (e.g. class X extends
 *           AppWidgetProviderWithCallbacks\<X>)
 */
public class AppWidgetProviderWithCallbacks<T extends CallbackReceiver> extends
        AppWidgetProvider implements CallbackReceiver<T>, CallbackBase<T> {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_BROADCAST_CALLBACK.equals(intent.getAction())) {
            CallbackHandlerRegistry.sInstance.invokeCallback(context, this, intent);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public T createRemoteCallback(Context context) {
        return CallbackHandlerRegistry.sInstance.getAndResetStub(getClass(), context, null);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Override
    public RemoteCallback toRemoteCallback(Class<T> cls, Context context, String authority,
            Bundle args, String method) {
        Intent intent = new Intent(ACTION_BROADCAST_CALLBACK);
        intent.setComponent(new ComponentName(context.getPackageName(), cls.getName()));
        args.putString(EXTRA_METHOD, method);
        intent.putExtras(args);
        return new RemoteCallback(context, TYPE_RECEIVER, intent, cls.getName(), args);
    }
}
