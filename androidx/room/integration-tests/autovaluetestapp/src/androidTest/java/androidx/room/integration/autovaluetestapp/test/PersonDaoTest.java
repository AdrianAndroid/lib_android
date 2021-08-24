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

package androidx.room.integration.autovaluetestapp.test;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.is;

import androidx.room.integration.autovaluetestapp.vo.Person;
import androidx.room.integration.autovaluetestapp.vo.PersonAndCat;
import androidx.room.integration.autovaluetestapp.vo.PersonWithCats;
import androidx.room.integration.autovaluetestapp.vo.Pet;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PersonDaoTest extends TestDatabaseTest {

    @Test
    public void readWrite() {
        Person entity = Person.create(1, "1stName", "lastName");
        mPersonDao.insert(entity);
        Person loaded = mPersonDao.getPerson(1);
        assertThat(loaded, is(entity));
    }

    @Test
    public void readWrite_listOfEntities() {
        List<Person> entities = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            entities.add(Person.create(i, "name" + i, "lastName"));
        }
        mPersonDao.insertAll(entities);

        List<Person> loaded = mPersonDao.getAllPersons();
        assertThat(entities, is(loaded));
    }

    @Test
    public void readEmbedded() {
        Person person = Person.create(1, "firstName", "lastName");
        Pet.Cat cat = Pet.Cat.create(1, 1, "Tom");
        mPersonDao.insert(person);
        mPetDao.insert(cat);

        PersonAndCat loaded = mPersonDao.getAllPersonAndCat().get(0);
        assertThat(person, is(loaded.getPerson()));
        assertThat(cat, is(loaded.getCat()));
    }

    @Test
    public void readRelation() {
        Person person = Person.create(1, "firstName", "lastName");
        ArrayList<Pet.Cat> cats = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Pet.Cat cat = Pet.Cat.create(i, 1, "Cat " + i);
            cats.add(cat);
        }
        mPersonDao.insert(person);
        mPetDao.insertAll(cats);

        PersonWithCats loaded = mPersonDao.getAllPersonWithCats().get(0);
        assertThat(person, is(loaded.getPerson()));
        assertThat(cats, is(loaded.getCats()));
    }
}
