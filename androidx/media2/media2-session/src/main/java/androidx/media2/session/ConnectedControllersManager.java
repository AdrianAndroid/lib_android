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

package androidx.media2.session;

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.media2.session.MediaSession.ControllerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages connected {@link ControllerInfo}. This is thread-safe.
 * The generic T denotes a key of connected MediaController, and it can be either IBinder or
 * RemoteUserInfo.
 */
class ConnectedControllersManager<T> {
    static final String TAG = "MS2ControllerMgr";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final ArrayMap<T, ControllerInfo> mControllerInfoMap = new ArrayMap<>();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, ConnectedControllerRecord> mControllerRecords =
            new ArrayMap<>();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaSession.MediaSessionImpl mSessionImpl;

    ConnectedControllersManager(MediaSession.MediaSessionImpl session) {
        mSessionImpl = session;
    }

    public void addController(T controllerKey, ControllerInfo controllerInfo,
            SessionCommandGroup commands) {
        if (controllerKey == null || controllerInfo == null) {
            if (DEBUG) {
                throw new IllegalArgumentException("controllerKey and controllerInfo shouldn't be"
                        + " null");
            }
            return;
        }
        synchronized (mLock) {
            ControllerInfo savedInfo = getController(controllerKey);
            if (savedInfo == null) {
                mControllerInfoMap.put(controllerKey, controllerInfo);
                mControllerRecords.put(controllerInfo, new ConnectedControllerRecord(
                        controllerKey, new SequencedFutureManager(), commands));
            } else {
                // already exist. Only update allowed commands.
                ConnectedControllerRecord record = mControllerRecords.get(savedInfo);
                record.allowedCommands = commands;
            }
        }
        // TODO: Also notify controller connected.
    }

    public void updateAllowedCommands(ControllerInfo controllerInfo,
            SessionCommandGroup commands) {
        if (controllerInfo == null) {
            return;
        }
        synchronized (mLock) {
            ConnectedControllerRecord record = mControllerRecords.get(controllerInfo);
            if (record != null) {
                record.allowedCommands = commands;
                return;
            }
        }
        // TODO: Also notify allowed command changes here.
    }

    public void removeController(T controllerKey) {
        if (controllerKey == null) {
            return;
        }
        removeController(getController(controllerKey));
    }

    public void removeController(final ControllerInfo controllerInfo) {
        if (controllerInfo == null) {
            return;
        }
        ConnectedControllerRecord record;
        synchronized (mLock) {
            record = mControllerRecords.remove(controllerInfo);
            if (record == null) {
                return;
            }
            mControllerInfoMap.remove(record.controllerKey);
        }

        if (DEBUG) {
            Log.d(TAG, "Controller " + controllerInfo + " is disconnected");
        }
        record.sequencedFutureManager.close();
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                mSessionImpl.getCallback().onDisconnected(mSessionImpl.getInstance(),
                        controllerInfo);
            }
        });
    }

    public final List<ControllerInfo> getConnectedControllers() {
        ArrayList<ControllerInfo> controllers = new ArrayList<>();
        synchronized (mLock) {
            controllers.addAll(mControllerInfoMap.values());
        }
        return controllers;
    }

    public final boolean isConnected(ControllerInfo controllerInfo) {
        synchronized (mLock) {
            return mControllerRecords.get(controllerInfo) != null;
        }
    }

    /**
     * Gets the sequenced future manager.
     *
     * @param controllerInfo controller info
     * @return sequenced future manager. Can be {@code null} if the controller was null or
     *         disconencted.
     */
    @Nullable
    public final SequencedFutureManager getSequencedFutureManager(
            @Nullable ControllerInfo controllerInfo) {
        ConnectedControllerRecord info;
        synchronized (mLock) {
            info = mControllerRecords.get(controllerInfo);
        }
        return info != null ? info.sequencedFutureManager : null;
    }

    /**
     * Gets the sequenced future manager.
     *
     * @param controllerKey key
     * @return sequenced future manager. Can be {@code null} if the controller was null or
     *         disconnected.
     */
    public SequencedFutureManager getSequencedFutureManager(@Nullable T controllerKey) {
        ConnectedControllerRecord info;
        synchronized (mLock) {
            info = mControllerRecords.get(getController(controllerKey));
        }
        return info != null ? info.sequencedFutureManager : null;
    }

    public boolean isAllowedCommand(ControllerInfo controllerInfo, SessionCommand command) {
        ConnectedControllerRecord info;
        synchronized (mLock) {
            info = mControllerRecords.get(controllerInfo);
        }
        return info != null && info.allowedCommands.hasCommand(command);
    }

    public boolean isAllowedCommand(
            ControllerInfo controllerInfo, @SessionCommand.CommandCode int commandCode) {
        ConnectedControllerRecord info;
        synchronized (mLock) {
            info = mControllerRecords.get(controllerInfo);
        }
        return info != null && info.allowedCommands.hasCommand(commandCode);
    }

    public ControllerInfo getController(T controllerKey) {
        synchronized (mLock) {
            return mControllerInfoMap.get(controllerKey);
        }
    }

    private class ConnectedControllerRecord {
        public final T controllerKey;
        public final SequencedFutureManager sequencedFutureManager;
        public SessionCommandGroup allowedCommands;

        ConnectedControllerRecord(T controllerKey, SequencedFutureManager sequencedFutureManager,
                SessionCommandGroup allowedCommands) {
            this.controllerKey = controllerKey;
            this.sequencedFutureManager = sequencedFutureManager;
            this.allowedCommands = allowedCommands;
            if (this.allowedCommands == null) {
                this.allowedCommands = new SessionCommandGroup();
            }
        }
    }
}
