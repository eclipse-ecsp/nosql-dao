package org.eclipse.ecsp.nosqldao.mongodb;

import org.awaitility.Awaitility;
import org.eclipse.ecsp.nosqldao.ecall.ECallEvent;
import org.eclipse.ecsp.nosqldao.ecall.EcallDAO;
import org.eclipse.ecsp.nosqldao.spring.config.IgniteDAOMongoConfigWithProps;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

/**
 * Integration test for IgniteDAO with DocumentDB using MongoDB configuration.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IgniteDAOMongoConfigWithProps.class })
@TestPropertySource("/ignite-dao-documentdb.properties")
public class IgniteBaseDAODocumentDBIntegrationTest {
    @Autowired
    private EcallDAO ecallDao;
    @Before
    public void setupEcallDAO() throws IOException {
        Properties daoProperties = new Properties();
        daoProperties.load(IgniteBaseDAOCosmosDBIntegrationTest.class.getResourceAsStream(
                "/ignite-dao-documentdb.properties"));
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
}
