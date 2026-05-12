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

import com.mongodb.MongoCommandException;
import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.internal.MorphiaCursor;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.Document;
import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.nosqldao.QueryTranslator;
import org.eclipse.ecsp.nosqldao.UpdatesTranslator;
import org.eclipse.ecsp.nosqldao.ecall.ECallDAOMongoImpl;
import org.eclipse.ecsp.nosqldao.ecall.ECallEvent;
import org.eclipse.ecsp.nosqldao.ecall.MockTestDAOMongoImpl;
import org.eclipse.ecsp.nosqldao.ecall.MockTestEvent;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;

/**
 * Test class for IgniteBaseDAOMongoImpl.
 */
public class IgniteBaseDAOMongoImplMockTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private MockTestDAOMongoImpl testDAOMongoImpl;

    @InjectMocks
    private ECallDAOMongoImpl testEcallDAOMongoImpl;

    @Mock
    private Datastore ds;

    @Mock
    private MongoCollection mongoCollection;

    @Mock
    private MongoDatabase mongoDatabase;

    @Mock
    private MongoNamespace namespace;

    @Mock
    private QueryTranslator queryTranslator;

    @Mock
    private Query query;

    @Mock
    private UpdatesTranslator<UpdateOperations<MockTestEvent>> updatesTranslator;

    private String collection;

    /**
     * Setup method.
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Manually inject queryTranslator mock using reflection for Java 25 compatibility
        Field queryTranslatorField = testDAOMongoImpl.getClass().getSuperclass().getDeclaredField("queryTranslator");
        queryTranslatorField.setAccessible(true);
        queryTranslatorField.set(testDAOMongoImpl, queryTranslator);
        
        collection = testDAOMongoImpl.getOverridingCollectionName();
        Mockito.when(ds.getCollection(Mockito.any())).thenReturn(mongoCollection);
        Mockito.when(mongoCollection.getNamespace()).thenReturn(namespace);
        Mockito.when(namespace.getCollectionName()).thenReturn(collection);
        Mockito.when(ds.getDatabase()).thenReturn(mongoDatabase);
    }

    @Test
    public void testFindAll() {
        Query<MockTestEvent> query = (Query<MockTestEvent>) Mockito.mock(Query.class);
        Mockito.when(ds.find(collection, MockTestEvent.class)).thenReturn(query);
        MorphiaCursor<MockTestEvent> morphiaCursor = (MorphiaCursor<MockTestEvent>) Mockito.mock(MorphiaCursor.class);
        Mockito.when(query.iterator()).thenReturn(morphiaCursor);
        testDAOMongoImpl.findAll();
        Mockito.verify(ds, Mockito.times(1)).find(collection, MockTestEvent.class);
    }

    @Test
    public void testSave() {
        ECallEvent event = new ECallEvent();
        testEcallDAOMongoImpl.save(event);
        Mockito.verify(ds, Mockito.times(1)).save(event);
    }

    @Test
    public void testSaveAll() {
        ECallEvent event = new ECallEvent();
        ECallEvent event2 = new ECallEvent();
        testEcallDAOMongoImpl.saveAll(event, event2);
        Mockito.verify(ds, Mockito.times(1)).save(event);
        Mockito.verify(ds, Mockito.times(1)).save(event2);
    }

    @Test
    public void testCountByQuery() {
        Query<MockTestEvent> query = Mockito.mock(Query.class);
        Mockito.when(query.count())
                .thenReturn(1L);

        IgniteCriteria igniteCriteria = new IgniteCriteria("id", Operator.EQ, "id1");
        IgniteCriteriaGroup igniteCriteriaGroup = new IgniteCriteriaGroup(igniteCriteria);
        IgniteQuery igniteQuery = new IgniteQuery(igniteCriteriaGroup);

        Mockito.when(queryTranslator.translate(eq(igniteQuery), eq(Optional.ofNullable(collection))))
                .thenReturn(query);

        long count = testDAOMongoImpl.countByQuery(igniteQuery);
        Assert.assertEquals(Long.valueOf(1), Long.valueOf(count));
        Mockito.verify(query, Mockito.times(1)).count();
    }

    @Test
    public void testUpdateE() {
        ECallEvent event = new ECallEvent();
        testEcallDAOMongoImpl.update(event);
        Mockito.verify(ds, Mockito.times(1)).save(event);
    }

    @Test
    public void testUpdateAll() {
        ECallEvent event = new ECallEvent();
        ECallEvent event2 = new ECallEvent();
        testEcallDAOMongoImpl.updateAll(event, event2);
        Mockito.verify(ds, Mockito.times(1)).save(event);
        Mockito.verify(ds, Mockito.times(1)).save(event2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDelete() {
        MockTestEvent event = new MockTestEvent();
        testDAOMongoImpl.delete(event);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDistinctWhenCustomCollectionIsSet() {
        IgniteQuery iq = new IgniteQuery();
        testDAOMongoImpl.distinct(iq, "field");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDistinct() {
        IgniteQuery igniteQuery = new IgniteQuery();
        testDAOMongoImpl.distinct(igniteQuery, "id");
    }

    @Test
    public void testSafeCreateCollection_SwallowsNamespaceExists() throws Exception {
        // Arrange: create a real MongoCommandException with code 48 (NamespaceExists)
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NumericConstants.FORTY_EIGHT))
                .append("errmsg", new BsonString("NamespaceExists"));
        MongoCommandException namespaceExists = new MongoCommandException(
            response, new ServerAddress("localhost", NumericConstants.MONGO_HOST));
        Mockito.doThrow(namespaceExists).when(mongoDatabase).createCollection("testColl");

        // Act: invoke private method via reflection
        Method m = testDAOMongoImpl.getClass().getSuperclass().getDeclaredMethod("safeCreateCollection", String.class);
        m.setAccessible(true);
        m.invoke(testDAOMongoImpl, "testColl");

        // Assert: method completed without exception and createCollection was called once
        Mockito.verify(mongoDatabase, Mockito.times(1)).createCollection("testColl");
    }

    @Test
    public void testSafeCreateCollection_CreatesWhenAbsent() throws Exception {
        // Arrange: default behavior (no exception)

        // Act
        Method m = testDAOMongoImpl.getClass().getSuperclass().getDeclaredMethod("safeCreateCollection", String.class);
        m.setAccessible(true);
        m.invoke(testDAOMongoImpl, "newColl");

        // Assert
        Mockito.verify(mongoDatabase, Mockito.times(1)).createCollection("newColl");
    }

    @Test
    public void testBackoff_SleepsProportionally() throws Exception {
        // Arrange: set base backoff to 20ms
        Field f = testDAOMongoImpl.getClass().getSuperclass().getDeclaredField("indexEnsureBackoffBaseMs");
        f.setAccessible(true);
        f.setLong(testDAOMongoImpl, NumericConstants.LONG_TWENTY);

        Method backoff = testDAOMongoImpl.getClass().getSuperclass().getDeclaredMethod("backoff", int.class);
        backoff.setAccessible(true);

        long start = System.nanoTime();
        // Act: attempt=2 => expected sleep ~40ms
        backoff.invoke(testDAOMongoImpl, NumericConstants.TWO);
        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        // Assert: Allow some jitter; ensure at least ~30ms (less than 40 to avoid flakiness)
        Assert.assertTrue("Backoff did not sleep long enough: " 
            + elapsedMs + "ms", elapsedMs >= NumericConstants.THIRTY);
    }

    @Test
    public void testEnsureIndexesWithRetry_ThrowsOnNonRetryable() throws Exception {
        // Arrange: non-retryable MongoCommandException (e.g., code 2)
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NumericConstants.TWO))
                .append("errmsg", new BsonString("SomeNonRetryableError"));
        final MongoCommandException nonRetryable = new MongoCommandException(
            response, new ServerAddress("localhost", NumericConstants.MONGO_HOST));

        Runnable indexCreationAction = () -> { 
            throw nonRetryable; 
        };

        Method m = testDAOMongoImpl.getClass().getSuperclass().getDeclaredMethod(
            "ensureIndexesWithRetry", String.class, Runnable.class);
        m.setAccessible(true);

        try {
            m.invoke(testDAOMongoImpl, "nonRetryColl", indexCreationAction);
            Assert.fail("Expected MongoCommandException to be thrown");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            Assert.assertTrue(cause instanceof MongoCommandException);
            Assert.assertEquals(NumericConstants.TWO, ((MongoCommandException) cause).getErrorCode());
        }

        // Verify no collection creation for non-retryable errors
        Mockito.verify(mongoDatabase, Mockito.never()).createCollection(Mockito.anyString());
    }

    @Test
    public void testEnsureIndexesWithRetry_ExhaustsRetriesThenThrows() throws Exception {
        // Arrange: retryable error code 26; force small backoff and 2 attempts
        Field attemptsField = testDAOMongoImpl.getClass().getSuperclass().getDeclaredField(
            "indexEnsureMaxAttempts");
        attemptsField.setAccessible(true);
        attemptsField.setInt(testDAOMongoImpl, NumericConstants.TWO);

        Field backoffField = testDAOMongoImpl.getClass().getSuperclass().getDeclaredField(
            "indexEnsureBackoffBaseMs");
        backoffField.setAccessible(true);
        backoffField.setLong(testDAOMongoImpl, NumericConstants.LONG_ONE);

        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NumericConstants.TWENTY_SIX))
                .append("errmsg", new BsonString("NamespaceNotFound"));
        final MongoCommandException retryable = new MongoCommandException(
            response, new ServerAddress("localhost", NumericConstants.MONGO_HOST));

        Runnable indexCreationAction = () -> { 
            throw retryable; 
        };

        Method m = testDAOMongoImpl.getClass().getSuperclass().getDeclaredMethod(
            "ensureIndexesWithRetry", String.class, Runnable.class);
        m.setAccessible(true);

        try {
            m.invoke(testDAOMongoImpl, "retryColl", indexCreationAction);
            Assert.fail("Expected MongoCommandException to be thrown after exhausting retries");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            Assert.assertTrue(cause instanceof MongoCommandException);
            Assert.assertEquals(NumericConstants.TWENTY_SIX, ((MongoCommandException) cause).getErrorCode());
        }

        // Verify collection creation was attempted once due to retry path
        Mockito.verify(mongoDatabase, Mockito.atLeastOnce()).createCollection("retryColl");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testCreateIndexesWithRetryForOverride_HandlesIndexOptionsConflict() throws Exception {
        // Arrange: Create IndexOptionsConflict MongoCommandException (error code 85)
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NumericConstants.EIGHTY_FIVE))
                .append("codeName", new BsonString("IndexOptionsConflict"))
                .append("errmsg", new BsonString("Index already exists with different options"));
        MongoCommandException indexOptionsConflict = new MongoCommandException(
            response, new ServerAddress("localhost", NumericConstants.MONGO_HOST));

        // Mock the collection returned for the override collection name
        MongoCollection mockCollection = Mockito.mock(MongoCollection.class);
        Mockito.when(mongoDatabase.getCollection(Mockito.eq("testOverrideCollection"), Mockito.any(Class.class)))
               .thenReturn(mockCollection);

        // Mock createIndex to throw IndexOptionsConflict
        Mockito.doThrow(indexOptionsConflict)
               .when(mockCollection)
               .createIndex(Mockito.any(Document.class), Mockito.any(com.mongodb.client.model.IndexOptions.class));

        // Act: invoke the private method via reflection with new signature
        Method m = testDAOMongoImpl.getClass().getSuperclass()
                                  .getDeclaredMethod("createIndexesWithRetryForOverride", String.class);
        m.setAccessible(true);

        // This should not throw an exception - it should handle the IndexOptionsConflict gracefully
        m.invoke(testDAOMongoImpl, "testOverrideCollection");

        // Verify that createIndex was called once
        Mockito.verify(mockCollection, Mockito.times(1))
               .createIndex(Mockito.any(Document.class), Mockito.any(com.mongodb.client.model.IndexOptions.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testCreateIndexesWithRetryForOverride_ThrowsOnOtherExceptions() throws Exception {
        // Arrange: Create a different MongoCommandException (error code 2)
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonDouble(0.0))
                .append("code", new BsonInt32(NumericConstants.TWO))
                .append("codeName", new BsonString("BadValue"))
                .append("errmsg", new BsonString("Some other error"));
        MongoCommandException otherException = new MongoCommandException(
            response, new ServerAddress("localhost", NumericConstants.MONGO_HOST));

        // Mock the collection returned for the override collection name
        MongoCollection mockCollection = Mockito.mock(MongoCollection.class);
        Mockito.when(mongoDatabase.getCollection(Mockito.eq("testOverrideCollection"), Mockito.any(Class.class)))
               .thenReturn(mockCollection);

        // Configure the mock to throw the exception
        Mockito.doThrow(otherException)
               .when(mockCollection)
               .createIndex(Mockito.any(Document.class), Mockito.any(com.mongodb.client.model.IndexOptions.class));

        // Act & Assert: invoke the private method via reflection and expect exception
        Method m = testDAOMongoImpl.getClass().getSuperclass()
                                  .getDeclaredMethod("createIndexesWithRetryForOverride", String.class);
        m.setAccessible(true);

        try {
            m.invoke(testDAOMongoImpl, "testOverrideCollection");
            Assert.fail("Expected MongoCommandException to be thrown for non-IndexOptionsConflict errors");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            Assert.assertTrue(cause instanceof MongoCommandException);
            Assert.assertEquals(NumericConstants.TWO, ((MongoCommandException) cause).getErrorCode());
        }

        // Verify that createIndex was called once
        Mockito.verify(mockCollection, Mockito.times(1))
               .createIndex(Mockito.any(Document.class), Mockito.any(com.mongodb.client.model.IndexOptions.class));
    }

}
