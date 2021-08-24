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

package androidx.wear.input;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.google.android.wearable.input.WearableInputDevice;

/** Class containing helpers for managing wearable buttons. */
public final class WearableButtons {

    private static WearableButtonsProvider sButtonsProvider = new DeviceWearableButtonsProvider();

    /** Represents that the count of available device buttons. */
    private static volatile int sButtonCount = -1;

    private WearableButtons() {
        throw new RuntimeException("WearableButtons should not be instantiated");
    }

    /**
     * Testing call to allow the underlying {@link WearableButtonsProvider} to be substituted in
     * test code.
     *
     * @param provider The new {@link WearableButtonsProvider} to use.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void setWearableButtonsProvider(@NonNull WearableButtonsProvider provider) {
        sButtonsProvider = provider;
    }

    /** Represents that the location zone is unknown. */
    @VisibleForTesting static final int LOC_UNKNOWN = -1;

    /** Represents the east position on a round device. */
    @VisibleForTesting static final int LOC_EAST = 0;

    /** Represents the east-northeast position on a round device. */
    @VisibleForTesting static final int LOC_ENE = 1;

    /** Represents the northeast position on a round device. */
    @VisibleForTesting static final int LOC_NE = 2;

    /** Represents the north-northeast position on a round device. */
    @VisibleForTesting static final int LOC_NNE = 3;

    /** Represents the north position on a round device. */
    @VisibleForTesting static final int LOC_NORTH = 4;

    /** Represents the north-northwest position on a round device. */
    @VisibleForTesting static final int LOC_NNW = 5;

    /** Represents the northwest position on a round device. */
    @VisibleForTesting static final int LOC_NW = 6;

    /** Represents the west-northwest position on a round device. */
    @VisibleForTesting static final int LOC_WNW = 7;

    /** Represents the west position on a round device. */
    @VisibleForTesting static final int LOC_WEST = 8;

    /** Represents the west-southwest position on a round device. */
    @VisibleForTesting static final int LOC_WSW = 9;

    /** Represents the southwest position on a round device. */
    @VisibleForTesting static final int LOC_SW = 10;

    /** Represents the south-southwest position on a round device. */
    @VisibleForTesting static final int LOC_SSW = 11;

    /** Represents the south position on a round device. */
    @VisibleForTesting static final int LOC_SOUTH = 12;

    /** Represents the south-southeast position on a round device. */
    @VisibleForTesting static final int LOC_SSE = 13;

    /** Represents the southeast position on a round device. */
    @VisibleForTesting static final int LOC_SE = 14;

    /** Represents the east-southeast position on a round device. */
    @VisibleForTesting static final int LOC_ESE = 15;

    private static final int LOC_ROUND_COUNT = 16;

    /** Represents the right third of the top side on a square device. */
    @VisibleForTesting static final int LOC_TOP_RIGHT = 100;

    /** Represents the center third of the top side on a square device. */
    @VisibleForTesting static final int LOC_TOP_CENTER = 101;

    /** Represents the left third of the top side on a square device. */
    @VisibleForTesting static final int LOC_TOP_LEFT = 102;

    /** Represents the top third of the left side on a square device. */
    @VisibleForTesting static final int LOC_LEFT_TOP = 103;

    /** Represents the center third of the left side on a square device. */
    @VisibleForTesting static final int LOC_LEFT_CENTER = 104;

    /** Represents the bottom third of the left side on a square device. */
    @VisibleForTesting static final int LOC_LEFT_BOTTOM = 105;

    /** Represents the left third of the bottom side on a square device. */
    @VisibleForTesting static final int LOC_BOTTOM_LEFT = 106;

    /** Represents the center third of the bottom side on a square device. */
    @VisibleForTesting static final int LOC_BOTTOM_CENTER = 107;

    /** Represents the right third of the bottom side on a square device. */
    @VisibleForTesting static final int LOC_BOTTOM_RIGHT = 108;

    /** Represents the bottom third of the right side on a square device. */
    @VisibleForTesting static final int LOC_RIGHT_BOTTOM = 109;

