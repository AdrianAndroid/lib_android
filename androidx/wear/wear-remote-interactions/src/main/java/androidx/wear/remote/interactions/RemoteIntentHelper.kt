/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.wear.remote.interactions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcel
import android.os.ResultReceiver
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.remote.interactions.RemoteInteractionsUtil.isCurrentDeviceAWatch
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Support for opening android intents on other devices.
 *
 *
 * The following example opens play store for the given app on another device:
 *
 * ```
 * val remoteIntentHelper = RemoteIntentHelper(context, executor)
 *
 * val result = remoteIntentHelper.startRemoteActivity(
 *     new Intent(Intent.ACTION_VIEW).setData(
 *         Uri.parse("http://play.google.com/store/apps/details?id=com.example.myapp")
 *     ),
 *     nodeId
 * )
 * ```
 *
 * [startRemoteActivity] returns a [ListenableFuture], which is completed after the intent has
 * been sent or failed if there was an issue with sending the intent.
 *
 * @param context The [Context] of the application for sending the intent.
 * @param executor [Executor] used for getting data to be passed in remote intent. If not
 * specified, default will be `Executors.newSingleThreadExecutor()`.
 */
public class RemoteIntentHelper(
    private val context: Context,
    private val executor: Executor = Executors.newSingleThreadExecutor()
) {
    public companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val ACTION_REMOTE_INTENT: String =
            "com.google.android.wearable.intent.action.REMOTE_INTENT"

        private const val EXTRA_INTENT: String = "com.google.android.wearable.intent.extra.INTENT"

        private const val EXTRA_NODE_ID: String = "com.google.android.wearable.intent.extra.NODE_ID"

        private const val EXTRA_RESULT_RECEIVER: String =
            "com.google.android.wearable.intent.extra.RESULT_RECEIVER"

        /**
         * Result code passed to [ResultReceiver.send] when a remote intent was sent successfully.
         */
        public const val RESULT_OK: Int = 0

        /** Result code passed to [ResultReceiver.send] when a remote intent failed to send.  */
        public const val RESULT_FAILED: Int = 1

        internal const val DEFAULT_PACKAGE = "com.google.android.wearable.app"

        /**
         * Creates [android.content.IntentFilter] with action specifying remote intent.
         */
        @JvmStatic
        public fun createActionRemoteIntentFilter(): IntentFilter =
            IntentFilter(ACTION_REMOTE_INTENT)

        /**
         * Checks whether action of the given [android.content.Intent] specifies the remote intent.
         */
        @JvmStatic
        public fun isActionRemoteIntent(intent: Intent): Boolean =
            ACTION_REMOTE_INTENT == intent.action

        /**
         * Checks whether the given [android.content.IntentFilter] has action that specifies the
         * remote intent.
         */
        @JvmStatic
        public fun hasActionRemoteIntent(intentFilter: IntentFilter): Boolean =
            intentFilter.hasAction(ACTION_REMOTE_INTENT)

        /**
         * Returns the [android.content.Intent] extra specifying remote intent.
         *
         * @param intent The intent holding configuration.
         * @return The remote intent, or null if none was set.
         */
        @JvmStatic
        public fun getRemoteIntentExtraIntent(intent: Intent): Intent? =
            intent.getParcelableExtra(EXTRA_INTENT)

        /**
         * Returns the [String] extra specifying node ID of remote intent.
         *
         * @param intent The intent holding configuration.
         * @return The node id, or null if none was set.
         */
        @JvmStatic
        public fun getRemoteIntentNodeId(intent: Intent): String? =
            intent.getStringExtra(EXTRA_NODE_ID)

        /**
         * Returns the [android.os.ResultReceiver] extra of remote intent.
         *
         * @param intent The intent holding configuration.
         * @return The result receiver, or null if none was set.
         */
        @JvmStatic
        internal fun getRemoteIntentResultReceiver(intent: Intent): ResultReceiver? =
            intent.getParcelableExtra(EXTRA_RESULT_RECEIVER)

        /** Re-package a result receiver as a vanilla version for cross-process sending  */
        @JvmStatic
        internal fun getResultReceiverForSending(receiver: ResultReceiver): ResultReceiver {
            val parcel = Parcel.obtain()
            receiver.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel)
            parcel.recycle()
            return receiverForSending
        }
    }

    /**
     * Used for testing only, so we can set mock NodeClient.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var nodeClient: NodeClient = Wearable.getNodeClient(context)

    /**
     * Start an activity on another device. This api currently supports sending intents with
     * action set to [android.content.Intent.ACTION_VIEW], a data uri populated using
     * [android.content.Intent.setData], and with the category
     * [android.content.Intent.CATEGORY_BROWSABLE] present. If the current device is a watch,
     * the activity will start on the companion phone device. Otherwise, the activity will
     * start on all connected watch devices.
     *
     * @param intent         The intent to open on the remote device. Action must be set to
     *                       [android.content.Intent.ACTION_VIEW], a data uri must be populated
     *                       using [android.content.Intent.setData], and the category
     *                       [android.content.Intent.CATEGORY_BROWSABLE] must be present.
     * @param nodeId         Wear OS node id for the device where the activity should be
     *                       started. If null, and the current device is a watch, the
     *                       activity will start on the companion phone device. Otherwise,
     *                       the activity will start on all connected watch devices.
     * @return The [ListenableFuture] which resolves if starting activity was successful or
     * throws [Exception] if any errors happens. If there's a problem with starting remote
     * activity, [RemoteIntentException] will be thrown.
     */
    @JvmOverloads
    public fun startRemoteActivity(
        intent: Intent,
        nodeId: String? = null,
    ): ListenableFuture<Void> {
        return CallbackToFutureAdapter.getFuture {
            require(Intent.ACTION_VIEW == intent.action) {
                "Only ${Intent.ACTION_VIEW} action is currently supported for starting a" +
                    " remote activity"
            }
            requireNotNull(intent.data) { "Data Uri is required when starting a remote activity" }
            require(intent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true) {
                "The category ${Intent.CATEGORY_BROWSABLE} must be present on the intent"
            }

            startCreatingIntentForRemoteActivity(
                intent, nodeId, it, nodeClient,
                object : Callback {
                    override fun intentCreated(intent: Intent) {
                        context.sendBroadcast(intent)
                    }

                    override fun onFailure(exception: Exception) {
                        it.setException(exception)
                    }
                }
            )
        }
    }

    private fun startCreatingIntentForRemoteActivity(
        intent: Intent,
        nodeId: String?,
        completer: CallbackToFutureAdapter.Completer<Void>,
        nodeClient: NodeClient,
        callback: Callback
    ) {
        if (isCurrentDeviceAWatch(context)) {
            callback.intentCreated(
                createIntent(
                    intent,
                    RemoteIntentResultReceiver(completer, numNodes = 1),
                    nodeId,
                    DEFAULT_PACKAGE
                )
            )
            return
        }

        if (nodeId != null) {
            nodeClient.getCompanionPackageForNode(nodeId)
                .addOnCompleteListener(
                    executor,
                    { taskPackageName ->
                        val packageName = taskPackageName.result ?: DEFAULT_PACKAGE
                        callback.intentCreated(
                            createIntent(
                                intent,
                                RemoteIntentResultReceiver(completer, numNodes = 1),
                                nodeId,
                                packageName
                            )
                        )
                    }
                ).addOnFailureListener(executor, { callback.onFailure(it) })
            return
        }

        nodeClient.connectedNodes.addOnCompleteListener(
            executor,
            { taskConnectedNodes ->
                val connectedNodes = taskConnectedNodes.result
                val resultReceiver = RemoteIntentResultReceiver(completer, connectedNodes.size)
                for (node in connectedNodes) {
                    nodeClient.getCompanionPackageForNode(node.id).addOnCompleteListener(
                        executor,
                        { taskPackageName ->
                            val packageName = taskPackageName.result ?: DEFAULT_PACKAGE
                            callback.intentCreated(
                                createIntent(intent, resultReceiver, node.id, packageName)
                            )
                        }
                    ).addOnFailureListener(executor, { callback.onFailure(it) })
                }
            }
        ).addOnFailureListener(executor, { callback.onFailure(it) })
    }

    /**
     * Creates [android.content.Intent] with action specifying remote intent. If any of
     * additional extras are specified, they will be added to it. If specified, [ResultReceiver]
     * will be re-packed to be parcelable. If specified, packageName will be set.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createIntent(
        extraIntent: Intent?,
        resultReceiver: ResultReceiver?,
        nodeId: String?,
        packageName: String? = null
    ): Intent {
        val remoteIntent = Intent(ACTION_REMOTE_INTENT)
        // Put the extra when non-null value is passed in
        extraIntent?.let { remoteIntent.putExtra(EXTRA_INTENT, extraIntent) }
        resultReceiver?.let {
            remoteIntent.putExtra(
                EXTRA_RESULT_RECEIVER,
                getResultReceiverForSending(resultReceiver)
            )
        }
        nodeId?.let { remoteIntent.putExtra(EXTRA_NODE_ID, nodeId) }
        packageName?.let { remoteIntent.setPackage(packageName) }
        return remoteIntent
    }

    /**
     * Result code passed to [ResultReceiver.send] for the status of remote intent.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(RESULT_OK, RESULT_FAILED)
    @Retention(AnnotationRetention.SOURCE)
    public annotation class SendResult

    public class RemoteIntentException(message: String) : Exception(message)

    private interface Callback {
        fun intentCreated(intent: Intent)
        fun onFailure(exception: Exception)
    }

    private class RemoteIntentResultReceiver(
        private val completer: CallbackToFutureAdapter.Completer<Void>,
        private var numNodes: Int
    ) : ResultReceiver(null) {
        private var numFailedResults: Int = 0

        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            numNodes--
            if (resultCode != RESULT_OK) numFailedResults++
            // Don't send result if not all nodes have finished.
            if (numNodes > 0) return

            if (numFailedResults == 0) {
                completer.set(null)
            } else {
                completer.setException(
                    RemoteIntentException("There was an error while starting remote activity.")
                )
            }
        }
    }
}
