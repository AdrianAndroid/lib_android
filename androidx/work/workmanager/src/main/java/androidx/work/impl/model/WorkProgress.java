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

package androidx.work.impl.model;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.work.Data;

/**
 * A Database entity which stores progress of a given {@link WorkSpec} id.
 *
 * @hide
 */
@Entity(foreignKeys = {
        @ForeignKey(
                entity = WorkSpec.class,
                parentColumns = "id",
                childColumns = "work_spec_id",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkProgress {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "work_spec_id")
    public final String mWorkSpecId;

    @NonNull
    @ColumnInfo(name = "progress")
    public final Data mProgress;

    public WorkProgress(@NonNull String workSpecId, @NonNull Data progress) {
        mWorkSpecId = workSpecId;
        mProgress = progress;
    }
}
