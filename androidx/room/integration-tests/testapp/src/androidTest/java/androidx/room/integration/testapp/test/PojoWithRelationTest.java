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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.room.integration.testapp.vo.Cluster;
import androidx.room.integration.testapp.vo.EmbeddedUserAndAllPets;
import androidx.room.integration.testapp.vo.Hivemind;
import androidx.room.integration.testapp.vo.House;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetAndOwner;
import androidx.room.integration.testapp.vo.PetWithToyIds;
import androidx.room.integration.testapp.vo.Robot;
import androidx.room.integration.testapp.vo.RobotAndHivemind;
import androidx.room.integration.testapp.vo.Toy;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndAllPets;
import androidx.room.integration.testapp.vo.UserAndAllPetsViaJunction;
import androidx.room.integration.testapp.vo.UserAndPetAdoptionDates;
import androidx.room.integration.testapp.vo.UserAndPetsAndHouses;
import androidx.room.integration.testapp.vo.UserIdAndPetNames;
import androidx.room.integration.testapp.vo.UserWithPetsAndToys;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PojoWithRelationTest extends TestDatabaseTest {
    @Test
    public void fetchAll() {
        User[] users = TestUtil.createUsersArray(1, 2, 3);
        Pet[][] userPets = new Pet[3][];
        mUserDao.insertAll(users);
        for (User user : users) {
            Pet[] pets = TestUtil.createPetsForUser(user.getId(), user.getId() * 10,
                    user.getId() - 1);
            mPetDao.insertAll(pets);
            userPets[user.getId() - 1] = pets;
        }
        List<UserAndAllPets> usersAndPets = mUserPetDao.loadAllUsersWithTheirPets();
        assertThat(usersAndPets.size(), is(3));
        assertThat(usersAndPets.get(0).user, is(users[0]));
        assertThat(usersAndPets.get(0).pets, is(Collections.<Pet>emptyList()));

        assertThat(usersAndPets.get(1).user, is(users[1]));
        assertThat(usersAndPets.get(1).pets, is(Arrays.asList(userPets[1])));

        assertThat(usersAndPets.get(2).user, is(users[2]));
        assertThat(usersAndPets.get(2).pets, is(Arrays.asList(userPets[2])));
    }

    private void createData() {
        User[] users = TestUtil.createUsersArray(1, 2);
        mUserDao.insertAll(users);
        Pet user1_pet1 = TestUtil.createPet(1);
        user1_pet1.setUserId(1);
        user1_pet1.setName("pet1");
        mPetDao.insertOrReplace(user1_pet1);

        Pet user1_pet2 = TestUtil.createPet(2);
        user1_pet2.setUserId(1);
        user1_pet2.setName("pet2");
        mPetDao.insertOrReplace(user1_pet2);

        Pet user2_pet1 = TestUtil.createPet(3);
        user2_pet1.setUserId(2);
        user2_pet1.setName("pet3");
        mPetDao.insertOrReplace(user2_pet1);
    }

    @Test
    public void fetchWithNames() {
        createData();

        List<UserIdAndPetNames> usersAndPets = mUserPetDao.loadUserAndPetNames();
        assertThat(usersAndPets.size(), is(2));
        assertThat(usersAndPets.get(0).userId, is(1));
        assertThat(usersAndPets.get(1).userId, is(2));
        assertThat(usersAndPets.get(0).names, is(Arrays.asList("pet1", "pet2")));
        assertThat(usersAndPets.get(1).names, is(Collections.singletonList("pet3")));
    }

    @Test
    public void nested() {
        createData();
        Toy pet1_toy1 = new Toy();
        pet1_toy1.setName("toy1");
        pet1_toy1.setPetId(1);
        Toy pet1_toy2 = new Toy();
        pet1_toy2.setName("toy2");
        pet1_toy2.setPetId(1);
        mToyDao.insert(pet1_toy1, pet1_toy2);
        List<UserWithPetsAndToys> userWithPetsAndToys = mUserPetDao.loadUserWithPetsAndToys();
        assertThat(userWithPetsAndToys.size(), is(2));
        UserWithPetsAndToys first = userWithPetsAndToys.get(0);
        List<Toy> toys = first.pets.get(0).toys;
        assertThat(toys.get(0).getName(), is("toy1"));
        assertThat(toys.get(1).getName(), is("toy2"));
        assertThat(userWithPetsAndToys.get(1).pets.get(0).toys, is(Collections.<Toy>emptyList()));
    }

    @Test
    public void duplicateParentField() {
        User[] users = TestUtil.createUsersArray(1, 2);
        Pet[] pets_1 = TestUtil.createPetsForUser(1, 1, 2);
        Pet[] pets_2 = TestUtil.createPetsForUser(2, 10, 1);
        mUserDao.insertAll(users);
        mPetDao.insertAll(pets_1);
        mPetDao.insertAll(pets_2);
        List<UserAndAllPets> userAndAllPets = mUserPetDao.unionByItself();
        assertThat(userAndAllPets.size(), is(4));
        for (int i = 0; i < 4; i++) {
            assertThat("user at " + i, userAndAllPets.get(i).user, is(users[i % 2]));
        }
        assertThat(userAndAllPets.get(0).pets, is(Arrays.asList(pets_1)));
        assertThat(userAndAllPets.get(2).pets, is(Arrays.asList(pets_1)));

        assertThat(userAndAllPets.get(1).pets, is(Arrays.asList(pets_2)));
        assertThat(userAndAllPets.get(3).pets, is(Arrays.asList(pets_2)));
    }

    @Test
    public void embeddedRelation() {
        createData();
        EmbeddedUserAndAllPets relationContainer = mUserPetDao.loadUserAndPetsAsEmbedded(1);
        assertThat(relationContainer.getUserAndAllPets(), notNullValue());
        assertThat(relationContainer.getUserAndAllPets().user.getId(), is(1));
        assertThat(relationContainer.getUserAndAllPets().pets.size(), is(2));
    }

    @Test
    public void boxedPrimitiveList() {
        Pet pet1 = TestUtil.createPet(3);
        Pet pet2 = TestUtil.createPet(5);

        Toy pet1_toy1 = TestUtil.createToyForPet(pet1, 10);
        Toy pet1_toy2 = TestUtil.createToyForPet(pet1, 20);
        Toy pet2_toy1 = TestUtil.createToyForPet(pet2, 30);

        mPetDao.insertOrReplace(pet1, pet2);
        mToyDao.insert(pet1_toy1, pet1_toy2, pet2_toy1);

        List<PetWithToyIds> petWithToyIds = mPetDao.allPetsWithToyIds();
        //noinspection ArraysAsListWithZeroOrOneArgument
        assertThat(petWithToyIds, is(
                Arrays.asList(
                        new PetWithToyIds(pet1, Arrays.asList(10, 20)),
                        new PetWithToyIds(pet2, Arrays.asList(30)))
        ));
    }

    @Test
    public void viaTypeConverter() {
        User user = TestUtil.createUser(3);
        Pet pet1 = TestUtil.createPet(3);
        Date date1 = new Date(300);
        pet1.setAdoptionDate(date1);
        Pet pet2 = TestUtil.createPet(5);
        Date date2 = new Date(700);
        pet2.setAdoptionDate(date2);

        pet1.setUserId(3);
        pet2.setUserId(3);
        mUserDao.insert(user);
        mPetDao.insertOrReplace(pet1, pet2);

        List<UserAndPetAdoptionDates> adoptions =
                mUserPetDao.loadUserWithPetAdoptionDates();

        assertThat(adoptions, is(Arrays.asList(
                new UserAndPetAdoptionDates(user, Arrays.asList(new Date(300), new Date(700)))
        )));
    }

    @Test
    public void largeRelation_child() {
        User user = TestUtil.createUser(3);
        List<Pet> pets = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            Pet pet = TestUtil.createPet(i + 1);
            pet.setUserId(3);
        }
        mUserDao.insert(user);
        mPetDao.insertAll(pets.toArray(new Pet[pets.size()]));
        List<UserAndAllPets> result = mUserPetDao.loadAllUsersWithTheirPets();
        assertThat(result.size(), is(1));
        assertThat(result.get(0).user, is(user));
        assertThat(result.get(0).pets, is(pets));
    }

    @Test
    public void largeRelation_parent() {
        final List<User> users = new ArrayList<>();
        final List<Pet> pets = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            User user = TestUtil.createUser(i + 1);
            users.add(user);
            Pet pet = TestUtil.createPet(i + 1);
            pet.setUserId(user.getId());
            pets.add(pet);
        }
        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mUserDao.insertAll(users.toArray(new User[users.size()]));
                mPetDao.insertAll(pets.toArray(new Pet[pets.size()]));
            }
        });
        List<UserAndAllPets> result = mUserPetDao.loadAllUsersWithTheirPets();
        assertThat(result.size(), is(2000));
        for (int i = 0; i < 2000; i++) {
            assertThat(result.get(i).user, is(users.get(i)));
            assertThat(result.get(i).pets, is(Collections.singletonList(pets.get(i))));
        }
    }

    @Test
    public void multipleRelations() {
        final List<User> users = new ArrayList<>();
        final List<List<Pet>> pets = new ArrayList<>();
        final List<List<House>> houses = new ArrayList<>();
        users.add(TestUtil.createUser(1));
        users.add(TestUtil.createUser(2));
        pets.add(Arrays.asList(TestUtil.createPetsForUser(1, 1, 2)));
        pets.add(Arrays.asList(TestUtil.createPetsForUser(2, 10, 1)));
        houses.add(Arrays.asList(TestUtil.createHousesForUser(1, 1, 1)));
        houses.add(Arrays.asList(TestUtil.createHousesForUser(2, 10, 5)));

        mUserDao.insertAll(users.toArray(new User[users.size()]));
        for (List<Pet> petList : pets) {
            mPetDao.insertAll(petList.toArray(new Pet[petList.size()]));
        }
        for (List<House> houseList : houses) {
            mUserHouseDao.insertAll(houseList.toArray(new House[houseList.size()]));
        }

        List<UserAndPetsAndHouses> result = mUserHouseDao.getUsersAndTheirPetsAndHouses();
        for (int i = 0; i < users.size(); i++) {
            assertThat(result.get(i).getUser(), is(users.get(i)));
            assertThat(result.get(i).getPets(), is(pets.get(i)));
            assertThat(result.get(i).getHouses(), is(houses.get(i)));
        }
    }

    @Test
    public void viaJunction() {
        User[] users = TestUtil.createUsersArray(1, 2, 3);
        Pet[][] userPets = new Pet[3][];
        mUserDao.insertAll(users);
        for (User user : users) {
            Pet[] pets = TestUtil.createPetsForUser(user.getId(), user.getId() * 10,
                    user.getId() - 1);
            mPetDao.insertAll(pets);
            userPets[user.getId() - 1] = pets;
        }
        List<UserAndAllPets> usersAndPets = mUserPetDao.loadAllUsersWithTheirPets();
        List<UserAndAllPetsViaJunction> userAndPetsViaJunctions =
                mUserPetDao.loadAllUsersWithTheirPetsViaJunction();
        assertThat(usersAndPets.size(), is(userAndPetsViaJunctions.size()));
        for (int i = 0; i < usersAndPets.size(); i++) {
            assertThat(usersAndPets.get(i).user, is(userAndPetsViaJunctions.get(i).user));
            assertThat(usersAndPets.get(i).pets, is(userAndPetsViaJunctions.get(i).pets));
        }
    }

    @Test
    public void nonCollectionRelation() {
        createData();

        List<PetAndOwner> petAndOwners = mPetDao.allPetsWithOwners();
        assertThat(petAndOwners.size(), is(3));
        assertThat(petAndOwners.get(0).getUser().getId(), is(1));
        assertThat(petAndOwners.get(1).getUser().getId(), is(1));
        assertThat(petAndOwners.get(2).getUser().getId(), is(2));
    }

    @Test
    public void large_nonCollectionRelation() {
        int count = 2000;
        mDatabase.runInTransaction(() -> {
            for (int i = 1; i <= count; i++) {
                mUserDao.insert(TestUtil.createUser(i));
                Pet pet = TestUtil.createPet(i);
                pet.setUserId(i);
                mPetDao.insertOrReplace(pet);
            }
        });

        List<PetAndOwner> petAndOwners = mPetDao.allPetsWithOwners();
        assertThat(petAndOwners.size(), is(count));
        for (int i = 0; i < count; i++) {
            assertThat(petAndOwners.get(i).getUser().getId(), is(i + 1));
        }
    }

    @Test
    public void large_nonCollectionRelation_withComplexKey() {
        int count = 2000;
        mDatabase.runInTransaction(() -> {
            for (int i = 1; i <= count; i++) {
                Hivemind hivemind = new Hivemind(UUID.randomUUID());
                mRobotsDao.putHivemind(hivemind);
                Robot robot = new Robot(UUID.randomUUID(), hivemind.mId);
                mRobotsDao.putRobot(robot);
            }
        });

        List<RobotAndHivemind> robotsWithHivemind = mRobotsDao.getRobotsWithHivemind();
        assertThat(robotsWithHivemind.size(), is(count));
        for (int i = 0; i < count; i++) {
            assertThat(robotsWithHivemind.get(i).getHivemind(), is(notNullValue()));
        }
    }

    @Test
    public void relationWithBlobKey() {
        UUID hiveId1 = UUID.randomUUID();
        UUID hiveId2 = UUID.randomUUID();
        UUID robotId1 = UUID.randomUUID();
        UUID robotId2 = UUID.randomUUID();
        UUID robotId3 = UUID.randomUUID();

        mRobotsDao.putHivemind(new Hivemind(hiveId1));
        mRobotsDao.putHivemind(new Hivemind(hiveId2));
        mRobotsDao.putRobot(new Robot(robotId1, hiveId1));
        mRobotsDao.putRobot(new Robot(robotId2, hiveId1));
        mRobotsDao.putRobot(new Robot(robotId3, hiveId2));

        List<Robot> firstHiveRobots = mRobotsDao.getHiveRobots(hiveId1);
        assertThat(firstHiveRobots.size(), is(2));
        assertThat(firstHiveRobots.get(0).mId, is(robotId1));
        assertThat(firstHiveRobots.get(1).mId, is(robotId2));

        List<Robot> secondHiveRobots = mRobotsDao.getHiveRobots(hiveId2);
        assertThat(secondHiveRobots.size(), is(1));
        assertThat(secondHiveRobots.get(0).mId, is(robotId3));

        List<Cluster> clusterResult = mRobotsDao.getCluster();
        assertThat(clusterResult.size(), is(2));
        assertThat(clusterResult.get(0).mRobotList.size(), is(2));
        assertThat(clusterResult.get(0).mRobotList.get(0).mId, is(robotId1));
        assertThat(clusterResult.get(0).mRobotList.get(1).mId, is(robotId2));
        assertThat(clusterResult.get(1).mRobotList.size(), is(1));
        assertThat(clusterResult.get(1).mRobotList.get(0).mId, is(robotId3));
    }
}