    /** Represents the center third of the right side on a square device. */
    @VisibleForTesting static final int LOC_RIGHT_CENTER = 110;

    /** Represents the top third of the right side on a square device. */
    @VisibleForTesting static final int LOC_RIGHT_TOP = 111;

    /**
     * Key used with the bundle returned by {@link #getButtonInfo}} to retrieve the x coordinate of
     * a button when the screen is rotated 180 degrees. (temporary copy from WearableInputDevice)
     */
    private static final String X_KEY_ROTATED = "x_key_rotated";

    /**
     * Key used with the bundle returned by {@link #getButtonInfo}} to retrieve the y coordinate of
     * a button when the screen is rotated 180 degrees. (temporary copy from WearableInputDevice)
     */
    private static final String Y_KEY_ROTATED = "y_key_rotated";

    /**
     * Returns a {@link ButtonInfo} containing the metadata for a specific button.
     *
     * <p>The location will be populated in the following manner:
     *
     * <ul>
     *   <li>The provided point will be on the screen, or more typically, on the edge of the screen.
     *   <li>The point won't be off the edge of the screen.
     *   <li>The location returned is a screen coordinate. The unit of measurement is in pixels. The
     *       coordinates do not take rotation into account and assume that the device is in the
     *       standard upright position.
     * </ul>
     *
     * <p>Common keycodes to use are {@link android.view.KeyEvent#KEYCODE_STEM_PRIMARY}, {@link
     * android.view.KeyEvent#KEYCODE_STEM_1}, {@link android.view.KeyEvent#KEYCODE_STEM_2}, and
     * {@link android.view.KeyEvent#KEYCODE_STEM_3}.
     *
     * @param context The context of the current activity
     * @param keycode The keycode associated with the hardware button of interest
     * @return A {@link ButtonInfo} containing the metadata for the given keycode or null if the
     *     information is not available
     */
    @SuppressWarnings("deprecation")
    @Nullable
    public static ButtonInfo getButtonInfo(@NonNull Context context, int keycode) {
        Bundle bundle = sButtonsProvider.getButtonInfo(context, keycode);

        if (bundle == null) {
            return null;
        }

        // If the information is not available, return null
        if (!bundle.containsKey(WearableInputDevice.X_KEY)
                || !bundle.containsKey(WearableInputDevice.Y_KEY)) {
            return null;
        }

        float screenLocationX = bundle.getFloat(WearableInputDevice.X_KEY);
        float screenLocationY = bundle.getFloat(WearableInputDevice.Y_KEY);

        // Get the screen size for the locationZone
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point screenSize = new Point();
        wm.getDefaultDisplay().getSize(screenSize);

        if (isLeftyModeEnabled(context)) {
            // By default, the rotated placement is exactly the opposite.
            // This may be overridden if there is a remapping of buttons applied as well.
            float screenRotatedX = screenSize.x - screenLocationX;
            float screenRotatedY = screenSize.y - screenLocationY;

            if (bundle.containsKey(X_KEY_ROTATED) && bundle.containsKey(Y_KEY_ROTATED)) {
                screenRotatedX = bundle.getFloat(X_KEY_ROTATED);
                screenRotatedY = bundle.getFloat(Y_KEY_ROTATED);
            }

            screenLocationX = screenRotatedX;
            screenLocationY = screenRotatedY;
        }

        boolean isRound = context.getResources().getConfiguration().isScreenRound();

        ButtonInfo info =
                new ButtonInfo(
                        keycode,
                        screenLocationX,
                        screenLocationY,
                        getLocationZone(isRound, screenSize, screenLocationX, screenLocationY));

        return info;
    }

    /**
     * Get the number of hardware buttons available. This count includes the primary stem key as
     * well as any secondary stem keys available.
     *
     * @param context The context of the current activity
     * @return The number of buttons available, or the information is not available.
     */
    public static int getButtonCount(@NonNull Context context) {
        if (sButtonCount == -1) {
            int[] buttonCodes = sButtonsProvider.getAvailableButtonKeyCodes(context);

            if (buttonCodes == null) {
                return -1;
            }

            sButtonCount = buttonCodes.length;
        }

        return sButtonCount;
    }

