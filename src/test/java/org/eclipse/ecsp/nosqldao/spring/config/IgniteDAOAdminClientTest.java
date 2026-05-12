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

package org.eclipse.ecsp.nosqldao.spring.config;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * Test class for IgniteDAOMongoAdminClient.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IgniteDAOMongoConfigWithProps.class, IgniteDAOMongoAdminClient.class })
@TestPropertySource("/ignite-dao-admin-client.properties")
@Testcontainers
public class IgniteDAOAdminClientTest {

    @Autowired
    IgniteDAOMongoAdminClient igniteDaoMongoClient;
    
    @Container
    private static MongoDBContainer mongoDbContainer = new MongoDBContainer("mongo:6.0.13");
    
    private static MongoClient mongoClient;

    /**
     * Create mongo server.
     *
     * @throws Exception the exception
     */
    @BeforeClass
    public static void createMongoServer() throws Exception {
        mongoDbContainer.start();
        AbstractIgniteDAOMongoConfig.overridingPort = mongoDbContainer.getFirstMappedPort();
        createMongoAdminUser("admin50", "password0");
    }

    private static void createMongoAdminUser(String user, String password) {
        try (MongoClient mongoClient = MongoClients.create(mongoDbContainer.getConnectionString())) {
            Map<String, Object> commandArguments = new BasicDBObject();
            commandArguments.put("createUser", user);
            commandArguments.put("pwd", password);
            String[] roles = { "readWrite" };
            commandArguments.put("roles", roles);
            MongoDatabase adminDatabase = mongoClient.getDatabase("admin");
            BasicDBObject command = new BasicDBObject(commandArguments);
            adminDatabase.runCommand(command);
        }
    }

    @Test
    public void testMongoAdminClient() {
        mongoClient = igniteDaoMongoClient.getAdminClient();
        MongoDatabase adminDatabase = mongoClient.getDatabase("admin");
        assertNotNull(adminDatabase.listCollections().first());
    }

    /**
     * Tear up mongo server.
     */
    @After
    public void tearUpMongoServer() {
        mongoClient.close();
        if (mongoDbContainer.isCreated()) {
            mongoDbContainer.stop();
        }
    }
}