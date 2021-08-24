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

package androidx.room.integration.testapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.dao.MailDao;
import androidx.room.integration.testapp.dao.SongDao;
import androidx.room.integration.testapp.vo.Mail;
import androidx.room.integration.testapp.vo.Song;
import androidx.room.integration.testapp.vo.SongDescription;

@Database(entities = {Mail.class, SongDescription.class, Song.class},
        version = 1, exportSchema = false)
public abstract class FtsTestDatabase extends RoomDatabase  {
    public abstract MailDao getMailDao();
    public abstract SongDao getSongDao();
}
