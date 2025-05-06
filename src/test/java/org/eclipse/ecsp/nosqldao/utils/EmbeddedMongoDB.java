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

package org.eclipse.ecsp.nosqldao.utils;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.eclipse.ecsp.nosqldao.spring.config.AbstractIgniteDAOMongoConfig;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.Map;

/**
 * The `EmbeddedMongoDB` class provides an embedded MongoDB instance for testing purposes.
 * It uses the Test containers library to spin up a MongoDB container and configure it
 * with a user and roles before running tests. This class extends `ExternalResource` 
 * to manage the life cycle of the embedded MongoDB instance, ensuring it starts before 
 * tests and stops after tests.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Starts an embedded MongoDB container using Test containers.</li>
 *   <li>Configures the MongoDB instance with a user and roles for testing.</li>
 *   <li>Automatically stops the MongoDB container after tests.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @Rule
 * public EmbeddedMongoDB embeddedMongoDB = new EmbeddedMongoDB();
 * }
 * </pre>
 *
 * <p>Dependencies:
 * <ul>
 *   <li>Test containers for managing the MongoDB container.</li>
 *   <li>MongoDB Java Driver for database operations.</li>
 *   <li>Custom logging via `IgniteLogger`.</li>
 * </ul>
 */
public class EmbeddedMongoDB extends ExternalResource {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(EmbeddedMongoDB.class);
    
    @Container
    MongoDBContainer mongoDbContainer = new MongoDBContainer("mongo:6.0.13");

    /**
     * Before executing tests, start the embedded MongoDB server.
     *
     * @throws Throwable the throwable
     */
    @Override
    protected void before() throws Throwable {
        mongoDbContainer.start();
        LOGGER.info("Embedded mongo DB started on address {} ", mongoDbContainer.getHost());
        AbstractIgniteDAOMongoConfig.overridingPort = mongoDbContainer.getFirstMappedPort();

        LOGGER.info("MongoClient connecting for pre-work DB configuration...");
        try (MongoClient mongoClient = MongoClients.create(mongoDbContainer.getConnectionString())) {
            Map<String, Object> commandArguments = new BasicDBObject();
            commandArguments.put("createUser", "admin");
            commandArguments.put("pwd", "password");
            String[] roles = { "readWrite" };
            commandArguments.put("roles", roles);
            MongoDatabase adminDatabase = mongoClient.getDatabase("admin");
            BasicDBObject command = new BasicDBObject(commandArguments);
            adminDatabase.runCommand(command);
        }
    }

    @Override
    protected void after() {
        kill();
    }

    /** Kill the embedded mongo db process. */
    public void kill() {
        if (mongoDbContainer.isCreated()) {
            mongoDbContainer.stop();
        }
    }
}