    /**
     * Returns an icon that can be used to represent the location of a button.
     *
     * @param context The context of the current activity
     * @param keycode The keycode associated with the hardware button of interest
     * @return A drawable representing the location of a button, or null if unavailable
     */
    @Nullable
    public static Drawable getButtonIcon(@NonNull Context context, int keycode) {
        ButtonInfo info = getButtonInfo(context, keycode);
        if (info == null) {
            return null;
        }
        return getButtonIconFromLocationZone(context, info.getLocationZone());
    }

    @VisibleForTesting
    static RotateDrawable getButtonIconFromLocationZone(Context context, int locationZone) {
        // To save memory for assets, we are using 4 icons to represent the 20+ possible
        // configurations.  These 4 base icons can be rotated to fit any configuration needed.
        // id is the drawable id for the base icon
        // degrees is the number of degrees the icon needs to be rotated to match the wanted
        // position
        int id;
        int degrees;

        switch (locationZone) {
                // Round constants
            case LOC_EAST:
                id = R.drawable.ic_cc_settings_button_e;
                degrees = 0;
                break;
            case LOC_ENE:
            case LOC_NE:
            case LOC_NNE:
                id = R.drawable.ic_cc_settings_button_e;
                degrees = -45;
                break;
            case LOC_NORTH:
                id = R.drawable.ic_cc_settings_button_e;
                degrees = -90;
                break;
            case LOC_NNW:
            case LOC_NW:
            case LOC_WNW:
                id = R.drawable.ic_cc_settings_button_e;
                degrees = -135;
                break;
            case LOC_WEST:
                id = R.drawable.ic_cc_settings_button_e;
                degrees = 180;
                break;
            case LOC_WSW:
            case LOC_SW:
            case LOC_SSW:
                id = R.drawable.ic_cc_settings_button_e;
                degrees = 135;
                break;
            case LOC_SOUTH:
                id = R.drawable.ic_cc_settings_button_e;
                degrees = 90;
                break;
            case LOC_SSE:
            case LOC_SE:
            case LOC_ESE:
                id = R.drawable.ic_cc_settings_button_e;
                degrees = 45;
                break;

                // Rectangular constants
            case LOC_LEFT_TOP:
                id = R.drawable.ic_cc_settings_button_bottom;
                degrees = 180;
                break;
            case LOC_LEFT_CENTER:
                id = R.drawable.ic_cc_settings_button_center;
                degrees = 180;
                break;
            case LOC_LEFT_BOTTOM:
                id = R.drawable.ic_cc_settings_button_top;
                degrees = 180;
                break;
            case LOC_RIGHT_TOP:
                id = R.drawable.ic_cc_settings_button_top;
                degrees = 0;
                break;
            case LOC_RIGHT_CENTER:
                id = R.drawable.ic_cc_settings_button_center;
                degrees = 0;
                break;
            case LOC_RIGHT_BOTTOM:
                id = R.drawable.ic_cc_settings_button_bottom;
                degrees = 0;
                break;
            case LOC_TOP_LEFT:
                id = R.drawable.ic_cc_settings_button_top;
                degrees = -90;
                break;
            case LOC_TOP_CENTER:
                id = R.drawable.ic_cc_settings_button_center;
                degrees = -90;
                break;
            case LOC_TOP_RIGHT:
                id = R.drawable.ic_cc_settings_button_bottom;
                degrees = -90;
                break;
            case LOC_BOTTOM_LEFT:
                id = R.drawable.ic_cc_settings_button_bottom;
                degrees = 90;
                break;
            case LOC_BOTTOM_CENTER:
                id = R.drawable.ic_cc_settings_button_center;
                degrees = 90;
                break;
            case LOC_BOTTOM_RIGHT:
                id = R.drawable.ic_cc_settings_button_top;
                degrees = 90;
                break;
            default:
                throw new IllegalArgumentException("Unexpected location zone");
        }
        RotateDrawable rotateIcon = new RotateDrawable();
        rotateIcon.setDrawable(context.getDrawable(id));
        rotateIcon.setFromDegrees(degrees);
        rotateIcon.setToDegrees(degrees);
        rotateIcon.setLevel(1);
        return rotateIcon;
    }

