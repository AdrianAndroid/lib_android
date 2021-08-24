/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.navigation.routing;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;

/** A screen that shows the navigation template in arrived state. */
public final class ArrivedDemoScreen extends Screen implements DefaultLifecycleObserver {
    public ArrivedDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Resources resources = getCarContext().getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.test_image_square);

        return new NavigationTemplate.Builder()
                .setNavigationInfo(
                        new MessageInfo.Builder("Arrived!")
                                .setText("Google Bellevue Office\n1120 112th Ave NE")
                                .setImage(
                                        new CarIcon.Builder(IconCompat.createWithBitmap(bitmap))
                                                .build())
                                .build())
                .setActionStrip(RoutingDemoModels.getActionStrip(getCarContext(), this::finish))
                .setBackgroundColor(CarColor.SECONDARY)
                .build();
    }
}
