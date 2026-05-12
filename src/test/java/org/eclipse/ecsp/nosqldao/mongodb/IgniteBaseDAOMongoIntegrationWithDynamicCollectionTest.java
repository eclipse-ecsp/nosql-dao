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

import com.mongodb.MongoWriteException;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.query.filters.Filters;
import org.bson.Document;
import org.eclipse.ecsp.nosqldao.Coordinate;
import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgniteOrderBy;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.nosqldao.Updates;
import org.eclipse.ecsp.nosqldao.ecall.GeoSpatialMockDAOImpl;
import org.eclipse.ecsp.nosqldao.ecall.GeoSpatialMockEvent;
import org.eclipse.ecsp.nosqldao.ecall.Location;
import org.eclipse.ecsp.nosqldao.ecall.MockTestDAOMongoImpl;
import org.eclipse.ecsp.nosqldao.ecall.MockTestEvent;
import org.eclipse.ecsp.nosqldao.spring.config.IgniteDAOMongoConfigWithProps;
import org.eclipse.ecsp.nosqldao.test.TestDAO;
import org.eclipse.ecsp.nosqldao.test.TestEvent;
import org.eclipse.ecsp.nosqldao.utils.EmbeddedMongoDB;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Test class for IgniteBaseDAOMongoImpl with dynamic collection name.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IgniteDAOMongoConfigWithProps.class })
@TestPropertySource("/ignite-dao.properties")
public class IgniteBaseDAOMongoIntegrationWithDynamicCollectionTest {

    private static final String SOURCEDEVICEID = "sourceDeviceId";

    @ClassRule
    public static EmbeddedMongoDB embeddedMongoDB = new EmbeddedMongoDB();
    @Autowired
    GeoSpatialMockDAOImpl geoMockDao;
    @Autowired
    private MockTestDAOMongoImpl mockDao;
    @Autowired
    private TestDAO dao;

    @Autowired
    private Datastore datastore;

    private String customCollection = "customEcall";

    /**
     * Setup method.
     *
     * @throws IOException if properties file not found.
     */
    @Before
    public void setupEcallDAO() throws IOException {
        Properties daoProperties = new Properties();
        daoProperties.load(IgniteBaseDAOMongoIntegrationWithDynamicCollectionTest.class.getResourceAsStream(
                "/ignite-dao.properties"));
        customCollection = mockDao.getOverridingCollectionName();
    }

    @Test
    public void testSave() throws InterruptedException {
        MockTestEvent mockEvent = new MockTestEvent();
        mockEvent.setId("ECallId_1");
        mockEvent.setSourceDeviceId("Device_1");
        mockEvent.setEventId("ECall");
        mockEvent.setRequestId("Request_1");
        mockEvent.setTimestamp(NumericConstants.TIMESTAMP);
        mockEvent.setVehicleId("Vehicle_1");
        mockEvent.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        mockDao.save(mockEvent);
        MockTestEvent mockGot = mockDao.findById("ECallId_1");
        Assert.assertEquals(mockGot.getId(), mockEvent.getId());
        Assert.assertEquals(mockGot.getId(), mockGot.getId());
        Assert.assertEquals(mockGot.getRequestId(), mockGot.getRequestId());
    }

    @Test
    public void testDeleteById() throws InterruptedException {
        MockTestEvent mockEvent = new MockTestEvent();
        mockEvent.setId("ECallId_1");
        mockEvent.setSourceDeviceId("Device_1");
        mockEvent.setEventId("ECall");
        mockEvent.setRequestId("Request_1");
        mockEvent.setTimestamp(NumericConstants.TIMESTAMP);
        mockEvent.setVehicleId("Vehicle_1");
        mockEvent.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        mockDao.save(mockEvent);
        MockTestEvent mockGot = mockDao.findById("ECallId_1");
        Assert.assertEquals(mockGot.getId(), mockEvent.getId());

        mockDao.deleteById("ECallId_1");
        MockTestEvent mockGotAfterDel = mockDao.findById("ECallId_1");
        Assert.assertNull(mockGotAfterDel);

    }

