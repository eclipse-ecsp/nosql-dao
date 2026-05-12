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

import dev.morphia.Datastore;
import org.eclipse.ecsp.nosqldao.ecall.ECallEvent;
import org.eclipse.ecsp.nosqldao.ecall.EcallDAO;
import org.eclipse.ecsp.nosqldao.ecall.MockTestDAOMongoImpl;
import org.eclipse.ecsp.nosqldao.ecall.MockTestEvent;
import org.eclipse.ecsp.nosqldao.spring.config.IgniteDAOMongoConfigWithProps;
import org.eclipse.ecsp.nosqldao.utils.EmbeddedMongoDB;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
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
import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * Test class for IgniteBaseDAO with shard key.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IgniteDAOMongoConfigWithProps.class })
@TestPropertySource("/ignite-dao-shardkey-collection.properties")
public class IgniteBaseDAOShardKeyIntegrationTest {

    @ClassRule
    public static EmbeddedMongoDB embeddedMongoDB = new EmbeddedMongoDB();

    @Autowired
    private EcallDAO ecallDao;

    @Autowired
    private MockTestDAOMongoImpl mockDao;

    @Autowired
    private Datastore datastore;

    /** Setup method. */
    @Before
    public void setupEcallDAO() throws IOException {
        Properties daoProperties = new Properties();
        daoProperties.load(IgniteBaseDAOShardKeyIntegrationTest.class.getResourceAsStream(
                "/ignite-dao-shardkey-collection.properties"));
    }

    @Test
    public void testSaveWithSingleShardKeys() throws InterruptedException {
        ecallDao.deleteAll();
        ECallEvent ecall = new ECallEvent();
        ecall.setEcallId("ECallId_1");
        ecall.setSourceDeviceId("Device_1");
        ecall.setEventId("ECall");
        ecall.setRequestId("Request_1");
        ecall.setTimestamp(NumericConstants.TIMESTAMP);
        ecall.setVehicleId("Vehicle_1");
        ecall.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        ecallDao.save(ecall);
        ECallEvent ecallGot = ecallDao.findById("ECallId_1");
        Assert.assertEquals(ecallGot.getEcallId(), ecall.getEcallId());
        assertNotNull(ecallGot.getLastUpdatedTime());
        Assert.assertTrue(ecallGot.getLastUpdatedTime().isBefore(LocalDateTime.now()));
    }

    @Test
    public void testSaveWithDynamicCollectionName() {
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
        Assert.assertEquals(mockGot.getId(), mockEvent.getId());
        Assert.assertEquals(mockGot.getRequestId(), mockEvent.getRequestId());
    }

}