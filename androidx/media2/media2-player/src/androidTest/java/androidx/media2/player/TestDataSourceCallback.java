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

package androidx.media2.player;

import android.content.res.AssetFileDescriptor;
import android.util.Log;

import androidx.media2.common.DataSourceCallback;

import java.io.IOException;
import java.io.InputStream;

/**
 * A DataSourceCallback that reads from a byte array for use in tests.
 */
public class TestDataSourceCallback extends DataSourceCallback {
    private static final String TAG = "TestDataSourceCallback";

    private byte[] mData;

    private Long mThrowFromReadAtPosition;
    private boolean mThrowFromGetSize;
    private Long mReturnFromGetSize;
    private boolean mIsClosed;

    // Read an asset fd into a new byte array media item. Closes afd.
    public static TestDataSourceCallback fromAssetFd(AssetFileDescriptor afd) throws IOException {
        try {
            InputStream in = afd.createInputStream();
            final int size = (int) afd.getDeclaredLength();
            byte[] data = new byte[size];
            int writeIndex = 0;
            int numRead;
            do {
                numRead = in.read(data, writeIndex, size - writeIndex);
                writeIndex += numRead;
            } while (numRead >= 0);
            return new TestDataSourceCallback(data);
        } finally {
            afd.close();
        }
    }

    public TestDataSourceCallback(byte[] data) {
        mData = data;
    }

    @Override
    public synchronized int readAt(long position, byte[] buffer, int offset, int size)
            throws IOException {
        if (mThrowFromReadAtPosition != null
                && position <= mThrowFromReadAtPosition
                && position + size > mThrowFromReadAtPosition) {
            throw new IOException("Test exception from readAt()");
        }

        // Clamp reads past the end of the source.
        if (position >= mData.length) {
            return -1; // -1 indicates EOF
        }
        if (position + size > mData.length) {
            size -= (position + size) - mData.length;
        }
        System.arraycopy(mData, (int) position, buffer, offset, size);
        return size;
    }

    @Override
    public synchronized long getSize() throws IOException {
        if (mThrowFromGetSize) {
            throw new IOException("Test exception from getSize()");
        }
        if (mReturnFromGetSize != null) {
            return mReturnFromGetSize;
        }

        Log.v(TAG, "getSize: " + mData.length);
        return mData.length;
    }

    // Note: it's fine to keep using this media item after closing it.
    @Override
    public synchronized void close() {
        Log.v(TAG, "close()");
        mIsClosed = true;
    }

    // Whether close() has been called.
    public synchronized boolean isClosed() {
        return mIsClosed;
    }

    public synchronized void throwFromReadAtPosition(long position) {
        mThrowFromReadAtPosition = position;
    }

    public synchronized void throwFromGetSize() {
        mThrowFromGetSize = true;
    }

    public synchronized void returnFromGetSize(long size) {
        mReturnFromGetSize = size;
    }
}