    @Test
    public void testDeleteByIds() throws InterruptedException {
        MockTestEvent mockEvent = new MockTestEvent();
        mockEvent.setId("ECallId_1");
        mockEvent.setSourceDeviceId("Device_1");
        mockEvent.setEventId("ECall");
        mockEvent.setRequestId("Request_1");
        mockEvent.setTimestamp(NumericConstants.TIMESTAMP);
        mockEvent.setVehicleId("Vehicle_1");
        mockEvent.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        mockDao.save(mockEvent);
        MockTestEvent mockGot = mockDao.findById("ECallId_1");
        Assert.assertEquals(mockGot.getId(), (mockEvent.getId()));

        MockTestEvent mockEvent2 = new MockTestEvent();
        mockEvent2.setId("ECallId_2");
        mockEvent2.setSourceDeviceId("Device_1");
        mockEvent2.setEventId("ECall");
        mockEvent2.setRequestId("Request_1");
        mockEvent2.setTimestamp(NumericConstants.TIMESTAMP);
        mockEvent2.setVehicleId("Vehicle_1");
        mockEvent2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        mockDao.save(mockEvent2);
        MockTestEvent mockGot2 = mockDao.findById("ECallId_2");
        Assert.assertEquals(mockGot2.getId(), mockEvent2.getId());

        mockDao.deleteByIds("ECallId_1", "ECallId_2");
        MockTestEvent mockGotAfterDel = mockDao.findById("ECallId_1");
        Assert.assertNull(mockGotAfterDel);
        MockTestEvent mockGotAfterDel2 = mockDao.findById("ECallId_2");
        Assert.assertNull(mockGotAfterDel2);

    }