    /**
     * Returns a CharSequence that describes the placement location of a button. An example might be
     * "Top right" or "Bottom".
     *
     * @param context The context of the current activity
     * @param keycode The keycode associated with the hardware button of interest
     * @return A CharSequence describing the placement location of the button, or null if no
     * location is available for that button.
     */
    @Nullable
    public static CharSequence getButtonLabel(@NonNull Context context, int keycode) {
        // 4 length array where the index uses the standard quadrant counting system (minus 1 for
        // 0 index)
        int[] buttonsInQuadrantCount = new int[4];

        // Retrieve ButtonInfo objects and count how many buttons are in a quadrant.  This is only
        // needed for round devices but will help us come up with friendly strings to show to the
        // user.
        // TODO(ahugh): We can cache quadrant counts to optimize. These values should be static for
        // each
        // device.
        int[] buttonCodes = sButtonsProvider.getAvailableButtonKeyCodes(context);

        if (buttonCodes == null) {
            return null;
        }

        for (int key : buttonCodes) {
            ButtonInfo info = getButtonInfo(context, key);

            if (info != null) {
                int quadrantIndex = getQuadrantIndex(info.getLocationZone());
                if (quadrantIndex != -1) {
                    ++buttonsInQuadrantCount[quadrantIndex];
                }
            }
        }

        ButtonInfo info = getButtonInfo(context, keycode);
        int quadrantIndex = (info != null ? getQuadrantIndex(info.getLocationZone()) : -1);
        return info == null
                ? null
                : context.getString(
                        getFriendlyLocationZoneStringId(
                                info.getLocationZone(),
                                (quadrantIndex == -1 ? 0 : buttonsInQuadrantCount[quadrantIndex])));
    }

    /**
     * Returns quadrant index if locationZone is for a round device. Follows the conventional
     * quadrant system with the top right quadrant being 0, incrementing the index by 1 going
     * counter-clockwise around.
     */
    private static int getQuadrantIndex(int locationZone) {
        switch (locationZone) {
            case LOC_ENE:
            case LOC_NE:
            case LOC_NNE:
                return 0;
            case LOC_NNW:
            case LOC_NW:
            case LOC_WNW:
                return 1;
            case LOC_WSW:
            case LOC_SW:
            case LOC_SSW:
                return 2;
            case LOC_SSE:
            case LOC_SE:
            case LOC_ESE:
                return 3;

            default:
                return -1;
        }
    }

