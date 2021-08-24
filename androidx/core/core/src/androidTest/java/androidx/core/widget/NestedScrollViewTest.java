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

package androidx.core.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NestedScrollViewTest {

    private NestedScrollView mNestedScrollView;
    private View mChild;

    @Test
    public void getBottomFadingEdgeStrength_childBottomIsBelowParentWithoutMargins_isCorrect() {
        setup(200);
        mNestedScrollView.setVerticalFadingEdgeEnabled(true);
        measureAndLayout(100);

        float expected = mNestedScrollView.getBottomFadingEdgeStrength();

        assertThat(expected, is(1.0f));
    }

    @Test
    public void getBottomFadingEdgeStrength_childBottomIsBelowParentDuetoMargins_isCorrect() {
        setup(100);
        mNestedScrollView.setVerticalFadingEdgeEnabled(true);
        setChildMargins(100, 0);
        measureAndLayout(100);

        float expected = mNestedScrollView.getBottomFadingEdgeStrength();

        assertThat(expected, is(1.0f));
    }

    @Test
    public void getBottomFadingEdgeStrength_childIsAboveButMarginIsBelowParent_isCorrect() {
        setup(100);
        mNestedScrollView.setVerticalFadingEdgeEnabled(true);
        setChildMargins(0, 100);
        measureAndLayout(100);

        float expected = mNestedScrollView.getBottomFadingEdgeStrength();

        assertThat(expected, is(1.0f));
    }

    @Test
    public void getBottomFadingEdgeStrength_childBottomIsAboveParentAndNoMargin_isZero() {
        setup(100);
        mNestedScrollView.setVerticalFadingEdgeEnabled(true);
        measureAndLayout(100);

        float expected = mNestedScrollView.getBottomFadingEdgeStrength();

        assertThat(expected, is(0f));
    }

    @Test
    public void onMeasure_fillViewPortEnabledChildSmallButWithMarginBig_childMeasuredCorrectly() {
        setup(50);
        setChildMargins(25, 25);
        mNestedScrollView.setFillViewport(true);

        measure(100);

        assertThat(mChild.getMeasuredHeight(), is(50));
    }

    @Test
    public void onMeasure_fillViewPortEnabledChildSmallWithMargins_childMeasuredCorrectly() {
        setup(50);
        setChildMargins(20, 20);
        mNestedScrollView.setFillViewport(true);

        measure(100);

        assertThat(mChild.getMeasuredHeight(), is(60));
    }

    @Test
    public void onMeasure_fillViewPortEnabledChildSmallNoMargins_childMeasuredCorrectly() {
        setup(50);
        setChildMargins(0, 0);
        mNestedScrollView.setFillViewport(true);

        measure(100);

        assertThat(mChild.getMeasuredHeight(), is(100));
    }

    @Test
    public void executeKeyEvent_spaceBarCanScrollDueToMargins_scrolls() {
        setup(75);
        setChildMargins(0, 50);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE);

        mNestedScrollView.executeKeyEvent(keyEvent);

        assertThat(mNestedScrollView.getScrollY(), is(25));
    }

    @Test
    public void pageScroll_takesAccountOfMargin() {
        setup(75);
        setChildMargins(20, 30);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);

        mNestedScrollView.pageScroll(View.FOCUS_DOWN);

        assertThat(mNestedScrollView.getScrollY(), is(25));
    }

    @Test
    public void getScrollRange_takesAccountOfMargin() {
        setup(100);
        setChildMargins(20, 30);
        measureAndLayout(100);

        int expected = mNestedScrollView.getScrollRange();

        assertThat(expected, is(50));
    }

    @Test
    public void fullScroll_scrollsToEndOfMargin() {
        setup(300);
        setChildMargins(20, 30);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);

        mNestedScrollView.fullScroll(View.FOCUS_DOWN);

        assertThat(mNestedScrollView.getScrollY(), is(250));
    }

    @Test
    public void arrowScroll_canScrollHalfDownDueToSizeAndMargin_scrollsHalfDown() {
        setup(130);
        setChildMargins(10, 20);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);

        mNestedScrollView.arrowScroll(View.FOCUS_DOWN);

        assertThat(mNestedScrollView.getScrollY(), is(50));
    }

    @Test
    public void arrowScroll_canScrollQuarterDownDueToSizeAndMargin_scrollsQuarterDown() {
        setup(75);
        setChildMargins(25, 25);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);

        mNestedScrollView.arrowScroll(View.FOCUS_DOWN);

        assertThat(mNestedScrollView.getScrollY(), is(25));
    }

    @Test
    public void arrowScroll_canOnlyScrollQuarterUp_scrollsQuarterUp() {
        setup(75);
        setChildMargins(25, 25);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);

        mNestedScrollView.scrollTo(0, 25);
        mNestedScrollView.arrowScroll(View.FOCUS_UP);

        assertThat(mNestedScrollView.getScrollY(), is(0));
    }

    @Test
    public void arrowScroll_canScroll_returnsTrue() {
        setup(75);
        setChildMargins(20, 30);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);

        boolean actualResult = mNestedScrollView.arrowScroll(View.FOCUS_DOWN);

        assertThat(actualResult, is(true));
    }

    @Test
    public void arrowScroll_cantScroll_returnsFalse() {
        setup(50);
        setChildMargins(25, 25);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);

        boolean actualResult = mNestedScrollView.arrowScroll(View.FOCUS_DOWN);

        assertThat(actualResult, is(false));
    }

    @Test
    public void computeVerticalScrollRange_takesAccountOfMargin() {
        setup(200);
        setChildMargins(20, 30);
        measureAndLayout(100);

        int actual = mNestedScrollView.computeVerticalScrollRange();

        assertThat(actual, is(250));
    }

    @Test
    public void computeScrollDeltaToGetChildRectOnScreen_marginRespectedToMakeRoomForFadingEdge() {
        setup(200);
        setChildMargins(0, 1);
        mNestedScrollView.setVerticalFadingEdgeEnabled(true);
        mNestedScrollView.setFadingEdgeLength(25);
        measureAndLayout(100);
        Rect rect = new Rect(0, 175, 100, 200);

        int actual = mNestedScrollView.computeScrollDeltaToGetChildRectOnScreen(rect);

        assertThat(actual, is(101));
    }

    @Test
    public void computeScrollDeltaToGetChildRectOnScreen_fadingEdgeNoMargin_clampsToEnd() {
        setup(200);
        setChildMargins(0, 0);
        mNestedScrollView.setVerticalFadingEdgeEnabled(true);
        mNestedScrollView.setFadingEdgeLength(25);
        measureAndLayout(100);
        Rect rect = new Rect(0, 175, 100, 200);

        int actual = mNestedScrollView.computeScrollDeltaToGetChildRectOnScreen(rect);

        assertThat(actual, is(100));
    }

    @Test
    public void onLayout_canScrollDistanceFromSavedInstanceStateDueToMargins_scrollsDistance() {

        // Arrange.

        setup(200);
        setChildMargins(0, 0);
        measureAndLayout(100);
        mNestedScrollView.scrollTo(0, 100);
        Parcelable savedState = mNestedScrollView.onSaveInstanceState();

        setup(100);
        setChildMargins(25, 75);
        mNestedScrollView.onRestoreInstanceState(savedState);

        // Act.

        measureAndLayout(100);

        // Assert

        assertThat(mNestedScrollView.getScrollY(), is(100));
    }

    @Test
    public void scrollTo_childHasMargins_scrollsToEndOfMargins() {
        setup(100);
        setChildMargins(25, 75);
        mNestedScrollView.setSmoothScrollingEnabled(false);
        measureAndLayout(100);

        mNestedScrollView.scrollTo(0, 100);

        assertThat(mNestedScrollView.getScrollY(), is(100));
    }

    private void setup(int childHeight) {
        Context context = ApplicationProvider.getApplicationContext();

        mChild = new View(context);
        mChild.setMinimumWidth(100);
        mChild.setMinimumHeight(childHeight);
        mChild.setBackgroundDrawable(
                new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFF0000, 0xFF00FF00}));

        mNestedScrollView = new NestedScrollView(context);
        mNestedScrollView.setBackgroundColor(0xFF0000FF);
        mNestedScrollView.addView(mChild);
    }

    private void setChildMargins(int top, int bottom) {
        NestedScrollView.LayoutParams childLayoutParams =
                new NestedScrollView.LayoutParams(100, 100);
        childLayoutParams.topMargin = top;
        childLayoutParams.bottomMargin = bottom;
        mChild.setLayoutParams(childLayoutParams);
    }

    private void measure(int height) {
        int measureSpecWidth =
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        int measureSpecHeight =
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        mNestedScrollView.measure(measureSpecWidth, measureSpecHeight);
    }

    @SuppressWarnings("SameParameterValue")
    private void measureAndLayout(int height) {
        measure(height);
        mNestedScrollView.layout(0, 0, 100, height);
    }
}
