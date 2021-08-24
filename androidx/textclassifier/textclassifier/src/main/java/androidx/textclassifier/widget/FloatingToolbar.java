/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Size;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.internal.view.SupportMenu;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.util.Preconditions;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.textclassifier.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A floating toolbar for showing contextual menu items.
 * This view shows as many menu item buttons as can fit in the horizontal toolbar and the
 * the remaining menu items in a vertical overflow view when the overflow button is clicked.
 * The horizontal toolbar morphs into the vertical overflow view.
 */
// TODO: Add nullability annotations.
@RequiresApi(Build.VERSION_CODES.M)
final class FloatingToolbar implements IFloatingToolbar {

    // This class is responsible for the public API of the floating toolbar.
    // It delegates rendering operations to the FloatingToolbarPopup.


    static final Object FLOATING_TOOLBAR_TAG = "floating_toolbar";
    static final Object MAIN_PANEL_TAG = "main_panel";
    static final Object OVERFLOW_PANEL_TAG = "main_overflow";

    private static final SupportMenuItem.OnMenuItemClickListener NO_OP_MENUITEM_CLICK_LISTENER =
            new SupportMenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    return false;
                }
            };

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final FloatingToolbarPopup mPopup;
    private final View mRootView;

    private final Rect mContentRect = new Rect();
    private final Rect mPreviousContentRect = new Rect();

    private SupportMenu mMenu;
    private List<SupportMenuItem> mShowingMenuItems = new ArrayList<>();
    private SupportMenuItem.OnMenuItemClickListener mMenuItemClickListener =
            NO_OP_MENUITEM_CLICK_LISTENER;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mWidthChanged = true;
    private int mSuggestedWidth;

    private final OnLayoutChangeListener mOrientationChangeHandler = new OnLayoutChangeListener() {

        private final Rect mNewRect = new Rect();
        private final Rect mOldRect = new Rect();

        @Override
        public void onLayoutChange(
                View view,
                int newLeft, int newRight, int newTop, int newBottom,
                int oldLeft, int oldRight, int oldTop, int oldBottom) {
            mNewRect.set(newLeft, newRight, newTop, newBottom);
            mOldRect.set(oldLeft, oldRight, oldTop, oldBottom);
            if (mPopup.isShowing() && (mNewRect.width() != mOldRect.width())) {
                mWidthChanged = true;
                updateLayout();
            }
        }
    };

    /**
     * Sorts the list of menu items to conform to certain requirements.
     */
    @VisibleForTesting
    final Comparator<SupportMenuItem> mMenuItemComparator =
            new Comparator<SupportMenuItem>() {
                @Override
                public int compare(SupportMenuItem menuItem1, SupportMenuItem menuItem2) {
                    // Ensure the assist menu item is always the first item:
                    if (menuItem1.getItemId() == MENU_ID_SMART_ACTION) {
                        return menuItem2.getItemId() == MENU_ID_SMART_ACTION ? 0 : -1;
                    }
                    if (menuItem2.getItemId() == MENU_ID_SMART_ACTION) {
                        return 1;
                    }

                    // Order by SHOW_AS_ACTION type:
                    if (requiresActionButton(menuItem1)) {
                        return requiresActionButton(menuItem2)
                                ? compareOrder(menuItem1, menuItem2) : -1;
                    }
                    if (requiresActionButton(menuItem2)) {
                        return 1;
                    }
                    if (requiresOverflow(menuItem1)) {
                        return requiresOverflow(menuItem2)
                                ? compareOrder(menuItem1, menuItem2) : 1;
                    }
                    if (requiresOverflow(menuItem2)) {
                        return -1;
                    }

                    // Order by order value:
                    return compareOrder(menuItem1, menuItem2);
                }

                private int compareOrder(SupportMenuItem menuItem1, SupportMenuItem menuItem2) {
                    return menuItem1.getOrder() - menuItem2.getOrder();
                }
            };

    /**
     * Initializes a floating toolbar.
     */
    FloatingToolbar(View view) {
        // TODO(b/65172902): Pass context in constructor when DecorView (and other callers)
        // supports multi-display.
        Preconditions.checkNotNull(view);
        final Context context = applyDefaultTheme(view.getContext());
        mRootView = view.getRootView();
        mPopup = new FloatingToolbarPopup(context, mRootView, new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        });
    }

    @Override
    public void setMenu(@NonNull SupportMenu menu) {
        mMenu = Preconditions.checkNotNull(menu);
    }

    @Nullable
    @Override
    public SupportMenu getMenu() {
        return mMenu;
    }

    @Override
    public void setOnMenuItemClickListener(
            @Nullable SupportMenuItem.OnMenuItemClickListener menuItemClickListener) {
        if (menuItemClickListener != null) {
            mMenuItemClickListener = menuItemClickListener;
        } else {
            mMenuItemClickListener = NO_OP_MENUITEM_CLICK_LISTENER;
        }
    }

    @Override
    public void setContentRect(Rect rect) {
        mContentRect.set(Preconditions.checkNotNull(rect));
    }

    @Override
    public void setSuggestedWidth(int suggestedWidth) {
        // Check if there's been a substantial width spec change.
        int difference = Math.abs(suggestedWidth - mSuggestedWidth);
        mWidthChanged = difference > (mSuggestedWidth * 0.2);

        mSuggestedWidth = suggestedWidth;
    }

    /**
     * Shows this floating toolbar.
     */
    @Override
    public void show() {
        registerOrientationHandler();
        doShow();
    }

    @Override
    public void updateLayout() {
        if (mPopup.isShowing()) {
            doShow();
        }
    }

    @Override
    public void dismiss() {
        unregisterOrientationHandler();
        mPopup.dismiss();
        mWidthChanged = true;
    }

    @Override
    public void hide() {
        mPopup.hide();
    }

    @Override
    public boolean isShowing() {
        return mPopup.isShowing();
    }

    @Override
    public boolean isHidden() {
        return mPopup.isHidden();
    }

    @Override
    public void setOnDismissListener(@Nullable PopupWindow.OnDismissListener onDismiss) {
        mPopup.setOnDismissListener(onDismiss);
    }

    @Override
    public void setDismissOnMenuItemClick(boolean dismiss) {
        mPopup.setDismissOnMenuItemClick(dismiss);
    }

    private void doShow() {
        List<SupportMenuItem> menuItems = getVisibleAndEnabledMenuItems(mMenu);
        Collections.sort(menuItems, mMenuItemComparator);
        if (!isCurrentlyShowing(menuItems) || mWidthChanged) {
            mPopup.hide();
            mPopup.layoutMenuItems(menuItems, mMenuItemClickListener, mSuggestedWidth);
            mShowingMenuItems = menuItems;
        }
        if (menuItems.isEmpty()) {
            mPopup.dismiss();
        } else if (!mPopup.isShowing()) {
            mPopup.show(mContentRect);
        } else if (!mPreviousContentRect.equals(mContentRect)) {
            mPopup.updateCoordinates(mContentRect);
        }
        mWidthChanged = false;
        mPreviousContentRect.set(mContentRect);
    }

    /**
     * Returns true if this floating toolbar is currently showing the specified menu items.
     */
    private boolean isCurrentlyShowing(List<SupportMenuItem> menuItems) {
        if (mShowingMenuItems == null || menuItems.size() != mShowingMenuItems.size()) {
            return false;
        }

        final int size = menuItems.size();
        for (int i = 0; i < size; i++) {
            final SupportMenuItem menuItem = menuItems.get(i);
            final SupportMenuItem showingItem = mShowingMenuItems.get(i);
            if (menuItem.getItemId() != showingItem.getItemId()
                    || !TextUtils.equals(menuItem.getTitle(), showingItem.getTitle())
                    || !Objects.equals(menuItem.getIcon(), showingItem.getIcon())
                    || menuItem.getGroupId() != showingItem.getGroupId()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the visible and enabled menu items in the specified menu.
     * This method is recursive.
     */
    private List<SupportMenuItem> getVisibleAndEnabledMenuItems(SupportMenu menu) {
        List<SupportMenuItem> menuItems = new ArrayList<>();
        for (int i = 0; (menu != null) && (i < menu.size()); i++) {
            SupportMenuItem menuItem = (SupportMenuItem) menu.getItem(i);
            if (menuItem.isVisible() && menuItem.isEnabled()) {
                SupportMenu subMenu = (SupportMenu) menuItem.getSubMenu();
                if (subMenu != null) {
                    menuItems.addAll(getVisibleAndEnabledMenuItems(subMenu));
                } else {
                    menuItems.add(menuItem);
                }
            }
        }
        return menuItems;
    }

    private void registerOrientationHandler() {
        unregisterOrientationHandler();
        mRootView.addOnLayoutChangeListener(mOrientationChangeHandler);
    }

    private void unregisterOrientationHandler() {
        mRootView.removeOnLayoutChangeListener(mOrientationChangeHandler);
    }

    @VisibleForTesting
    Rect getToolbarContainerBoundsForTesting() {
        return mPopup.getContainerBounds();
    }


    /**
     * A popup window used by the floating toolbar.
     *
     * This class is responsible for the rendering/animation of the floating toolbar.
     * It holds 2 panels (i.e. main panel and overflow panel) and an overflow button
     * to transition between panels.
     */
    private static final class FloatingToolbarPopup {

        /* Minimum and maximum number of items allowed in the overflow. */
        private static final int MIN_OVERFLOW_SIZE = 2;
        private static final int MAX_OVERFLOW_SIZE = 4;

        final Context mContext;
        final View mParent;  // Parent for the popup window.
        final PopupWindow mPopupWindow;

        /* Margins between the popup window and it's content. */
        final int mMarginHorizontal;
        final int mMarginVertical;

        /* View components */
        final ViewGroup mContentContainer;  // holds all contents.
        final ViewGroup mMainPanel;  // holds menu items that are initially displayed.
        final OverflowPanel mOverflowPanel;  // holds menu items hidden in the overflow.
        final ImageButton mOverflowButton;  // opens/closes the overflow.
        /* overflow button drawables. */
        final Drawable mArrow;
        final Drawable mOverflow;
        final AnimatedVectorDrawable mToArrow;
        final AnimatedVectorDrawable mToOverflow;

        final OverflowPanelViewHelper mOverflowPanelViewHelper;

        /* Animation interpolators. */
        final Interpolator mLogAccelerateInterpolator;
        final Interpolator mFastOutSlowInInterpolator;
        final Interpolator mLinearOutSlowInInterpolator;
        final Interpolator mFastOutLinearInInterpolator;

        /* Animations. */
        final AnimatorSet mShowAnimation;
        final AnimatorSet mDismissAnimation;
        final AnimatorSet mHideAnimation;
        final AnimationSet mOpenOverflowAnimation;
        final AnimationSet mCloseOverflowAnimation;
        final Animation.AnimationListener mOverflowAnimationListener;

        final Rect mViewPortOnScreen = new Rect();  // portion of screen we can draw in.
        final Point mCoordsOnWindow = new Point();  // popup window coordinates.
        /* Temporary data holders. Reset values before using. */
        final int[] mTmpCoords = new int[2];

        final int mLineHeight;
        final int mIconTextSpacing;

        /**
         * @see OverflowPanelViewHelper#preparePopupContent().
         */
        final Runnable mPreparePopupContentRTLHelper = new Runnable() {
            @Override
            public void run() {
                setPanelsStatesAtRestingPosition();
                mContentContainer.setAlpha(1);
            }
        };

        boolean mDismissed = true; // tracks whether this popup is dismissed or dismissing.
        boolean mHidden; // tracks whether this popup is hidden or hiding.

        /* Calculated sizes for panels and overflow button. */
        final Size mOverflowButtonSize;
        Size mOverflowPanelSize;  // Should be null when there is no overflow.
        Size mMainPanelSize;

        /* Item click listeners */
        boolean mDismissOnMenuItemClick;
        SupportMenuItem.OnMenuItemClickListener mOnMenuItemClickListener;
        final View.OnClickListener mMenuItemButtonOnClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (v.getTag() instanceof SupportMenuItem) {
                            if (mOnMenuItemClickListener != null) {
                                mOnMenuItemClickListener.onMenuItemClick(
                                        (SupportMenuItem) v.getTag());
                                if (mDismissOnMenuItemClick) {
                                    mDismissRunnable.run();
                                }
                            }
                        }
                    }
                };

        /* Outside touch handling */
        final Runnable mDismissRunnable;
        final View.OnClickListener mOnOutsideTouchHandler;

        boolean mOpenOverflowUpwards;  // Whether the overflow opens upwards or downwards.
        boolean mIsOverflowOpen;

        int mTransitionDurationScale;  // Used to scale the toolbar transition duration.

        /**
         * Initializes a new floating toolbar popup.
         *
         * @param parent  A parent view to get the {@link android.view.View#getWindowToken()} token
         *      from.
         */
        FloatingToolbarPopup(
                Context context, View parent, Runnable dismissRunnable) {
            mParent = Preconditions.checkNotNull(parent);
            mContext = Preconditions.checkNotNull(context);
            mContentContainer = createContentContainer(context);
            mDismissRunnable = Preconditions.checkNotNull(dismissRunnable);
            mOnOutsideTouchHandler = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hide();
                    mDismissRunnable.run();
                }
            };
            mPopupWindow = createPopupWindow(mContentContainer, mOnOutsideTouchHandler);
            mMarginHorizontal = parent.getResources()
                    .getDimensionPixelSize(R.dimen.floating_toolbar_horizontal_margin);
            mMarginVertical = parent.getResources()
                    .getDimensionPixelSize(R.dimen.floating_toolbar_vertical_margin);
            mLineHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.floating_toolbar_height);
            mIconTextSpacing = context.getResources()
                    .getDimensionPixelSize(R.dimen.floating_toolbar_icon_text_spacing);

            // Interpolators
            mLogAccelerateInterpolator = new LogAccelerateInterpolator();
            mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                    mContext, android.R.interpolator.fast_out_slow_in);
            mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                    mContext, android.R.interpolator.linear_out_slow_in);
            mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(
                    mContext, android.R.interpolator.fast_out_linear_in);

            // Drawables. Needed for views.
            mArrow = mContext.getResources()
                    .getDrawable(R.drawable.ft_avd_tooverflow, mContext.getTheme());
            mArrow.setAutoMirrored(true);
            mOverflow = mContext.getResources()
                    .getDrawable(R.drawable.ft_avd_toarrow, mContext.getTheme());
            mOverflow.setAutoMirrored(true);
            mToArrow = (AnimatedVectorDrawable) mContext.getResources()
                    .getDrawable(R.drawable.ft_avd_toarrow_animation, mContext.getTheme());
            mToArrow.setAutoMirrored(true);
            mToOverflow = (AnimatedVectorDrawable) mContext.getResources()
                    .getDrawable(R.drawable.ft_avd_tooverflow_animation, mContext.getTheme());
            mToOverflow.setAutoMirrored(true);

            // Views
            mOverflowButton = createOverflowButton();
            mOverflowButtonSize = measure(mOverflowButton);
            mMainPanel = createMainPanel();
            mOverflowPanelViewHelper = new OverflowPanelViewHelper(mContext, mIconTextSpacing);
            mOverflowPanel = createOverflowPanel();

            // Animation. Need views.
            mOverflowAnimationListener = createOverflowAnimationListener();
            mOpenOverflowAnimation = new AnimationSet(true);
            mOpenOverflowAnimation.setAnimationListener(mOverflowAnimationListener);
            mCloseOverflowAnimation = new AnimationSet(true);
            mCloseOverflowAnimation.setAnimationListener(mOverflowAnimationListener);
            mShowAnimation = createEnterAnimation(mContentContainer);
            mDismissAnimation = createExitAnimation(
                    mContentContainer,
                    150,  // startDelay
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mPopupWindow.dismiss();
                            mContentContainer.removeAllViews();
                        }
                    });
            mHideAnimation = createExitAnimation(
                    mContentContainer,
                    0,  // startDelay
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mPopupWindow.dismiss();
                        }
                    });
        }

        /**
         * Sets the floating popup's onDismissListener.
         */
        public void setOnDismissListener(@Nullable final PopupWindow.OnDismissListener onDismiss) {
            mPopupWindow.setOnDismissListener(onDismiss);
        }

        /**
         * Sets whether or not to dismiss the floating toolbar after a menu item click has been
         * handled.
         */
        void setDismissOnMenuItemClick(boolean dismiss) {
            mDismissOnMenuItemClick = dismiss;
        }

        /**
         * Lays out buttons for the specified menu items.
         * Requires a subsequent call to {@link #show()} to show the items.
         */
        public void layoutMenuItems(
                List<SupportMenuItem> menuItems,
                SupportMenuItem.OnMenuItemClickListener menuItemClickListener,
                int suggestedWidth) {
            mOnMenuItemClickListener = menuItemClickListener;
            cancelOverflowAnimations();
            clearPanels();
            menuItems = layoutMainPanelItems(menuItems, getAdjustedToolbarWidth(suggestedWidth));
            if (!menuItems.isEmpty()) {
                // Add remaining items to the overflow.
                layoutOverflowPanelItems(menuItems);
            }
            updatePopupSize();
        }

        /**
         * Shows this popup at the specified coordinates.
         * The specified coordinates may be adjusted to make sure the popup is entirely on-screen.
         */
        public void show(Rect contentRectOnScreen) {
            Preconditions.checkNotNull(contentRectOnScreen);

            if (isShowing()) {
                return;
            }

            mHidden = false;
            mDismissed = false;
            cancelDismissAndHideAnimations();
            cancelOverflowAnimations();

            refreshCoordinatesAndOverflowDirection(contentRectOnScreen);
            preparePopupContent();
            // We need to specify the position in window coordinates.
            // TODO: Consider to use PopupWindow.setLayoutInScreenEnabled(true) so that we can
            // specify the popup position in screen coordinates.
            mPopupWindow.showAtLocation(
                    mParent, Gravity.NO_GRAVITY, mCoordsOnWindow.x, mCoordsOnWindow.y);
            runShowAnimation();
        }

        /**
         * Gets rid of this popup. If the popup isn't currently showing, this will be a no-op.
         */
        public void dismiss() {
            if (mDismissed) {
                return;
            }

            mHidden = false;
            mDismissed = true;
            mHideAnimation.cancel();

            runDismissAnimation();
        }

        /**
         * Hides this popup. This is a no-op if this popup is not showing.
         * Use {@link #isHidden()} to distinguish between a hidden and a dismissed popup.
         */
        public void hide() {
            if (!isShowing()) {
                return;
            }

            mHidden = true;
            runHideAnimation();
        }

        /**
         * Returns {@code true} if this popup is currently showing. {@code false} otherwise.
         */
        public boolean isShowing() {
            return !mDismissed && !mHidden && mPopupWindow.isShowing();
        }

        /**
         * Returns {@code true} if this popup is currently hidden. {@code false} otherwise.
         */
        public boolean isHidden() {
            return mHidden;
        }

        /**
         * Updates the coordinates of this popup.
         * The specified coordinates may be adjusted to make sure the popup is entirely on-screen.
         * This is a no-op if this popup is not showing.
         */
        void updateCoordinates(Rect contentRectOnScreen) {
            Preconditions.checkNotNull(contentRectOnScreen);

            if (!isShowing()) {
                return;
            }

            cancelOverflowAnimations();
            refreshCoordinatesAndOverflowDirection(contentRectOnScreen);
            preparePopupContent();
            // We need to specify the position in window coordinates.
            // TODO: Consider to use PopupWindow.setLayoutInScreenEnabled(true) so that we can
            // specify the popup position in screen coordinates.
            mPopupWindow.update(
                    mCoordsOnWindow.x, mCoordsOnWindow.y,
                    mPopupWindow.getWidth(), mPopupWindow.getHeight());
        }

        void refreshCoordinatesAndOverflowDirection(Rect contentRectOnScreen) {
            refreshViewPort();

            // Initialize x ensuring that the toolbar isn't rendered behind the nav bar in
            // landscape.
            final int x = Math.min(
                    contentRectOnScreen.centerX() - mPopupWindow.getWidth() / 2,
                    mViewPortOnScreen.right - mPopupWindow.getWidth());

            final int y;

            final int availableHeightAboveContent =
                    contentRectOnScreen.top - mViewPortOnScreen.top;
            final int availableHeightBelowContent =
                    mViewPortOnScreen.bottom - contentRectOnScreen.bottom;

            final int margin = 2 * mMarginVertical;
            final int toolbarHeightWithVerticalMargin = mLineHeight + margin;

            if (!hasOverflow()) {
                if (availableHeightAboveContent >= toolbarHeightWithVerticalMargin) {
                    // There is enough space at the top of the content.
                    y = contentRectOnScreen.top - toolbarHeightWithVerticalMargin;
                } else if (availableHeightBelowContent >= toolbarHeightWithVerticalMargin) {
                    // There is enough space at the bottom of the content.
                    y = contentRectOnScreen.bottom;
                } else if (availableHeightBelowContent >= mLineHeight) {
                    // Just enough space to fit the toolbar with no vertical margins.
                    y = contentRectOnScreen.bottom - mMarginVertical;
                } else {
                    // Not enough space. Prefer to position as high as possible.
                    y = Math.max(
                            mViewPortOnScreen.top,
                            contentRectOnScreen.top - toolbarHeightWithVerticalMargin);
                }
            } else {
                // Has an overflow.
                final int minimumOverflowHeightWithMargin =
                        calculateOverflowHeight(MIN_OVERFLOW_SIZE) + margin;
                final int availableHeightThroughContentDown = mViewPortOnScreen.bottom
                        - contentRectOnScreen.top + toolbarHeightWithVerticalMargin;
                final int availableHeightThroughContentUp = contentRectOnScreen.bottom
                        - mViewPortOnScreen.top + toolbarHeightWithVerticalMargin;

                if (availableHeightAboveContent >= minimumOverflowHeightWithMargin) {
                    // There is enough space at the top of the content rect for the overflow.
                    // Position above and open upwards.
                    updateOverflowHeight(availableHeightAboveContent - margin);
                    y = contentRectOnScreen.top - mPopupWindow.getHeight();
                    mOpenOverflowUpwards = true;
                } else if (availableHeightAboveContent >= toolbarHeightWithVerticalMargin
                        && availableHeightThroughContentDown >= minimumOverflowHeightWithMargin) {
                    // There is enough space at the top of the content rect for the main panel
                    // but not the overflow.
                    // Position above but open downwards.
                    updateOverflowHeight(availableHeightThroughContentDown - margin);
                    y = contentRectOnScreen.top - toolbarHeightWithVerticalMargin;
                    mOpenOverflowUpwards = false;
                } else if (availableHeightBelowContent >= minimumOverflowHeightWithMargin) {
                    // There is enough space at the bottom of the content rect for the overflow.
                    // Position below and open downwards.
                    updateOverflowHeight(availableHeightBelowContent - margin);
                    y = contentRectOnScreen.bottom;
                    mOpenOverflowUpwards = false;
                } else if (availableHeightBelowContent >= toolbarHeightWithVerticalMargin
                        && mViewPortOnScreen.height() >= minimumOverflowHeightWithMargin) {
                    // There is enough space at the bottom of the content rect for the main panel
                    // but not the overflow.
                    // Position below but open upwards.
                    updateOverflowHeight(availableHeightThroughContentUp - margin);
                    y = contentRectOnScreen.bottom + toolbarHeightWithVerticalMargin
                            - mPopupWindow.getHeight();
                    mOpenOverflowUpwards = true;
                } else {
                    // Not enough space.
                    // Position at the top of the view port and open downwards.
                    updateOverflowHeight(mViewPortOnScreen.height() - margin);
                    y = mViewPortOnScreen.top;
                    mOpenOverflowUpwards = false;
                }
            }

            // We later specify the location of PopupWindow relative to the attached window.
            // The idea here is that 1) we can get the location of a View in both window coordinates
            // and screen coordiantes, where the offset between them should be equal to the window
            // origin, and 2) we can use an arbitrary for this calculation while calculating the
            // location of the rootview is supposed to be least expensive.
            // TODO: Consider to use PopupWindow.setLayoutInScreenEnabled(true) so that we can avoid
            // the following calculation.
            mParent.getRootView().getLocationOnScreen(mTmpCoords);
            int rootViewLeftOnScreen = mTmpCoords[0];
            int rootViewTopOnScreen = mTmpCoords[1];
            mParent.getRootView().getLocationInWindow(mTmpCoords);
            int rootViewLeftOnWindow = mTmpCoords[0];
            int rootViewTopOnWindow = mTmpCoords[1];
            int windowLeftOnScreen = rootViewLeftOnScreen - rootViewLeftOnWindow;
            int windowTopOnScreen = rootViewTopOnScreen - rootViewTopOnWindow;
            mCoordsOnWindow.set(
                    Math.max(0, x - windowLeftOnScreen), Math.max(0, y - windowTopOnScreen));
        }

        /**
         * Performs the "show" animation on the floating popup.
         */
        void runShowAnimation() {
            mShowAnimation.start();
        }

        /**
         * Performs the "dismiss" animation on the floating popup.
         */
        void runDismissAnimation() {
            mDismissAnimation.start();
        }

        /**
         * Performs the "hide" animation on the floating popup.
         */
        void runHideAnimation() {
            mHideAnimation.start();
        }

        void cancelDismissAndHideAnimations() {
            mDismissAnimation.cancel();
            mHideAnimation.cancel();
        }

        void cancelOverflowAnimations() {
            mContentContainer.clearAnimation();
            mMainPanel.animate().cancel();
            mOverflowPanel.animate().cancel();
            mToArrow.stop();
            mToOverflow.stop();
        }

        void openOverflow() {
            final int targetWidth = mOverflowPanelSize.getWidth();
            final int targetHeight = mOverflowPanelSize.getHeight();
            final int startWidth = mContentContainer.getWidth();
            final int startHeight = mContentContainer.getHeight();
            final float startY = mContentContainer.getY();
            final float left = mContentContainer.getX();
            final float right = left + mContentContainer.getWidth();
            Animation widthAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    int deltaWidth = (int) (interpolatedTime * (targetWidth - startWidth));
                    setWidth(mContentContainer, startWidth + deltaWidth);
                    if (isInRTLMode()) {
                        mContentContainer.setX(left);

                        // Lock the panels in place.
                        mMainPanel.setX(0);
                        mOverflowPanel.setX(0);
                    } else {
                        mContentContainer.setX(right - mContentContainer.getWidth());

                        // Offset the panels' positions so they look like they're locked in place
                        // on the screen.
                        mMainPanel.setX(mContentContainer.getWidth() - startWidth);
                        mOverflowPanel.setX(mContentContainer.getWidth() - targetWidth);
                    }
                }
            };
            Animation heightAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    int deltaHeight = (int) (interpolatedTime * (targetHeight - startHeight));
                    setHeight(mContentContainer, startHeight + deltaHeight);
                    if (mOpenOverflowUpwards) {
                        mContentContainer.setY(
                                startY - (mContentContainer.getHeight() - startHeight));
                        positionContentYCoordinatesIfOpeningOverflowUpwards();
                    }
                }
            };
            final float overflowButtonStartX = mOverflowButton.getX();
            final float overflowButtonTargetX = isInRTLMode()
                    ? overflowButtonStartX + targetWidth - mOverflowButton.getWidth()
                    : overflowButtonStartX - targetWidth + mOverflowButton.getWidth();
            Animation overflowButtonAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    float overflowButtonX = overflowButtonStartX
                            + interpolatedTime * (overflowButtonTargetX - overflowButtonStartX);
                    float deltaContainerWidth = isInRTLMode()
                            ? 0 : mContentContainer.getWidth() - startWidth;
                    float actualOverflowButtonX = overflowButtonX + deltaContainerWidth;
                    mOverflowButton.setX(actualOverflowButtonX);
                }
            };
            widthAnimation.setInterpolator(mLogAccelerateInterpolator);
            widthAnimation.setDuration(getAdjustedDuration(250));
            heightAnimation.setInterpolator(mFastOutSlowInInterpolator);
            heightAnimation.setDuration(getAdjustedDuration(250));
            overflowButtonAnimation.setInterpolator(mFastOutSlowInInterpolator);
            overflowButtonAnimation.setDuration(getAdjustedDuration(250));
            mOpenOverflowAnimation.getAnimations().clear();
            mOpenOverflowAnimation.getAnimations().clear();
            mOpenOverflowAnimation.addAnimation(widthAnimation);
            mOpenOverflowAnimation.addAnimation(heightAnimation);
            mOpenOverflowAnimation.addAnimation(overflowButtonAnimation);
            mContentContainer.startAnimation(mOpenOverflowAnimation);
            mIsOverflowOpen = true;
            mMainPanel.animate()
                    .alpha(0).withLayer()
                    .setInterpolator(mLinearOutSlowInInterpolator)
                    .setDuration(250)
                    .start();
            mOverflowPanel.setAlpha(1); // fadeIn in 0ms.
        }

        void closeOverflow() {
            final int targetWidth = mMainPanelSize.getWidth();
            final int startWidth = mContentContainer.getWidth();
            final float left = mContentContainer.getX();
            final float right = left + mContentContainer.getWidth();
            Animation widthAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    int deltaWidth = (int) (interpolatedTime * (targetWidth - startWidth));
                    setWidth(mContentContainer, startWidth + deltaWidth);
                    if (isInRTLMode()) {
                        mContentContainer.setX(left);

                        // Lock the panels in place.
                        mMainPanel.setX(0);
                        mOverflowPanel.setX(0);
                    } else {
                        mContentContainer.setX(right - mContentContainer.getWidth());

                        // Offset the panels' positions so they look like they're locked in place
                        // on the screen.
                        mMainPanel.setX(mContentContainer.getWidth() - targetWidth);
                        mOverflowPanel.setX(mContentContainer.getWidth() - startWidth);
                    }
                }
            };
            final int targetHeight = mMainPanelSize.getHeight();
            final int startHeight = mContentContainer.getHeight();
            final float bottom = mContentContainer.getY() + mContentContainer.getHeight();
            Animation heightAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    int deltaHeight = (int) (interpolatedTime * (targetHeight - startHeight));
                    setHeight(mContentContainer, startHeight + deltaHeight);
                    if (mOpenOverflowUpwards) {
                        mContentContainer.setY(bottom - mContentContainer.getHeight());
                        positionContentYCoordinatesIfOpeningOverflowUpwards();
                    }
                }
            };
            final float overflowButtonStartX = mOverflowButton.getX();
            final float overflowButtonTargetX = isInRTLMode()
                    ? overflowButtonStartX - startWidth + mOverflowButton.getWidth()
                    : overflowButtonStartX + startWidth - mOverflowButton.getWidth();
            Animation overflowButtonAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    float overflowButtonX = overflowButtonStartX
                            + interpolatedTime * (overflowButtonTargetX - overflowButtonStartX);
                    float deltaContainerWidth = isInRTLMode()
                            ? 0 : mContentContainer.getWidth() - startWidth;
                    float actualOverflowButtonX = overflowButtonX + deltaContainerWidth;
                    mOverflowButton.setX(actualOverflowButtonX);
                }
            };
            widthAnimation.setInterpolator(mFastOutSlowInInterpolator);
            widthAnimation.setDuration(getAdjustedDuration(250));
            heightAnimation.setInterpolator(mLogAccelerateInterpolator);
            heightAnimation.setDuration(getAdjustedDuration(250));
            overflowButtonAnimation.setInterpolator(mFastOutSlowInInterpolator);
            overflowButtonAnimation.setDuration(getAdjustedDuration(250));
            mCloseOverflowAnimation.getAnimations().clear();
            mCloseOverflowAnimation.addAnimation(widthAnimation);
            mCloseOverflowAnimation.addAnimation(heightAnimation);
            mCloseOverflowAnimation.addAnimation(overflowButtonAnimation);
            mContentContainer.startAnimation(mCloseOverflowAnimation);
            mIsOverflowOpen = false;
            mMainPanel.animate()
                    .alpha(1).withLayer()
                    .setInterpolator(mFastOutLinearInInterpolator)
                    .setDuration(100)
                    .start();
            mOverflowPanel.animate()
                    .alpha(0).withLayer()
                    .setInterpolator(mLinearOutSlowInInterpolator)
                    .setDuration(150)
                    .start();
        }

        /**
         * Defines the position of the floating toolbar popup panels when transition animation has
         * stopped.
         */
        void setPanelsStatesAtRestingPosition() {
            mOverflowButton.setEnabled(true);
            mOverflowPanel.awakenScrollBars();

            if (mIsOverflowOpen) {
                // Set open state.
                final Size containerSize = mOverflowPanelSize;
                setSize(mContentContainer, containerSize);
                mMainPanel.setAlpha(0);
                mMainPanel.setVisibility(View.INVISIBLE);
                mOverflowPanel.setAlpha(1);
                mOverflowPanel.setVisibility(View.VISIBLE);
                mOverflowButton.setImageDrawable(mArrow);
                mOverflowButton.setContentDescription(mContext.getString(
                        R.string.floating_toolbar_close_overflow_description));

                // Update x-coordinates depending on RTL state.
                if (isInRTLMode()) {
                    mContentContainer.setX(mMarginHorizontal);  // align left
                    mMainPanel.setX(0);  // align left
                    mOverflowButton.setX(containerSize.getWidth()
                            - mOverflowButtonSize.getWidth());  // align right
                    mOverflowPanel.setX(0);  // align left
                } else {
                    mContentContainer.setX(mPopupWindow.getWidth()
                            - containerSize.getWidth() - mMarginHorizontal);  // align right
                    mMainPanel.setX(-mContentContainer.getX());  // align right
                    mOverflowButton.setX(0);  // align left
                    mOverflowPanel.setX(0);  // align left
                }

                // Update y-coordinates depending on overflow's open direction.
                if (mOpenOverflowUpwards) {
                    mContentContainer.setY(mMarginVertical);  // align top
                    mMainPanel.setY(containerSize.getHeight()
                            - mContentContainer.getHeight());  // align bottom
                    mOverflowButton.setY(containerSize.getHeight()
                            - mOverflowButtonSize.getHeight());  // align bottom
                    mOverflowPanel.setY(0);  // align top
                } else {
                    // opens downwards.
                    mContentContainer.setY(mMarginVertical);  // align top
                    mMainPanel.setY(0);  // align top
                    mOverflowButton.setY(0);  // align top
                    mOverflowPanel.setY(mOverflowButtonSize.getHeight());  // align bottom
                }
            } else {
                // Overflow not open. Set closed state.
                final Size containerSize = mMainPanelSize;
                setSize(mContentContainer, containerSize);
                mMainPanel.setAlpha(1);
                mMainPanel.setVisibility(View.VISIBLE);
                mOverflowPanel.setAlpha(0);
                mOverflowPanel.setVisibility(View.INVISIBLE);
                mOverflowButton.setImageDrawable(mOverflow);
                mOverflowButton.setContentDescription(mContext.getString(
                        R.string.floating_toolbar_open_overflow_description));

                if (hasOverflow()) {
                    // Update x-coordinates depending on RTL state.
                    if (isInRTLMode()) {
                        mContentContainer.setX(mMarginHorizontal);  // align left
                        mMainPanel.setX(0);  // align left
                        mOverflowButton.setX(0);  // align left
                        mOverflowPanel.setX(0);  // align left
                    } else {
                        mContentContainer.setX(mPopupWindow.getWidth()
                                - containerSize.getWidth() - mMarginHorizontal);  // align right
                        mMainPanel.setX(0);  // align left
                        mOverflowButton.setX(containerSize.getWidth()
                                - mOverflowButtonSize.getWidth());  // align right
                        mOverflowPanel.setX(containerSize.getWidth()
                                - mOverflowPanelSize.getWidth());  // align right
                    }

                    // Update y-coordinates depending on overflow's open direction.
                    if (mOpenOverflowUpwards) {
                        // align bottom
                        mContentContainer.setY(mMarginVertical
                                + mOverflowPanelSize.getHeight() - containerSize.getHeight());
                        mMainPanel.setY(0);  // align top
                        mOverflowButton.setY(0);  // align top
                        mOverflowPanel.setY(containerSize.getHeight()
                                - mOverflowPanelSize.getHeight());  // align bottom
                    } else {
                        // opens downwards.
                        mContentContainer.setY(mMarginVertical);  // align top
                        mMainPanel.setY(0);  // align top
                        mOverflowButton.setY(0);  // align top
                        mOverflowPanel.setY(mOverflowButtonSize.getHeight());  // align bottom
                    }
                } else {
                    // No overflow.
                    mContentContainer.setX(mMarginHorizontal);  // align left
                    mContentContainer.setY(mMarginVertical);  // align top
                    mMainPanel.setX(0);  // align left
                    mMainPanel.setY(0);  // align top
                }
            }
        }

        void updateOverflowHeight(int suggestedHeight) {
            if (hasOverflow()) {
                final int maxItemSize = (suggestedHeight - mOverflowButtonSize.getHeight())
                        / mLineHeight;
                final int newHeight = calculateOverflowHeight(maxItemSize);
                if (mOverflowPanelSize.getHeight() != newHeight) {
                    mOverflowPanelSize = new Size(mOverflowPanelSize.getWidth(), newHeight);
                }
                setSize(mOverflowPanel, mOverflowPanelSize);
                if (mIsOverflowOpen) {
                    setSize(mContentContainer, mOverflowPanelSize);
                    if (mOpenOverflowUpwards) {
                        final int deltaHeight = mOverflowPanelSize.getHeight() - newHeight;
                        mContentContainer.setY(mContentContainer.getY() + deltaHeight);
                        mOverflowButton.setY(mOverflowButton.getY() - deltaHeight);
                    }
                } else {
                    setSize(mContentContainer, mMainPanelSize);
                }
                updatePopupSize();
            }
        }

        void updatePopupSize() {
            int width = 0;
            int height = 0;
            if (mMainPanelSize != null) {
                width = Math.max(width, mMainPanelSize.getWidth());
                height = Math.max(height, mMainPanelSize.getHeight());
            }
            if (mOverflowPanelSize != null) {
                width = Math.max(width, mOverflowPanelSize.getWidth());
                height = Math.max(height, mOverflowPanelSize.getHeight());
            }
            mPopupWindow.setWidth(width + mMarginHorizontal * 2);
            mPopupWindow.setHeight(height + mMarginVertical * 2);
            maybeComputeTransitionDurationScale();
        }

        void refreshViewPort() {
            mParent.getWindowVisibleDisplayFrame(mViewPortOnScreen);
        }

        int getAdjustedToolbarWidth(int suggestedWidth) {
            int width = suggestedWidth;
            refreshViewPort();
            int maximumWidth = mViewPortOnScreen.width() - 2 * mParent.getResources()
                    .getDimensionPixelSize(R.dimen.floating_toolbar_horizontal_margin);
            if (width <= 0) {
                width = mParent.getResources()
                        .getDimensionPixelSize(R.dimen.floating_toolbar_preferred_width);
            }
            return Math.min(width, maximumWidth);
        }

        boolean isInRTLMode() {
            final int rtlFlag = mContext.getApplicationInfo().flags
                    & ApplicationInfo.FLAG_SUPPORTS_RTL;
            final int layoutDirection = mContext.getResources().getConfiguration()
                    .getLayoutDirection();
            return rtlFlag == ApplicationInfo.FLAG_SUPPORTS_RTL
                    && layoutDirection == View.LAYOUT_DIRECTION_RTL;
        }

        boolean hasOverflow() {
            return mOverflowPanelSize != null;
        }

        /**
         * Fits as many menu items in the main panel and returns a list of the menu items that
         * were not fit in.
         *
         * @return The menu items that are not included in this main panel.
         */
        List<SupportMenuItem> layoutMainPanelItems(
                List<SupportMenuItem> menuItems, final int toolbarWidth) {
            Preconditions.checkNotNull(menuItems);

            int availableWidth = toolbarWidth;

            final List<SupportMenuItem> remainingMenuItems = new ArrayList<>();
            // add the overflow menu items to the end of the remainingMenuItems list.
            final List<SupportMenuItem> overflowMenuItems = new ArrayList<>();
            for (SupportMenuItem menuItem : menuItems) {
                if (menuItem.getItemId() != MENU_ID_SMART_ACTION && requiresOverflow(menuItem)) {
                    overflowMenuItems.add(menuItem);
                } else {
                    remainingMenuItems.add(menuItem);
                }
            }
            remainingMenuItems.addAll(overflowMenuItems);

            mMainPanel.removeAllViews();
            mMainPanel.setPaddingRelative(0, 0, 0, 0);

            boolean isFirstItem = true;
            while (!remainingMenuItems.isEmpty()) {
                final SupportMenuItem menuItem = remainingMenuItems.get(0);

                // if this is the first item, regardless of requiresOverflow(), it should be
                // displayed on the main panel. Otherwise all items including this one will be
                // overflow items, and should be displayed in overflow panel.
                if (!isFirstItem && requiresOverflow(menuItem)) {
                    break;
                }

                final boolean showIcon = isFirstItem
                        && menuItem.getItemId() == MENU_ID_SMART_ACTION;
                final View menuItemButton = createMenuItemButton(
                        mContext, menuItem, mIconTextSpacing, showIcon);
                if (!showIcon && menuItemButton instanceof LinearLayout) {
                    ((LinearLayout) menuItemButton).setGravity(Gravity.CENTER);
                }

                // Adding additional start padding for the first button to even out button spacing.
                if (isFirstItem) {
                    menuItemButton.setPaddingRelative(
                            (int) (1.5 * menuItemButton.getPaddingStart()),
                            menuItemButton.getPaddingTop(),
                            menuItemButton.getPaddingEnd(),
                            menuItemButton.getPaddingBottom());
                }

                // Adding additional end padding for the last button to even out button spacing.
                boolean isLastItem = remainingMenuItems.size() == 1;
                if (isLastItem) {
                    menuItemButton.setPaddingRelative(
                            menuItemButton.getPaddingStart(),
                            menuItemButton.getPaddingTop(),
                            (int) (1.5 * menuItemButton.getPaddingEnd()),
                            menuItemButton.getPaddingBottom());
                }

                menuItemButton.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
                final int menuItemButtonWidth = Math.min(
                        menuItemButton.getMeasuredWidth(), toolbarWidth);

                // Check if we can fit an item while reserving space for the overflowButton.
                final boolean canFitWithOverflow =
                        menuItemButtonWidth <= availableWidth - mOverflowButtonSize.getWidth();
                final boolean canFitNoOverflow =
                        isLastItem && menuItemButtonWidth <= availableWidth;
                if (canFitWithOverflow || canFitNoOverflow) {
                    setButtonTagAndClickListener(menuItemButton, menuItem);
                    // Set tooltips for main panel items, but not overflow items (b/35726766).
                    CharSequence tooltip = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        tooltip = menuItem.getTooltipText();
                    }
                    tooltip = tooltip == null ? menuItem.getTitle() : tooltip;
                    ViewCompat.setTooltipText(menuItemButton, tooltip);
                    mMainPanel.addView(menuItemButton);
                    final ViewGroup.LayoutParams params = menuItemButton.getLayoutParams();
                    params.width = menuItemButtonWidth;
                    menuItemButton.setLayoutParams(params);
                    availableWidth -= menuItemButtonWidth;
                    remainingMenuItems.remove(0);
                } else {
                    break;
                }
                isFirstItem = false;
            }

            if (!remainingMenuItems.isEmpty()) {
                // Reserve space for overflowButton.
                mMainPanel.setPaddingRelative(0, 0, mOverflowButtonSize.getWidth(), 0);
            }

            mMainPanelSize = measure(mMainPanel);
            return remainingMenuItems;
        }

        @SuppressWarnings("unchecked")
        void layoutOverflowPanelItems(List<SupportMenuItem> menuItems) {
            ArrayAdapter<SupportMenuItem> overflowPanelAdapter =
                    (ArrayAdapter<SupportMenuItem>) mOverflowPanel.getAdapter();
            overflowPanelAdapter.clear();
            final int size = menuItems.size();
            for (int i = 0; i < size; i++) {
                overflowPanelAdapter.add(menuItems.get(i));
            }
            mOverflowPanel.setAdapter(overflowPanelAdapter);
            if (mOpenOverflowUpwards) {
                mOverflowPanel.setY(0);
            } else {
                mOverflowPanel.setY(mOverflowButtonSize.getHeight());
            }

            int width = Math.max(getOverflowWidth(), mOverflowButtonSize.getWidth());
            int height = calculateOverflowHeight(MAX_OVERFLOW_SIZE);
            mOverflowPanelSize = new Size(width, height);
            setSize(mOverflowPanel, mOverflowPanelSize);
        }

        /**
         * Resets the content container and appropriately position it's panels.
         */
        void preparePopupContent() {
            mContentContainer.removeAllViews();

            // Add views in the specified order so they stack up as expected.
            // Order: overflowPanel, mainPanel, overflowButton.
            if (hasOverflow()) {
                mContentContainer.addView(mOverflowPanel);
            }
            mContentContainer.addView(mMainPanel);
            if (hasOverflow()) {
                mContentContainer.addView(mOverflowButton);
            }
            setPanelsStatesAtRestingPosition();

            // The positioning of contents in RTL is wrong when the view is first rendered.
            // Hide the view and post a runnable to recalculate positions and render the view.
            // TODO: Investigate why this happens and fix.
            if (isInRTLMode()) {
                mContentContainer.setAlpha(0);
                mContentContainer.post(mPreparePopupContentRTLHelper);
            }
        }

        /**
         * Clears out the panels and their container. Resets their calculated sizes.
         */
        @SuppressWarnings("unchecked")
        void clearPanels() {
            mOverflowPanelSize = null;
            mMainPanelSize = null;
            mIsOverflowOpen = false;
            mMainPanel.removeAllViews();
            ArrayAdapter<SupportMenuItem> overflowPanelAdapter =
                    (ArrayAdapter<SupportMenuItem>) mOverflowPanel.getAdapter();
            overflowPanelAdapter.clear();
            mOverflowPanel.setAdapter(overflowPanelAdapter);
            mContentContainer.removeAllViews();
        }

        void positionContentYCoordinatesIfOpeningOverflowUpwards() {
            if (mOpenOverflowUpwards) {
                mMainPanel.setY(mContentContainer.getHeight() - mMainPanelSize.getHeight());
                mOverflowButton.setY(mContentContainer.getHeight() - mOverflowButton.getHeight());
                mOverflowPanel.setY(mContentContainer.getHeight() - mOverflowPanelSize.getHeight());
            }
        }

        int getOverflowWidth() {
            int overflowWidth = 0;
            final int count = mOverflowPanel.getAdapter().getCount();
            for (int i = 0; i < count; i++) {
                SupportMenuItem menuItem = (SupportMenuItem) mOverflowPanel.getAdapter().getItem(i);
                overflowWidth =
                        Math.max(mOverflowPanelViewHelper.calculateWidth(menuItem), overflowWidth);
            }
            return overflowWidth;
        }

        int calculateOverflowHeight(int maxItemSize) {
            // Maximum of 4 items, minimum of 2 if the overflow has to scroll.
            int actualSize = Math.min(
                    MAX_OVERFLOW_SIZE,
                    Math.min(
                            Math.max(MIN_OVERFLOW_SIZE, maxItemSize),
                            mOverflowPanel.getCount()));
            int extension = 0;
            if (actualSize < mOverflowPanel.getCount()) {
                // The overflow will require scrolling to get to all the items.
                // Extend the height so that part of the hidden items is displayed.
                extension = (int) (mLineHeight * 0.5f);
            }
            return actualSize * mLineHeight
                    + mOverflowButtonSize.getHeight()
                    + extension;
        }

        void setButtonTagAndClickListener(View menuItemButton, SupportMenuItem menuItem) {
            menuItemButton.setTag(menuItem);
            menuItemButton.setOnClickListener(mMenuItemButtonOnClickListener);
        }

        /**
         * NOTE: Use only in android.view.animation.* animations. Do not use in android.animation.*
         * animations. See comment about this in the code.
         */
        int getAdjustedDuration(int originalDuration) {
            if (mTransitionDurationScale < 150) {
                // For smaller transition, decrease the time.
                return Math.max(originalDuration - 50, 0);
            } else if (mTransitionDurationScale > 300) {
                // For bigger transition, increase the time.
                return originalDuration + 50;
            }
            return originalDuration;
        }

        void maybeComputeTransitionDurationScale() {
            if (mMainPanelSize != null && mOverflowPanelSize != null) {
                int w = mMainPanelSize.getWidth() - mOverflowPanelSize.getWidth();
                int h = mOverflowPanelSize.getHeight() - mMainPanelSize.getHeight();
                mTransitionDurationScale = (int) (Math.sqrt(w * w + h * h)
                        / mContentContainer.getContext().getResources().getDisplayMetrics()
                                .density);
            }
        }

        ViewGroup createMainPanel() {
            ViewGroup mainPanel = new LinearLayout(mContext) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    if (isOverflowAnimating() && mMainPanelSize != null) {
                        // Update widthMeasureSpec to make sure that this view is not clipped
                        // as we offset it's coordinates with respect to it's parent.
                        widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                                mMainPanelSize.getWidth(),
                                MeasureSpec.EXACTLY);
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }

                @Override
                public boolean onInterceptTouchEvent(MotionEvent ev) {
                    // Intercept the touch event while the overflow is animating.
                    return isOverflowAnimating();
                }
            };
            mainPanel.setTag(MAIN_PANEL_TAG);
            return mainPanel;
        }

        ImageButton createOverflowButton() {
            final ImageButton overflowButton = (ImageButton) LayoutInflater.from(mContext)
                    .inflate(R.layout.floating_popup_overflow_button, null);
            overflowButton.setImageDrawable(mOverflow);
            overflowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mIsOverflowOpen) {
                        overflowButton.setImageDrawable(mToOverflow);
                        mToOverflow.start();
                        FloatingToolbarPopup.this.closeOverflow();
                    } else {
                        overflowButton.setImageDrawable(mToArrow);
                        mToArrow.start();
                        FloatingToolbarPopup.this.openOverflow();
                    }
                }
            });
            return overflowButton;
        }

        OverflowPanel createOverflowPanel() {
            final OverflowPanel overflowPanel = new OverflowPanel(this);
            overflowPanel.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            overflowPanel.setDivider(null);
            overflowPanel.setDividerHeight(0);

            final ArrayAdapter adapter =
                    new ArrayAdapter<SupportMenuItem>(mContext, 0) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            return mOverflowPanelViewHelper.getView(
                                    getItem(position), mOverflowPanelSize.getWidth(), convertView);
                        }
                    };
            overflowPanel.setAdapter(adapter);

            overflowPanel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final SupportMenuItem menuItem =
                            (SupportMenuItem) overflowPanel.getAdapter().getItem(position);
                    if (mOnMenuItemClickListener != null) {
                        mOnMenuItemClickListener.onMenuItemClick(menuItem);
                        if (mDismissOnMenuItemClick) {
                            mDismissRunnable.run();
                        }
                    }
                }
            });

            overflowPanel.setTag(OVERFLOW_PANEL_TAG);
            return overflowPanel;
        }

        boolean isOverflowAnimating() {
            final boolean overflowOpening = mOpenOverflowAnimation.hasStarted()
                    && !mOpenOverflowAnimation.hasEnded();
            final boolean overflowClosing = mCloseOverflowAnimation.hasStarted()
                    && !mCloseOverflowAnimation.hasEnded();
            return overflowOpening || overflowClosing;
        }

        private Animation.AnimationListener createOverflowAnimationListener() {
            Animation.AnimationListener listener = new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // Disable the overflow button while it's animating.
                    // It will be re-enabled when the animation stops.
                    mOverflowButton.setEnabled(false);
                    // Ensure both panels have visibility turned on when the overflow animation
                    // starts.
                    mMainPanel.setVisibility(View.VISIBLE);
                    mOverflowPanel.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // Posting this because it seems like this is called before the animation
                    // actually ends.
                    mContentContainer.post(new Runnable() {
                        @Override
                        public void run() {
                            setPanelsStatesAtRestingPosition();
                        }
                    });
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            };
            return listener;
        }

        private static Size measure(View view) {
            Preconditions.checkState(view.getParent() == null);
            view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            return new Size(view.getMeasuredWidth(), view.getMeasuredHeight());
        }

        private static void setSize(View view, int width, int height) {
            view.setMinimumWidth(width);
            view.setMinimumHeight(height);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params = (params == null) ? new ViewGroup.LayoutParams(0, 0) : params;
            params.width = width;
            params.height = height;
            view.setLayoutParams(params);
        }

        private static void setSize(View view, Size size) {
            setSize(view, size.getWidth(), size.getHeight());
        }

        static void setWidth(View view, int width) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            setSize(view, width, params.height);
        }

        static void setHeight(View view, int height) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            setSize(view, params.width, height);
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Rect getContainerBounds() {
            final View content;
            if (mIsOverflowOpen) {
                content = mOverflowPanel;
            } else {
                content = mMainPanel;
            }
            final int[] leftTop = new int[2];
            content.getLocationOnScreen(leftTop);
            final int left = leftTop[0];
            final int right = left + content.getWidth();
            final int top = leftTop[1];
            final int bottom = top + content.getHeight();
            return new Rect(left, top, right, bottom);
        }

        /**
         * A custom ListView for the overflow panel.
         */
        private static final class OverflowPanel extends ListView {

            private final FloatingToolbarPopup mPopup;

            OverflowPanel(FloatingToolbarPopup popup) {
                super(Preconditions.checkNotNull(popup).mContext);
                this.mPopup = popup;
                setScrollBarDefaultDelayBeforeFade(ViewConfiguration.getScrollDefaultDelay() * 3);
                setScrollIndicators(View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_BOTTOM);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                // Update heightMeasureSpec to make sure that this view is not clipped
                // as we offset it's coordinates with respect to it's parent.
                int height = mPopup.mOverflowPanelSize.getHeight()
                        - mPopup.mOverflowButtonSize.getHeight();
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (mPopup.isOverflowAnimating()) {
                    // Eat the touch event.
                    return true;
                }
                return super.dispatchTouchEvent(ev);
            }

            @Override
            protected boolean awakenScrollBars() {
                return super.awakenScrollBars();
            }
        }

        /**
         * A custom interpolator used for various floating toolbar animations.
         */
        static final class LogAccelerateInterpolator implements Interpolator {

            private static final int BASE = 100;
            private static final float LOGS_SCALE = 1f / computeLog(1, BASE);

            private static float computeLog(float t, int base) {
                return (float) (1 - Math.pow(base, -t));
            }

            @Override
            public float getInterpolation(float t) {
                return 1 - computeLog(1 - t, BASE) * LOGS_SCALE;
            }
        }

        /**
         * A helper for generating views for the overflow panel.
         */
        private static final class OverflowPanelViewHelper {

            private final View mCalculator;
            private final int mIconTextSpacing;
            private final int mSidePadding;

            private final Context mContext;

            OverflowPanelViewHelper(Context context, int iconTextSpacing) {
                mContext = Preconditions.checkNotNull(context);
                mIconTextSpacing = iconTextSpacing;
                mSidePadding = context.getResources()
                        .getDimensionPixelSize(R.dimen.floating_toolbar_overflow_side_padding);
                mCalculator = createMenuButton(null);
            }

            public View getView(SupportMenuItem menuItem, int minimumWidth, View convertView) {
                Preconditions.checkNotNull(menuItem);
                if (convertView != null) {
                    updateMenuItemButton(
                            convertView, menuItem, mIconTextSpacing, shouldShowIcon(menuItem));
                } else {
                    convertView = createMenuButton(menuItem);
                }
                convertView.setMinimumWidth(minimumWidth);
                return convertView;
            }

            public int calculateWidth(SupportMenuItem menuItem) {
                updateMenuItemButton(
                        mCalculator, menuItem, mIconTextSpacing, shouldShowIcon(menuItem));
                mCalculator.measure(
                        View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                return mCalculator.getMeasuredWidth();
            }

            private View createMenuButton(SupportMenuItem menuItem) {
                View button = createMenuItemButton(
                        mContext, menuItem, mIconTextSpacing, shouldShowIcon(menuItem));
                button.setPadding(mSidePadding, 0, mSidePadding, 0);
                return button;
            }

            private boolean shouldShowIcon(SupportMenuItem menuItem) {
                if (menuItem != null) {
                    return menuItem.getGroupId() == MENU_ID_SMART_ACTION;
                }
                return false;
            }
        }
    }

    /**
     * Creates and returns a menu button for the specified menu item.
     */
    static View createMenuItemButton(
            Context context, SupportMenuItem menuItem, int iconTextSpacing, boolean showIcon) {
        final View menuItemButton = LayoutInflater.from(context)
                .inflate(R.layout.floating_popup_menu_button, null);
        if (menuItem != null) {
            updateMenuItemButton(menuItemButton, menuItem, iconTextSpacing, showIcon);
        }
        return menuItemButton;
    }

    /**
     * Updates the specified menu item button with the specified menu item data.
     */
    static void updateMenuItemButton(
            View menuItemButton, SupportMenuItem menuItem, int iconTextSpacing, boolean showIcon) {
        final TextView buttonText = menuItemButton.findViewById(
                R.id.floating_toolbar_menu_item_text);
        buttonText.setEllipsize(null);
        if (TextUtils.isEmpty(menuItem.getTitle())) {
            buttonText.setVisibility(View.GONE);
        } else {
            buttonText.setVisibility(View.VISIBLE);
            buttonText.setText(menuItem.getTitle());
        }
        final ImageView buttonIcon = menuItemButton.findViewById(
                R.id.floating_toolbar_menu_item_image);
        if (menuItem.getIcon() == null || !showIcon) {
            buttonIcon.setVisibility(View.GONE);
            if (buttonText != null) {
                buttonText.setPaddingRelative(0, 0, 0, 0);
            }
        } else {
            buttonIcon.setVisibility(View.VISIBLE);
            buttonIcon.setImageDrawable(menuItem.getIcon());
            if (buttonText != null) {
                buttonText.setPaddingRelative(iconTextSpacing, 0, 0, 0);
            }
        }
        final CharSequence contentDescription = MenuItemCompat.getContentDescription(menuItem);
        if (TextUtils.isEmpty(contentDescription)) {
            menuItemButton.setContentDescription(menuItem.getTitle());
        } else {
            menuItemButton.setContentDescription(contentDescription);
        }
    }

    static ViewGroup createContentContainer(Context context) {
        ViewGroup contentContainer = (ViewGroup) LayoutInflater.from(context)
                .inflate(R.layout.floating_popup_container, null);
        contentContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentContainer.setTag(FLOATING_TOOLBAR_TAG);
        contentContainer.setClipToOutline(true);
        return contentContainer;
    }

    static PopupWindow createPopupWindow(ViewGroup content, View.OnClickListener onClick) {
        ViewGroup popupContentHolder = new LinearLayout(content.getContext());
        popupContentHolder.setOnClickListener(onClick);
        popupContentHolder.setSoundEffectsEnabled(false);
        PopupWindow popupWindow = new PopupWindow(popupContentHolder);
        // TODO: Use .setLayoutInScreenEnabled(true) instead of .setClippingEnabled(false)
        // unless FLAG_LAYOUT_IN_SCREEN has any unintentional side-effects.
        popupWindow.setClippingEnabled(false);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
        popupWindow.setAnimationStyle(0);
        int color = Color.TRANSPARENT;
        // Want to see the floating window? Uncomment the next line.
        //color = Color.argb(50, 0, 0, 0);
        popupWindow.setBackgroundDrawable(new ColorDrawable(color));
        content.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        popupContentHolder.addView(content);
        return popupWindow;
    }

    /**
     * Creates an "appear" animation for the specified view.
     *
     * @param view  The view to animate
     */
    static AnimatorSet createEnterAnimation(View view) {
        AnimatorSet animation = new AnimatorSet();
        animation.playTogether(
                ObjectAnimator.ofFloat(view, View.ALPHA, 0, 1).setDuration(150));
        return animation;
    }

    /**
     * Creates a "disappear" animation for the specified view.
     *
     * @param view  The view to animate
     * @param startDelay  The start delay of the animation
     * @param listener  The animation listener
     */
    static AnimatorSet createExitAnimation(
            View view, int startDelay, Animator.AnimatorListener listener) {
        AnimatorSet animation =  new AnimatorSet();
        animation.playTogether(
                ObjectAnimator.ofFloat(view, View.ALPHA, 1, 0).setDuration(100));
        animation.setStartDelay(startDelay);
        animation.addListener(listener);
        return animation;
    }

    // TODO: Replace with SupportMenuItem method when those are implemented.
    static boolean requiresOverflow(SupportMenuItem menuItem) {
        if (menuItem instanceof MenuItemImpl) {
            final MenuItemImpl impl = (MenuItemImpl) menuItem;
            return !impl.requiresActionButton() && !impl.requestsActionButton();
        }
        return false;
    }

    // TODO: Replace with SupportMenuItem method when those are implemented.
    static boolean requiresActionButton(SupportMenuItem menuItem) {
        return menuItem instanceof MenuItemImpl
                && ((MenuItemImpl) menuItem).requiresActionButton();
    }

    /**
     * Returns a re-themed context with controlled look and feel for views.
     */
    private static Context applyDefaultTheme(Context originalContext) {
        TypedArray a = originalContext.obtainStyledAttributes(
                new int[]{androidx.appcompat.R.attr.isLightTheme});
        boolean isLightTheme = a.getBoolean(0, true);
        int themeId = isLightTheme
                ? R.style.Theme_TextClassifier_FloatingToolbar_Light
                : R.style.Theme_TextClassifier_FloatingToolbar;
        a.recycle();
        return new ContextThemeWrapper(originalContext, themeId);
    }
}