    /**
     * If the screen is round, there is special logic we use to determine the string that should
     * show on the screen. Simple strings are broad descriptors like "top right". Detailed strings
     * are narrow descriptors like, "top right, upper" 1) If there are exactly 2 buttons in a
     * quadrant, use detailed strings to describe button locations. 2) Otherwise, use simple strings
     * to describe the button locations.
     *
     * @param locationZone The location zone to get a string id for
     * @param buttonsInQuadrantCount The number of buttons in the quadrant of the button
     * @return The string id to use to represent this button zone
     */
    @VisibleForTesting
    static int getFriendlyLocationZoneStringId(int locationZone, int buttonsInQuadrantCount) {
        if (buttonsInQuadrantCount == 2) {
            switch (locationZone) {
                case LOC_ENE:
                    return R.string.buttons_round_top_right_lower;
                case LOC_NE:
                case LOC_NNE:
                    return R.string.buttons_round_top_right_upper;
                case LOC_NNW:
                case LOC_NW:
                    return R.string.buttons_round_top_left_upper;
                case LOC_WNW:
                    return R.string.buttons_round_top_left_lower;
                case LOC_ESE:
                case LOC_SE:
                    return R.string.buttons_round_bottom_left_upper;
                case LOC_SSE:
                    return R.string.buttons_round_bottom_left_lower;
                case LOC_SSW:
                    return R.string.buttons_round_bottom_right_lower;
                case LOC_SW:
                case LOC_WSW:
                    return R.string.buttons_round_bottom_right_upper;
                default: // fall out
            }
        }

        // If we couldn't find a detailed string, or we need a simple string
        switch (locationZone) {
                // Round constants
            case LOC_EAST:
                return R.string.buttons_round_center_right;
            case LOC_ENE:
            case LOC_NE:
            case LOC_NNE:
                return R.string.buttons_round_top_right;
            case LOC_NORTH:
                return R.string.buttons_round_top_center;
            case LOC_NNW:
            case LOC_NW:
            case LOC_WNW:
                return R.string.buttons_round_top_left;
            case LOC_WEST:
                return R.string.buttons_round_center_left;
            case LOC_WSW:
            case LOC_SW:
            case LOC_SSW:
                return R.string.buttons_round_bottom_left;
            case LOC_SOUTH:
                return R.string.buttons_round_bottom_center;
            case LOC_SSE:
            case LOC_SE:
            case LOC_ESE:
                return R.string.buttons_round_bottom_right;

                // Rectangular constants
            case LOC_LEFT_TOP:
                return R.string.buttons_rect_left_top;
            case LOC_LEFT_CENTER:
                return R.string.buttons_rect_left_center;
            case LOC_LEFT_BOTTOM:
                return R.string.buttons_rect_left_bottom;
            case LOC_RIGHT_TOP:
                return R.string.buttons_rect_right_top;
            case LOC_RIGHT_CENTER:
                return R.string.buttons_rect_right_center;
            case LOC_RIGHT_BOTTOM:
                return R.string.buttons_rect_right_bottom;
            case LOC_TOP_LEFT:
                return R.string.buttons_rect_top_left;
            case LOC_TOP_CENTER:
                return R.string.buttons_rect_top_center;
            case LOC_TOP_RIGHT:
                return R.string.buttons_rect_top_right;
            case LOC_BOTTOM_LEFT:
                return R.string.buttons_rect_bottom_left;
            case LOC_BOTTOM_CENTER:
                return R.string.buttons_rect_bottom_center;
            case LOC_BOTTOM_RIGHT:
                return R.string.buttons_rect_bottom_right;
            default:
                throw new IllegalArgumentException("Unexpected location zone");
        }
    }

    /**
     * For round devices, the location zone is defined using 16 points in a compass arrangement. If
     * a button falls between anchor points, this method will return the closest anchor.
     *
     * <p>For rectangular devices, the location zone is defined by splitting each side into thirds.
     * If a button falls anywhere within a zone, the method will return that zone. The constants for
     * these zones are named LOC_[side in question]_[which third is affected]. E.g. LOC_TOP_RIGHT
     * would refer to the right third of the top side of the device.
     */
    @VisibleForTesting
    /* package */ static int getLocationZone(
            boolean isRound, Point screenSize, float screenLocationX, float screenLocationY) {
        if (screenLocationX == Float.MAX_VALUE || screenLocationY == Float.MAX_VALUE) {
            return LOC_UNKNOWN;
        }

        return isRound
                ? getLocationZoneRound(screenSize, screenLocationX, screenLocationY)
                : getLocationZoneRectangular(screenSize, screenLocationX, screenLocationY);
    }

    private static int getLocationZoneRound(
            Point screenSize, float screenLocationX, float screenLocationY) {
        // Convert screen coordinate to Cartesian coordinate
        float cartesianX = screenLocationX - screenSize.x / 2;
        float cartesianY = screenSize.y / 2 - screenLocationY;

        // Use polar coordinates to figure out which zone the point is in
        double angle = Math.atan2(cartesianY, cartesianX);

        // Convert angle to all positive values
        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        // Return the associated section rounded to the nearest anchor.
        // Using some clever math tricks and enum declaration, we can reduce this calculation
        // down to a single formula that converts angle to enum value.
        return Math.round((float) (angle / (Math.PI / 8))) % LOC_ROUND_COUNT;
    }

