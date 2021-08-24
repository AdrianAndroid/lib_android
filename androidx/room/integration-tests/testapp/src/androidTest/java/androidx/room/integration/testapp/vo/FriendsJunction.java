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

package androidx.room.integration.testapp.vo;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.RoomWarnings;

@Entity(
        primaryKeys = {"friendA", "friendB"},
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "mId",
                        childColumns = "friendA",
                        onDelete = CASCADE),
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "mId",
                        childColumns = "friendB",
                        onDelete = CASCADE),
        })
@SuppressWarnings(RoomWarnings.MISSING_INDEX_ON_FOREIGN_KEY_CHILD)
public class FriendsJunction {
    public final int friendA;
    public final int friendB;

    public FriendsJunction(int friendA, int friendB) {
        this.friendA = friendA;
        this.friendB = friendB;
    }
}
