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

import com.mongodb.MongoClientSettings;
import org.eclipse.ecsp.nosqldao.NoSqlDatabaseType;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Test class for AbstractIgniteDAOMongoConfig.
 */
public class AbstractIgniteDAOMongoConfigTest {

    @InjectMocks
    AbstractIgniteDAOMongoConfig igniteDAOMongoConfig = new IgniteDAOMongoConfigWithProps();

    @Mock
    private CustomConnectionPoolListener customConnectionPoolListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getDatastoreTestWhenExceptionOccurs() {
        Assert.assertThrows(RuntimeException.class, () -> igniteDAOMongoConfig.mongoDatastore());
    }

    @Test
    public void testConnectionPoolSettings() {
        igniteDAOMongoConfig.maintenanceFrequency = NumericConstants.FORTY_K;
        igniteDAOMongoConfig.maxConnectionIdleTime = NumericConstants.SIXTY_K;
        igniteDAOMongoConfig.maxConnectionLifeTime = NumericConstants.SIXTY_K;
        igniteDAOMongoConfig.maxConnectionsPerHost = NumericConstants.TWO_HUNDRED;
        igniteDAOMongoConfig.poolMaxSize = NumericConstants.FOUR;
        igniteDAOMongoConfig.poolMinSize = NumericConstants.TWO;
        igniteDAOMongoConfig.maxWaitTime = NumericConstants.SIXTY_K;
        igniteDAOMongoConfig.hosts = "localhost";
        igniteDAOMongoConfig.maintenanceInitialDelay = NumericConstants.THIRTY_K;
        igniteDAOMongoConfig.readPreference = "secondaryPreferred";
        igniteDAOMongoConfig.noSqlDatabaseType = NoSqlDatabaseType.MONGODB;
        MongoClientSettings.Builder mongoClientSettingsBuilder = igniteDAOMongoConfig
                .createMongoClientSettingsBuilder();
        assertEquals(NumericConstants.FOUR, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings().getMaxSize());
        assertEquals(NumericConstants.TWO_HUNDRED, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings().getMaxConnecting());
        assertEquals(NumericConstants.SIXTY_K, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings()
                .getMaxWaitTime(TimeUnit.MILLISECONDS));
        assertEquals(NumericConstants.TWO, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings().getMinSize());
        assertEquals(NumericConstants.SIXTY_K, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings()
                .getMaxConnectionIdleTime(TimeUnit.MILLISECONDS));
        assertEquals(NumericConstants.SIXTY_K, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings()
                .getMaxConnectionLifeTime(TimeUnit.MILLISECONDS));
        assertEquals(NumericConstants.FORTY_K, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings()
                .getMaintenanceFrequency(TimeUnit.MILLISECONDS));
        assertEquals(NumericConstants.THIRTY_K,
                mongoClientSettingsBuilder.build().getConnectionPoolSettings()
                        .getMaintenanceInitialDelay(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testConnectionPoolSettingWithDefaultValue() {
        igniteDAOMongoConfig.hosts = "localhost";
        igniteDAOMongoConfig.maxConnectionsPerHost = NumericConstants.TWO;
        igniteDAOMongoConfig.noSqlDatabaseType = NoSqlDatabaseType.MONGODB;
        igniteDAOMongoConfig.readPreference = "secondaryPreferred";
        igniteDAOMongoConfig.morphiaConverters = "org.eclipse.ecsp.nosqldao.mongodb.BytesBufferConverter";
        MongoClientSettings.Builder mongoClientSettingsBuilder = igniteDAOMongoConfig
                .createMongoClientSettingsBuilder();
        Assert.assertEquals(NumericConstants.HUNDRED, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings().getMaxSize());
        Assert.assertEquals(NumericConstants.TWO, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings().getMaxConnecting());
        Assert.assertEquals(NumericConstants.ZERO, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings()
                .getMaxWaitTime(TimeUnit.MILLISECONDS));
        Assert.assertEquals(NumericConstants.ZERO, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings().getMinSize());
        Assert.assertEquals(NumericConstants.ZERO,
                mongoClientSettingsBuilder.build().getConnectionPoolSettings()
                        .getMaxConnectionIdleTime(TimeUnit.MILLISECONDS));
        Assert.assertEquals(NumericConstants.ZERO,
                mongoClientSettingsBuilder.build().getConnectionPoolSettings()
                        .getMaxConnectionLifeTime(TimeUnit.MILLISECONDS));
        Assert.assertEquals(NumericConstants.SIXTY_K,
                mongoClientSettingsBuilder.build().getConnectionPoolSettings()
                        .getMaintenanceFrequency(TimeUnit.MILLISECONDS));
        Assert.assertEquals(NumericConstants.ZERO,
                mongoClientSettingsBuilder.build().getConnectionPoolSettings()
                        .getMaintenanceInitialDelay(TimeUnit.MILLISECONDS));
    }

    @Test()
    public void testConnectionPoolSettingWithClassNotFoundException() {
        igniteDAOMongoConfig.hosts = "localhost";
        igniteDAOMongoConfig.maxConnectionsPerHost = NumericConstants.TWO;
        igniteDAOMongoConfig.noSqlDatabaseType = NoSqlDatabaseType.MONGODB;
        igniteDAOMongoConfig.readPreference = "secondaryPreferred";
        igniteDAOMongoConfig.morphiaConverters = "org.eclipse.ecsp.nosqldao.mongodb.BytesBufferConvert";
        MongoClientSettings.Builder mongoClientSettingsBuilder = igniteDAOMongoConfig
                .createMongoClientSettingsBuilder();
        Assert.assertEquals(NumericConstants.TWO, mongoClientSettingsBuilder.build()
                .getConnectionPoolSettings().getMaxConnecting());
    }

    @Test
    public void testConnectionPoolSettingWhenExceptionOccurs() {
        igniteDAOMongoConfig.hosts = "localhost";
        igniteDAOMongoConfig.maxConnectionsPerHost = NumericConstants.TWO;
        igniteDAOMongoConfig.poolMaxSize = NumericConstants.FOUR;
        igniteDAOMongoConfig.poolMinSize = NumericConstants.SEVEN;
        igniteDAOMongoConfig.username = "test";
        igniteDAOMongoConfig.password = "test";
        igniteDAOMongoConfig.authDb = "test1";
        igniteDAOMongoConfig.readPreference = "secondaryPreferred";
        igniteDAOMongoConfig.noSqlDatabaseType = NoSqlDatabaseType.MONGODB;
        assertThrows(RuntimeException.class, () -> igniteDAOMongoConfig.getDatastore());
    }

    @Test
    public void testGetDatastoreDbNameForMongoDb() throws Exception {
        igniteDAOMongoConfig.dbName = "mongoDb";
        igniteDAOMongoConfig.docDbName = "docDb";
        igniteDAOMongoConfig.noSqlDatabaseType = NoSqlDatabaseType.MONGODB;

        java.lang.reflect.Method m = AbstractIgniteDAOMongoConfig.class.getDeclaredMethod("getDatastoreDbName");
        m.setAccessible(true);
        String name = (String) m.invoke(igniteDAOMongoConfig);

        assertEquals("mongoDb", name);
    }

    @Test
    public void testGetDatastoreDbNameForDocumentDb() throws Exception {
        igniteDAOMongoConfig.dbName = "mongoDb";
        igniteDAOMongoConfig.docDbName = "docDb";
        igniteDAOMongoConfig.noSqlDatabaseType = NoSqlDatabaseType.DOCUMENTDB;

        java.lang.reflect.Method m = AbstractIgniteDAOMongoConfig.class.getDeclaredMethod("getDatastoreDbName");
        m.setAccessible(true);
        String name = (String) m.invoke(igniteDAOMongoConfig);

        assertEquals("docDb", name);
    }
}