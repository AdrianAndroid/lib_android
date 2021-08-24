/*
 * Copyright 2017 The Android Open Source Project
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
package com.example.android.supportv7.widget.selection.fancy;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;

import com.example.android.supportv7.R;

final class DemoItemHolder extends DemoHolder {

    private static final String TAG = "SelectionDemos";
    private final LinearLayout mContainer;
    private final TextView mSelector;
    private final TextView mLabel;
    private final ItemDetails<Uri> mDetails;

    private @Nullable Uri mKey;

    DemoItemHolder(@NonNull Context context, @NonNull ViewGroup parent) {
        this(inflateLayout(context, parent, R.layout.selection_demo_list_item));
    }

    private DemoItemHolder(LinearLayout layout) {
        super(layout);

        mContainer = layout.findViewById(R.id.container);
        mSelector = layout.findViewById(R.id.selector);
        mLabel = layout.findViewById(R.id.label);
        mDetails = new ItemDetails<Uri>() {
            @Override
            public int getPosition() {
                return DemoItemHolder.this.getAbsoluteAdapterPosition();
            }

            @Override
            public Uri getSelectionKey() {
                return DemoItemHolder.this.mKey;
            }

            @Override
            public boolean inDragRegion(@NonNull MotionEvent e) {
                return DemoItemHolder.this.inDragRegion(e);
            }

            @Override
            public boolean inSelectionHotspot(@NonNull MotionEvent e) {
                return DemoItemHolder.this.inSelectRegion(e);
            }

            @NonNull
            @Override
            public String toString() {
                return DemoItemHolder.this.toString();
            }
        };

        mLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setBackgroundColor(0xAA000000);
                Log.d(TAG, "Unexpected click received on item: " + mDetails.getSelectionKey());
            }
        });
    }

    @Override
    void update(@NonNull Uri uri) {
        mKey = uri;
        mLabel.setText(Uris.getCheese(uri));
    }

    void setSmallLayoutMode(boolean small) {
        mSelector.setVisibility(small ? View.GONE : View.VISIBLE);
        mLabel.setTextSize(Dimension.SP, small ? 14f : 20f);
    }

    void setSelected(boolean selected) {
        mContainer.setActivated(selected);
        mSelector.setActivated(selected);
    }

    boolean inDragRegion(MotionEvent event) {
        // If itemView is activated = selected, then whole region is interactive
        if (itemView.isActivated()) {
            return true;
        }

        // Do everything in global coordinates - it makes things simpler.
        int[] coords = new int[2];
        mSelector.getLocationOnScreen(coords);

        Rect textBounds = new Rect();
        mLabel.getPaint().getTextBounds(
                mLabel.getText().toString(), 0, mLabel.getText().length(), textBounds);

        Rect rect = new Rect(
                coords[0],
                coords[1],
                coords[0] + mSelector.getWidth() + textBounds.width(),
                coords[1] + Math.max(mSelector.getHeight(), textBounds.height()));

        // If the tap occurred inside icon or the text, these are interactive spots.
        return rect.contains((int) event.getRawX(), (int) event.getRawY());
    }

    boolean inSelectRegion(MotionEvent e) {
        Rect iconRect = new Rect();
        return mSelector.getGlobalVisibleRect(iconRect)
                && iconRect.contains((int) e.getRawX(), (int) e.getRawY());
    }

    ItemDetails<Uri> getItemDetails() {
        return mDetails;
    }

    @Override
    public String toString() {
        return "Item{name:" + mLabel.getText() + ", url:" + mKey + "}";
    }
}
