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
import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.nosqldao.ecall.MockTestDAOMongoImpl;
import org.eclipse.ecsp.nosqldao.ecall.MockTestEvent;
import org.eclipse.ecsp.nosqldao.spring.config.IgniteDAOMongoConfigWithProps;
import org.eclipse.ecsp.nosqldao.utils.Constants;
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
import java.util.List;
import java.util.Properties;

/**
 * Test class for IgniteBaseDAO metrics.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IgniteDAOMongoConfigWithProps.class })
@TestPropertySource("/ignite-dao-metrics-enabled.properties")
public class IgniteBaseDAOMongoMetricsTest {

    private static final String SOURCEDEVICEID = "sourceDeviceId";

    @ClassRule
    public static EmbeddedMongoDB embeddedMongoDB = new EmbeddedMongoDB();

    @Autowired
    private MockTestDAOMongoImpl mockDao;

    @Autowired
    private Datastore datastore;

    /** Setup method. */
    @Before
    public void setupEcallDAO() throws IOException {
        Properties daoProperties = new Properties();
        daoProperties.load(IgniteBaseDAOMongoMetricsTest.class.getResourceAsStream(
                "/ignite-dao-metrics-enabled.properties"));
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

        igQuery = new IgniteQuery(cg1).or(cg2);
        ecallEvents = mockDao.find(igQuery);
        Assert.assertEquals("Expected query count does not match actual count",
                NumericConstants.TWO, ecallEvents.size());

        assertMetrics(igQuery.toTemplatedQueryString());
    }

    /** Test method for countByQuery.
     *
     * @param templatedQueryString templated query string
     */
    private void assertMetrics(String templatedQueryString) {
        Double latency = CollectorRegistry.defaultRegistry.getSampleValue(Constants.LATENCY_HISTO_NAME + "_bucket",
                new String[] { Constants.SVC, Constants.OPERATION_TYPE_LABEL,
                    Constants.ENTITY_LABEL, Constants.PAGINATION_LABEL, Constants.FULL_QUERY_LABEL, "le" },
                new String[] { "test", Constants.OPERATION_TYPE_FIND_QUERY, "MockTestEvent",
                    "false", templatedQueryString, "0.005" });
        Assert.assertNotNull(latency);
        Assert.assertTrue(latency >= 0);
        Double rate = CollectorRegistry.defaultRegistry.getSampleValue(Constants.REQ_COUNTER_NAME,
                new String[] { Constants.SVC, Constants.OPERATION_TYPE_LABEL,
                    Constants.ENTITY_LABEL, Constants.PAGINATION_LABEL, Constants.FULL_QUERY_LABEL },
                new String[] { "test", Constants.OPERATION_TYPE_FIND_QUERY, "MockTestEvent",
                    "false", templatedQueryString });
        Assert.assertNotNull(rate);
        Assert.assertTrue(rate > 0);
        Double requests = CollectorRegistry.defaultRegistry.getSampleValue(Constants.REQ_GAUGE_NAME,
                new String[] { Constants.SVC, Constants.OPERATION_TYPE_LABEL,
                    Constants.ENTITY_LABEL, Constants.PAGINATION_LABEL, Constants.FULL_QUERY_LABEL },
                new String[] { "test", Constants.OPERATION_TYPE_FIND_QUERY, "MockTestEvent",
                    "false", templatedQueryString });
        Assert.assertNotNull(requests);
        Assert.assertTrue(requests > 0);
    }
}