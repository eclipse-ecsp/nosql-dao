/*
 *
 *
 *   *******************************************************************************
 *
 *     Copyright (c) 2023-24 Harman International
 *
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *
 *     you may not use this file except in compliance with the License.
 *
 *     You may obtain a copy of the License at
 *
 *
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *
 *     Unless required by applicable law or agreed to in writing, software
 *
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *     See the License for the specific language governing permissions and
 *
 *     limitations under the License.
 *
 *
 *
 *     SPDX-License-Identifier: Apache-2.0
 *
 *    *******************************************************************************
 *
 *
 */

package org.eclipse.ecsp.nosqldao.mongodb;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import com.mongodb.ReadPreference;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import org.awaitility.Awaitility;
import org.bson.Document;
import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgniteOrderBy;
import org.eclipse.ecsp.nosqldao.IgnitePagingInfoResponse;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.nosqldao.Updates;
import org.eclipse.ecsp.nosqldao.ecall.ECallEvent;
import org.eclipse.ecsp.nosqldao.ecall.ECallEvent.AuthUsers;
import org.eclipse.ecsp.nosqldao.ecall.EcallDAO;
import org.eclipse.ecsp.nosqldao.ecall.TestEntity;
import org.eclipse.ecsp.nosqldao.ecall.TestEntity2;
import org.eclipse.ecsp.nosqldao.spring.config.IgniteDAOMongoConfigWithProps;
import org.eclipse.ecsp.nosqldao.test.TestDAO;
import org.eclipse.ecsp.nosqldao.test.TestEvent;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test Class for IgniteBaseDAOMongo operations with CosmosDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IgniteDAOMongoConfigWithProps.class })
@TestPropertySource("/ignite-dao-cosmosdb.properties")
public class IgniteBaseDAOCosmosDBIntegrationTest {

    private static final String SOURCEDEVICEID = "sourceDeviceId";

    @Autowired
    private EcallDAO ecallDao;

    @Autowired
    private TestDAO dao;

    @Autowired
    private Datastore datastore;

    @Autowired
    private IgniteDAOMongoConfigWithProps igniteDAOMongoConfigWithProps;

    /**
     * Method to load properties before executing test cases.
     * @throws IOException : exception
     */
    @Before
    public void setupEcallDAO() throws IOException {
        Properties daoProperties = new Properties();
        daoProperties.load(IgniteBaseDAOCosmosDBIntegrationTest.class.getResourceAsStream(
                "/ignite-dao-cosmosdb.properties"));
    }