    @Test
    public void testSaveAll() {
        MockTestEvent ecall1 = new MockTestEvent();
        ecall1.setId("ECallIdAll_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        MockTestEvent ecall2 = new MockTestEvent();
        ecall2.setId("ECallIdAll_2");
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_1");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        mockDao.saveAll(ecall1, ecall2);

        List<MockTestEvent> ecallEvents = mockDao.findByIds("ECallIdAll_1", "ECallIdAll_2");
        Assert.assertEquals(NumericConstants.TWO, ecallEvents.size());

        MockTestEvent result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallIdAll_1")).first();
        Assert.assertEquals(ecall1.getRequestId(), result.getRequestId());
        result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallIdAll_2")).first();
        Assert.assertEquals(ecall2.getRequestId(), result.getRequestId());
    }

    @Test
    public void testSaveAllWithNullID() {
        MockTestEvent ecall1 = new MockTestEvent();
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        MockTestEvent ecall2 = new MockTestEvent();
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_1");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        List<MockTestEvent> eventSaved = mockDao.saveAll(ecall1, ecall2);
        Assert.assertEquals(NumericConstants.TWO, eventSaved.size());
        Assert.assertNotNull(eventSaved.get(0));
        Assert.assertNotNull(eventSaved.get(1));

        List<MockTestEvent> ecallEvents = mockDao.findByIds(eventSaved.get(0).getId(), eventSaved.get(1).getId());
        Assert.assertEquals(NumericConstants.TWO, ecallEvents.size());
    }

    @Test
    public void testUpsert() {
        MockTestEvent ecall1 = getMockTestEvent("ECallIdAll_9", "Device_1", "Request_1", "Vehicle_1");

        IgniteCriteria igniteCriteria = new IgniteCriteria("_id", Operator.EQ, "ECallIdAll_9");
        IgniteCriteriaGroup icg = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(icg);

        // 1. Insert if not present
        boolean upsertFlag = false;
        upsertFlag = mockDao.upsert(igniteQuery, ecall1);
        try {
            Thread.sleep(NumericConstants.TWO_K);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<MockTestEvent> ecallEventList = mockDao.findByIds("ECallIdAll_9");
        Assert.assertTrue(upsertFlag && (ecallEventList.size() > 0));

        MockTestEvent result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallIdAll_9")).first();
        Assert.assertEquals(ecall1.getRequestId(), result.getRequestId());

        MockTestEvent ecall2 = getMockTestEvent();

        igniteCriteria = new IgniteCriteria("_id", Operator.EQ, "ECallIdAll_9");
        icg = new IgniteCriteriaGroup(igniteCriteria);
        igniteQuery = new IgniteQuery(icg);

        try {
            upsertFlag = mockDao.upsert(igniteQuery, ecall2);
        } catch (MongoWriteException dke) {
            dke.printStackTrace();
        }
        ecallEventList = mockDao.findByIds("ECallIdAll_9");
        try {
            Thread.sleep(NumericConstants.THREE_K);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(upsertFlag && ecall2.getHits() == ecallEventList.get(0).getHits());
        result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallIdAll_9")).first();
        Assert.assertEquals(ecall2.getRequestId(), result.getRequestId());

        // 3. Duplicate key exception;
        MockTestEvent ecall3 = new MockTestEvent();
        ecall3.setId("ECallIdAll_9");
        ecall3.setSourceDeviceId("Device_2");
        ecall3.setEventId("ECall");
        ecall3.setRequestId("Request_2");
        ecall3.setTimestamp(NumericConstants.TIMESTAMP);
        ecall3.setVehicleId("Vehicle_2");
        ecall3.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        igniteCriteria = new IgniteCriteria("ecallId", Operator.EQ, "ECallIdAll_9");
        IgniteCriteria igniteCriteria2 = new IgniteCriteria("eventId",
                Operator.EQ, " NO_ECall");
        icg = new IgniteCriteriaGroup(igniteCriteria).and(igniteCriteria2);
        igniteQuery = new IgniteQuery(icg);
        boolean duplicateKeyException = false;
        try {
            mockDao.upsert(igniteQuery, ecall3);
        } catch (MongoWriteException dke) {
            duplicateKeyException = true;
        }
        mockDao.findByIds("ECallIdAll_9");
        Assert.assertTrue(duplicateKeyException);
    }

    @NotNull
    private static MockTestEvent getMockTestEvent() {
        // 2. Update fields existing records.
        MockTestEvent ecall2 = new MockTestEvent();
        ecall2.setId("ECallIdAll_9");
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_2");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecall2.setHits(NumericConstants.HITS);
        return ecall2;
    }

    @NotNull
    private static MockTestEvent getMockTestEvent(String ECallIdAll_9, String Device_1,
            String Request_1, String Vehicle_1) {
        MockTestEvent ecall1 = new MockTestEvent();
        ecall1.setId(ECallIdAll_9);
        ecall1.setSourceDeviceId(Device_1);
        ecall1.setEventId("ECall");
        ecall1.setRequestId(Request_1);
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId(Vehicle_1);
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        return ecall1;
    }

    @Test
    public void testUpdate() {
        MockTestEvent ecall = new MockTestEvent();
        ecall.setId("ECallId_Update_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        mockDao.save(ecall);
        ecall.setSourceDeviceId("Device_1_Updated");
        mockDao.update(ecall);
        MockTestEvent ecallUpdated = mockDao.findById("ECallId_Update_1");
        Assert.assertEquals("Device_1_Updated", (ecallUpdated.getSourceDeviceId()));
        MockTestEvent result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallId_Update_1")).first();
        Assert.assertEquals(ecall.getSourceDeviceId(), result.getSourceDeviceId());
    }

    @Test
    public void testUpdateAll() {
        MockTestEvent ecall1 = new MockTestEvent();
        ecall1.setId("ECallId_UpdateAll_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        MockTestEvent ecall2 = new MockTestEvent();
        ecall2.setId("ECallId_UpdateAll_2");
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_2");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        mockDao.saveAll(ecall1, ecall2);
        ecall1.setSourceDeviceId("Device_1_Updated");
        ecall2.setSourceDeviceId("Device_2_Updated");
        mockDao.updateAll(ecall1, ecall2);
        MockTestEvent ecallUpdated1 = mockDao.findById("ECallId_UpdateAll_1");
        MockTestEvent ecallUpdated2 = mockDao.findById("ECallId_UpdateAll_2");
        Assert.assertEquals("Device_1_Updated", (ecallUpdated1.getSourceDeviceId()));
        Assert.assertEquals("Device_2_Updated", (ecallUpdated2.getSourceDeviceId()));

        MockTestEvent result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallId_UpdateAll_1")).first();
        Assert.assertEquals(ecall1.getSourceDeviceId(), result.getSourceDeviceId());
        result =  datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallId_UpdateAll_2")).first();
        Assert.assertEquals(ecall2.getSourceDeviceId(), result.getSourceDeviceId());
    }

    @Test
    public void testQuery() {
        mockDao.deleteAll();
        MockTestEvent ecall1 = new MockTestEvent();
        ecall1.setId("ECallId_UpdateAll_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        MockTestEvent ecall2 = new MockTestEvent();
        ecall2.setId("ECallId_UpdateAll_2");
        ecall2.setSourceDeviceId("Device_2");
        ecall2.setEventId("ECall");
        ecall2.setRequestId("Request_2");
        ecall2.setTimestamp(NumericConstants.TIMESTAMP);
        ecall2.setVehicleId("Vehicle_2");
        ecall2.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        mockDao.saveAll(ecall1, ecall2);

        IgniteCriteria c1 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_1");
        IgniteCriteria c2 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_2");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1).or(c2);
        IgniteQuery igQuery = new IgniteQuery(cg1);

        List<MockTestEvent> ecallEvents = mockDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count",
                NumericConstants.TWO, ecallEvents.size());

        c1 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_1");
        c2 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device_1");
        cg1 = new IgniteCriteriaGroup(c1).and(c2);

        IgniteCriteria c3 = new IgniteCriteria("vehicleId", Operator.EQ, "Vehicle_2");
        IgniteCriteria c4 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device_2");
        IgniteCriteriaGroup cg2 = new IgniteCriteriaGroup(c3).and(c4);

        IgniteOrderBy igniteOrderBy = new IgniteOrderBy().byfield("vehicleId");

        igQuery = new IgniteQuery(cg1).or(cg2);
        igQuery.orderBy(igniteOrderBy);
        ecallEvents = mockDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count",
                NumericConstants.TWO, ecallEvents.size());
    }

    @Test
    public void testUpdateOperationsIncr() {
        MockTestEvent e1 = createUOEntity("ECallId_UO_Inc");
        mockDao.save(e1);
        Updates u = new Updates();
        u.addIncr("hits");
        mockDao.update(e1.getId(), u);
        MockTestEvent e2 = mockDao.findById("ECallId_UO_Inc");
        Assert.assertEquals(1, e2.getHits());
        MockTestEvent result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallId_UO_Inc")).first();
        Assert.assertEquals(1L, result.getHits());
    }

    @Test
    public void testUpdateOperationsDec() {
        MockTestEvent e1 = createUOEntity("ECallId_UO_Dec");
        mockDao.save(e1);
        Updates u = new Updates();
        u.addDecr("dunks");
        mockDao.update(e1.getId(), u);
        MockTestEvent e2 = mockDao.findById("ECallId_UO_Dec");
        Assert.assertEquals(NumericConstants.MINUS_ONE, e2.getDunks());
        MockTestEvent result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallId_UO_Dec")).first();
        Assert.assertEquals(NumericConstants.LONG_MINUS_ONE, result.getDunks());
    }

    @Test
    public void testUpdateOperationsFieldSet() {
        MockTestEvent e1 = createUOEntity("ECallId_UO_FieldSet");
        mockDao.save(e1);
        Updates u = new Updates();
        u.addFieldSet(SOURCEDEVICEID, "Device_1_updated");
        mockDao.update(e1.getId(), u);
        MockTestEvent e2 = mockDao.findById("ECallId_UO_FieldSet");
        Assert.assertEquals("Device_1_updated", e2.getSourceDeviceId());
        MockTestEvent result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallId_UO_FieldSet")).first();
        Assert.assertEquals("Device_1_updated", result.getSourceDeviceId());
    }

    @Test
    public void testUpdateOperationsFieldUnset() {
        MockTestEvent e1 = createUOEntity("ECallId_UO_FieldUnset");
        mockDao.save(e1);
        Updates u = new Updates();
        u.addFieldUnset(SOURCEDEVICEID);
        mockDao.update(e1.getId(), u);
        MockTestEvent e2 = mockDao.findById("ECallId_UO_FieldUnset");
        Assert.assertNull(e2.getSourceDeviceId());
        MockTestEvent result = datastore.find(customCollection, MockTestEvent.class)
                .filter(Filters.eq("_id", "ECallId_UO_FieldUnset"))
                .first();
        Assert.assertNull(result.getSourceDeviceId());
    }

    @Test
    public void testDeleteAllFindAll() {
        mockDao.deleteAll();
        List<MockTestEvent> events = mockDao.findAll();
        Assert.assertEquals(0, events.size());
        MockTestEvent e1 = createUOEntity("ECallId_FIND_ALL_1");
        mockDao.save(e1);
        events = mockDao.findAll();
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(e1.getId(), events.get(0).getId());
        MockTestEvent e2 = createUOEntity("ECallId_FIND_ALL_2");
        mockDao.save(e2);
        events = mockDao.findAll();
        Assert.assertTrue(Arrays.asList(e1.getId(), e2.getId())
                .containsAll(Arrays.asList(events.get(0).getId(), events.get(1).getId())));
    }

    private MockTestEvent createUOEntity(String id) {
        MockTestEvent ecall = new MockTestEvent();
        ecall.setId(id);
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1_" + id);
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        return ecall;
    }

    @Test()
    public void testDeleteWithEntity() {

        MockTestEvent ecall = new MockTestEvent();
        ecall.setId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        mockDao.save(ecall);

        MockTestEvent ecall1 = new MockTestEvent();
        ecall1.setId("ECallId_2");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_2");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        mockDao.save(ecall1);

        MockTestEvent ecallGot = mockDao.findById("ECallId_1");
        Assert.assertEquals("ECallId_1", ecallGot.getId());

        MockTestEvent ecallGot1 = mockDao.findById("ECallId_2");
        Assert.assertEquals("ECallId_2", ecallGot1.getId());

        Assert.assertThrows(UnsupportedOperationException.class, () -> mockDao.delete(ecallGot));
    }

    @Test
    public void testGetEcallByEqualsIgnoreCase() {
        mockDao.deleteAll();
        MockTestEvent ecall1 = new MockTestEvent();
        ecall1.setId("ECallId_1");
        ecall1.setSourceDeviceId("Device_1");
        ecall1.setEventId("ECall");
        ecall1.setRequestId("Request_1");
        ecall1.setTimestamp(NumericConstants.TIMESTAMP);
        ecall1.setVehicleId("Vehicle_1");
        ecall1.setVersion(org.eclipse.ecsp.domain.Version.V1_0);

        mockDao.saveAll(ecall1);

        IgniteCriteria c1 = new IgniteCriteria("vehicleId", Operator.EQI, "veHICle_1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);
        IgniteQuery igQuery = new IgniteQuery(cg1);

        List<MockTestEvent> ecallEvents = mockDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count", 1, ecallEvents.size());

        c1 = new IgniteCriteria("vehicleId", Operator.EQI, "VEHICLE_1");
        cg1 = new IgniteCriteriaGroup(c1);

        igQuery = new IgniteQuery(cg1);
        ecallEvents = mockDao.find(igQuery);
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
        TestEvent event = new TestEvent();
        event.setManufacturer("dummyMfr");
        event.setModel("A3");
        event.setYear("1992");
        event.setPrice(NumericConstants.PRICE);
        dao.save(event);
        MongoCollection<TestEvent> collection = datastore.getDatabase().getCollection("testEvents", TestEvent.class);
        ListIndexesIterable<Document> indexesList = collection.listIndexes();
        List<Document> indexes = indexesList.into(new ArrayList<>());
        Assert.assertEquals(NumericConstants.TWO, indexes.size());
        Document object = indexes.get(1);
        Document val = (Document) object.get("key");
        Assert.assertNotNull(val.get("manufacturer"));
        Assert.assertNotNull(val.get("model"));
        Assert.assertNotNull(val.get("year"));
        Assert.assertEquals("testEvents", collection.getNamespace().getCollectionName());
    }

    // geospatial feature test
    @Test
    public void testGeospatialQuery() {

        // create records for mongoDB
        Double[] coordinates1 = { NumericConstants.COORD_ONE, NumericConstants.COORD_FOUR};
        Location loc1 = new Location();
        loc1.setType("Point");
        loc1.setCoordinates(coordinates1);
        GeoSpatialMockEvent ecall1 = new GeoSpatialMockEvent();
        ecall1.setDealername("Dealer1");
        ecall1.setLocation(loc1);

        Double[] coordinates2 = { NumericConstants.COORD_SIX, NumericConstants.COORD_FIVE };
        Location loc2 = new Location();
        loc2.setType("Point");
        loc2.setCoordinates(coordinates2);
        GeoSpatialMockEvent ecall2 = new GeoSpatialMockEvent();
        ecall2.setDealername("Dealer2");
        ecall2.setLocation(loc2);

        //insert sample records in mongodb
        geoMockDao.saveAll(ecall1, ecall2);

        // Prepare Ignite Query

        Coordinate coordinates = new Coordinate(NumericConstants.COORD_FOUR,
                NumericConstants.COORD_ONE, NumericConstants.TWENTY_K);
        coordinates.setLatitude(NumericConstants.COORD_FOUR);
        coordinates.setLongitude(NumericConstants.COORD_ONE);
        coordinates.setRadius(NumericConstants.TWENTY_K);
        IgniteCriteria c1 = new IgniteCriteria("location", Operator.NEAR, coordinates);
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);
        IgniteQuery igQuery = new IgniteQuery();
        igQuery.and(cg1);

        // prepare morphia query and search in mongo
        List<GeoSpatialMockEvent> ecallEvents = geoMockDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count", 1, ecallEvents.size());
    }

    @Test
    public void testSaveForNullableObjects() throws InterruptedException {
        MockTestEvent mockEvent = new MockTestEvent();
        mockEvent.setId("ECallId_1");
        mockEvent.setSourceDeviceId("Device_1");
        mockEvent.setEventId("ECall");
        mockEvent.setRequestId("Request_1");
        mockEvent.setTimestamp(NumericConstants.TIMESTAMP);
        mockEvent.setVehicleId("Vehicle_1");
        mockEvent.setTargetDeviceId("device1");
        mockEvent.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        mockDao.save(mockEvent);
        MockTestEvent mockGot = mockDao.findById("ECallId_1");
        Assert.assertEquals(mockGot.getId(), (mockGot.getId()));
        Assert.assertEquals(mockGot.getRequestId(), (mockGot.getRequestId()));
        Assert.assertEquals(mockGot.getTargetDeviceId().get(), (mockGot.getTargetDeviceId().get()));
    }

    @After
    public void deleteAllRecords() {
        mockDao.deleteAll();
    }

}