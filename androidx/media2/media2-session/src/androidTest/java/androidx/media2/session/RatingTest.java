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

import static org.junit.Assert.assertEquals;

import android.os.Parcel;

import androidx.media2.common.Rating;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link Rating} and its subclasses.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RatingTest extends MediaTestBase {
    @Test
    public void unratedHeartRating() {
        HeartRating rating = new HeartRating();
        assertEquals(false, rating.isRated());
        assertEquals(rating, writeToParcelAndCreateRating(rating));
    }

    @Test
    public void ratedHeartRating() {
        final boolean hasHeart = true;
        HeartRating rating = new HeartRating(hasHeart);
        assertEquals(true, rating.isRated());
        assertEquals(hasHeart, rating.hasHeart());
        assertEquals(rating, writeToParcelAndCreateRating(rating));
    }

    @Test
    public void unratedPercentageRating() {
        PercentageRating rating = new PercentageRating();
        assertEquals(false, rating.isRated());
        assertEquals(rating, writeToParcelAndCreateRating(rating));
    }

    @Test
    public void ratedPercentageRating() {
        double delta = 0.000001;
        float percentage = 20.5f;
        PercentageRating rating = new PercentageRating(percentage);
        assertEquals(true, rating.isRated());
        assertEquals(percentage, rating.getPercentRating(), delta);
        assertEquals(rating, writeToParcelAndCreateRating(rating));
    }

    @Test
    public void unratedThumbRating() {
        ThumbRating rating = new ThumbRating();
        assertEquals(false, rating.isRated());
        assertEquals(rating, writeToParcelAndCreateRating(rating));
    }

    @Test
    public void ratedThumbRating() {
        boolean isThumbUp = true;
        ThumbRating rating = new ThumbRating(isThumbUp);
        assertEquals(true, rating.isRated());
        assertEquals(isThumbUp, rating.isThumbUp());
        assertEquals(rating, writeToParcelAndCreateRating(rating));
    }

    @Test
    public void unratedStarRating() {
        int maxStars = 5;
        StarRating rating = new StarRating(maxStars);
        assertEquals(false, rating.isRated());
        assertEquals(maxStars, rating.getMaxStars());
        assertEquals(rating, writeToParcelAndCreateRating(rating));
    }

    @Test
    public void ratedStarRating() {
        double delta = 0.000001;
        int maxStars = 5;
        float starRating = 3.1f;
        StarRating rating = new StarRating(maxStars, starRating);
        assertEquals(true, rating.isRated());
        assertEquals(maxStars, rating.getMaxStars());
        assertEquals(starRating, rating.getStarRating(), delta);
        assertEquals(rating, writeToParcelAndCreateRating(rating));
    }

    private Rating writeToParcelAndCreateRating(Rating rating) {
        ParcelImpl parcelImpl = (ParcelImpl) ParcelUtils.toParcelable(rating);
        Parcel parcel = Parcel.obtain();
        try {
            parcelImpl.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            ParcelImpl newParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
            return ParcelUtils.fromParcelable(newParcelImpl);
        } finally {
            parcel.recycle();
        }
    }
}
