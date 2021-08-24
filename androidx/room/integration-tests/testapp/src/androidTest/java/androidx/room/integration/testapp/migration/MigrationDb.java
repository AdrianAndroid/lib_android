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

package androidx.room.integration.testapp.migration;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.DatabaseView;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.RoomWarnings;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

@SuppressWarnings("WeakerAccess")
@Database(version = MigrationDb.LATEST_VERSION,
        entities = {MigrationDb.Entity1.class, MigrationDb.Entity2.class,
                MigrationDb.Entity4.class},
        views = {MigrationDb.View1.class})
public abstract class MigrationDb extends RoomDatabase {
    static final int LATEST_VERSION = 13;
    static final int MAX_VERSION = 1000;
    abstract MigrationDao dao();
    @Entity(indices = {
            @Index(value = "name", unique = true),
            @Index(value = "addedInV10", unique = false)})
    static class Entity1 {
        public static final String TABLE_NAME = "Entity1";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "0")
        public int addedInV10;
        @ColumnInfo(defaultValue = "(0)")
        public int added1InV13;
    }

    @Entity
    static class Entity2 {
        public static final String TABLE_NAME = "Entity2";
        @PrimaryKey(autoGenerate = true)
        public int id;
        public String addedInV3;
        @ColumnInfo(defaultValue = "Unknown") // Added in version 11
        public String name;
        public String addedInV9; // added via alter table with default value pre Room 2.2.0
    }

    @Entity
    static class Entity3 { // added in version 4, removed at 6
        public static final String TABLE_NAME = "Entity3";
        @PrimaryKey
        public int id;
        @Ignore //removed at 5
        public String removedInV5;
        public String name;
    }

    @SuppressWarnings(RoomWarnings.MISSING_INDEX_ON_FOREIGN_KEY_CHILD)
    @Entity(foreignKeys = {
            @ForeignKey(entity = Entity1.class,
            parentColumns = "name",
            childColumns = "name",
            deferred = true)})
    static class Entity4 {
        public static final String TABLE_NAME = "Entity4";
        @PrimaryKey
        public int id;
        @ColumnInfo(collate = ColumnInfo.NOCASE)
        public String name;
    }

    @DatabaseView("SELECT Entity4.id, Entity4.name, Entity1.id AS entity1Id "
            + "FROM Entity4 INNER JOIN Entity1 ON Entity4.name = Entity1.name")
    static class View1 {
        public static final String VIEW_NAME = "View1";
        public int id;
        public String name;
        public int entity1Id;
    }

    @Dao
    interface MigrationDao {
        @Query("SELECT * from Entity1 ORDER BY id ASC")
        List<Entity1> loadAllEntity1s();
        @Query("SELECT * from Entity2 ORDER BY id ASC")
        List<Entity2> loadAllEntity2s();
        @Query("SELECT * from Entity2 ORDER BY id ASC")
        List<Entity2Pojo> loadAllEntity2sAsPojo();
        @Insert
        void insert(Entity2... entity2);
    }

    static class Entity2Pojo extends Entity2 {
    }

    /**
     * not a real dao because database will change.
     */
    static class Dao_V1 {
        final SupportSQLiteDatabase mDb;

        Dao_V1(SupportSQLiteDatabase db) {
            mDb = db;
        }

        public void insertIntoEntity1(int id, String name) {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("name", name);
            long insertionId = mDb.insert(Entity1.TABLE_NAME,
                    SQLiteDatabase.CONFLICT_REPLACE, values);
            if (insertionId == -1) {
                throw new RuntimeException("test failure");
            }
        }
    }

    /**
     * not a real dao because database will change.
     */
    static class Dao_V2 {
        final SupportSQLiteDatabase mDb;

        Dao_V2(SupportSQLiteDatabase db) {
            mDb = db;
        }

        public void insertIntoEntity2(int id, String name) {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("name", name);
            long insertionId = mDb.insert(Entity2.TABLE_NAME,
                    SQLiteDatabase.CONFLICT_REPLACE, values);
            if (insertionId == -1) {
                throw new RuntimeException("test failure");
            }
        }
    }
}