    @Test
    public void testSave() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall);
        Awaitility.await().atMost(NumericConstants.THREE_THOUSAND, TimeUnit.MILLISECONDS);
        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(ecallGot.getEcallId(), (ecall.getEcallId()));
        assertNotNull(ecall.getLastUpdatedTime());
        Assert.assertTrue(ecall.getLastUpdatedTime().isBefore(LocalDateTime.now()));
    }

    @Test
    public void testSaveAll() {
        ECallEvent ecall1 = new ECallEvent();
        ecall1.setEcallId("ECallIdAll_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ECallEvent ecall2 = new ECallEvent();
        ecall2.setEcallId("ECallIdAll_2");
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_1");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ecallDao.saveAll(ecall1, ecall2);
        Awaitility.await().atMost(NumericConstants.THREE_THOUSAND, TimeUnit.MILLISECONDS);
        List<ECallEvent> ecallEvents = ecallDao.findByIds("ECallIdAll_1", "ECallIdAll_2");
        Assert.assertEquals(NumericConstants.TWO, ecallEvents.size());
        ecallEvents.stream().forEach(ecallEvent -> {
            assertNotNull(ecallEvent.getLastUpdatedTime());
            Assert.assertTrue(ecallEvent.getLastUpdatedTime().isBefore(LocalDateTime.now()));
        });
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void testDistinct() {
        ecallDao.deleteAll();

        for (int i = 1; i <= NumericConstants.FIVE; i++) {
            ECallEvent ecalli = new ECallEvent();
            ecalli.setEcallId("ECallIdAll_" + i);
            ecalli.setSourceDeviceId("Device_" + i);
            ecalli.setEventId("ECall");
            ecalli.setRequestId("Request_" + i);
            ecalli.setTimestamp(NumericConstants.TIMESTAMP);
            ecalli.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
            switch (i) {
                case 1:
                    ecalli.setVehicleId("Vehicle_4");
                    break;
                case 2:
                    ecalli.setVehicleId("Vehicle_1");
                    break;
                case 3:
                    ecalli.setVehicleId("Vehicle_4");
                    break;
                case 4:
                    ecalli.setVehicleId("Vehicle_4");
                    break;
                case 5:
                    ecalli.setVehicleId("Vehicle_7");
                    break;
                default:
            }
            ecallDao.save(ecalli);
        }
        IgniteCriteria criteria = new IgniteCriteria("eventId", Operator.EQ, "ECall");
        IgniteCriteriaGroup criteriaGroup = new IgniteCriteriaGroup(criteria);
        IgniteQuery query = new IgniteQuery(criteriaGroup);

        List list = ecallDao.distinct(query, "vehicleId");

        for (Object element : list) {
            Assert.assertTrue(element.equals("Vehicle_4") || element.equals("Vehicle_1")
                    || element.equals("Vehicle_7"));
        }
        Assert.assertEquals(NumericConstants.THREE, list.size());
    }

    @Test
    public void testFindForNonPaginatedQuery() {

        ecallDao.deleteAll();
        for (int i = 1; i <= NumericConstants.TWENTY_ONE; i++) {
            ECallEvent ecalli = new ECallEvent();
            ecalli.setEcallId("ECallIdAll_" + i);
            ecalli.setSourceDeviceId("Device_" + i);
            ecalli.setEventId("ECall");
            ecalli.setRequestId("Request_" + i);
            ecalli.setTimestamp(NumericConstants.TIMESTAMP);
            ecalli.setVehicleId("Vehicle_" + i);
            ecalli.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
            ecallDao.save(ecalli);
        }

        IgniteCriteria igniteCriteria = new IgniteCriteria("eventId", Operator.EQ, "ECall");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(icg);
        IgniteOrderBy igniteOrderBy = new IgniteOrderBy().byfield("vehicleId");
        igniteQuery.orderBy(igniteOrderBy);
        igniteQuery.setReadPreference(ReadPreference.primaryPreferred());

        List<ECallEvent> list = ecallDao.find(igniteQuery);
        Assert.assertEquals(NumericConstants.TWENTY_ONE, list.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindForPageNumberOnly() {

        IgniteCriteria igniteCriteria = new IgniteCriteria("eventId", Operator.EQ, "ECall");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(icg);
        igniteQuery.setPageNumber(NumericConstants.THREE);

        ecallDao.find(igniteQuery);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindForPageSizeOnly() {

        IgniteCriteria igniteCriteria = new IgniteCriteria("eventId", Operator.EQ, "ECall");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(icg);
        igniteQuery.setPageSize(NumericConstants.THREE);

        ecallDao.find(igniteQuery);
    }

    @Test
    public void testPaginatedQuery() {

        for (int i = 1; i <= NumericConstants.TWENTY_ONE; i++) {
            ECallEvent ecalli = new ECallEvent();
            ecalli.setEcallId("ECallIdAll_" + i);
            ecalli.setSourceDeviceId("Device_" + i);
            ecalli.setEventId("ECall");
            ecalli.setRequestId("Request_" + i);
            ecalli.setTimestamp(NumericConstants.TIMESTAMP);
            ecalli.setVehicleId("Vehicle_" + i);
            ecalli.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
            ecallDao.save(ecalli);
        }
        IgniteCriteria igniteCriteria = new IgniteCriteria("eventId", Operator.EQ, "ECall");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(icg);
        igniteQuery.setPageSize(NumericConstants.THREE);
        igniteQuery.setPageNumber(NumericConstants.THREE);

        List<ECallEvent> list = ecallDao.find(igniteQuery);
        Assert.assertEquals(NumericConstants.THREE, list.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindForNegativePageSize() {
        IgniteCriteria igniteCriteria = new IgniteCriteria("eventId", Operator.EQ, "ECall");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(icg);

        igniteQuery.setPageSize(NumericConstants.MINUS_EIGHT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindForNegativePageNumber() {
        IgniteCriteria igniteCriteria = new IgniteCriteria("eventId", Operator.EQ, "ECall");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(icg);

        igniteQuery.setPageNumber(NumericConstants.MINUS_THREE);
    }

    @Test
    public void testUpsert() {
        ecallDao.deleteAll();
        ECallEvent ecall1 = getCallEvent();

        IgniteCriteria igniteCriteria = new IgniteCriteria("ecallId", Operator.EQ, "ECallIdAll_1");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(icg);

        // 1. Insert if not present
        boolean upsertFlag = false;
        upsertFlag = ecallDao.upsert(igniteQuery, ecall1);
        Awaitility.await().atMost(NumericConstants.THREE_THOUSAND, TimeUnit.MILLISECONDS);
        List<ECallEvent> ecallEventList = ecallDao.findByIds("ECallIdAll_1");
        Assert.assertTrue(upsertFlag && (ecallEventList.size() > 0));

        LocalDateTime lastUpdatedTime = ecallEventList.get(0).getLastUpdatedTime();
        assertNotNull(lastUpdatedTime);
        Assert.assertTrue(lastUpdatedTime.isBefore(LocalDateTime.now()));

        // 2. Update fields existing records.
        ECallEvent ecall2 = geteCallEvent();

        ecall2.setHits(NumericConstants.HITS);
        igniteCriteria = new IgniteCriteria("ecallId", Operator.EQ, "ECallIdAll_1");
        icg = new IgniteCriteriaGroup(igniteCriteria);
        igniteQuery = new IgniteQuery(icg);

        try {
            upsertFlag = ecallDao.upsert(igniteQuery, ecall2);
        } catch (DuplicateKeyException dke) {
            dke.printStackTrace();
        }
        ecallEventList = ecallDao.findByIds("ECallIdAll_1");
        Awaitility.await().atMost(NumericConstants.THREE_THOUSAND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(upsertFlag && ecall2.getHits() == ecallEventList.get(0).getHits());
        assertNotNull(ecallEventList.get(0).getLastUpdatedTime());
        Assert.assertTrue(ecallEventList.get(0).getLastUpdatedTime().isBefore(LocalDateTime.now()));
        Assert.assertTrue(lastUpdatedTime.isBefore(ecallEventList.get(0).getLastUpdatedTime()));

        // 3. Duplicate key exception;
        ECallEvent ecall3 = getEvent();

        igniteCriteria = new IgniteCriteria("ecallId", Operator.EQ, "ECallIdAll_1");
        IgniteCriteria igniteCriteria2 = new IgniteCriteria("eventId",
                Operator.EQ, " NO_ECall");
        icg = new IgniteCriteriaGroup(igniteCriteria).and(igniteCriteria2);
        igniteQuery = new IgniteQuery(icg);
        boolean duplicateKeyException = false;
        try {
            ecallDao.upsert(igniteQuery, ecall3);
        } catch (Exception dke) {
            if (dke instanceof MongoWriteException) {
                duplicateKeyException = true;
            }
        }
        ecallDao.findByIds("ECallIdAll_1");
        Assert.assertTrue(duplicateKeyException);
    }

    @NotNull
    private static ECallEvent getEvent() {
        ECallEvent ecall3 = new ECallEvent();
        ecall3.setEcallId("ECallIdAll_1");
        ecall3.setSourceDeviceId("Device_2");
        ecall3.setEventId("ECall");
        ecall3.setRequestId("Request_2");
        ecall3.setTimestamp(NumericConstants.TIMESTAMP);
        ecall3.setVehicleId("Vehicle_2");
        ecall3.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        return ecall3;
    }

    @NotNull
    private static ECallEvent getCallEvent() {
        ECallEvent ecall1 = new ECallEvent();
        ecall1.setEcallId("ECallIdAll_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        return ecall1;
    }

    @NotNull
    private static ECallEvent geteCallEvent() {
        ECallEvent ecall2 = new ECallEvent();
        ecall2.setEcallId("ECallIdAll_1");
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_2");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        return ecall2;
    }

    @Test
    public void testUpdate() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_Update_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall);

        ECallEvent saveEcallUpdated = ecallDao.findById("ECallId_Update_1");

        LocalDateTime saveLastUpdatedTime = saveEcallUpdated.getLastUpdatedTime();
        assertNotNull(saveLastUpdatedTime);
        Assert.assertTrue(saveLastUpdatedTime.isBefore(LocalDateTime.now()));

        ecall.setSourceDeviceId("Device_1_Updated");
        ecallDao.update(ecall);
        ECallEvent ecallUpdated = ecallDao.findById("ECallId_Update_1");
        Assert.assertEquals("Device_1_Updated", (ecallUpdated.getSourceDeviceId()));

        LocalDateTime updateLastUpdatedTime = ecallUpdated.getLastUpdatedTime();
        assertNotNull(updateLastUpdatedTime);
        Assert.assertTrue(saveLastUpdatedTime.isBefore(updateLastUpdatedTime));
    }

    @Test
    public void testGetAndUpdate() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_Update_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecall.setCounter(NumericConstants.TEN);
        ecall.setTimestamp(NumericConstants.NINE_EIGHT_SEVEN);
        ecall.setDoubleData(NumericConstants.DATA);
        ecall.setShortData((short) NumericConstants.THREE);
        ecall.setFloatData((float) NumericConstants.FLOAT_DATA);
        ecall.setIntData(NumericConstants.TEN);
        //Create customParams(a map of map like DS) and save the entity.
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("data", "123");
        Map<String, Map<String, String>> inventory = new HashMap<>();
        inventory.put("inventory", innerMap);
        ecall.setCustomParams(inventory);
        ecallDao.save(ecall);

        //Assert that the map of map type field got saved successfully
        ECallEvent savedEntity = ecallDao.findById("ECallId_Update_1");
        Assert.assertEquals("123", savedEntity.getCustomParams().get("inventory").get("data"));

        //Create a new entity, with the same @Id value and update the map of map type field.
        ECallEvent ecall2 = new ECallEvent();
        ecall2.setEcallId("ECallId_Update_1");
        Map<String, String> innerMap2 = new HashMap<>();
        //Changed the value against an EXISTING key in the inner map.
        innerMap2.put("data", "345");
        Map<String, Map<String, String>> inventory2 = new HashMap<>();
        inventory2.put("inventory", innerMap2);
        ecall2.setCustomParams(inventory2);
        ecall2.setTimestamp(NumericConstants.NINE_EIGHT_SEVEN);
        ecall2.setDoubleData(NumericConstants.TWO_TWENTY_THREE);
        ecall2.setShortData((short) NumericConstants.THIRTY_TWO);
        ecall2.setIntData(NumericConstants.TEN);

        //Update the entity with @Id = ECallId_Update_1 and assert that ONLY the map of map type field
        //got updated and rest of the other fields remained unchanged.
        ecallDao.getAndUpdate(ecall2);
        ECallEvent updatedEntity = ecallDao.findById("ECallId_Update_1");
        Assert.assertEquals("345", updatedEntity.getCustomParams().get("inventory").get("data"));
        Assert.assertEquals("Device_1", updatedEntity.getSourceDeviceId());
        Assert.assertEquals("ECall", updatedEntity.getEventId());
        Assert.assertEquals("Request_1", updatedEntity.getRequestId());
        Assert.assertEquals(NumericConstants.NINE_EIGHT_SEVEN, updatedEntity.getTimestamp());
        Assert.assertEquals(NumericConstants.TEN, updatedEntity.getCounter());
        Assert.assertEquals(NumericConstants.TWO_TWENTY_THREE, updatedEntity.getDoubleData(),
                NumericConstants.POINT_ZERO_ONE);
        Assert.assertEquals(NumericConstants.THIRTY_TWO, updatedEntity.getShortData());
        Assert.assertEquals(NumericConstants.FLOAT_DATA, updatedEntity.getFloatData(), NumericConstants.POINT_ZERO_ONE);
        Assert.assertEquals(NumericConstants.TEN, updatedEntity.getIntData());
    }

    @Test
    public void testUpdateAll() {
        ECallEvent ecall1 = new ECallEvent();
        ecall1.setEcallId("ECallId_UpdateAll_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ECallEvent ecall2 = new ECallEvent();
        ecall2.setEcallId("ECallId_UpdateAll_2");
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_2");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ecallDao.saveAll(ecall1, ecall2);

        ECallEvent saveEcallUpdated1 = ecallDao.findById("ECallId_UpdateAll_1");
        ECallEvent saveEcallUpdated2 = ecallDao.findById("ECallId_UpdateAll_2");

        LocalDateTime saveLastUpdatedTime1 = saveEcallUpdated1.getLastUpdatedTime();
        assertNotNull(saveLastUpdatedTime1);
        Assert.assertTrue(saveLastUpdatedTime1.isBefore(LocalDateTime.now()));

        LocalDateTime saveLastUpdatedTime2 = saveEcallUpdated2.getLastUpdatedTime();
        assertNotNull(saveLastUpdatedTime2);
        Assert.assertTrue(saveLastUpdatedTime2.isBefore(LocalDateTime.now()));

        ecall1.setSourceDeviceId("Device_1_Updated");
        ecall2.setSourceDeviceId("Device_2_Updated");
        ecallDao.updateAll(ecall1, ecall2);
        ECallEvent ecallUpdated1 = ecallDao.findById("ECallId_UpdateAll_1");
        ECallEvent ecallUpdated2 = ecallDao.findById("ECallId_UpdateAll_2");
        Assert.assertEquals("Device_1_Updated", ecallUpdated1.getSourceDeviceId());
        Assert.assertEquals("Device_2_Updated", ecallUpdated2.getSourceDeviceId());

        LocalDateTime updateLastUpdatedTime1 = ecallUpdated1.getLastUpdatedTime();
        assertNotNull(updateLastUpdatedTime1);
        Assert.assertTrue(saveLastUpdatedTime1.isBefore(updateLastUpdatedTime1));

        LocalDateTime updateLastUpdatedTime2 = ecallUpdated2.getLastUpdatedTime();
        assertNotNull(updateLastUpdatedTime2);
        Assert.assertTrue(saveLastUpdatedTime1.isBefore(updateLastUpdatedTime2));

    }

    @Test
    public void testQuery() {
        ecallDao.deleteAll();
        ECallEvent ecall1 = new ECallEvent();
        ecall1.setEcallId("ECallId_UpdateAll_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ECallEvent ecall2 = new ECallEvent();
        ecall2.setEcallId("ECallId_UpdateAll_2");
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_2");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ecallDao.saveAll(ecall1, ecall2);

        IgniteCriteria c1 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_1");
        IgniteCriteria c2 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_2");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1).or(c2);
        IgniteQuery igQuery = new IgniteQuery(cg1);

        List<ECallEvent> ecallEvents = ecallDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count",
                NumericConstants.TWO, ecallEvents.size());

        c1 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_1");
        c2 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device_1");
        cg1 = new IgniteCriteriaGroup(c1).and(c2);

        IgniteCriteria c3 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_2");
        IgniteCriteria c4 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device_2");
        IgniteCriteriaGroup cg2 = new IgniteCriteriaGroup(c3).and(c4);

        igQuery = new IgniteQuery(cg1).or(cg2);
        ecallEvents = ecallDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count",
                NumericConstants.TWO, ecallEvents.size());
    }

    @Test
    public void testUpdateOperationsIncr() {
        ECallEvent e1 = createUOEntity("ECallId_UO_Inc");
        ecallDao.save(e1);
        Updates u = new Updates();
        u.addIncr("hits");
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_Inc");
        Assert.assertEquals(1, e2.getHits());
    }

    @Test
    public void testUpdateOperationsIncrBy() {
        ECallEvent e1 = createUOEntity("ECallId_UO_Inc");
        ecallDao.save(e1);
        Updates u = new Updates();
        u.addIncr("hits", NumericConstants.TWO);
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_Inc");
        Assert.assertEquals(NumericConstants.TWO, e2.getHits());
    }

    @Test
    public void testUpdateOperationsDec() {
        ECallEvent e1 = createUOEntity("ECallId_UO_Dec");
        ecallDao.save(e1);

        ECallEvent savedE1 = ecallDao.findById("ECallId_UO_Dec");

        LocalDateTime savedLastUpdatedTime = savedE1.getLastUpdatedTime();
        assertNotNull(savedLastUpdatedTime);
        Assert.assertTrue(savedLastUpdatedTime.isBefore(LocalDateTime.now()));

        Updates u = new Updates();
        u.addDecr("dunks");
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_Dec");
        Assert.assertEquals(NumericConstants.MINUS_ONE, e2.getDunks());

        assertNotNull(e2.getLastUpdatedTime());
        Assert.assertTrue(savedLastUpdatedTime.isBefore(e2.getLastUpdatedTime()));

    }

    @Test
    public void testUpdateOperationsDecrBy() {
        ECallEvent e1 = createUOEntity("ECallId_UO_Dec");
        e1.setDunks(NumericConstants.LONG_TEN);
        ecallDao.save(e1);

        Updates u = new Updates();
        u.addDecr("dunks", NumericConstants.TWO);
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_Dec");
        Assert.assertEquals(NumericConstants.EIGHT, e2.getDunks());
    }

    @Test
    public void testUpdateOperationsFieldSet() {
        ECallEvent e1 = createUOEntity("ECallId_UO_FieldSet");
        ecallDao.save(e1);
        Updates u = new Updates();
        u.addFieldSet(SOURCEDEVICEID, "Device_1_updated");
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_FieldSet");
        Assert.assertEquals("Device_1_updated", e2.getSourceDeviceId());
    }

    @Test
    public void testUpdateOperationsFieldUnset() {
        ECallEvent e1 = createUOEntity("ECallId_UO_FieldUnset");
        ecallDao.save(e1);
        Updates u = new Updates();
        u.addFieldUnset(SOURCEDEVICEID);
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_FieldUnset");
        Assert.assertNull(e2.getSourceDeviceId());
    }

    @Test
    public void testUpdateOperationsListMod() {
        ECallEvent e1 = createUOEntity("ECallId_UO_List");
        ecallDao.save(e1);
        Updates u = new Updates();
        u.addListAppend("listAttr1", "Hello");
        // this should take effect, not the one above
        u.addListAppend("listAttr1", "World");
        List<Object> strings = Arrays.asList(new String[] { "Hello", "World" });
        u.addListAppendMulti("listAttr2", strings);
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_List");
        // We can add only one entry to the list if we pass individual objects
        Assert.assertFalse(e2.getListAttr1().contains("Hello"));
        Assert.assertTrue(e2.getListAttr1().contains("World"));
        // But can add multiple passing a list
        Assert.assertTrue(e2.getListAttr2().contains("Hello"));
        Assert.assertTrue(e2.getListAttr2().contains("World"));
    }

    @Test
    public void testUpdateOperationsSetMod() {
        ECallEvent e1 = createUOEntity("ECallId_UO_Set");
        ecallDao.save(e1);
        Updates u = new Updates();
        u.addSetAppend("setAttr1", "12");
        u.addSetAppend("setAttr1", "12");
        u.addSetAppend("setAttr1", "13");
        List<String> stringsSet = new ArrayList<String>();
        stringsSet.add("12");
        stringsSet.add("13");
        u.addSetAppendMulti("setAttr2", stringsSet);
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_Set");
        // 12 will be lost
        Assert.assertFalse(e2.getSetAttr1().contains("12"));
        Assert.assertTrue(e2.getSetAttr1().contains("13"));
        Assert.assertEquals(1, e2.getSetAttr1().size());
        Assert.assertTrue(e2.getSetAttr2().contains("12"));
        Assert.assertTrue(e2.getSetAttr2().contains("13"));
        Assert.assertEquals(NumericConstants.TWO, e2.getSetAttr2().size());
    }

    @Test
    public void testUpdateOperationsCombined() {
        ECallEvent e1 = createUOEntity("ECallId_UO_ALL");
        ecallDao.save(e1);
        Updates u = new Updates();
        u.addFieldSet(SOURCEDEVICEID, "Device_1_updated");
        u.addIncr("hits");
        u.addDecr("dunks");
        u.addListAppend("listAttr1", "Hello");
        u.addListAppend("listAttr1", "World");
        List<String> strings = Arrays.asList(new String[] { "Hello", "World" });
        u.addListAppend("listAttr2", strings);
        u.addSetAppend("setAttr1", "12");
        u.addSetAppend("setAttr1", "12");
        u.addSetAppend("setAttr1", "13");
        List<String> stringsSet = new ArrayList<String>();
        stringsSet.add("12");
        stringsSet.add("13");
        u.addSetAppendMulti("setAttr2", stringsSet);
        ecallDao.update(e1.getEcallId(), u);
        ECallEvent e2 = ecallDao.findById("ECallId_UO_ALL");
        Assert.assertEquals("Device_1_updated", e2.getSourceDeviceId());
        Assert.assertEquals(1, e2.getHits());
        Assert.assertEquals(NumericConstants.MINUS_ONE, e2.getDunks());
        // Hello - we allow adding one element to a list not multiple right now
        Assert.assertFalse(e2.getListAttr1().contains("Hello"));
        Assert.assertTrue(e2.getListAttr1().contains("World"));
        Assert.assertTrue(e2.getListAttr2().contains("Hello"));
        Assert.assertTrue(e2.getListAttr2().contains("World"));
        // 12 will be lost
        Assert.assertFalse(e2.getSetAttr1().contains("12"));
        Assert.assertTrue(e2.getSetAttr1().contains("13"));
        Assert.assertEquals(1, e2.getSetAttr1().size());
        Assert.assertTrue(e2.getSetAttr2().contains("12"));
        Assert.assertTrue(e2.getSetAttr2().contains("13"));
        Assert.assertEquals(NumericConstants.TWO, e2.getSetAttr2().size());
    }

    @Test
    public void testDeleteAllFindAll() {
        ecallDao.deleteAll();
        List<ECallEvent> events = ecallDao.findAll();
        Assert.assertEquals(0, events.size());
        ECallEvent e1 = createUOEntity("ECallId_FIND_ALL_1");
        ecallDao.save(e1);
        events = ecallDao.findAll();
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(e1.getEcallId(), events.get(0).getEcallId());
        ECallEvent e2 = createUOEntity("ECallId_FIND_ALL_2");
        ecallDao.save(e2);
        events = ecallDao.findAll();
        Assert.assertTrue(Arrays.asList(e1.getEcallId(), e2.getEcallId())
                .containsAll(Arrays.asList(events.get(0).getEcallId(), events.get(1).getEcallId())));
    }

    private ECallEvent createUOEntity(String id) {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId(id);
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1_" + id);
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        return ecall;
    }

    @Test
    public void testMorphiaBytesBufferConverter() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_ByteBuffer1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall);
        ECallEvent ecallOut = ecallDao.findById("ECallId_ByteBuffer1");
        assertEquals("Error in converting ByteBuffer", ecall.getBytesBuffer(), ecallOut.getBytesBuffer());
    }

    @Test
    public void testMorphiaTestEntityConverter() {
        TestEntity2 entity2 = new TestEntity2();
        entity2.setId(NumericConstants.THREE);
        entity2.setName("test12");

        TestEntity entity = new TestEntity();
        entity.setId(1);
        entity.setName("test");
        entity.setEntity(entity2);

        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_ByteBuffer1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecall.setEntity(entity);
        ecallDao.save(ecall);
        ECallEvent ecallOut = ecallDao.findById("ECallId_ByteBuffer1");

        TestEntity actualEntity = ecallOut.getEntity();
        TestEntity2 actualEntity2 = actualEntity.getEntity();

        Assert.assertEquals(actualEntity.getId(), entity.getId());
        Assert.assertEquals(actualEntity2.getId(), actualEntity2.getId());
        Assert.assertEquals(actualEntity.getName(), entity.getName());
        Assert.assertEquals(actualEntity2.getName(), entity2.getName());
    }

    /**
     * Testing whether we are able to update an object in the array based on certain condition.
     * In this test case we have single matching
     * array element.
     *
     */
    @Test
    public void testUpdateFieldInArrayList() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ECallEvent.AuthUsers au2 = new ECallEvent.AuthUsers();
        au2.setUserId("user1");
        au2.setRole("VEHICLE_OWNER");
        au2.setStatus("ASSOCIATED");
        au2.setCreatedOn(LocalDateTime.now());
        au2.setUpdatedOn(LocalDateTime.now());
        ECallEvent.AuthUsers au1 = new ECallEvent.AuthUsers();
        au1.setUserId("user2");
        au1.setRole("VEHICLE_OWNER");
        au1.setStatus("ASSOCIATED");
        au1.setCreatedOn(LocalDateTime.now());
        au1.setUpdatedOn(LocalDateTime.now());

        ArrayList<AuthUsers> aus = new ArrayList<AuthUsers>();
        aus.add(au1);
        aus.add(au2);
        ecall.setAuthorizedUsers(aus);
        ecallDao.save(ecall);
        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(ecallGot.getEcallId(), ecall.getEcallId());

        Updates u = new Updates();
        u.addFieldSet("authorizedUsers.$.status", "DISASSOCIATED");
        IgniteCriteria makeCriteria = new IgniteCriteria("authorizedUsers.userId", Operator.EQ, "user1");
        IgniteCriteriaGroup igniteCriteriaGroup = new IgniteCriteriaGroup(makeCriteria);
        IgniteQuery query = new IgniteQuery(igniteCriteriaGroup);
        boolean result = ecallDao.update(query, u);
        Assert.assertTrue(result);
    }

    /**
     * Testcase to delete object in array based on condition.
     */
    @Test
    public void testRemoveObjectInArrayList() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ECallEvent.AuthUsers au1 = new ECallEvent.AuthUsers();
        au1.setUserId("user1");
        au1.setRole("VEHICLE_OWNER");
        au1.setStatus("ASSOCIATED");
        au1.setCreatedOn(LocalDateTime.now());
        au1.setUpdatedOn(LocalDateTime.now());

        ECallEvent.AuthUsers au2 = new ECallEvent.AuthUsers();
        au2.setUserId("user2");
        au2.setRole("VEHICLE_OWNER");
        au2.setStatus("ASSOCIATED");
        au2.setCreatedOn(LocalDateTime.now());
        au2.setUpdatedOn(LocalDateTime.now());

        ArrayList<AuthUsers> aus = new ArrayList<AuthUsers>();
        aus.add(au1);
        aus.add(au2);
        ecall.setAuthorizedUsers(aus);
        ecallDao.save(ecall);
        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(ecallGot.getEcallId(), ecall.getEcallId());
        Assert.assertEquals(NumericConstants.TWO, ecallGot.getAuthorizedUsers().size());

        Updates u = new Updates();
        u.addRemoveOp("authorizedUsers.userId", "user1");
        IgniteCriteria makeCriteria = new IgniteCriteria("authorizedUsers.userId", Operator.EQ, "user1");
        IgniteCriteriaGroup igniteCriteriaGroup = new IgniteCriteriaGroup(makeCriteria);
        IgniteQuery query = new IgniteQuery(igniteCriteriaGroup);
        boolean result = ecallDao.removeAll(query, u);
        Assert.assertTrue(result);
        ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(1, ecallGot.getAuthorizedUsers().size());
    }

    /**
     * Testcase to delete objects in array based on condition.
     */
    @Test
    public void testRemoveObjectsInArrayList() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ECallEvent.AuthUsers au1 = new ECallEvent.AuthUsers();
        au1.setUserId("user1");
        au1.setRole("VEHICLE_OWNER");
        au1.setStatus("ASSOCIATED");
        au1.setCreatedOn(LocalDateTime.now());
        au1.setUpdatedOn(LocalDateTime.now());

        ECallEvent.AuthUsers au2 = new ECallEvent.AuthUsers();
        au2.setUserId("user1");
        au2.setRole("VEHICLE_OWNER");
        au2.setStatus("ASSOCIATED");
        au2.setCreatedOn(LocalDateTime.now());
        au2.setUpdatedOn(LocalDateTime.now());

        ArrayList<AuthUsers> aus = new ArrayList<AuthUsers>();
        aus.add(au1);
        aus.add(au2);
        ecall.setAuthorizedUsers(aus);
        ecallDao.save(ecall);
        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(ecallGot.getEcallId(), ecall.getEcallId());
        Assert.assertEquals(NumericConstants.TWO, ecallGot.getAuthorizedUsers().size());

        Updates u = new Updates();
        u.addRemoveOp("authorizedUsers.userId", "user1");
        IgniteCriteria makeCriteria = new IgniteCriteria("authorizedUsers.userId", Operator.EQ, "user1");
        IgniteCriteriaGroup igniteCriteriaGroup = new IgniteCriteriaGroup(makeCriteria);
        IgniteQuery query = new IgniteQuery(igniteCriteriaGroup);
        boolean result = ecallDao.removeAll(query, u);
        Assert.assertTrue(result);
        ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(Collections.emptyList(), ecallGot.getAuthorizedUsers());
    }

    /**
     * Testcase to delete objects in array based on condition.
     */
    @Test
    public void testRemoveObjectsInArrayListWithDocuments() {
        ECallEvent.AuthUsers au1 = new ECallEvent.AuthUsers();
        au1.setUserId("user1");
        au1.setRole("VEHICLE_OWNER");
        au1.setStatus("ASSOCIATED");
        au1.setCreatedOn(LocalDateTime.now());
        au1.setUpdatedOn(LocalDateTime.now());

        ECallEvent.AuthUsers au2 = new ECallEvent.AuthUsers();
        au2.setUserId("user2");
        au2.setRole("VEHICLE_OWNER");
        au2.setStatus("DISASSOCIATED");
        au2.setCreatedOn(LocalDateTime.now());
        au2.setUpdatedOn(LocalDateTime.now());

        ECallEvent.AuthUsers au3 = new ECallEvent.AuthUsers();
        au3.setUserId("user1");
        au3.setRole("VEHICLE_OWNER");
        au3.setStatus("DISASSOCIATED");
        au3.setCreatedOn(LocalDateTime.now());
        au3.setUpdatedOn(LocalDateTime.now());

        ArrayList<AuthUsers> aus = new ArrayList<AuthUsers>();
        aus.add(au1);
        aus.add(au2);
        ArrayList<AuthUsers> aus1 = new ArrayList<AuthUsers>();
        aus1.add(au3);

        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecall.setAuthorizedUsers(aus);
        ecallDao.save(ecall);

        ECallEvent ecall1 = new ECallEvent();
        ecall1.setEcallId("ECallId_2");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_2");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecall1.setAuthorizedUsers(aus1);
        ecallDao.save(ecall1);

        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(NumericConstants.TWO, ecallGot.getAuthorizedUsers().size());

        ECallEvent ecallGot1 = ecallDao.findById("ECallId_2");
        Assert.assertEquals(1, ecallGot1.getAuthorizedUsers().size());

        Updates u = new Updates();
        u.addRemoveOp("authorizedUsers.status", "DISASSOCIATED");
        IgniteCriteria makeCriteria = new IgniteCriteria("eventId", Operator.EQ, "ECall");
        IgniteCriteriaGroup igniteCriteriaGroup = new IgniteCriteriaGroup(makeCriteria);
        IgniteQuery query = new IgniteQuery(igniteCriteriaGroup);
        boolean result = ecallDao.removeAll(query, u);
        Assert.assertTrue(result);
        ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(1, ecallGot.getAuthorizedUsers().size());
        ecallGot = ecallDao.findById("ECallId_2");
        Assert.assertEquals(Collections.emptyList(), ecallGot.getAuthorizedUsers());
    }

    @Test
    public void testDeleteWithEntity() {

        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall);

        ECallEvent ecall1 = new ECallEvent();
        ecall1.setEcallId("ECallId_2");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_2");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall1);

        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals("ECallId_1", ecallGot.getEcallId());

        ECallEvent ecallGot1 = ecallDao.findById("ECallId_2");
        Assert.assertEquals("ECallId_2", ecallGot1.getEcallId());

        boolean result = ecallDao.delete(ecallGot);
        Assert.assertTrue(result);

        ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(null, ecallGot);

        ecallGot = ecallDao.findById("ECallId_2");
        assertNotNull(ecallGot);
    }

    private void prepareDeleteByQueryData() {
        // ecall1
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP_ONE);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall);

        // ecall2
        ECallEvent ecall2 = new ECallEvent();
        ecall2.setEcallId("ECallId_2");
        ecall2.setSourceDeviceId("Device_1");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP_TWO);
        ecall2.setVehicleId("Vehicle_1");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall2);

        // ecall3
        ECallEvent ecall3 = new ECallEvent();
        ecall3.setEcallId("ECallId_3");
        ecall3.setSourceDeviceId("Device_2");
        ecall3.setEventId("ECall");
        ecall3.setRequestId("Request_3");
        ecall3.setTimestamp(NumericConstants.TIMESTAMP_THREE);
        ecall3.setVehicleId("Vehicle_2");
        ecall3.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall3);
    }

    @Test
    public void testDeleteByQuery() {
        ecallDao.deleteAll();

        prepareDeleteByQueryData();

        // prepared 3 records
        Assert.assertEquals(NumericConstants.THREE, ecallDao.findAll().size());

        IgniteCriteria igniteCriteria1 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_1");
        IgniteCriteriaGroup igniteCriteriaGroup1 = new IgniteCriteriaGroup(igniteCriteria1);

        IgniteQuery igniteQuery1 = new IgniteQuery(igniteCriteriaGroup1);

        // deleted 2
        int count = ecallDao.deleteByQuery(igniteQuery1);
        Assert.assertEquals(NumericConstants.TWO, count);
        Assert.assertEquals(1, ecallDao.findAll().size());

        // 1 left
        ECallEvent ecallEventVehicle = ecallDao.findById("ECallId_3");
        assertNotNull(ecallEventVehicle);
        Assert.assertEquals("Vehicle_2", ecallEventVehicle.getVehicleId());

        // try delete not exists records
        IgniteCriteria igniteCriteria2 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_4");
        IgniteCriteriaGroup igniteCriteriaGroup2 = new IgniteCriteriaGroup(igniteCriteria2);

        IgniteQuery igniteQuery2 = new IgniteQuery(igniteCriteriaGroup2);

        int count2 = ecallDao.deleteByQuery(igniteQuery2);
        Assert.assertEquals(0, count2);
    }

    @Test
    public void testCountByQuery() {
        ecallDao.deleteAll();

        prepareDeleteByQueryData();

        // prepared 3 records
        Assert.assertEquals(NumericConstants.THREE, ecallDao.findAll().size());

        IgniteCriteria igniteCriteria1 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_1");
        IgniteCriteriaGroup igniteCriteriaGroup1 = new IgniteCriteriaGroup(igniteCriteria1);

        IgniteQuery igniteQuery1 = new IgniteQuery(igniteCriteriaGroup1);

        long count = ecallDao.countByQuery(igniteQuery1);
        Assert.assertEquals(NumericConstants.TWO, count);
        Assert.assertEquals(NumericConstants.THREE, ecallDao.findAll().size());

        IgniteCriteria igniteCriteria2 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_4");
        IgniteCriteriaGroup igniteCriteriaGroup2 = new IgniteCriteriaGroup(igniteCriteria2);

        IgniteQuery igniteQuery2 = new IgniteQuery(igniteCriteriaGroup2);

        long count2 = ecallDao.countByQuery(igniteQuery2);
        Assert.assertEquals(0, count2);
        ecallDao.deleteAll();
    }

    @Test
    public void testGetEcallByEqualsIgnoreCase() {
        ecallDao.deleteAll();
        ECallEvent ecall1 = new ECallEvent();
        ecall1.setEcallId("ECallId_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        ecallDao.saveAll(ecall1);

        IgniteCriteria c1 = new IgniteCriteria("vehicleId", Operator.EQI, "veHICle_1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);
        IgniteQuery igQuery = new IgniteQuery(cg1);

        List<ECallEvent> ecallEvents = ecallDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count", 1, ecallEvents.size());

        c1 = new IgniteCriteria("vehicleId", Operator.EQI, "VEHICLE_1");
        cg1 = new IgniteCriteriaGroup(c1);

        igQuery = new IgniteQuery(cg1);
        ecallEvents = ecallDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count", 1, ecallEvents.size());
    }

    /**
     * The following index should get created [ { "v" : 2, "key" : { "_id" : 1 },
     * "name" : "_id_", "ns" : "admin.testEvents" }, { "v" : 2,
     * "unique" : true, "key" : { "manufacturer" : 1, "model" : 1, "year" : 1 },
     * "name" : "manufacturer_1_model_1_year_1", "ns" :
     * "admin.testEvents" } ].
     */
    @Test
    public void testIndexCreation() {
        dao.deleteAll();
        TestEvent event = new TestEvent();
        event.setManufacturer("dummyMfr");
        event.setModel("A3");
        event.setYear("1992");
        event.setPrice(NumericConstants.PRICE);
        dao.save(event);
        MongoCollection collection = datastore.getDatabase().getCollection("testEvents", TestEvent.class);
        ListIndexesIterable<Document> listIndexesIterable = collection.listIndexes();
        List<Document> indexes = listIndexesIterable.into(new ArrayList<>());
        Assert.assertEquals(NumericConstants.TWO, indexes.size());
        Document object = indexes.get(1);
        Document val = (Document) object.get("key");
        assertNotNull(val.get("manufacturer"));
        assertNotNull(val.get("model"));
        assertNotNull(val.get("year"));
        Assert.assertEquals("testEvents", collection.getNamespace().getCollectionName());
    }

    @Test
    public void testLocalDateSaveAndRetrival() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        LocalDate localDate = LocalDate.of(NumericConstants.YEAR, NumericConstants.EIGHT, NumericConstants.NINETEEN);
        ecall.setLocalDate(localDate);
        ecallDao.save(ecall);
        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(localDate, ecallGot.getLocalDate());
    }

    @Test
    public void testLocalTimeSaveAndRetrival() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        LocalTime localTime = LocalTime.of(NumericConstants.TEN, NumericConstants.TEN, NumericConstants.TEN);
        ecall.setLocalTime(localTime);
        ecallDao.save(ecall);
        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(localTime, ecallGot.getLocalTime());
    }

    @Test
    public void testOrderByQuery() throws InterruptedException {
        ecallDao.deleteAll();
        List<ECallEvent> ecalls = new ArrayList<>();
        List<String> devices = new ArrayList<>();
        for (int i = 0; i < NumericConstants.FIVE; i++) {
            ECallEvent ecall = new ECallEvent();
            ecall.setEcallId("ECallId_OrderBy_" + i);
            ecall.setSourceDeviceId(UUID.randomUUID().toString());
            ecall.setEventId("ECall");
            ecall.setRequestId("Request_" + i);
            ecall.setVehicleId("VehicleOrderBy");
            devices.add(ecall.getSourceDeviceId());
            ecalls.add(ecall);
        }
        ecallDao.saveAll(ecalls.toArray(new ECallEvent[NumericConstants.FIVE]));
        IgniteCriteria c1 = new IgniteCriteria("vehicleId", Operator.EQ, "VehicleOrderBy");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);
        IgniteQuery igQuery = new IgniteQuery(cg1);
        igQuery.orderBy(new IgniteOrderBy().byfield("sourceDeviceId").asc());
        Thread.sleep(NumericConstants.THREE_K);
        List<ECallEvent> ecallEvents = ecallDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count",
                NumericConstants.FIVE, ecallEvents.size());
        Collections.sort(devices);
        List<String> devicesOutAsc = new ArrayList<>();
        ecallEvents.forEach(event -> devicesOutAsc.add(event.getSourceDeviceId()));
        assertEquals("Failed in ASC  ordering", devicesOutAsc, devices);
        igQuery.orderBy(new IgniteOrderBy().byfield("sourceDeviceId").desc());
        ecallEvents = ecallDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count",
                NumericConstants.FIVE, ecallEvents.size());
        Collections.reverse(devices);
        List<String> devicesOutDesc = new ArrayList<>();
        ecallEvents.forEach(event -> devicesOutDesc.add(event.getSourceDeviceId()));
        assertEquals("Failed in DESC ordering", devicesOutDesc, devices);
    }

    @Test
    public void testStreamAll() {

        initEcallEventData(NumericConstants.TWELVE);
        Flux<ECallEvent> result = ecallDao.streamFindAll();
        Long count = result.count().block();
        assertNotNull(count);
        assertEquals(NumericConstants.TWELVE, count.longValue());
    }

    @Test
    public void testStreamFind() {

        initEcallEventData(NumericConstants.TWENTY_ONE);

        IgniteCriteria igniteCriteria = new IgniteCriteria("sourceDeviceId", Operator.EQ, "Device_1");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);

        IgniteCriteria igniteCriteria2 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_3");
        IgniteCriteriaGroup icg2 = new IgniteCriteriaGroup(igniteCriteria2);

        Flux<ECallEvent> result = ecallDao.streamFind(new IgniteQuery(icg).or(icg2));
        Long count = result.count().block();
        assertNotNull(count);
        assertEquals(NumericConstants.TWO, count.longValue());
    }

    @Test
    public void testCountAll() {

        initEcallEventData(NumericConstants.THIRTEEN);
        long count = ecallDao.countAll();
        assertEquals(NumericConstants.THIRTEEN, count);
    }

    @Test
    public void testFindWithPagingInfo() {

        initEcallEventData(NumericConstants.THREE);

        IgniteCriteria criteria1 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_1");
        IgniteCriteria criteria2 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_2");
        IgniteCriteria criteria3 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_3");

        IgniteCriteriaGroup criteriaGroup = new IgniteCriteriaGroup(criteria1).or(criteria2).or(criteria3);
        IgniteQuery query = new IgniteQuery(criteriaGroup);
        query.setPageNumber(1);
        query.setPageSize(1);
        IgnitePagingInfoResponse<ECallEvent> actual = ecallDao.findWithPagingInfo(query);
        assertEquals(1, actual.getData().size());
        assertEquals(NumericConstants.LONG_THREE, actual.getTotal());
    }

    private void initEcallEventData(int numOfRecords) {
        ecallDao.deleteAll();
        for (int i = 1; i <= numOfRecords; i++) {
            ECallEvent ecalli = new ECallEvent();
            ecalli.setEcallId("ECallIdAll_" + i);
            ecalli.setSourceDeviceId("Device_" + i);
            ecalli.setEventId("ECall");
            ecalli.setRequestId("Request_" + i);
            ecalli.setTimestamp(NumericConstants.TIMESTAMP);
            ecalli.setVehicleId("Vehicle_" + i);
            ecalli.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
            ecallDao.save(ecalli);
        }
    }

    @Test
    public void testMultipleMorphiaConverters() {
        Assert.assertNotNull(igniteDAOMongoConfigWithProps.getCodecRegistry());
        Assert.assertNotNull(igniteDAOMongoConfigWithProps.getCodecRegistry().get(BytesBuffer.class));
        Assert.assertNotNull(igniteDAOMongoConfigWithProps.getCodecRegistry().get(TestBuffer.class));
    }

    @Test
    public void testSaveForNullableObjects() {
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setTargetDeviceId("device1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall);
        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(ecallGot.getEcallId(), ecall.getEcallId());
        Assert.assertEquals(ecallGot.getTargetDeviceId().get(), ecall.getTargetDeviceId().get());
        assertNotNull(ecall.getLastUpdatedTime());
        Assert.assertTrue(ecall.getLastUpdatedTime().isBefore(LocalDateTime.now()));
    }

}