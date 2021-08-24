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

package androidx.work;

import static android.content.Context.MODE_PRIVATE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL;

import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_3_4;
import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_4_5;
import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_6_7;
import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_7_8;
import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_8_9;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_1;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_10;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_11;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_2;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_3;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_4;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_5;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_6;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_7;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_8;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_9;
import static androidx.work.impl.utils.IdGenerator.NEXT_ALARM_MANAGER_ID_KEY;
import static androidx.work.impl.utils.IdGenerator.NEXT_JOB_SCHEDULER_ID_KEY;
import static androidx.work.impl.utils.IdGenerator.PREFERENCE_FILE_KEY;
import static androidx.work.impl.utils.PreferenceUtils.KEY_LAST_CANCEL_ALL_TIME_MS;
import static androidx.work.impl.utils.PreferenceUtils.KEY_RESCHEDULE_NEEDED;
import static androidx.work.impl.utils.PreferenceUtils.PREFERENCES_FILE_NAME;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkDatabaseMigrations;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkTypeConverters;
import androidx.work.worker.TestWorker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class WorkDatabaseMigrationTest {

    private static final String TEST_DATABASE = "workdatabase-test";
    private static final boolean VALIDATE_DROPPED_TABLES = true;
    private static final String COLUMN_WORKSPEC_ID = "work_spec_id";
    private static final String COLUMN_SYSTEM_ID = "system_id";
    private static final String COLUMN_ALARM_ID = "alarm_id";
    private static final String COLUMN_RUN_IN_FOREGROUND = "run_in_foreground";

    // Queries
    private static final String INSERT_ALARM_INFO = "INSERT INTO alarmInfo VALUES (?, ?)";
    private static final String INSERT_SYSTEM_ID_INFO = "INSERT INTO SystemIdInfo VALUES (?, ?)";
    private static final String CHECK_SYSTEM_ID_INFO = "SELECT * FROM SystemIdInfo";
    private static final String CHECK_ALARM_INFO = "SELECT * FROM alarmInfo";
    private static final String CHECK_TABLE_NAME = "SELECT * FROM %s";
    private static final String CHECK_INDEX = "PRAGMA index_list(%s)";
    private static final String CHECK_TABLE_FIELD = "PRAGMA table_info(%s)";

    private static final String TABLE_ALARM_INFO = "alarmInfo";
    private static final String TABLE_SYSTEM_ID_INFO = "SystemIdInfo";
    private static final String TABLE_WORKSPEC = "WorkSpec";
    private static final String TABLE_WORKTAG = "WorkTag";
    private static final String TABLE_WORKNAME = "WorkName";
    private static final String TABLE_WORKPROGRESS = "WorkProgress";
    private static final String TABLE_PREFERENCE = "Preference";
    private static final String INDEX_PERIOD_START_TIME = "index_WorkSpec_period_start_time";

    private static final String NAME = "name";
    private static final String TRIGGER_CONTENT_UPDATE_DELAY = "trigger_content_update_delay";
    private static final String TRIGGER_MAX_CONTENT_DELAY = "trigger_max_content_delay";

    private Context mContext;
    private File mDatabasePath;

    @Rule
    public MigrationTestHelper mMigrationTestHelper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            WorkDatabase.class.getCanonicalName(),
            new FrameworkSQLiteOpenHelperFactory());

    @Before
    public void setUp() {
        // Delete the database if it exists.
        mContext = ApplicationProvider.getApplicationContext();
        mDatabasePath = ApplicationProvider.getApplicationContext().getDatabasePath(TEST_DATABASE);
        if (mDatabasePath.exists()) {
            mDatabasePath.delete();
        }
    }

    @Test
    @MediumTest
    public void testMigrationVersion1To2() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_1);

        String[] prepopulatedWorkSpecIds = new String[]{
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        };
        for (String workSpecId : prepopulatedWorkSpecIds) {
            ContentValues contentValues = contentValues(workSpecId);
            database.insert("workspec", CONFLICT_FAIL, contentValues);

            if (workSpecId.equals(prepopulatedWorkSpecIds[0])) {
                ContentValues tagValues = new ContentValues();
                tagValues.put("tag", TestWorker.class.getName());
                tagValues.put("work_spec_id", workSpecId);
                database.insert("worktag", CONFLICT_FAIL, tagValues);
            }
        }

        String workSpecId1 = UUID.randomUUID().toString();
        String workSpecId2 = UUID.randomUUID().toString();

        // insert alarmInfos
        database.execSQL(INSERT_ALARM_INFO, new Object[]{workSpecId1, 1});
        database.execSQL(INSERT_ALARM_INFO, new Object[]{workSpecId2, 2});

        database.close();

        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_2,
                VALIDATE_DROPPED_TABLES,
                WorkDatabaseMigrations.MIGRATION_1_2);

        Cursor tagCursor = database.query("SELECT * FROM worktag");
        assertThat(tagCursor.getCount(), is(prepopulatedWorkSpecIds.length));
        boolean[] foundWorkSpecId = new boolean[prepopulatedWorkSpecIds.length];
        for (int i = 0; i < prepopulatedWorkSpecIds.length; ++i) {
            tagCursor.moveToPosition(i);
            assertThat(tagCursor.getString(tagCursor.getColumnIndex("tag")),
                    is(TestWorker.class.getName()));
            String currentId = tagCursor.getString(tagCursor.getColumnIndex("work_spec_id"));
            for (int j = 0; j < prepopulatedWorkSpecIds.length; ++j) {
                if (prepopulatedWorkSpecIds[j].equals(currentId)) {
                    foundWorkSpecId[j] = true;
                    break;
                }
            }
        }
        for (int i = 0; i < prepopulatedWorkSpecIds.length; ++i) {
            assertThat(foundWorkSpecId[i], is(true));
        }
        tagCursor.close();

        Cursor cursor = database.query(CHECK_SYSTEM_ID_INFO);
        assertThat(cursor.getCount(), is(2));
        cursor.moveToFirst();
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_WORKSPEC_ID)), is(workSpecId1));
        assertThat(cursor.getInt(cursor.getColumnIndex(COLUMN_SYSTEM_ID)), is(1));
        cursor.moveToNext();
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_WORKSPEC_ID)), is(workSpecId2));
        assertThat(cursor.getInt(cursor.getColumnIndex(COLUMN_SYSTEM_ID)), is(2));
        cursor.close();

        assertThat(checkExists(database, TABLE_ALARM_INFO), is(false));
        assertThat(checkExists(database, TABLE_WORKSPEC), is(true));
        assertThat(checkExists(database, TABLE_WORKTAG), is(true));
        assertThat(checkExists(database, TABLE_WORKNAME), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion2To3() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_2);
        WorkDatabaseMigrations.RescheduleMigration migration2To3 =
                new WorkDatabaseMigrations.RescheduleMigration(mContext, VERSION_2, VERSION_3);

        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_3,
                VALIDATE_DROPPED_TABLES,
                migration2To3);

        SharedPreferences sharedPreferences =
                mContext.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
        assertThat(sharedPreferences.getBoolean(KEY_RESCHEDULE_NEEDED, false), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion3To4() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_3);

        String oneTimeWorkSpecId = UUID.randomUUID().toString();
        long scheduleRequestedAt = System.currentTimeMillis();
        ContentValues oneTimeWorkSpecContentValues = contentValues(oneTimeWorkSpecId);
        oneTimeWorkSpecContentValues.put("schedule_requested_at", scheduleRequestedAt);

        String periodicWorkSpecId = UUID.randomUUID().toString();
        ContentValues periodicWorkSpecContentValues = contentValues(periodicWorkSpecId);
        periodicWorkSpecContentValues.put("interval_duration", 15 * 60 * 1000L);

        database.insert("workspec", CONFLICT_FAIL, oneTimeWorkSpecContentValues);
        database.insert("workspec", CONFLICT_FAIL, periodicWorkSpecContentValues);

        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_4,
                VALIDATE_DROPPED_TABLES,
                MIGRATION_3_4);

        Cursor cursor = database.query("SELECT * from workspec");
        assertThat(cursor.getCount(), is(2));
        cursor.moveToFirst();
        assertThat(cursor.getString(cursor.getColumnIndex("id")),
                is(oneTimeWorkSpecId));
        assertThat(cursor.getLong(cursor.getColumnIndex("schedule_requested_at")),
                is(scheduleRequestedAt));
        cursor.moveToNext();
        assertThat(cursor.getString(cursor.getColumnIndex("id")),
                is(periodicWorkSpecId));
        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            assertThat(cursor.getLong(cursor.getColumnIndex("schedule_requested_at")),
                    is(0L));
        } else {
            assertThat(cursor.getLong(cursor.getColumnIndex("schedule_requested_at")),
                    is(WorkSpec.SCHEDULE_NOT_REQUESTED_YET));
        }
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion4To5() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_4);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_5,
                VALIDATE_DROPPED_TABLES,
                MIGRATION_4_5);
        assertThat(checkExists(database, TABLE_WORKSPEC), is(true));
        assertThat(
                checkColumnExists(database, TABLE_WORKSPEC, TRIGGER_CONTENT_UPDATE_DELAY),
                is(true));
        assertThat(
                checkColumnExists(database, TABLE_WORKSPEC, TRIGGER_MAX_CONTENT_DELAY),
                is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion5To6() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_5);
        WorkDatabaseMigrations.RescheduleMigration migration5To6 =
                new WorkDatabaseMigrations.RescheduleMigration(mContext, VERSION_5, VERSION_6);

        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_6,
                VALIDATE_DROPPED_TABLES,
                migration5To6);

        SharedPreferences sharedPreferences =
                mContext.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
        assertThat(sharedPreferences.getBoolean(KEY_RESCHEDULE_NEEDED, false), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion6To7() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_6);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_7,
                VALIDATE_DROPPED_TABLES,
                MIGRATION_6_7);
        assertThat(checkExists(database, TABLE_WORKPROGRESS), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion7To8() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_7);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_8,
                VALIDATE_DROPPED_TABLES,
                MIGRATION_7_8);

        assertThat(checkIndexExists(database, INDEX_PERIOD_START_TIME, TABLE_WORKSPEC), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion8To9() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_8);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_9,
                VALIDATE_DROPPED_TABLES,
                MIGRATION_8_9);

        assertThat(checkColumnExists(database, TABLE_WORKSPEC, COLUMN_RUN_IN_FOREGROUND),
                is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion9To10() throws IOException {
        long lastCancelTimeMillis = 1L;
        int nextJobSchedulerId = 10;
        int nextAlarmId = 20;
        // Setup
        mContext.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_CANCEL_ALL_TIME_MS, lastCancelTimeMillis)
                .putBoolean(KEY_RESCHEDULE_NEEDED, true)
                .apply();

        mContext.getSharedPreferences(PREFERENCE_FILE_KEY, MODE_PRIVATE)
                .edit()
                .putInt(NEXT_JOB_SCHEDULER_ID_KEY, nextJobSchedulerId)
                .putInt(NEXT_ALARM_MANAGER_ID_KEY, nextAlarmId)
                .apply();

        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_9);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_10,
                VALIDATE_DROPPED_TABLES,
                new WorkDatabaseMigrations.WorkMigration9To10(mContext));

        assertThat(checkExists(database, TABLE_PREFERENCE), is(true));
        String query = "SELECT * FROM `Preference` where `key`=@key";
        String[] keys = new String[]{
                KEY_RESCHEDULE_NEEDED,
                KEY_LAST_CANCEL_ALL_TIME_MS,
                NEXT_JOB_SCHEDULER_ID_KEY,
                NEXT_ALARM_MANAGER_ID_KEY
        };
        long[] expectedValues = new long[]{
                1L,
                lastCancelTimeMillis,
                nextJobSchedulerId,
                nextAlarmId
        };
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            long expected = expectedValues[i];
            Cursor cursor = database.query(query, new Object[]{key});
            assertThat(cursor.getCount(), is(1));
            cursor.moveToFirst();
            assertThat(cursor.getLong(cursor.getColumnIndex("long_value")), is(expected));
            cursor.close();
        }
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion10To11() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_10);
        WorkDatabaseMigrations.RescheduleMigration migration10To11 =
                new WorkDatabaseMigrations.RescheduleMigration(mContext, VERSION_10, VERSION_11);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_11,
                VALIDATE_DROPPED_TABLES,
                migration10To11);

        String[] keys = new String[]{
                KEY_RESCHEDULE_NEEDED,
        };
        long[] expectedValues = new long[]{
                1L,
        };

        String query = "SELECT * FROM `Preference` where `key`=@key";
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            long expected = expectedValues[i];
            Cursor cursor = database.query(query, new Object[]{key});
            assertThat(cursor.getCount(), is(1));
            cursor.moveToFirst();
            assertThat(cursor.getLong(cursor.getColumnIndex("long_value")), is(expected));
            cursor.close();
        }
        database.close();
    }

    @NonNull
    private ContentValues contentValues(String workSpecId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("id", workSpecId);
        contentValues.put("state", WorkTypeConverters.StateIds.ENQUEUED);
        contentValues.put("worker_class_name", TestWorker.class.getName());
        contentValues.put("input_merger_class_name", OverwritingInputMerger.class.getName());
        contentValues.put("input", Data.EMPTY.toByteArray());
        contentValues.put("output", Data.EMPTY.toByteArray());
        contentValues.put("initial_delay", 0L);
        contentValues.put("interval_duration", 0L);
        contentValues.put("flex_duration", 0L);
        contentValues.put("required_network_type", false);
        contentValues.put("requires_charging", false);
        contentValues.put("requires_device_idle", false);
        contentValues.put("requires_battery_not_low", false);
        contentValues.put("requires_storage_not_low", false);
        contentValues.put("content_uri_triggers",
                WorkTypeConverters.contentUriTriggersToByteArray(new ContentUriTriggers()));
        contentValues.put("run_attempt_count", 0);
        contentValues.put("backoff_policy",
                WorkTypeConverters.backoffPolicyToInt(BackoffPolicy.EXPONENTIAL));
        contentValues.put("backoff_delay_duration", WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS);
        contentValues.put("period_start_time", 0L);
        contentValues.put("minimum_retention_duration", 0L);
        contentValues.put("schedule_requested_at", WorkSpec.SCHEDULE_NOT_REQUESTED_YET);
        return contentValues;
    }

    private boolean checkExists(SupportSQLiteDatabase database, String tableName) {
        Cursor cursor = null;
        try {
            cursor = database.query(String.format(CHECK_TABLE_NAME, tableName));
            return true;
        } catch (SQLiteException ignored) {
            // Should fail with a SQLiteException (no such table: tableName)
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean checkIndexExists(
            @NonNull SupportSQLiteDatabase database,
            @NonNull String indexName,
            @NonNull String tableName) {

        Cursor cursor = null;
        try {
            cursor = database.query(String.format(CHECK_INDEX, tableName));
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                // https://www.sqlite.org/pragma.html#pragma_table_info
                // Columns are: (seq, name, uniq)
                String name = cursor.getString(cursor.getColumnIndex(NAME));
                if (indexName.equals(name)) {
                    return true;
                }
                cursor.moveToNext();
            }
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean checkColumnExists(
            SupportSQLiteDatabase database,
            String tableName,
            String columnName) {

        Cursor cursor = null;
        try {
            cursor = database.query(String.format(CHECK_TABLE_FIELD, tableName));
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                // https://www.sqlite.org/pragma.html#pragma_table_info
                // Columns are: (cid, name, type, notnull, dfit_value, pk)
                String name = cursor.getString(cursor.getColumnIndex(NAME));
                if (columnName.equals(name)) {
                    return true;
                }
                cursor.moveToNext();
            }
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
