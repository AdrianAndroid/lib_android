/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static androidx.room.OnConflictStrategy.IGNORE;
import static androidx.work.impl.model.WorkTypeConverters.StateIds.COMPLETED_STATES;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.work.Data;
import androidx.work.WorkInfo;

import java.util.List;

/**
 * The Data Access Object for {@link WorkSpec}s.
 */
@Dao
@SuppressLint("UnknownNullness")
public interface WorkSpecDao {
    /**
     * Attempts to insert a {@link WorkSpec} into the database.
     *
     * @param workSpec The WorkSpec to insert.
     */
    @Insert(onConflict = IGNORE)
    void insertWorkSpec(WorkSpec workSpec);

    /**
     * Deletes {@link WorkSpec}s from the database.
     *
     * @param id The WorkSpec id to delete.
     */
    @Query("DELETE FROM workspec WHERE id=:id")
    void delete(String id);

    /**
     * @param id The identifier
     * @return The WorkSpec associated with that id
     */
    @Query("SELECT * FROM workspec WHERE id=:id")
    WorkSpec getWorkSpec(String id);

    /**
     * Retrieves {@link WorkSpec}s with the identifiers.
     *
     * @param ids The identifiers of desired {@link WorkSpec}s
     * @return The {@link WorkSpec}s with the requested IDs
     */
    @Query("SELECT * FROM workspec WHERE id IN (:ids)")
    WorkSpec[] getWorkSpecs(List<String> ids);

    /**
     *
     * @param name The work graph name
     * @return The {@link WorkSpec}s labelled with the given name
     */
    @Query("SELECT id, state FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM workname WHERE name=:name)")
    List<WorkSpec.IdAndState> getWorkSpecIdAndStatesForName(String name);

    /**
     * @return All WorkSpec ids in the database.
     */
    @Query("SELECT id FROM workspec")
    List<String> getAllWorkSpecIds();

    /**
     * @return A {@link LiveData} list of all WorkSpec ids in the database.
     */
    @Transaction
    @Query("SELECT id FROM workspec")
    LiveData<List<String>> getAllWorkSpecIdsLiveData();

    /**
     * Updates the state of at least one {@link WorkSpec} by ID.
     *
     * @param state The new state
     * @param ids The IDs for the {@link WorkSpec}s to update
     * @return The number of rows that were updated
     */
    @Query("UPDATE workspec SET state=:state WHERE id IN (:ids)")
    int setState(WorkInfo.State state, String... ids);

    /**
     * Updates the output of a {@link WorkSpec}.
     *
     * @param id The {@link WorkSpec} identifier to update
     * @param output The {@link Data} to set as the output
     */
    @Query("UPDATE workspec SET output=:output WHERE id=:id")
    void setOutput(String id, Data output);

    /**
     * Updates the period start time of a {@link WorkSpec}.
     *
     * @param id The {@link WorkSpec} identifier to update
     * @param periodStartTime The time when the period started.
     */
    @Query("UPDATE workspec SET period_start_time=:periodStartTime WHERE id=:id")
    void setPeriodStartTime(String id, long periodStartTime);

    /**
     * Increment run attempt count of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET run_attempt_count=run_attempt_count+1 WHERE id=:id")
    int incrementWorkSpecRunAttemptCount(String id);

    /**
     * Reset run attempt count of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET run_attempt_count=0 WHERE id=:id")
    int resetWorkSpecRunAttemptCount(String id);

    /**
     * Retrieves the state of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The state of the {@link WorkSpec}
     */
    @Query("SELECT state FROM workspec WHERE id=:id")
    WorkInfo.State getState(String id);

    /**
     * For a {@link WorkSpec} identifier, retrieves its {@link WorkSpec.WorkInfoPojo}.
     *
     * @param id The identifier of the {@link WorkSpec}
     * @return A list of {@link WorkSpec.WorkInfoPojo}
     */
    @Transaction
    @Query("SELECT id, state, output, run_attempt_count FROM workspec WHERE id=:id")
    WorkSpec.WorkInfoPojo getWorkStatusPojoForId(String id);

