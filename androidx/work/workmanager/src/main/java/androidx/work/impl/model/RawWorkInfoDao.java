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

package androidx.work.impl.model;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.work.WorkQuery;

import java.util.List;

/**
 * A Data Access Object for accessing {@link androidx.work.WorkInfo}s that uses raw SQL queries.
 */
@Dao
public interface RawWorkInfoDao {
    /**
     * @param query The raw query obtained using {@link WorkQuery}
     * @return A {@link List} of {@link WorkSpec.WorkInfoPojo}s using the raw query.
     */
    @RawQuery(observedEntities = WorkSpec.class)
    @NonNull
    List<WorkSpec.WorkInfoPojo> getWorkInfoPojos(@NonNull SupportSQLiteQuery query);

    /**
     * @param query The raw query obtained using {@link WorkQuery}
     * @return A {@link LiveData} of a {@link List} of {@link WorkSpec.WorkInfoPojo}s using the
     * raw query.
     */
    @RawQuery(observedEntities = WorkSpec.class)
    @NonNull
    LiveData<List<WorkSpec.WorkInfoPojo>> getWorkInfoPojosLiveData(
            @NonNull SupportSQLiteQuery query);
}
