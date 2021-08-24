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
package androidx.core.animation;

import android.graphics.Path;
import android.graphics.PointF;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PathKeyframes relies on approximating the Path as a series of line segments.
 * The line segments are recursively divided until there is less than 1/2 pixel error
 * between the lines and the curve. Each point of the line segment is converted
 * to a Keyframe and a linear interpolation between Keyframes creates a good approximation
 * of the curve.
 * <p>
 * PathKeyframes is optimized to reduce the number of objects created when there are
 * many keyframes for a curve.
 * </p>
 * <p>
 * Typically, the returned type is a PointF, but the individual components can be extracted
 * as either an IntKeyframes or FloatKeyframes.
 * </p>
 */
class PathKeyframes implements Keyframes<PointF> {
    private static final int FRACTION_OFFSET = 0;
    private static final int X_OFFSET = 1;
    private static final int Y_OFFSET = 2;
    private static final int NUM_COMPONENTS = 3;
    private static final ArrayList<Keyframe<PointF>> EMPTY_KEYFRAMES = new ArrayList<>();

    private PointF mTempPointF = new PointF();
    private final float[] mKeyframeData;

    PathKeyframes(Path path) {
        this(path, 0.5f);
    }

    PathKeyframes(Path path, float error) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("The path must not be null or empty");
        }
        mKeyframeData = PathUtils.createKeyFrameData(path, error);
    }

    @Override
    public List<Keyframe<PointF>> getKeyframes() {
        return EMPTY_KEYFRAMES;
    }

    @Override
    public PointF getValue(float fraction) {
        int numPoints = mKeyframeData.length / 3;
        if (fraction < 0) {
            return interpolateInRange(fraction, 0, 1);
        } else if (fraction > 1) {
            return interpolateInRange(fraction, numPoints - 2, numPoints - 1);
        } else if (fraction == 0) {
            return pointForIndex(0);
        } else if (fraction == 1) {
            return pointForIndex(numPoints - 1);
        } else {
            // Binary search for the correct section
            int low = 0;
            int high = numPoints - 1;

            while (low <= high) {
                int mid = (low + high) / 2;
                float midFraction = mKeyframeData[(mid * NUM_COMPONENTS) + FRACTION_OFFSET];

                if (fraction < midFraction) {
                    high = mid - 1;
                } else if (fraction > midFraction) {
                    low = mid + 1;
                } else {
                    return pointForIndex(mid);
                }
            }

            // now high is below the fraction and low is above the fraction
            return interpolateInRange(fraction, high, low);
        }
    }

    private PointF interpolateInRange(float fraction, int startIndex, int endIndex) {
        int startBase = (startIndex * NUM_COMPONENTS);
        int endBase = (endIndex * NUM_COMPONENTS);

        float startFraction = mKeyframeData[startBase + FRACTION_OFFSET];
        float endFraction = mKeyframeData[endBase + FRACTION_OFFSET];

        float intervalFraction = (fraction - startFraction) / (endFraction - startFraction);

        float startX = mKeyframeData[startBase + X_OFFSET];
        float endX = mKeyframeData[endBase + X_OFFSET];
        float startY = mKeyframeData[startBase + Y_OFFSET];
        float endY = mKeyframeData[endBase + Y_OFFSET];

        float x = interpolate(intervalFraction, startX, endX);
        float y = interpolate(intervalFraction, startY, endY);

        mTempPointF.set(x, y);
        return mTempPointF;
    }

    @Override
    public void setEvaluator(TypeEvaluator evaluator) {
    }

    @Override
    public Class<PointF> getType() {
        return PointF.class;
    }

    @NonNull
    @Override
    public Keyframes clone() {
        Keyframes clone = null;
        try {
            clone = (Keyframes) super.clone();
        } catch (CloneNotSupportedException e) { }
        return clone;
    }

    private PointF pointForIndex(int index) {
        int base = (index * NUM_COMPONENTS);
        int xOffset = base + X_OFFSET;
        int yOffset = base + Y_OFFSET;
        mTempPointF.set(mKeyframeData[xOffset], mKeyframeData[yOffset]);
        return mTempPointF;
    }

    private static float interpolate(float fraction, float startValue, float endValue) {
        float diff = endValue - startValue;
        return startValue + (diff * fraction);
    }

    /**
     * Returns a FloatKeyframes for the X component of the Path.
     * @return a FloatKeyframes for the X component of the Path.
     */
    public FloatKeyframes createXFloatKeyframes() {
        return new FloatKeyframesBase() {
            @Override
            public float getFloatValue(float fraction) {
                PointF pointF = (PointF) PathKeyframes.this.getValue(fraction);
                return pointF.x;
            }
        };
    }

    /**
     * Returns a FloatKeyframes for the Y component of the Path.
     * @return a FloatKeyframes for the Y component of the Path.
     */
    public FloatKeyframes createYFloatKeyframes() {
        return new FloatKeyframesBase() {
            @Override
            public float getFloatValue(float fraction) {
                PointF pointF = (PointF) PathKeyframes.this.getValue(fraction);
                return pointF.y;
            }
        };
    }

    /**
     * Returns an IntKeyframes for the X component of the Path.
     * @return an IntKeyframes for the X component of the Path.
     */
    public IntKeyframes createXIntKeyframes() {
        return new IntKeyframesBase() {
            @Override
            public int getIntValue(float fraction) {
                PointF pointF = (PointF) PathKeyframes.this.getValue(fraction);
                return Math.round(pointF.x);
            }
        };
    }

    /**
     * Returns an IntKeyframeSet for the Y component of the Path.
     * @return an IntKeyframeSet for the Y component of the Path.
     */
    public IntKeyframes createYIntKeyframes() {
        return new IntKeyframesBase() {
            @Override
            public int getIntValue(float fraction) {
                PointF pointF = (PointF) PathKeyframes.this.getValue(fraction);
                return Math.round(pointF.y);
            }
        };
    }

    private abstract static class SimpleKeyframes<T> implements Keyframes<T> {
        private final ArrayList<Keyframe<T>> mEmptyFrames = new ArrayList<>();
        @Override
        public void setEvaluator(TypeEvaluator<T> evaluator) {
        }

        @Override
        public List<Keyframe<T>> getKeyframes() {
            return mEmptyFrames;
        }

        @NonNull
        @Override
        @SuppressWarnings({"unchecked", "CatchAndPrintStackTrace"})
        public Keyframes<T> clone() {
            Keyframes<T> clone = null;
            try {
                clone = (Keyframes<T>) super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return clone;
        }
    }

    abstract static class IntKeyframesBase extends SimpleKeyframes<Integer>
            implements IntKeyframes {
        @Override
        public Class<Integer> getType() {
            return Integer.class;
        }

        @Override
        public Integer getValue(float fraction) {
            return getIntValue(fraction);
        }
    }

    abstract static class FloatKeyframesBase extends SimpleKeyframes<Float>
            implements FloatKeyframes {
        @Override
        public Class<Float> getType() {
            return Float.class;
        }

        @Override
        public Float getValue(float fraction) {
            return getFloatValue(fraction);
        }
    }
}