    private static int getLocationZoneRectangular(
            Point screenSize, float screenLocationX, float screenLocationY) {
        // Calculate distance to each edge.
        float deltaFromLeft = screenLocationX;
        float deltaFromRight = screenSize.x - screenLocationX;
        float deltaFromTop = screenLocationY;
        float deltaFromBottom = screenSize.y - screenLocationY;
        float minDelta =
                Math.min(
                        deltaFromLeft,
                        Math.min(deltaFromRight, Math.min(deltaFromTop, deltaFromBottom)));

        // Prioritize ties to left and right sides of watch since they're more likely to be placed
        // on the side. Buttons directly on the corner are not accounted for with this API.
        if (minDelta == deltaFromLeft) {
            // Left is the primary side
            switch (whichThird(screenSize.y, screenLocationY)) {
                case 0:
                    return LOC_LEFT_TOP;
                case 1:
                    return LOC_LEFT_CENTER;
                default:
                    return LOC_LEFT_BOTTOM;
            }
        } else if (minDelta == deltaFromRight) {
            // Right is primary side
            switch (whichThird(screenSize.y, screenLocationY)) {
                case 0:
                    return LOC_RIGHT_TOP;
                case 1:
                    return LOC_RIGHT_CENTER;
                default:
                    return LOC_RIGHT_BOTTOM;
            }
        } else if (minDelta == deltaFromTop) {
            // Top is primary side
            switch (whichThird(screenSize.x, screenLocationX)) {
                case 0:
                    return LOC_TOP_LEFT;
                case 1:
                    return LOC_TOP_CENTER;
                default:
                    return LOC_TOP_RIGHT;
            }
        } else /* if (minDelta == deltaFromBottom) */ {
            // Bottom is primary side
            switch (whichThird(screenSize.x, screenLocationX)) {
                case 0:
                    return LOC_BOTTOM_LEFT;
                case 1:
                    return LOC_BOTTOM_CENTER;
                default:
                    return LOC_BOTTOM_RIGHT;
            }
        }
    }

    // Returns 0, 1, or 2 which correspond to the index of the third the screen point lies in
    // from 'left to right' or 'top to bottom'.
    private static int whichThird(float screenLength, float screenLocation) {
        if (screenLocation <= screenLength / 3) {
            return 0;
        } else if (screenLocation <= screenLength * 2 / 3) {
            return 1;
        } else {
            return 2;
        }
    }

    private static boolean isLeftyModeEnabled(Context context) {
        return Settings.System.getInt(
                        context.getContentResolver(),
                        Settings.System.USER_ROTATION,
                        Surface.ROTATION_0)
                == Surface.ROTATION_180;
    }

    /** Metadata for a specific button. */
    public static final class ButtonInfo {
        private final int mKeycode;
        private final float mX;
        private final float mY;

        /**
         * The location zone of the button as defined in the {@link #getButtonInfo(Context, int)}
         * method. The intended use is to help developers attach a friendly String to the button
         * location. This value is LOC_UNKNOWN if the information is not available.
         */
        private final int mLocationZone;

        /**
         * Gets the keycode this {@code ButtonInfo} provides information for.
         *
         * @return The keycode this {@code ButtonInfo} provides information for
         */
        public int getKeycode() {
            return mKeycode;
        }

        /** The x coordinate of the button in screen coordinates. */
        public float getX() {
            return mX;
        }

        /** The y coordinate of the button in screen coordinates. */
        public float getY() {
            return mY;
        }

        /** The location zone of the button (e.g. LOC_EAST) */
        public int getLocationZone() {
            return mLocationZone;
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @VisibleForTesting
        public ButtonInfo(int keycode, float x, float y, int locationZone) {
            this.mKeycode = keycode;
            this.mX = x;
            this.mY = y;
            this.mLocationZone = locationZone;
        }
    }
}
