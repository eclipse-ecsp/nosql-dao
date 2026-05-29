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

import com.mongodb.MongoClientException;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoSocketException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.eclipse.ecsp.nosqldao.NoSqlDatabaseType;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Test class for AbstractIgniteDAOMongoConfig.
 */
public class AbstractIgniteDAOMongoConfigTest {

    private static final int NON_CRITICAL_MONGO_ERROR_CODE = 99;

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

    @Test
    public void testIsHealthy_ReturnsFalseWithoutForce() {
        AbstractIgniteDAOMongoConfig.setHealthy(false);
        Assert.assertFalse(igniteDAOMongoConfig.isHealthy(false));
    }

    @Test
    public void testIsHealthy_ReturnsTrueWithoutForce() {
        AbstractIgniteDAOMongoConfig.setHealthy(true);
        Assert.assertTrue(igniteDAOMongoConfig.isHealthy(false));
    }

    @Test
    public void testIsHealthy_DoesNotRecreateWhenHealthyAndForced() throws Exception {
        MongoClient mockClient = Mockito.mock(MongoClient.class);
        Field clientField = AbstractIgniteDAOMongoConfig.class.getDeclaredField("mongoClient");
        clientField.setAccessible(true);
        clientField.set(igniteDAOMongoConfig, mockClient);
        AbstractIgniteDAOMongoConfig.setHealthy(true);

        boolean result = igniteDAOMongoConfig.isHealthy(true);

        Assert.assertTrue(result);
        Mockito.verify(mockClient, Mockito.never()).close();
    }

    /**
     * Wires up the private peInvocationHandler in a fresh IgniteDAOMongoConfigWithProps with
     * mock Datastore and MongoClient, then returns the handler for direct invocation.
     */
    private InvocationHandler getConfiguredHandler(AbstractIgniteDAOMongoConfig config,
            Datastore mockDatastore, MongoClient mockClient) throws Exception {
        Field clientField = AbstractIgniteDAOMongoConfig.class.getDeclaredField("mongoClient");
        clientField.setAccessible(true);
        clientField.set(config, mockClient);

        Field handlerField = IgniteDAOMongoConfigWithProps.class.getDeclaredField("peInvocationHandler");
        handlerField.setAccessible(true);
        Object handler = handlerField.get(config);

        Method setDs = handler.getClass().getDeclaredMethod("setDatastore", Datastore.class);
        setDs.setAccessible(true);
        setDs.invoke(handler, mockDatastore);

        return (InvocationHandler) handler;
    }

    @Test
    public void testProxyInvoke_SuccessSetsHealthyTrue() throws Throwable {
        IgniteDAOMongoConfigWithProps config = new IgniteDAOMongoConfigWithProps();
        Datastore mockDs = Mockito.mock(Datastore.class);
        MongoClient mockClient = Mockito.mock(MongoClient.class);
        MongoDatabase mockDb = Mockito.mock(MongoDatabase.class);
        Mockito.when(mockDs.getDatabase()).thenReturn(mockDb);

        InvocationHandler handler = getConfiguredHandler(config, mockDs, mockClient);
        AbstractIgniteDAOMongoConfig.setHealthy(false);

        Method getDbMethod = Datastore.class.getMethod("getDatabase");
        Object result = handler.invoke(null, getDbMethod, null);

        Assert.assertTrue(AbstractIgniteDAOMongoConfig.healthy);
        Assert.assertEquals(mockDb, result);
    }

    @Test
    public void testProxyInvoke_MongoSocketExceptionSetsUnhealthy() throws Exception {
        IgniteDAOMongoConfigWithProps config = new IgniteDAOMongoConfigWithProps();
        Datastore mockDs = Mockito.mock(Datastore.class);
        MongoClient mockClient = Mockito.mock(MongoClient.class);
        MongoSocketException socketEx = new MongoSocketException("socket error", new ServerAddress());
        Mockito.when(mockDs.getDatabase()).thenThrow(socketEx);

        InvocationHandler handler = getConfiguredHandler(config, mockDs, mockClient);
        AbstractIgniteDAOMongoConfig.setHealthy(true);

        Method getDbMethod = Datastore.class.getMethod("getDatabase");
        try {
            handler.invoke(null, getDbMethod, null);
            Assert.fail("Expected MongoSocketException");
        } catch (Throwable t) {
            Assert.assertSame(socketEx, t);
        }
        Assert.assertFalse(AbstractIgniteDAOMongoConfig.healthy);
    }

    @Test
    public void testProxyInvoke_MongoClientExceptionSetsUnhealthy() throws Exception {
        IgniteDAOMongoConfigWithProps config = new IgniteDAOMongoConfigWithProps();
        Datastore mockDs = Mockito.mock(Datastore.class);
        MongoClient mockClient = Mockito.mock(MongoClient.class);
        MongoClientException clientEx = new MongoClientException("client error");
        Mockito.when(mockDs.getDatabase()).thenThrow(clientEx);

        InvocationHandler handler = getConfiguredHandler(config, mockDs, mockClient);
        AbstractIgniteDAOMongoConfig.setHealthy(true);

        Method getDbMethod = Datastore.class.getMethod("getDatabase");
        try {
            handler.invoke(null, getDbMethod, null);
            Assert.fail("Expected MongoClientException");
        } catch (Throwable t) {
            Assert.assertSame(clientEx, t);
        }
        Assert.assertFalse(AbstractIgniteDAOMongoConfig.healthy);
    }

