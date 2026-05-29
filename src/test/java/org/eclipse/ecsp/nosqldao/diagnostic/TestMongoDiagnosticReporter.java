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

package org.eclipse.ecsp.nosqldao.diagnostic;

import dev.morphia.Datastore;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.eclipse.ecsp.diagnostic.DiagnosticData;
import org.eclipse.ecsp.diagnostic.DiagnosticResult;
import org.eclipse.ecsp.nosqldao.MongoDiagnosticReporterImpl;
import org.eclipse.ecsp.nosqldao.ecall.EcallDAO;
import org.eclipse.ecsp.nosqldao.mongodb.BytesBuffer;
import org.eclipse.ecsp.nosqldao.mongodb.IgniteBaseDAOMongoIntegrationTest;
import org.eclipse.ecsp.nosqldao.mongodb.TestBuffer;
import org.eclipse.ecsp.nosqldao.spring.config.IgniteDAOMongoConfigWithProps;
import org.eclipse.ecsp.nosqldao.test.TestDAO;
import org.eclipse.ecsp.nosqldao.utils.EmbeddedMongoDB;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Properties;

/** Test class for MongoDiagnosticReporterImpl. */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IgniteDAOMongoConfigWithProps.class,
    TestMongoDiagnosticConfig.class })
@TestPropertySource("/ignite-dao-diagnostic.properties")
@Testcontainers
public class TestMongoDiagnosticReporter {

    @ClassRule
    public static EmbeddedMongoDB embeddedMongoDB = new EmbeddedMongoDB();
    @Autowired
    MongoDiagnosticReporterImpl mongoDiagnosticReporterImpl;
    @Autowired
    private EcallDAO ecallDao;
    @Autowired
    private TestDAO dao;
    @Autowired
    private Datastore mockedDatastore;

    @Autowired
    private IgniteDAOMongoConfigWithProps igniteDAOMongoConfigWithProps;

    @Before
    public void setUp() throws IOException {
        Properties daoProperties = new Properties();
        daoProperties.load(IgniteBaseDAOMongoIntegrationTest.class.getResourceAsStream("/ignite-dao.properties"));
    }

    @Test
    public void testMongoDiagnosticReporter() {
        mongoDiagnosticReporterImpl.getDiagnosticData().keySet().forEach(property -> {
            System.out.println("property : " + property
                    + " " + mongoDiagnosticReporterImpl.getDiagnosticData().get(property));
        });
        DiagnosticData diagnosticData = mongoDiagnosticReporterImpl.getDiagnosticData();
        Assert.assertEquals(DiagnosticResult.FAIL, diagnosticData.get("customEcall.sharded"));
        Assert.assertEquals(DiagnosticResult.PASS, diagnosticData.get("customEcall.indexed"));
        Assert.assertEquals(DiagnosticResult.FAIL, diagnosticData.get("ecallEvents.sharded"));
        Assert.assertEquals(DiagnosticResult.PASS, diagnosticData.get("ecallEvents.indexed"));
        Assert.assertEquals(DiagnosticResult.PASS, diagnosticData.get("testEvents.indexed"));
        Assert.assertEquals(DiagnosticResult.FAIL, diagnosticData.get("testEvents.sharded"));

    }

    @Test
    public void testDiagnosticReporterName() {
        Assert.assertEquals("DIAGNOSTIC_MONGO_REPORTER", mongoDiagnosticReporterImpl.getDiagnosticReporterName());
    }

    @Test
    public void testDiagnosticMetricName() {
        Assert.assertEquals("DIAGNOSTIC_MONGO_METRIC", mongoDiagnosticReporterImpl.getDiagnosticMetricName());
    }

    @Test
    public void isDiagnosticReporterEnabled() {

        Assert.assertTrue(mongoDiagnosticReporterImpl.isDiagnosticReporterEnabled());

    }

    @Test()
    public void testSingleMorphiaConverter() throws IOException {
        Properties daoProperties = new Properties();
        daoProperties.load(IgniteBaseDAOMongoIntegrationTest.class.getResourceAsStream(
                "/ignite-dao-diagnostic.properties"));
        Assert.assertNotNull(igniteDAOMongoConfigWithProps.getCodecRegistry());
        Assert.assertNotNull(igniteDAOMongoConfigWithProps.getCodecRegistry().get(BytesBuffer.class));
        Assert.assertThrows(CodecConfigurationException.class, () -> igniteDAOMongoConfigWithProps.getCodecRegistry()
                .get(TestBuffer.class));
    }

}
