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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate;
import androidx.recyclerview.selection.testing.Bundles;
import androidx.recyclerview.selection.testing.SelectionProbe;
import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.selection.testing.TestSelectionObserver;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DefaultSelectionTrackerTest {

    private static final String SELECTION_ID = "test-selection";

    private Set<String> mIgnored;
    private TestAdapter<String> mAdapter;
    private SelectionPredicate<String> mSelectionPredicate;
    private ItemKeyProvider<String> mKeyProvider;
    private DefaultSelectionTracker<String> mTracker;
    private TestSelectionObserver<String> mListener;
    private SelectionProbe mSelection;

    @Before
    public void setUp() throws Exception {
        mIgnored = new HashSet<>();
        mAdapter = TestAdapter.createStringAdapter(100);
        mListener = new TestSelectionObserver<>();

        mSelectionPredicate = new SelectionPredicate<String>() {

            @Override
            public boolean canSetStateForKey(@NonNull String id, boolean nextState) {
                return !nextState || !mIgnored.contains(id);
            }

            @Override
            public boolean canSetStateAtPosition(int position, boolean nextState) {
                throw new UnsupportedOperationException("Not implemented.");
            }

            @Override
            public boolean canSelectMultiple() {
                return true;
            }
        };

        mKeyProvider = new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, mAdapter);

        mTracker = new DefaultSelectionTracker<>(
                SELECTION_ID,
                mKeyProvider,
                mSelectionPredicate,
                StorageStrategy.createStringStorage());

        EventBridge.install(
                mAdapter, mTracker, mKeyProvider, new Consumer<Runnable>() {
                    @Override
                    public void accept(Runnable runnable) {
                        runnable.run();
                    }
                });

        mTracker.addObserver(mListener);

        mSelection = new SelectionProbe(mTracker, mListener);

        mIgnored.clear();
    }

    @Test
    public void testSelect() {
        mTracker.select(mAdapter.getSelectionKey(7));

        mSelection.assertSelection(7);
    }

    @Test
    public void testDeselect() {
        mTracker.select(mAdapter.getSelectionKey(7));
        mTracker.deselect(mAdapter.getSelectionKey(7));

        mSelection.assertNoSelection();
    }

    @Test
    public void testSelection_DoNothingOnUnselectableItem() {
        mIgnored.add(mAdapter.getSelectionKey(7));
        boolean selected = mTracker.select(mAdapter.getSelectionKey(7));

        assertFalse(selected);
        mSelection.assertNoSelection();
    }

    @Test
    public void testSelection_UpdatedWhenAdapterItemsRemoved() {
        mTracker.select("2");
        mTracker.select("7");
        mTracker.select("11");

        mAdapter.removeItem("2");
        mSelection.assertSelection(7, 11);

        mAdapter.removeItem("7");
        mAdapter.removeItem("11");
        mSelection.assertNoSelection();
    }

    @Test
    public void testSelect_NotifiesListenersOfChange() {
        mTracker.select(mAdapter.getSelectionKey(7));

        mListener.assertSelectionChanged();
    }

    @Test
    public void testSelect_NotifiesListenersWhenSelectedItemRemoved() {
        mTracker.select("2");
        mTracker.select("7");
        mTracker.select("11");
        mListener.reset();
        mAdapter.removeItem("7");
        mListener.assertSelectionChanged();
    }

    @Test
    public void testSelect_NotifiesAdapterOfSelect() {
        mTracker.select(mAdapter.getSelectionKey(7));

        mAdapter.assertNotifiedOfSelectionChange(7);
    }

    @Test
    public void testSelect_NotifiesAdapterOfDeselect() {
        mTracker.select(mAdapter.getSelectionKey(7));
        mAdapter.resetSelectionNotifications();
        mTracker.deselect(mAdapter.getSelectionKey(7));
        mAdapter.assertNotifiedOfSelectionChange(7);
    }

    @Test
    public void testDeselect_SelectionChange_Notifies() {
        mTracker.select(mAdapter.getSelectionKey(7));
        mTracker.deselect(mAdapter.getSelectionKey(7));

        mListener.assertSelectionChanged();
    }

    @Test
    public void testClearSelection_ClearSelection_Notifies() {
        mTracker.select(mAdapter.getSelectionKey(7));
        mTracker.clearSelection();

        mListener.assertSelectionCleared();
    }

    // This test is important as failure to short circuit
    // would result in infinte recursion during reset operations.
    @Test
    public void testClearSelection_ClearSelection_EmptyDoesNotNotify() {
        // Already empty...but we'll try anyway.
        mTracker.clearSelection();

        assertFalse(mListener.wasSelectionCleared());
    }

    @Test
    public void testSelection_PersistsOnUpdate() {
        mTracker.select(mAdapter.getSelectionKey(7));

        List<String> newItems = new ArrayList<>();
        newItems.addAll(mAdapter.getItems().subList(0, 10));
        mAdapter.updateTestModelIds(newItems);

        mSelection.assertSelection(7);
    }

    @Test
    public void testSetItemsSelected() {
        mTracker.setItemsSelected(getStringIds(6, 7, 8), true);

        mSelection.assertRangeSelected(6, 8);
    }

    @Test
    public void testSetItemsSelected_SkipUnselectableItem() {
        mIgnored.add(mAdapter.getSelectionKey(7));

        mTracker.setItemsSelected(getStringIds(6, 7, 8), true);

        mSelection.assertSelected(6);
        mSelection.assertNotSelected(7);
        mSelection.assertSelected(8);
    }

    @Test
    public void testClearSelection_RemovesPrimarySelection() {
        mTracker.select(mAdapter.getSelectionKey(1));
        mTracker.select(mAdapter.getSelectionKey(2));

        assertTrue(mTracker.clearSelection());

        assertFalse(mTracker.hasSelection());
    }

    @Test
    public void testClearSelection_RemovesProvisionalSelection() {
        Set<String> prov = new HashSet<>();
        prov.add(mAdapter.getSelectionKey(1));
        prov.add(mAdapter.getSelectionKey(2));

        assertFalse(mTracker.clearSelection());
        assertFalse(mTracker.hasSelection());
    }

    @Test
    public void testRequiresReset_ForSelection() {
        mTracker.select(mAdapter.getSelectionKey(1));

        assertTrue(mTracker.isResetRequired());
    }

    @Test
    public void testRequiresReset_ForProvisionalSelection() {
        Set<String> items = new HashSet<>();
        items.add(mAdapter.getSelectionKey(1));

        mTracker.setProvisionalSelection(items);

        assertTrue(mTracker.isResetRequired());
    }

    @Test
    public void testRequiresReset_ForEstablishedRange() {
        mTracker.startRange(15);

        assertTrue(mTracker.isResetRequired());
    }

    @Test
    public void testReset_ForSelection() {
        mTracker.select(mAdapter.getSelectionKey(1));

        mTracker.reset();
        assertFalse(mTracker.isResetRequired());
    }

    @Test
    public void testReset_ForProvisionalSelection() {
        Set<String> items = new HashSet<>();
        items.add(mAdapter.getSelectionKey(1));
        mTracker.setProvisionalSelection(items);

        mTracker.reset();
        assertFalse(mTracker.isResetRequired());
    }

    @Test
    public void testReset_Combined() {
        mTracker.select(mAdapter.getSelectionKey(1));

        Set<String> items = new HashSet<>();
        items.add(mAdapter.getSelectionKey(1));
        mTracker.setProvisionalSelection(items);

        mTracker.startRange(15);

        mTracker.reset();
        assertFalse(mTracker.isResetRequired());
    }

    @Test
    public void testReset_ForEstablishedRange() {
        mTracker.startRange(15);

        mTracker.reset();
        assertFalse(mTracker.isResetRequired());
    }

    @Test
    public void testRangeSelection() {
        mTracker.startRange(15);
        mTracker.extendRange(19);
        mSelection.assertRangeSelection(15, 19);
    }

    @Test
    public void testRangeSelection_SkipUnselectableItem() {
        mIgnored.add(mAdapter.getSelectionKey(17));

        mTracker.startRange(15);
        mTracker.extendRange(19);

        mSelection.assertRangeSelected(15, 16);
        mSelection.assertNotSelected(17);
        mSelection.assertRangeSelected(18, 19);
    }

    @Test
    public void testRangeSelection_snapExpand() {
        mTracker.startRange(15);
        mTracker.extendRange(19);
        mTracker.extendRange(27);
        mSelection.assertRangeSelection(15, 27);
    }

    @Test
    public void testRangeSelection_snapContract() {
        mTracker.startRange(15);
        mTracker.extendRange(27);
        mTracker.extendRange(19);
        mSelection.assertRangeSelection(15, 19);
    }

    @Test
    public void testRangeSelection_snapInvert() {
        mTracker.startRange(15);
        mTracker.extendRange(27);
        mTracker.extendRange(3);
        mSelection.assertRangeSelection(3, 15);
    }

    @Test
    public void testRangeSelection_multiple() {
        mTracker.startRange(15);
        mTracker.extendRange(27);
        mTracker.endRange();
        mTracker.startRange(42);
        mTracker.extendRange(57);
        mSelection.assertSelectionSize(29);
        mSelection.assertRangeSelected(15, 27);
        mSelection.assertRangeSelected(42, 57);
    }

    @Test
    public void testProvisionalRangeSelection() {
        mTracker.startRange(13);
        mTracker.extendProvisionalRange(15);
        mSelection.assertRangeSelection(13, 15);
        mTracker.getSelection().mergeProvisionalSelection();
        mTracker.endRange();
        mSelection.assertSelectionSize(3);
    }

    @Test
    public void testProvisionalRangeSelection_endEarly() {
        mTracker.startRange(13);
        mTracker.extendProvisionalRange(15);
        mSelection.assertRangeSelection(13, 15);

        mTracker.endRange();
        // If we end range selection prematurely for provision selection, nothing should be selected
        // except the first item
        mSelection.assertSelectionSize(1);
    }

    @Test
    public void testProvisionalRangeSelection_snapExpand() {
        mTracker.startRange(13);
        mTracker.extendProvisionalRange(15);
        mSelection.assertRangeSelection(13, 15);
        mTracker.getSelection().mergeProvisionalSelection();
        mTracker.extendRange(18);
        mSelection.assertRangeSelection(13, 18);
    }

    @Test
    public void testCombinationRangeSelection_IntersectsOldSelection() {
        mTracker.startRange(13);
        mTracker.extendRange(15);
        mSelection.assertRangeSelection(13, 15);

        mTracker.startRange(11);
        mTracker.extendProvisionalRange(18);
        mSelection.assertRangeSelected(11, 18);
        mTracker.endRange();
        mSelection.assertRangeSelected(13, 15);
        mSelection.assertRangeSelected(11, 11);
        mSelection.assertSelectionSize(4);
    }

    @Test
    public void testProvisionalSelection() {
        Selection<String> s = mTracker.getSelection();
        mSelection.assertNoSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mAdapter.getSelectionKey(1), true);
        mListener.onItemStateChanged(mAdapter.getSelectionKey(2), true);

        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));
        mSelection.assertSelection(1, 2);
    }

    @Test
    public void testProvisionalSelection_Replace() {
        Selection<String> s = mTracker.getSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mAdapter.getSelectionKey(1), true);
        mListener.onItemStateChanged(mAdapter.getSelectionKey(2), true);
        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));

        mListener.onItemStateChanged(mAdapter.getSelectionKey(1), false);
        mListener.onItemStateChanged(mAdapter.getSelectionKey(2), false);
        provisional.clear();

        mListener.onItemStateChanged(mAdapter.getSelectionKey(3), true);
        mListener.onItemStateChanged(mAdapter.getSelectionKey(4), true);
        provisional.append(3, true);
        provisional.append(4, true);
        s.setProvisionalSelection(getItemIds(provisional));
        mSelection.assertSelection(3, 4);
    }

    @Test
    public void testProvisionalSelection_IntersectsExistingProvisionalSelection() {
        Selection<String> s = mTracker.getSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mAdapter.getSelectionKey(1), true);
        mListener.onItemStateChanged(mAdapter.getSelectionKey(2), true);
        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));

        mListener.onItemStateChanged(mAdapter.getSelectionKey(1), false);
        mListener.onItemStateChanged(mAdapter.getSelectionKey(2), false);
        provisional.clear();

        mListener.onItemStateChanged(mAdapter.getSelectionKey(1), true);
        provisional.append(1, true);
        s.setProvisionalSelection(getItemIds(provisional));
        mSelection.assertSelection(1);
    }

    @Test
    public void testProvisionalSelection_Apply() {
        Selection<String> s = mTracker.getSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mAdapter.getSelectionKey(1), true);
        mListener.onItemStateChanged(mAdapter.getSelectionKey(2), true);
        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));
        s.mergeProvisionalSelection();

        mSelection.assertSelection(1, 2);
    }

    @Test
    public void testProvisionalSelection_Cancel() {
        mTracker.select(mAdapter.getSelectionKey(1));
        mTracker.select(mAdapter.getSelectionKey(2));
        Selection<String> s = mTracker.getSelection();

        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(3, true);
        provisional.append(4, true);
        s.setProvisionalSelection(getItemIds(provisional));
        s.clearProvisionalSelection();

        // Original selection should remain.
        mSelection.assertSelection(1, 2);
    }

    @Test
    public void testProvisionalSelection_IntersectsAppliedSelection() {
        mTracker.select(mAdapter.getSelectionKey(1));
        mTracker.select(mAdapter.getSelectionKey(2));
        Selection<String> s = mTracker.getSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mAdapter.getSelectionKey(3), true);
        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(2, true);
        provisional.append(3, true);
        s.setProvisionalSelection(getItemIds(provisional));
        mSelection.assertSelection(1, 2, 3);
    }

    private Set<String> getItemIds(SparseBooleanArray selection) {
        Set<String> ids = new HashSet<>();

        int count = selection.size();
        for (int i = 0; i < count; ++i) {
            ids.add(mAdapter.getSelectionKey(selection.keyAt(i)));
        }

        return ids;
    }

    @Test
    public void testOnDataSetChanged_UnselectableRemovedFromSelection() {

        mListener = new TestSelectionObserver<>();
        mTracker = new DefaultSelectionTracker<>(
                SELECTION_ID,
                mKeyProvider,
                mSelectionPredicate,
                StorageStrategy.createStringStorage());

        EventBridge.install(
                mAdapter, mTracker, mKeyProvider, new Consumer<Runnable>() {
                    @Override
                    public void accept(Runnable runnable) {
                        runnable.run();
                    }
                });

        mTracker.addObserver(mListener);

        mSelection = new SelectionProbe(mTracker, mListener);

        for (int i = 2; i < 7; i++) {
            mTracker.select(mAdapter.getSelectionKey(i));
        }

        mIgnored.add(mAdapter.getSelectionKey(3));
        mIgnored.add(mAdapter.getSelectionKey(5));

        mAdapter.notifyDataSetChanged();

        mSelection.assertNotSelected(3);
        mSelection.assertNotSelected(5);

        mSelection.assertSelected(2);
        mSelection.assertSelected(4);
        mSelection.assertSelected(6);
    }

    @Test
    public void testInstanceState() {
        Bundle state = new Bundle();
        MutableSelection<String> orig = new MutableSelection<>();

        mTracker.select("10");
        mTracker.select("20");
        mTracker.copySelection(orig);

        mTracker.onSaveInstanceState(state);
        mTracker.clearSelection();

        Bundle parceled = Bundles.forceParceling(state);

        mTracker.onRestoreInstanceState(parceled);
        assertEquals(orig, mTracker.getSelection());
    }

    @Test
    public void testIgnoresNullBundle() {
        mTracker.onRestoreInstanceState(null);  // simply doesn't blow up.
    }

    private Iterable<String> getStringIds(int... ids) {
        List<String> stringIds = new ArrayList<>(ids.length);
        for (int id : ids) {
            stringIds.add(mAdapter.getSelectionKey(id));
        }
        return stringIds;
    }
}
