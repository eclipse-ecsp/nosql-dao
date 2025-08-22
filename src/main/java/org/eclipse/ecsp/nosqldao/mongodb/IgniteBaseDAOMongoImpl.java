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

import com.google.common.reflect.TypeToken;
import com.mongodb.MongoCommandException;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.AdvancedDatastore;
import dev.morphia.DeleteOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.builders.IndexHelper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.ecsp.diagnostic.DiagnosticData;
import org.eclipse.ecsp.diagnostic.DiagnosticResult;
import org.eclipse.ecsp.entities.AuditableIgniteEntity;
import org.eclipse.ecsp.entities.IgniteEntity;
import org.eclipse.ecsp.nosqldao.IgniteBaseDAO;
import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgnitePagingInfoResponse;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.MongoDiagnosticReporterImpl;
import org.eclipse.ecsp.nosqldao.NoSqlDatabaseType;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.nosqldao.QueryTranslator;
import org.eclipse.ecsp.nosqldao.Updates;
import org.eclipse.ecsp.nosqldao.UpdatesTranslator;
import org.eclipse.ecsp.nosqldao.utils.Constants;
import org.eclipse.ecsp.nosqldao.utils.MetricsUtil;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.eclipse.ecsp.nosqldao.utils.PropertyNames;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.eclipse.ecsp.utils.metrics.GenericIgniteHistogram;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract base class for MongoDB DAO implementation.
 * This class provides common functionality for MongoDB operations and implements the IgniteBaseDAO interface.
 *
 * @param <K> the type of the primary key
 * @param <E> the type of the entity extending IgniteEntity
 */
@SuppressWarnings("checkstyle:MatchXpath")
public abstract class IgniteBaseDAOMongoImpl<K, E extends IgniteEntity> implements IgniteBaseDAO<K, E> {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(IgniteBaseDAOMongoImpl.class);

    private static final String LAST_UPDATED_TIME = "lastUpdatedTime";
    private static volatile GenericIgniteHistogram requestLatencyHisto;
    private static volatile Counter requestCounter;
    private static volatile Gauge requestGauge;
    private static volatile boolean metricsInitialized = false;
    private final boolean isAuditable;

    /**
     * The service where this DAO library is embedded.
     */
    @Value("${service.name}")
    protected String serviceName;

    @Autowired
    MongoDiagnosticReporterImpl mongoDiagnosticReporterImpl;

    private static final String FALSE = "false";
    private static final String EXCEPTION_MESSAGE = "Exception while accessing a field. Exception is: {}";
    private static final String FIELD_UPDATED_WITH_VALUE = "Field: {} updated with new value: {}";

    /**
     * The Mongo datastore.
     */
    @SuppressWarnings("removal")
    @Autowired
    private AdvancedDatastore mongoDatastore;

    /**
     * The class type of the entity.
     */
    private Class<E> entityClass;

    /**
     * The query translator for translating Ignite queries.
     */
    private QueryTranslator<Query<E>> queryTranslator;

    /**
     * The updates translator for translating Ignite updates.
     */
    private UpdatesTranslator<List<UpdateOperator>> updatesTranslator;

    /**
     * Indicates whether the Mongo diagnostic reporter is enabled.
     */
    @Value("${" + PropertyNames.MONGO_DIAGNOSTIC_REPORTER_ENABLED + ": false }")
    private boolean diagnosticMongoReporterEnabled;

    /**
     * The histogram buckets for Prometheus metrics.
     */
    @Value("#{'${prometheus.dao.latency.histogram.buckets:0.005, 0.010, "
            + "0.015, 0.020, 0.025, 0.030, 0.080, 0.1, 0.2, 0.3}'.split(',')}")
    private double[] histogramBuckets;

    /**
     * Indicates whether Prometheus is enabled.
     */
    @Value("${" + PropertyNames.ENABLE_PROMETHEUS + "}")
    private boolean prometheusEnabled;

    /**
     * Indicates whether DAO metrics are enabled.
     */
    @Value("${" + PropertyNames.DAO_METRICS_ENABLED + ": true }")
    private boolean daoMetricsEnabled;

    /**
     * The maximum number of attempts to ensure the DocumentDB index.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_INDEX_ENSURE_MAX_ATTEMPTS + ":2}")
    private int indexEnsureMaxAttempts;

    /**
     * The backoff time in milliseconds for ensuring the DocumentDB index.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_INDEX_ENSURE_BACKOFF_BASE_MS + ":100}")
    private long indexEnsureBackoffBaseMs;

    /**
     * The name of the entity class.
     */
    private String entityClassName;

    /**
     * The list of MongoDB shard keys.
     */
    @Value("${mongodb.collection.shardkey.map:#{null}}")
    private String mongoShardKeyList;

    /**
     * The map of shard keys.
     */
    private Map<String, List<String>> shardKeyMap;

    /**
     * The NoSQL database type.
     */
    protected NoSqlDatabaseType noSqlDatabaseType;

    /**
     * Setter for reading property and assigning appropriate enumeration for database type.
     * @param noSqlDatabaseTypeStr : String
     */
    @Autowired
    public void setNoSqlDatabaseType(@Value("${" + PropertyNames.NO_SQL_DATABASE_TYPE + ":}")
                                     String noSqlDatabaseTypeStr) {
        if (StringUtils.isBlank(noSqlDatabaseTypeStr)) {
            noSqlDatabaseType = NoSqlDatabaseType.MONGODB;
        } else {
            noSqlDatabaseType = NoSqlDatabaseType.valueOf(noSqlDatabaseTypeStr.toUpperCase());
        }
    }

    /**
     * Instantiates a new Ignite base DAO Mongo.
     */
    @SuppressWarnings("unchecked")
    protected IgniteBaseDAOMongoImpl() {
        @SuppressWarnings("rawtypes")
        TypeToken<? extends IgniteBaseDAOMongoImpl> typeToken = TypeToken.of(getClass());
        Type superclassType = typeToken.getSupertype(IgniteBaseDAOMongoImpl.class).getType();
        ParameterizedType pt = (ParameterizedType) superclassType;
        this.entityClass = (Class<E>) pt.getActualTypeArguments()[1];
        this.entityClassName = this.entityClass.getSimpleName();
        this.isAuditable = this.isAuditableEntity(this.entityClass);
        LOGGER.debug("entity class:{}, isAuditable:{}", this.entityClass, this.isAuditable);
    }