    /**
     * For a list of {@link WorkSpec} identifiers, retrieves a {@link List} of their
     * {@link WorkSpec.WorkInfoPojo}.
     *
     * @param ids The identifier of the {@link WorkSpec}s
     * @return A {@link List} of {@link WorkSpec.WorkInfoPojo}
     */
    @Transaction
    @Query("SELECT id, state, output, run_attempt_count FROM workspec WHERE id IN (:ids)")
    List<WorkSpec.WorkInfoPojo> getWorkStatusPojoForIds(List<String> ids);

    /**
     * For a list of {@link WorkSpec} identifiers, retrieves a {@link LiveData} list of their
     * {@link WorkSpec.WorkInfoPojo}.
     *
     * @param ids The identifier of the {@link WorkSpec}s
     * @return A {@link LiveData} list of {@link WorkSpec.WorkInfoPojo}
     */
    @Transaction
    @Query("SELECT id, state, output, run_attempt_count FROM workspec WHERE id IN (:ids)")
    LiveData<List<WorkSpec.WorkInfoPojo>> getWorkStatusPojoLiveDataForIds(List<String> ids);

    /**
     * Retrieves a list of {@link WorkSpec.WorkInfoPojo} for all work with a given tag.
     *
     * @param tag The tag for the {@link WorkSpec}s
     * @return A list of {@link WorkSpec.WorkInfoPojo}
     */
    @Transaction
    @Query("SELECT id, state, output, run_attempt_count FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    List<WorkSpec.WorkInfoPojo> getWorkStatusPojoForTag(String tag);

    /**
     * Retrieves a {@link LiveData} list of {@link WorkSpec.WorkInfoPojo} for all work with a
     * given tag.
     *
     * @param tag The tag for the {@link WorkSpec}s
     * @return A {@link LiveData} list of {@link WorkSpec.WorkInfoPojo}
     */
    @Transaction
    @Query("SELECT id, state, output, run_attempt_count FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    LiveData<List<WorkSpec.WorkInfoPojo>> getWorkStatusPojoLiveDataForTag(String tag);

    /**
     * Retrieves a list of {@link WorkSpec.WorkInfoPojo} for all work with a given name.
     *
     * @param name The name of the {@link WorkSpec}s
     * @return A list of {@link WorkSpec.WorkInfoPojo}
     */
    @Transaction
    @Query("SELECT id, state, output, run_attempt_count FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM workname WHERE name=:name)")
    List<WorkSpec.WorkInfoPojo> getWorkStatusPojoForName(String name);

    /**
     * Retrieves a {@link LiveData} list of {@link WorkSpec.WorkInfoPojo} for all work with a
     * given name.
     *
     * @param name The name for the {@link WorkSpec}s
     * @return A {@link LiveData} list of {@link WorkSpec.WorkInfoPojo}
     */
    @Transaction
    @Query("SELECT id, state, output, run_attempt_count FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM workname WHERE name=:name)")
    LiveData<List<WorkSpec.WorkInfoPojo>> getWorkStatusPojoLiveDataForName(String name);

    /**
     * Gets all inputs coming from prerequisites for a particular {@link WorkSpec}.  These are
     * {@link Data} set via {@code Worker#setOutputData()}.
     *
     * @param id The {@link WorkSpec} identifier
     * @return A list of all inputs coming from prerequisites for {@code id}
     */
    @Query("SELECT output FROM workspec WHERE id IN "
            + "(SELECT prerequisite_id FROM dependency WHERE work_spec_id=:id)")
    List<Data> getInputsFromPrerequisites(String id);

    /**
     * Retrieves work ids for unfinished work with a given tag.
     *
     * @param tag The tag used to identify the work
     * @return A list of work ids
     */
    @Query("SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES
            + " AND id IN (SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    List<String> getUnfinishedWorkWithTag(@NonNull String tag);

    /**
     * Retrieves work ids for unfinished work with a given name.
     *
     * @param name THe tag used to identify the work
     * @return A list of work ids
     */
    @Query("SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES
            + " AND id IN (SELECT work_spec_id FROM workname WHERE name=:name)")
    List<String> getUnfinishedWorkWithName(@NonNull String name);

    /**
     * Retrieves work ids for all unfinished work.
     *
     * @return A list of work ids
     */
    @Query("SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES)
    List<String> getAllUnfinishedWork();

    /**
     * @return {@code true} if there is pending work.
     */
    @Query("SELECT COUNT(*) > 0 FROM workspec WHERE state NOT IN " + COMPLETED_STATES + " LIMIT 1")
    boolean hasUnfinishedWork();

    /**
     * Marks a {@link WorkSpec} as scheduled.
     *
     * @param id        The identifier for the {@link WorkSpec}
     * @param startTime The time at which the {@link WorkSpec} was scheduled.
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET schedule_requested_at=:startTime WHERE id=:id")
    int markWorkSpecScheduled(@NonNull String id, long startTime);

    /**
     * @return The time at which the {@link WorkSpec} was scheduled.
     */
    @Query("SELECT schedule_requested_at FROM workspec WHERE id=:id")
    LiveData<Long> getScheduleRequestedAtLiveData(@NonNull String id);

    /**
     * Resets the scheduled state on the {@link WorkSpec}s that are not in a a completed state.
     * @return The number of rows that were updated
     */
    @Query("UPDATE workspec SET schedule_requested_at=" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
            + " WHERE state NOT IN " + COMPLETED_STATES)
    int resetScheduledState();

    /**
     * @return The List of {@link WorkSpec}s that are eligible to be scheduled.
     */
    @Query("SELECT * FROM workspec WHERE "
            + "state=" + WorkTypeConverters.StateIds.ENQUEUED
            // We only want WorkSpecs which have not been previously scheduled.
            + " AND schedule_requested_at=" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
            // Order by period start time so we execute scheduled WorkSpecs in FIFO order
            + " ORDER BY period_start_time"
            + " LIMIT "
                + "(SELECT MAX(:schedulerLimit" + "-COUNT(*), 0) FROM workspec WHERE"
                    + " schedule_requested_at<>" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
                    + " AND state NOT IN " + COMPLETED_STATES
                + ")"
    )
    List<WorkSpec> getEligibleWorkForScheduling(int schedulerLimit);

    /**
     * @return The List of {@link WorkSpec}s that can be scheduled irrespective of scheduling
     * limits.
     */
    @Query("SELECT * FROM workspec WHERE "
            + "state=" + WorkTypeConverters.StateIds.ENQUEUED
            // Order by period start time so we execute scheduled WorkSpecs in FIFO order
            + " ORDER BY period_start_time"
            + " LIMIT :maxLimit"
    )
    List<WorkSpec> getAllEligibleWorkSpecsForScheduling(int maxLimit);

    /**
     * @return The List of {@link WorkSpec}s that are unfinished and scheduled.
     */
    @Query("SELECT * FROM workspec WHERE "
            // Unfinished work
            + "state=" + WorkTypeConverters.StateIds.ENQUEUED
            // We only want WorkSpecs which have been scheduled.
            + " AND schedule_requested_at<>" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
    )
    List<WorkSpec> getScheduledWork();

    /**
     * @return The List of {@link WorkSpec}s that are running.
     */
    @Query("SELECT * FROM workspec WHERE "
            // Unfinished work
            + "state=" + WorkTypeConverters.StateIds.RUNNING
    )
    List<WorkSpec> getRunningWork();

    /**
     * @return The List of {@link WorkSpec} which completed recently.
     */
    @Query("SELECT * FROM workspec WHERE "
            + "period_start_time >= :startingAt"
            + " AND state IN " + COMPLETED_STATES
            + " ORDER BY period_start_time DESC"
    )
    List<WorkSpec> getRecentlyCompletedWork(long startingAt);

    /**
     * Immediately prunes eligible work from the database meeting the following criteria:
     * - Is finished (succeeded, failed, or cancelled)
     * - Has zero unfinished dependents
     */
    @Query("DELETE FROM workspec WHERE "
            + "state IN " + COMPLETED_STATES
            + " AND (SELECT COUNT(*)=0 FROM dependency WHERE "
            + "    prerequisite_id=id AND "
            + "    work_spec_id NOT IN "
            + "        (SELECT id FROM workspec WHERE state IN " + COMPLETED_STATES + "))")
    void pruneFinishedWorkWithZeroDependentsIgnoringKeepForAtLeast();
}