    @Test
    public void testProxyInvoke_MongoCodeElevenSetsUnhealthy() throws Exception {
        IgniteDAOMongoConfigWithProps config = new IgniteDAOMongoConfigWithProps();
        Datastore mockDs = Mockito.mock(Datastore.class);
        MongoClient mockClient = Mockito.mock(MongoClient.class);
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NumericConstants.ELEVEN))
                .append("errmsg", new BsonString("UserNotFound"));
        MongoCommandException ex = new MongoCommandException(response, new ServerAddress());
        Mockito.when(mockDs.getDatabase()).thenThrow(ex);

        InvocationHandler handler = getConfiguredHandler(config, mockDs, mockClient);
        AbstractIgniteDAOMongoConfig.setHealthy(true);

        Method getDbMethod = Datastore.class.getMethod("getDatabase");
        try {
            handler.invoke(null, getDbMethod, null);
            Assert.fail("Expected MongoCommandException");
        } catch (Throwable t) {
            Assert.assertSame(ex, t);
        }
        Assert.assertFalse(AbstractIgniteDAOMongoConfig.healthy);
    }

    @Test
    public void testProxyInvoke_MongoCodeThirteenSetsUnhealthy() throws Exception {
        IgniteDAOMongoConfigWithProps config = new IgniteDAOMongoConfigWithProps();
        Datastore mockDs = Mockito.mock(Datastore.class);
        MongoClient mockClient = Mockito.mock(MongoClient.class);
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NumericConstants.THIRTEEN))
                .append("errmsg", new BsonString("Unauthorized"));
        MongoCommandException ex = new MongoCommandException(response, new ServerAddress());
        Mockito.when(mockDs.getDatabase()).thenThrow(ex);

        InvocationHandler handler = getConfiguredHandler(config, mockDs, mockClient);
        AbstractIgniteDAOMongoConfig.setHealthy(true);

        Method getDbMethod = Datastore.class.getMethod("getDatabase");
        try {
            handler.invoke(null, getDbMethod, null);
            Assert.fail("Expected MongoCommandException");
        } catch (Throwable t) {
            Assert.assertSame(ex, t);
        }
        Assert.assertFalse(AbstractIgniteDAOMongoConfig.healthy);
    }

    @Test
    public void testProxyInvoke_MongoCodeThirtyOneSetsUnhealthy() throws Exception {
        IgniteDAOMongoConfigWithProps config = new IgniteDAOMongoConfigWithProps();
        Datastore mockDs = Mockito.mock(Datastore.class);
        MongoClient mockClient = Mockito.mock(MongoClient.class);
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NumericConstants.THIRTY_ONE))
                .append("errmsg", new BsonString("CursorNotFound"));
        MongoCommandException ex = new MongoCommandException(response, new ServerAddress());
        Mockito.when(mockDs.getDatabase()).thenThrow(ex);

        InvocationHandler handler = getConfiguredHandler(config, mockDs, mockClient);
        AbstractIgniteDAOMongoConfig.setHealthy(true);

        Method getDbMethod = Datastore.class.getMethod("getDatabase");
        try {
            handler.invoke(null, getDbMethod, null);
            Assert.fail("Expected MongoCommandException");
        } catch (Throwable t) {
            Assert.assertSame(ex, t);
        }
        Assert.assertFalse(AbstractIgniteDAOMongoConfig.healthy);
    }

    @Test
    public void testProxyInvoke_NonCriticalMongoCodePreservesHealth() throws Exception {
        IgniteDAOMongoConfigWithProps config = new IgniteDAOMongoConfigWithProps();
        Datastore mockDs = Mockito.mock(Datastore.class);
        MongoClient mockClient = Mockito.mock(MongoClient.class);
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NON_CRITICAL_MONGO_ERROR_CODE))
                .append("errmsg", new BsonString("SomeOtherError"));
        MongoCommandException ex = new MongoCommandException(response, new ServerAddress());
        Mockito.when(mockDs.getDatabase()).thenThrow(ex);

        InvocationHandler handler = getConfiguredHandler(config, mockDs, mockClient);
        AbstractIgniteDAOMongoConfig.setHealthy(true);

        Method getDbMethod = Datastore.class.getMethod("getDatabase");
        try {
            handler.invoke(null, getDbMethod, null);
            Assert.fail("Expected MongoCommandException");
        } catch (Throwable t) {
            Assert.assertSame(ex, t);
        }
        Assert.assertTrue(AbstractIgniteDAOMongoConfig.healthy);
    }
}