    /**
     * Gets non-static fields from the specified class.
     *
     * @param clazz the class to get fields from
     * @return an array of non-static fields
     */
    public static Field[] getNonStaticsFieldsFrom(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).filter(f ->
                !Modifier.isStatic(f.getModifiers())).toList().toArray(new Field[0]);
    }

    /**
     * Recursively finds whether the entity is extending AuditableIgniteEntity interface or not.
     *
     * @param source the class to check for AuditableIgniteEntity interface
     * @return true if the class or any of its superclasses implement AuditableIgniteEntity, false otherwise
     */
    private boolean isAuditableEntity(Class<?> source) {
        if (null != source.getInterfaces() && Arrays.asList(source.getInterfaces())
                .contains(AuditableIgniteEntity.class)) {
            return true;
        }
        Class<?> superClass = source.getSuperclass();
        if (source.getSuperclass() != null) {
            return isAuditableEntity(superClass);
        }
        return false;
    }

    /**
     * Initializes the DAO.
     */
    @PostConstruct
    public void init() {
        @SuppressWarnings("rawtypes")
        MongoCollection collection = null;
        queryTranslator = new QueryTranslatorMorphiaImpl<>(mongoDatastore, entityClass);
        updatesTranslator = new UpdatesTranslatorMorphiaImpl();
        String overridingCollection = getOverridingCollectionName();
        if (StringUtils.isEmpty(overridingCollection)) {
            if (noSqlDatabaseType == NoSqlDatabaseType.DOCUMENTDB) {
                ensureIndexesWithRetryForEntityClass();
            } else {
                mongoDatastore.ensureIndexes(entityClass);
            }
            if (diagnosticMongoReporterEnabled) {
                collection = mongoDatastore.getMapper().getCollection(entityClass);
            }
        } else {
            EntityModel model = mongoDatastore.getMapper().getEntityModel(entityClass);
            IndexHelper indexHelper = new IndexHelper(mongoDatastore.getMapper());
            if (noSqlDatabaseType == NoSqlDatabaseType.DOCUMENTDB) {
                ensureIndexesWithRetryForOverride(indexHelper, model, overridingCollection);
            } else {
                indexHelper.createIndex(
                    mongoDatastore.getDatabase().getCollection(overridingCollection, entityClass), model);
            }
            if (diagnosticMongoReporterEnabled) {
                collection = mongoDatastore.getDatabase().getCollection(overridingCollection);
            }
        }

        if (diagnosticMongoReporterEnabled) {
            boolean isIndexed = false;
            DiagnosticData mongoDiagnosticReport = new DiagnosticData();
            String collectionName = collection.getNamespace().getCollectionName();
            String indexedPropertyName = new StringBuilder().append(collectionName)
                    .append(".").append(PropertyNames.INDEXED).toString();
            String shardedPropertyName = new StringBuilder().append(collectionName)
                    .append(".").append(PropertyNames.SHARDED).toString();
            mongoDiagnosticReport.put(shardedPropertyName, DiagnosticResult.FAIL);
            mongoDiagnosticReport.put(indexedPropertyName, DiagnosticResult.FAIL);

            if (collectionExists(collectionName)) {
                createMongoDiagnosticReport(collection, isIndexed, mongoDiagnosticReport,
                        collectionName, indexedPropertyName, shardedPropertyName);
            }
            mongoDiagnosticReporterImpl.put(mongoDiagnosticReport);
        }
        initializeMetricsObjects();
        loadShardKeys();
    }

    /**
     * Create collection and swallow NamespaceExists in concurrent starts.
     */
    private void safeCreateCollection(String collectionName) {
        try {
            mongoDatastore.getDatabase().createCollection(collectionName);
            LOGGER.info("Created collection: {}", collectionName);
        } catch (MongoCommandException mce) {
            // 48 = NamespaceExists
            if (mce.getErrorCode() == NumericConstants.FORTY_EIGHT 
                    || String.valueOf(mce.getErrorCode()).equals(String.valueOf(NumericConstants.FORTY_EIGHT))) {
                LOGGER.info("Collection {} already exists (race); continuing", collectionName);
            } else {
                throw mce;
            }
        }
    }

    /**
     * Common retry helper for ensuring/creating indexes that may race with collection creation.
     * Retries on Mongo error codes 26 (NamespaceNotFound) and 85 (implicit creation not supported).
     *
     * @param collectionName the name of the collection to create if missing
     * @param indexCreationAction the action that creates indexes
     */
    private void ensureIndexesWithRetry(String collectionName, Runnable indexCreationAction) {
        final int MAX_ATTEMPTS = Math.max(1, indexEnsureMaxAttempts);
        int attempt = 0;
        while (true) {
            try {
                indexCreationAction.run();
                return;
            } catch (MongoCommandException mce) {
                int code = mce.getErrorCode();
                boolean retryable = code == NumericConstants.TWENTY_SIX || code == NumericConstants.EIGHTY_FIVE;
                if (retryable && attempt < MAX_ATTEMPTS - 1) {
                    attempt++;
                    LOGGER.warn("Index creation failed for {} with code {}; "
                        + "creating collection and retrying (attempt {}/{})",
                        collectionName, code, attempt, MAX_ATTEMPTS);
                    safeCreateCollection(collectionName);
                    backoff(attempt);
                } else {
                    throw mce;
                }
            }
        }
    }

    /**
     * Ensure indexes for entity class with retry if collection isn't present yet.
     * Concurrency-safe ensure indexes with catch-and-retry if collection creation races across pods.
     */
    private void ensureIndexesWithRetryForEntityClass() {
        MongoCollection<E> coll = mongoDatastore.getMapper().getCollection(entityClass);
        String collName = coll.getNamespace().getCollectionName();
        ensureIndexesWithRetry(collName, () -> mongoDatastore.ensureIndexes(entityClass));
    }

    /**
     * Ensure indexes for overriding collection with retry if collection isn't present yet.
     *
     * @param indexHelper the index helper to create indexes
     * @param model the entity model
     * @param overridingCollection the name of the overriding collection
     */
    private void ensureIndexesWithRetryForOverride(IndexHelper indexHelper, 
            EntityModel model, String overridingCollection) {
        ensureIndexesWithRetry(
            overridingCollection,
            () -> indexHelper.createIndex(
                mongoDatastore.getDatabase().getCollection(overridingCollection, entityClass), model));
    }

    /**
     * Backoff strategy for retrying operations.
     * @param attempt the current attempt number
     */
    private void backoff(int attempt) {
        try {
            Thread.sleep(indexEnsureBackoffBaseMs * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates a MongoDB diagnostic report.
     *
     * @param collection the MongoDB collection
     * @param isIndexed whether the collection is indexed
     * @param mongoDiagnosticReport the diagnostic report
     * @param collectionName the name of the collection
     * @param indexedPropertyName the indexed property name
     * @param shardedPropertyName the sharded property name
     */
    private void createMongoDiagnosticReport(MongoCollection<?> collection, boolean isIndexed,
                                             DiagnosticData mongoDiagnosticReport, String collectionName,
                                             String indexedPropertyName, String shardedPropertyName) {
        boolean isSharded;
        ListIndexesIterable<Document> listIndexesIterable = collection.listIndexes();
        List<Document> indexes = listIndexesIterable.into(new ArrayList<>());
        Document stats = mongoDatastore.getDatabase().runCommand(new Document("collStats",
                collectionName));
        isSharded = ((Boolean) stats.get(PropertyNames.SHARDED) == null) ? false
                : ((Boolean) stats.get(PropertyNames.SHARDED)).booleanValue();
        if (isSharded) {
            mongoDiagnosticReport.put(shardedPropertyName, DiagnosticResult.PASS);
        } else {
            LOGGER.warn("Diagnostic warning - mongo collection : {} is not sharded.",
                    collectionName);
        }
        if ((isSharded && indexes.size() > NumericConstants.TWO)
                || (!isSharded && indexes.size() > 1)) {
            isIndexed = true;
        }
        if (isIndexed) {
            mongoDiagnosticReport.put(indexedPropertyName, DiagnosticResult.PASS);
        } else {
            StringBuilder indexInfo = new StringBuilder();
            indexes.forEach(index ->
                    indexInfo.append(index.get("name").toString()).append(",")
            );
            LOGGER.warn(
                    "Diagnostic warning - mongo collection : {}, doesn't contain index(s) "
                            + "other than default (_id) and/or shard index {} ",
                    collectionName, indexInfo.toString());
        }
    }

    /**
     * Loads the shard keys from the configuration.
     */
    private void loadShardKeys() {
        LOGGER.debug("Attempting to load shard key map from list {}", mongoShardKeyList);
        shardKeyMap = new HashMap<>();
        if (StringUtils.isNotEmpty(mongoShardKeyList)) {
            String[] sharedKeyNameList = mongoShardKeyList.split(",");
            for (String shardKeyName : sharedKeyNameList) {
                String[] shardKeydetails = shardKeyName.split(":");
                List<String> shardKeysList = Arrays.stream(shardKeyName.split(":"))
                        .collect(Collectors.toList());
                shardKeysList.remove(0);
                shardKeyMap.put(shardKeydetails[0], shardKeysList);
            }
            LOGGER.info("ShardKeyMap : {}, loaded from properties parsing ShardKeyList : {} ",
                    shardKeyMap.toString(), mongoShardKeyList);
        }
    }

    /**
     * Finds all entities.
     *
     * @return a list of all entities
     */
    @Override
    public List<E> findAll() {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    Query<E> query = null;
                    String collection = getOverridingCollectionName();
                    if (StringUtils.isNotEmpty(collection)) {
                        query = mongoDatastore.find(collection, entityClass);
                    } else {
                        query = mongoDatastore.find(entityClass);
                    }
                    return query.iterator().toList();
                }, () ->
                        new String[] {serviceName, Constants.OPERATION_TYPE_FIND_ALL,
                            entityClassName, FALSE, Constants.FULL_QUERY_NA}
        );
    }

    /**
     * Saves the specified entity.
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    @Override
    public E save(E entity) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    /*
                     * adding/updating LastUpdatedTime of entity - to be used for data
                     * retention
                     */
                    if (null != entity && isAuditable) {
                        ((AuditableIgniteEntity) entity).setLastUpdatedTime(LocalDateTime.now());
                    }

                    String collectionName;
                    String dynamicCollectionName = getOverridingCollectionName();
                    if (StringUtils.isNotEmpty(dynamicCollectionName)) {
                        collectionName = dynamicCollectionName;
                    } else {
                        collectionName = mongoDatastore.getMapper().getCollection(entityClass)
                                .getNamespace().getCollectionName();
                    }

                    if (isSharded(collectionName)) {
                        LOGGER.debug("Performing save operation for entity : {}, with shard "
                                + "key map populated", entity.toString());
                        executeSaveOperationForShardKeyCollection(collectionName, entity);
                    } else {
                        LOGGER.debug("No shard key map configured when saving entity : {}",
                                entity.toString());
                        if (StringUtils.isNotEmpty(dynamicCollectionName)) {
                            executeSaveOperationForDynamicCollectionName(dynamicCollectionName,
                                    entity);
                        } else {
                            mongoDatastore.save(entity);
                        }
                    }
                    return entity;
                }, () -> new String[]{serviceName, Constants.OPERATION_TYPE_SAVE,
                    entityClassName, FALSE, Constants.FULL_QUERY_NA}
        );
    }

    /**
     * Checks if the collection is sharded.
     *
     * @param collectionName the name of the collection
     * @return true if the collection is sharded, false otherwise
     */
    private boolean isSharded(String collectionName) {
        return null != shardKeyMap && !shardKeyMap.isEmpty()
                && null != shardKeyMap.get(collectionName) && !(shardKeyMap.get(collectionName)
                .isEmpty());
    }

    /**
     * Saves all the specified entities.
     *
     * @param entities the entities to save
     * @return a list of saved entities
     */
    @Override
    public List<E> saveAll(@SuppressWarnings("unchecked") E... entities) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    List<E> entitiesList = Arrays.asList(entities);
                    List<E> savedEntities = new ArrayList<>();
                    entitiesList.parallelStream().forEach(entity ->
                            savedEntities.add(save(entity))
                    );
                    return savedEntities;
                }, () ->
                        new String[] {serviceName, Constants.OPERATION_TYPE_SAVE,
                            entityClassName, FALSE, Constants.FULL_QUERY_NA}
        );
    }

    /**
     * Finds an entity by its ID.
     *
     * @param id the ID of the entity
     * @return the found entity, or null if not found
     */
    @Override
    public E findById(K id) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    String collection = getOverridingCollectionName();
                    if (StringUtils.isNotEmpty(collection)) {
                        return mongoDatastore.find(collection, entityClass).filter(Filters.eq(
                                Constants.ID_FILTER_CONSTANT, id)).first();
                    } else {
                        return mongoDatastore.find(entityClass).filter(Filters.eq(
                                Constants.ID_FILTER_CONSTANT, id)).first();
                    }
                }, () ->
                        new String[] {serviceName, Constants.OPERATION_TYPE_FIND,
                            entityClassName, FALSE, Constants.FULL_QUERY_NA}
        );
    }

    /**
     * Finds entities by their IDs.
     *
     * @param ids the IDs of the entities
     * @return a list of found entities
     */
    @Override
    public List<E> findByIds(@SuppressWarnings("unchecked") K... ids) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    List<E> result = new ArrayList<>();
                    String collection = getOverridingCollectionName();
                    if (StringUtils.isNotEmpty(collection)) {
                        List<K> idList = Arrays.asList(ids);
                        idList.parallelStream().forEach(id -> result.add(findById(id)));
                        return result;
                    } else {
                        Query<E> entities = mongoDatastore.find(entityClass).filter(Filters.in(
                                Constants.ID_FILTER_CONSTANT, Arrays.asList(ids)));
                        return entities.iterator().toList();
                    }
                }, () ->
                        new String[]{serviceName, Constants.OPERATION_TYPE_FIND,
                            entityClassName, FALSE, Constants.FULL_QUERY_NA}
        );
    }

    /**
     * Upserts an entity based on the specified query.
     *
     * @param igniteQuery the query to match
     * @param entity the entity to upsert
     * @return true if the entity was upserted, false otherwise
     */
    @Override
    public boolean upsert(IgniteQuery igniteQuery, E entity) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    Query<E> query = queryTranslator.translate(igniteQuery, Optional.ofNullable(
                            getOverridingCollectionName()));
                    LOGGER.debug("Executing upsert operation with the following query on mongoDB "
                            + ": {}", query);
                    /*
                     * adding/updating LastUpdatedTime of entity - to be used for data
                     * retention
                     */
                    if (null != entity && isAuditable) {
                        ((AuditableIgniteEntity) entity).setLastUpdatedTime(LocalDateTime.now());
                    }
                    String collectionName = getOverridingCollectionName();
                    MongoCollection<E> collection;
                    if (StringUtils.isNotEmpty(collectionName)) {
                        collection = mongoDatastore.getDatabase().getCollection(collectionName,
                                entityClass);
                    } else {
                        collection = mongoDatastore.getMapper().getCollection(entityClass);
                    }
                    UpdateResult updateResult = collection.replaceOne(query.toDocument(), entity,
                            new ReplaceOptions().upsert(true));
                    return updateResult.getModifiedCount() > 0 || updateResult.getUpsertedId() != null;
                }, () ->
                        new String[]{serviceName, Constants.OPERATION_TYPE_UPSERT,
                            entityClassName, FALSE, igniteQuery.toTemplatedQueryString()}
        );
    }

    /**
     * Finds entities based on the specified query.
     *
     * @param c the query to match
     * @return a list of found entities
     */
    @Override
    public List<E> find(IgniteQuery c) {
        int pageSize = c.getPageSize();
        int pageNumber = c.getPageNumber();
        var readPreference = c.getReadPreference() != null ? c.getReadPreference()
                : mongoDatastore.getDatabase().getReadPreference();

        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    Query<E> query = queryTranslator.translate(c, Optional
                            .ofNullable(getOverridingCollectionName()));
                    LOGGER.debug("Executing find operation with query {}, and pageNumber : {} "
                                    + ", pageSize  : {}, readPreference : {}",
                            query, pageNumber, pageSize, readPreference.getName());
                    if (pageNumber > 0 && pageSize > 0) {
                        int offset = (pageNumber - 1) * pageSize;
                        return query.iterator(queryTranslator.getFindOptions().skip(offset).limit(pageSize)
                                        .readPreference(readPreference))
                                .toList();
                    } else if (pageNumber == 0 && pageSize == 0) {
                        return query.iterator(queryTranslator.getFindOptions()).toList();
                    } else {
                        throw new IllegalArgumentException("Both pageSize and pageNumber should be set.");
                    }
                }, () ->
                        new String[]{serviceName, Constants.OPERATION_TYPE_FIND_QUERY,
                            entityClassName, String.valueOf(pageNumber > 0), c.toTemplatedQueryString()}
        );
    }

    /**
     * Finds entities with paging information based on the specified query.
     *
     * @param query the query to match
     * @return a response containing the found entities and paging information
     */
    @Override
    public IgnitePagingInfoResponse<E> findWithPagingInfo(IgniteQuery query) {
        long count = countByQuery(query);
        List<E> result = find(query);
        return new IgnitePagingInfoResponse<>(result, count);
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id the ID of the entity
     * @return true if the entity was deleted, false otherwise
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean deleteById(K id) {
        return deleteByIds(id) > 0;
    }

    /**
     * Deletes entities by their IDs.
     *
     * @param ids the IDs of the entities
     * @return the number of deleted entities
     */
    @SuppressWarnings({ "unchecked", "removal" })
    @Override
    public int deleteByIds(K... ids) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                DeleteResult result = null;
                int count = 0;
                String collection = getOverridingCollectionName();
                if (StringUtils.isNotEmpty(collection)) {
                    Arrays.asList(ids).parallelStream().forEach(id -> {
                        Query<?> filterByIdQuery = mongoDatastore.find(collection, entityClass)
                                .filter(Filters.eq(Constants.ID_FILTER_CONSTANT, id));
                        mongoDatastore.delete(filterByIdQuery, new DeleteOptions().multi(false));
                    });
                } else {
                    Query<?> filterByIdQuery = mongoDatastore.createQuery(entityClass)
                            .filter(Filters.in(Constants.ID_FILTER_CONSTANT, Arrays.asList(ids)));
                    result = mongoDatastore.delete(filterByIdQuery, new DeleteOptions().multi(true));
                    count = (int) result.getDeletedCount();
                }

                return count;
            }, () ->
                 new String[]{serviceName, Constants.OPERATION_TYPE_DELETE,
                     entityClassName, FALSE, Constants.FULL_QUERY_NA}
            );
    }

    /**
     * Deletes entities based on the specified query.
     *
     * @param igniteQuery the query to match
     * @return the number of deleted entities
     */
    @SuppressWarnings("removal")
    @Override
    public int deleteByQuery(IgniteQuery igniteQuery) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                Optional<String> collection = Optional.ofNullable(getOverridingCollectionName());
                Query<E> query = queryTranslator.translate(igniteQuery, collection);
                LOGGER.debug("Executing delete operation with the following query on mongoDB : {}",
                        query);
                return (int) mongoDatastore.delete(query, new DeleteOptions().multi(true))
                        .getDeletedCount();
            }, () ->
                 new String[]{serviceName, Constants.OPERATION_TYPE_DELETE_QUERY,
                     entityClassName, FALSE, igniteQuery.toTemplatedQueryString()}
            );
    }

    /**
     * Counts the number of entities that match the specified query.
     *
     * @param igniteQuery the query to match
     * @return the number of entities that match the query
     */
    @Override
    public long countByQuery(IgniteQuery igniteQuery) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                    requestCounter, requestGauge, () -> {
                Optional<String> collection = Optional.ofNullable(getOverridingCollectionName());

                Query<E> query = queryTranslator.translate(igniteQuery, collection);
                LOGGER.debug("Executing count operation with the following query on mongoDB : {}",
                        query);
                return query.count();
            }, () ->
                     new String[] {serviceName, Constants.OPERATION_TYPE_COUNT_QUERY,
                       entityClassName, FALSE, igniteQuery.toTemplatedQueryString()}
        );
    }

    /**
     * Retrieves an existing record by its ID and updates its fields with the values from the provided entity.
     *
     * @param entity the entity containing the updated values
     * @return true if the update was successful, false otherwise
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean getAndUpdate(E entity) {
        K id = null;
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            Field[] newEntityFields = clazz.getDeclaredFields();
            LOGGER.debug("New entity fields are {}", Arrays.toString(newEntityFields));
            for (Field field : newEntityFields) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    try {
                        id = (K) field.get(entity);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        LOGGER.error("Exception while accessing the entity annotated with @Id. "
                                + "Exception is: {}", e);
                    }
                    LOGGER.debug("Entity annotated with @Id is {}. Fetching record from database "
                            + "with this ID.", id);
                }
            }
            clazz = clazz.getSuperclass();
        }

        E existingRecord = findById(id);
        LOGGER.debug("Fetched record: {} from database with ID: {}", existingRecord != null
                ? existingRecord.toString() : "", id);

        clazz = entity.getClass();
        while (clazz != null) {
            Field[] newEntityFields = getNonStaticsFieldsFrom(clazz);
            for (Field f : newEntityFields) {
                updateEntityField(entity, clazz, existingRecord, f);
            }
            clazz = clazz.getSuperclass();
        }
        save(existingRecord);
        return true;
    }

    /**
     * Updates a field of the existing record with the value from the new entity.
     *
     * @param entity the new entity containing updated values
     * @param clazz the class of the entity
     * @param existingRecord the existing record to be updated
     * @param f the field to be updated
     */
    private void updateEntityField(E entity, Class<?> clazz, E existingRecord, Field f) {
        f.setAccessible(true);
        LOGGER.debug("Field: {} of class: {}", f.getName(), clazz.getCanonicalName());
        try {
            //if the new field is not null or primitive, if the field needs to be updated.
            if (f.get(entity) != null && !isFieldPrimitiveValueAndZero(f, entity)) {
                checkFieldExistsInSavedRecordAndUpdate(f, entity, existingRecord, clazz);
            }
        } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
            LOGGER.error(EXCEPTION_MESSAGE, e);
        }
    }
    
    /*
     * This method checks:
     *
     */
    /**
     * Check field exists in saved record and update.
     * If it is an object that exists in the record that is saved.
     * then update in the existing record.
     * else Add it to the existing record.
     *
     * @param f
     *         the f
     * @param entity
     *         the entity
     * @param existingRecord
     *         the existing record
     * @param clazz
     *         the clazz
     */
    @SuppressWarnings("unchecked")
    private void checkFieldExistsInSavedRecordAndUpdate(Field f, E entity, E existingRecord, Class<?> clazz) {
        try {
            //if it is an object that exists in the one that is saved
            if (f.get(entity) instanceof Map && existingRecord != null && f.get(existingRecord)
                    != null) {
                //scroll through the attributes
                for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) f.get(entity))
                        .entrySet()) {
                    //if the attribute exists in the one that is saved
                    if (((Map<Object, Object>) f.get(existingRecord)).get(entry.getKey()) != null) {
                        LOGGER.debug("Field: {} exists with value {} and will be updated with "
                                        + "value {}", f.getName(),
                                ((Map<Object, Object>) f.get(existingRecord)).get(entry.getKey()),
                                entry.getValue());
                        updateInExistingRecord(entry, existingRecord, f);
                    } else {
                        //if the attribute does not exist in the one that is saved
                        Field existingField = clazz.getDeclaredField(f.getName());
                        existingField.setAccessible(true);
                        Map<Object, Object> existingMap = ((Map<Object, Object>) existingField.get(
                                existingRecord));
                        existingMap.put(entry.getKey(), entry.getValue());
                        LOGGER.debug(FIELD_UPDATED_WITH_VALUE, f.getName(),
                                existingMap.toString());
                    }
                }
            } else {
                //if it is an object that does not exist in the one that is saved
                Field existingField = clazz.getDeclaredField(f.getName());
                existingField.setAccessible(true);
                existingField.set(existingRecord, f.get(entity));
                LOGGER.debug(FIELD_UPDATED_WITH_VALUE, f.getName(),
                        f.get(entity).toString());
            }
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
                | SecurityException e) {
            LOGGER.error(EXCEPTION_MESSAGE, e);
        }
    }

    /**
     * Update in existing record.
     *
     * @param entry
     *         the entry
     * @param existingRecord
     *         the existing record
     * @param f
     *         the f
     */
    @SuppressWarnings("unchecked")
    private void updateInExistingRecord(Map.Entry<Object, Object> entry, E existingRecord, Field f) {
        try {
            //if there's no mapping to a class.
            if (entry.getValue() instanceof Map) {
                Map<Object, Object> existingMap = ((Map<Object, Object>) f.get(existingRecord));
                for (Map.Entry<Object, Object> e : ((Map<Object, Object>) entry.getValue())
                        .entrySet()) {
                    ((Map<Object, Object>) existingMap.get(entry.getKey())).put(e.getKey(),
                            e.getValue());
                }
                LOGGER.debug(FIELD_UPDATED_WITH_VALUE, f.getName(),
                        existingMap.toString());
            } else {
                //if there is mapping to a class
                getAndUpdateInner(entry.getValue(), ((Map<Object, Object>) f.get(existingRecord))
                        .get(entry.getKey()));
            }
        } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
            LOGGER.error(EXCEPTION_MESSAGE, e);
        }
    }

    /**
     * Gets and update inner.
     *
     * @param entity
     *         the entity
     * @param existingRecord
     *         the existing record
     */
    public void getAndUpdateInner(Object entity, Object existingRecord) {
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            Field[] newEntityFields = clazz.getDeclaredFields();
            for (Field f : newEntityFields) {
                f.setAccessible(true);
                LOGGER.debug("Field: {} of class: {}", f.getName(), clazz.getCanonicalName());
                try {
                    //if the new field is not null or primitive
                    if (f.get(entity) != null && !isInnerFieldPrimitiveValueAndZero(f, entity)) {
                        getAndUpdateFieldValue(entity, existingRecord, clazz, f);
                    }
                } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
                         | SecurityException e) {
                    LOGGER.error(EXCEPTION_MESSAGE, e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Gets and update field value.
     *
     * @param entity
     *         the entity
     * @param existingRecord
     *         the existing record
     * @param clazz
     *         the clazz
     * @param f
     *         the f
     */
    @SuppressWarnings("unchecked")
    private void getAndUpdateFieldValue(Object entity, Object existingRecord, Class<?> clazz,
             Field f)
            throws IllegalAccessException, NoSuchFieldException {
        //if it is an object that exists in the one that is saved
        if (f.get(entity) instanceof Map && existingRecord != null && f.get(existingRecord) != null) {
            //scroll through the attributes
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) f.get(entity))
                    .entrySet()) {
                getAndUpdateInnerHelper(entry, f, existingRecord, clazz);
            }
            //if it is an Object
        } else if (f.get(entity).getClass().isAnnotationPresent(
                dev.morphia.annotations.Entity.class)
                && existingRecord != null && f.get(
                existingRecord) != null) {
            //if there is mapping to a class
            getAndUpdateInner(f.get(entity), f.get(existingRecord));
        } else {
            //if it is an object that does not exist in the one that is saved
            Field existingField = clazz.getDeclaredField(f.getName());
            existingField.setAccessible(true);
            existingField.set(existingRecord, f.get(entity));
            LOGGER.debug(FIELD_UPDATED_WITH_VALUE, f.getName(), f.get(entity)
                    .toString());
        }
    }

    /**
     * Gets and update inner helper.
     *
     * @param entry
     *         the entry
     * @param f
     *         the f
     * @param existingRecord
     *         the existing record
     * @param clazz
     *         the clazz
     */
    @SuppressWarnings("unchecked")
    private void getAndUpdateInnerHelper(Map.Entry<Object, Object> entry, Field f,
             Object existingRecord, Class<?> clazz) {
        try {
            //if the attribute exists in the one that is saved
            if ((( Map<Object, Object>) f.get(existingRecord)).get(entry.getKey()) != null) {
                LOGGER.debug("Field: {} exists with value {} and will be updated with value {}", f.getName(),
                      ((Map<Object, Object>) f.get(existingRecord)).get(entry.getKey()), entry.getValue());
                getAndUpdateInner(entry.getValue(), ((Map<Object, Object>) f.get(existingRecord)).get(entry.getKey()));
            } else {
                //if the attribute does not exist in the one that is saved
                Field existingField = clazz.getDeclaredField(f.getName());
                existingField.setAccessible(true);
                Map<Object, Object> existingMap = ((Map<Object, Object>) existingField.get(existingRecord));
                existingMap.put(entry.getKey(), entry.getValue());
                LOGGER.debug(FIELD_UPDATED_WITH_VALUE, f.getName(), existingMap.toString());
            }
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
                | SecurityException e) {
            LOGGER.error(EXCEPTION_MESSAGE, e);
        }
    }

    /**
     * Checks if the field is a primitive value and zero.
     *
     * @param field the field to check
     * @param entity the entity to check
     * @return true if the field is a primitive value and zero, false otherwise
     * @throws IllegalAccessException if the field is inaccessible
     */
    private boolean isInnerFieldPrimitiveValueAndZero(Field field, Object entity)
            throws IllegalAccessException {
        Class<? extends Object> fieldType = field.getType();
        if (fieldType.isPrimitive()) {
            return Double.parseDouble(field.get(entity).toString()) <= 0.0d;
        }
        return false;
    }

    /**
     * Checks if the field is a primitive value and zero.
     *
     * @param field the field to check
     * @param entity the entity to check
     * @return true if the field is a primitive value and zero, false otherwise
     * @throws IllegalAccessException if the field is inaccessible
     */
    private boolean isFieldPrimitiveValueAndZero(Field field, E entity)
            throws IllegalAccessException {
        Class<? extends Object> fieldType = field.getType();
        if (fieldType.isPrimitive()) {
            return Double.parseDouble(field.get(entity).toString()) <= 0.0d;
        }
        return false;
    }

    /**
     * Updates all the specified entities.
     *
     * @param entities the entities to update
     * @return an array of booleans indicating whether each entity was updated
     */
    @Override
    public boolean[] updateAll(@SuppressWarnings("unchecked") E... entities) {
        saveAll(entities);
        boolean[] results = new boolean[entities.length];
        Arrays.fill(results, true);
        return results;
    }

    /**
     * Deletes all entities.
     *
     * @return true if the entities were deleted, false otherwise
     */
    @SuppressWarnings("removal")
    @Override
    public boolean deleteAll() {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
            requestCounter, requestGauge, () -> {
                Query<E> query = null;
                String collection = getOverridingCollectionName();
                if (StringUtils.isNotEmpty(collection)) {
                    query = mongoDatastore.find(collection, entityClass);
                } else {
                    query = mongoDatastore.find(entityClass);
                }
                DeleteResult deleteResult = mongoDatastore.delete(query, new DeleteOptions()
                        .multi(true));
                return deleteResult.getDeletedCount() > 0;
            }, () ->
                 new String[] {serviceName, Constants.OPERATION_TYPE_DELETE,
                     entityClassName, FALSE, Constants.FULL_QUERY_NA }
        );
    }

    /**
     * Updates the specified entity.
     *
     * @param entity the entity to update
     * @return true if the entity was updated, false otherwise
     */
    @Override
    public boolean update(E entity) {
        save(entity);
        return true;
    }

    /**
     * Updates entities based on the specified query and updates.
     *
     * @param c the query to match
     * @param updates the updates to apply
     * @return true if the entities were updated, false otherwise
     */
    @Override
    public boolean update(IgniteQuery c, Updates updates) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                Optional<String> collection = Optional.ofNullable(getOverridingCollectionName());
                Query<E> query = queryTranslator.translate(c, collection);
                LOGGER.debug("Executing update operation with the following query on mongoDB : {}",
                        query);
                MongoCollection<E> mongoCollection = null;
                if (collection.isPresent()) {
                    mongoCollection = mongoDatastore.getDatabase().getCollection(collection.get(),
                            entityClass);
                } else {
                    mongoCollection = mongoDatastore.getMapper().getCollection(entityClass);
                }
                List<UpdateOperator> updateOperations = updatesTranslator.translate(updates,
                        collection);
                /*
                 * adding/updating LastUpdatedTime of entity - to be used for data
                 * retention
                 */
                Bson bsonUpdates = com.mongodb.client.model.Updates.combine(query.update(
                        updateOperations).toDocument(),
                        com.mongodb.client.model.Updates.set(LAST_UPDATED_TIME, LocalDateTime.now()));
                UpdateResult ur = mongoCollection.updateMany(query.toDocument(), bsonUpdates,
                        new UpdateOptions().multi(true).upsert(false));
                return ur.getModifiedCount() > 0;
            }, () ->
                 new String[] {serviceName, Constants.OPERATION_TYPE_UPDATE_QUERY,
                     entityClassName, FALSE, c.toTemplatedQueryString()}
            );
    }

    /**
     * Updates the specified entity.
     *
     * @param id the ID of the entity to update
     * @param updates the updates to apply
     * @return true if the entity was updated, false otherwise
     */
    @SuppressWarnings("removal")
    @Override
    public boolean update(K id, Updates updates) {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    Query<E> q = null;
                    String collection = getOverridingCollectionName();
                    MongoCollection<E> mongoCollection = null;
                    if (StringUtils.isNotEmpty(collection)) {
                        q = mongoDatastore.find(collection, entityClass).filter(Filters.eq(
                                Constants.ID_FILTER_CONSTANT, id)).disableValidation();
                        mongoCollection = mongoDatastore.getDatabase().getCollection(collection,
                                entityClass);
                    } else {
                        q = mongoDatastore.createQuery(entityClass).filter(Filters.eq(Constants
                                .ID_FILTER_CONSTANT, id)).disableValidation();
                        mongoCollection = mongoDatastore.getMapper().getCollection(entityClass);
                    }
                    List<UpdateOperator> updateOperations = updatesTranslator.translate(updates,
                            Optional.ofNullable(collection));
                    /*
                     * adding/updating LastUpdatedTime of entity - to be used for data
                     * retention
                     */
                    Bson bsonUpdates = com.mongodb.client.model.Updates.combine(q.update(updateOperations).toDocument(),
                            com.mongodb.client.model.Updates.set(LAST_UPDATED_TIME, LocalDateTime.now()));
                    UpdateResult ur = mongoCollection.updateMany(q.toDocument(), bsonUpdates, new UpdateOptions()
                            .multi(true).upsert(false));
                    return ur.getModifiedCount() > 0;
                }, () ->
                        new String[]{serviceName, Constants.OPERATION_TYPE_SAVE,
                            entityClassName, FALSE, Constants.FULL_QUERY_NA}
        );
    }

    /**
     * Removes all entities based on the specified query.
     *
     * @param c the query to match
     * @return true if the entities were removed, false otherwise
     */
    @Override
    public boolean removeAll(IgniteQuery c, Updates updates) {
        Optional<String> collection = Optional.ofNullable(getOverridingCollectionName());
        Query<E> query = queryTranslator.translate(c, collection);
        LOGGER.debug("Executing removeAll operation with the following query on mongoDB : {}",
                query);
        List<UpdateOperator> updateOperations = updatesTranslator
                .translate(updates, collection);
        UpdateResult ur = query.update(updateOperations).execute(new UpdateOptions().multi(true));
        return ur.getModifiedCount() > 0;
    }

    /**
     * Deletes the specified entity.
     * Delete of Entity E is not supported by advanced datastore for custom collection.
     * Hence, if a custom collection is set then it throws.
     * UnsupportedOperationException. Instead, use deleteById.
     *
     * @param entity the entity to delete
     * @return true if the entity was deleted, false otherwise
     */
    @Override
    public boolean delete(E entity) throws UnsupportedOperationException {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                DeleteResult deleteResult = null;
                String collection = getOverridingCollectionName();
                if (StringUtils.isNotEmpty(collection)) {
                    throw new UnsupportedOperationException("Delete entity is not supported "
                            + "for dynamic collection name.");
                } else {
                    deleteResult = mongoDatastore.delete(entity);
                }
                return deleteResult.getDeletedCount() > 0;
            }, () ->
                 new String[] {serviceName, Constants.OPERATION_TYPE_DELETE,
                     entityClassName, FALSE, Constants.FULL_QUERY_NA });
    }

    /**
     * Finds distinct values based on the specified query.
     * Distinct is not supported by advanced datastore for custom collection.
     * Hence, if a custom collection is set then it throws UnsupportedOperationException.
     * @param igniteQuery the query to match
     * @param field the field to find distinct values for
     * @return a list of distinct values
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<K> distinct(IgniteQuery igniteQuery, String field) throws
            UnsupportedOperationException {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                String collection = getOverridingCollectionName();
                if (StringUtils.isNotEmpty(collection)) {
                    throw new UnsupportedOperationException("Distinct is not supported for dynamic collection name.");
                } else {
                    MongoCollection<?> dbCollection = mongoDatastore.getMapper().getCollection(
                            entityClass);
                    Query<E> query = queryTranslator.translate(igniteQuery,
                            Optional.empty());
                    LOGGER.debug("Executing distinct operation with the following query on mongoDB : {}", query);
                    DistinctIterable<K> distinctDocs  = (DistinctIterable<K>) dbCollection.distinct(field,
                            query.toDocument()
                                    .toBsonDocument(), String.class);
                    MongoCursor<K> results = distinctDocs.iterator();
                    List<K> resultList = new ArrayList<>();
                    while (results.hasNext()) {
                        resultList.add(results.next());
                    }
                        return resultList;
                }
                }, () ->
                 new String[]{serviceName, Constants.OPERATION_TYPE_FIND_DISTINCT,
                     entityClassName, FALSE, igniteQuery.toTemplatedQueryString()}
            );
    }

    /**
     * Streams all entities as a Flux.
     *
     * @return a Flux containing all entities
     */
    @Override
    public Flux<E> streamFindAll() {
        return Flux.fromStream(this.findAll().stream());
    }

    /**
     * Streams entities based on the specified query.
     *
     * @param igniteQuery the query to match
     * @return a Flux containing the found entities
     */
    @Override
    public Flux<E> streamFind(IgniteQuery igniteQuery) {
        return Flux.fromStream(this.find(igniteQuery).stream());
    }

    /**
     * Counts all entities in the collection.
     *
     * @return the total number of entities
     */
    @SuppressWarnings("removal")
    @Override
    public long countAll() {
        return MetricsUtil.observeIfEnabled(metricsInitialized, requestLatencyHisto,
                requestCounter, requestGauge, () -> {
                    Query<E> query;
                    if (getOverridingCollectionName() != null) {
                        query = mongoDatastore.find(getOverridingCollectionName(),
                                entityClass).disableValidation();
                    } else {
                        query = mongoDatastore.createQuery(entityClass).disableValidation();
                    }
                    LOGGER.debug("Query is {}", query);
                    return query.count();
                }, () ->
                        new String[] {serviceName, Constants.OPERATION_TYPE_COUNT_ALL,
                            entityClassName, FALSE, Constants.FULL_QUERY_NA}
                    );
    }

    /**
     * Checks if a collection exists.
     *
     * @param collectionName the name of the collection
     * @return true if the collection exists, false otherwise
     */
    @Override
    public boolean collectionExists(String collectionName) {
        Set<String> collectionNames = mongoDatastore.getDatabase().listCollectionNames()
                .into(new HashSet<>());
        for (final String name : collectionNames) {
            if (name.equals(collectionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Initializes the metrics objects for tracking MongoDB calls.
     * This method creates and registers Prometheus metrics objects such as histograms, gauges, and counters
     * for monitoring the latency, request count, and other metrics related to MongoDB operations.
     * The metrics are only initialized if Prometheus and DAO metrics are enabled.
     */
    private void initializeMetricsObjects() {
        if (prometheusEnabled && daoMetricsEnabled) {
            LOGGER.info("Creating metrics objects for tracking mongodb calls");
            synchronized (IgniteBaseDAOMongoImpl.class) {
                if (requestLatencyHisto == null) {
                    requestLatencyHisto = new GenericIgniteHistogram(Constants.LATENCY_HISTO_NAME,
                            Constants.LATENCY_HISTO_HELP_STR, histogramBuckets,
                            Constants.SVC, Constants.OPERATION_TYPE_LABEL,
                            Constants.ENTITY_LABEL, Constants.PAGINATION_LABEL,
                            Constants.FULL_QUERY_LABEL);
                }
                if (requestGauge == null) {
                    requestGauge = Gauge.build(Constants.REQ_GAUGE_NAME,
                                    Constants.REQ_GAUGE_HELP_STR)
                            .labelNames(Constants.SVC, Constants.OPERATION_TYPE_LABEL,
                                    Constants.ENTITY_LABEL, Constants.PAGINATION_LABEL,
                                    Constants.FULL_QUERY_LABEL)
                            .register(CollectorRegistry.defaultRegistry);
                }
                if (requestCounter == null) {
                    requestCounter = Counter.build(Constants.REQ_COUNTER_NAME,
                                     Constants.REQ_COUNTER_HELP_STR)
                            .labelNames(Constants.SVC, Constants.OPERATION_TYPE_LABEL,
                            Constants.ENTITY_LABEL, Constants.PAGINATION_LABEL,
                                    Constants.FULL_QUERY_LABEL)
                            .register(CollectorRegistry.defaultRegistry);
                }
                metricsInitialized = true;
            }
        }
    }

    /**
     * Executes the save operation for a collection with shard keys.
     * This method handles the save operation for collections that have shard keys defined.
     * It constructs the appropriate query and performs an upsert operation.
     *
     * @param collection the name of the collection
     * @param entity the entity to save
     */
    private void executeSaveOperationForShardKeyCollection(String collection, E entity) {

        var igniteCriteriaGroup = new IgniteCriteriaGroup();
        var isIdValueSet = false;
        List<String> shardKeysList = shardKeyMap.get(collection);
        LOGGER.debug("Performing save operation on collection present in shard key map "
                        + ": {}, with following shard keys : {} ",
                collection, shardKeysList.toString());

        Object id = mongoDatastore.getMapper().findIdProperty(entity.getClass())
                .getValue(entity);
        if (id == null) {
            executeSaveOperationForDynamicCollectionName(collection, entity);
        } else {
            for (String shardKey : shardKeysList) {
                Object shardKeyValue = PropertyAccessorFactory.forDirectFieldAccess(entity)
                        .getPropertyValue(shardKey);
                var igniteCriteria = new IgniteCriteria(shardKey, Operator.EQ,
                        String.valueOf(shardKeyValue));
                igniteCriteriaGroup.and(igniteCriteria);
                var shardKeyDescriptor = PropertyAccessorFactory
                        .forDirectFieldAccess(entity).getPropertyTypeDescriptor(shardKey);
                if (null != shardKeyDescriptor && null != shardKeyDescriptor
                        .getAnnotation(Id.class)) {
                    isIdValueSet = true;
                }
            }
            // _id must be set in ignite criteria, if not mentioned as shard key for a collection in the defined map
            if (!isIdValueSet) {
                LOGGER.debug("ID field not set in shard key map for entity : {}, "
                                + "collection : {}. Adding {} to ignite criteria.",
                        entity.toString(), collection, Constants.ID_FILTER_CONSTANT);
                var igniteCriteria = new IgniteCriteria(Constants.ID_FILTER_CONSTANT,
                        Operator.EQ, id);
                igniteCriteriaGroup.and(igniteCriteria);
            }
            executeUpsertStatement(igniteCriteriaGroup, entity, collection);
        }
    }

    /**
     * Executes the save operation for a collection with a dynamic collection name.
     * This method handles the save operation for collections that do not have shard keys defined.
     * It constructs the appropriate query and performs an upsert operation.
     *
     * @param collection the name of the collection
     * @param entity the entity to save
     */
    private void executeSaveOperationForDynamicCollectionName(String collection, E entity) {
        var insertOneOptions = new InsertOneOptions();
        MongoCollection<E> collectionName = mongoDatastore.getDatabase().getCollection(
                collection, entityClass);
        Object id = mongoDatastore.getMapper().findIdProperty(entity.getClass()).getValue(entity);
        if (id != null) {
            LOGGER.info("Existing record found for entity : {}, in collection : {}. with id : {}. Updating record.",
                    entity.toString(), collectionName, id.toString());
            var replaceOptions = (new ReplaceOptions()).bypassDocumentValidation(
                    insertOneOptions.getBypassDocumentValidation())
                    .upsert(true);
            var filter = new Document(Constants.ID_FILTER_CONSTANT, id);
            insertOneOptions.prepare(collectionName).replaceOne(filter, entity, replaceOptions);
        } else {
            LOGGER.info("Inserting record in collection {}, entity : {}", collectionName,
                    entity.toString());
            insertOneOptions.prepare(collectionName).insertOne(entity, insertOneOptions.getOptions()).getInsertedId();
        }
        updateEntityId(entity);
    }

    /**
     * Executes an upsert statement for the specified entity and criteria group.
     *
     * @param igniteCriteriaGroup the criteria group to match
     * @param entity the entity to upsert
     * @param collection the name of the collection
     */
    private void executeUpsertStatement(IgniteCriteriaGroup igniteCriteriaGroup, E entity,
            String collection) {

        var igniteQuery = new IgniteQuery(igniteCriteriaGroup);
        Query<E> query = queryTranslator.translate(igniteQuery, Optional.ofNullable(
                getOverridingCollectionName()));
        MongoCollection<E> mongoCollection = mongoDatastore.getDatabase().getCollection(
                collection, entityClass);

        LOGGER.info("Performing upsert operation on collection : {}, entity : {}",
                collection, entity.toString());
        mongoCollection.replaceOne(query.toDocument(), entity, new ReplaceOptions().upsert(true));
        updateEntityId(entity);
    }

    /**
     * Updates the entity ID after a save operation.
     *
     * @param entity the entity to update
     */
    private void updateEntityId(E entity) {
        Object newId = mongoDatastore.getMapper().findIdProperty(entity.getClass()).getValue(entity);

        if (newId != null) {
            LOGGER.info("Entity saved successfully {}, Updated entity id : {}", entity.toString(), newId.toString());
            for (Field field : FieldUtils.getFieldsWithAnnotation(entityClass, Id.class)) {
                try {
                    field.setAccessible(true);
                    field.set(entity, newId);
                    field.setAccessible(false);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Failed to set newId in entity object");
                }
            }
        }
    }

}