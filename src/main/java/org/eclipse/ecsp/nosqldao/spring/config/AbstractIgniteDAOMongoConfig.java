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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.Tag;
import com.mongodb.TagSet;
import com.mongodb.client.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.eclipse.ecsp.healthcheck.HealthMonitor;
import org.eclipse.ecsp.nosqldao.NoSqlDatabaseType;
import org.eclipse.ecsp.nosqldao.mongodb.MongoReadPreference;
import org.eclipse.ecsp.nosqldao.utils.Constants;
import org.eclipse.ecsp.nosqldao.utils.PropertyNames;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class for configuring MongoDB settings for NoSQL DAO.
 * This class implements the HealthMonitor interface to provide health monitoring capabilities.
 * It includes various configuration properties for MongoDB and methods to set up and validate the MongoDB connection.
 */
public abstract class AbstractIgniteDAOMongoConfig implements HealthMonitor {

    /**
     * Logger instance for logging.
     */
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(AbstractIgniteDAOMongoConfig.class);

    /**
     * Constants for MongoDB health monitoring.
     */
    protected static final String MONGO_HEALTH_MONITOR = "MONGO_HEALTH_MONITOR";

    /**
     * Gauge name for MongoDB health monitoring.
     */
    protected static final String MONGO_HEALTH_GUAGE = "MONGO_HEALTH_GUAGE";

    /**
     * Error message for failed MongoDB connection initialization.
     */
    protected  static final String FAILED_TO_INITIALIZE_MONGO_CONNECTION = "Failed to initialize mongodb connection";

    /**
     * The health status of the MongoDB connection.
     */
    protected static volatile boolean healthy;

    /**
     * The port number for the MongoDB connection.
     * The default value is 27017.
     */
    public static Integer overridingPort;

    /**
     * The MongoDB hosts to connect to.
     * The default value is empty.
     */
    @Value("${mongodb.hosts:}")
    protected String hosts;

    /**
     * The port number for the MongoDB connection.
     * The default value is 27017.
     */
    @Value("${mongodb.port:27017}")
    protected int port;

    /**
     * The username for MongoDB authentication.
     * The default value is empty.
     */
    @Value("${mongodb.username:}")
    protected String username;

    /**
     * The password for MongoDB authentication.
     * The default value is empty.
     */
    @Value("${mongodb.password:}")
    protected String password;

    /**
     * The authentication database for MongoDB.
     * The default value is empty.
     */
    @Value("${mongodb.auth.db:}")
    protected String authDb;

    /**
     * The name of the MongoDB database.
     * The default value is empty.
     */
    @Value("${mongodb.name:}")
    protected String dbName;

    /**
     * The maximum size of the MongoDB connection pool.
     * The default value is 100.
     */
    @Value("${mongodb.pool.max.size:100}")
    protected Integer poolMaxSize;

    /**
     * The maximum wait time in milliseconds for a connection to become available.
     * The default value is 120000 milliseconds.
     */
    @Value("${mongodb.max.wait.time.ms:120000}")
    protected int maxWaitTime;

    /**
     * The connection timeout in milliseconds for MongoDB.
     * The default value is 30000 milliseconds.
     */
    @Value("${mongodb.connection.timeout.ms:30000}")
    protected int connectionTimeout;

    /**
     * The socket timeout in milliseconds for MongoDB.
     * The default value is 0 milliseconds.
     */
    @Value("${mongodb.socket.timeout.ms:0}")
    protected int socketTimeout;

    /**
     * The maximum number of connections per host for MongoDB.
     * The default value is 200.
     */
    @Value("${mongodb.max.connections.per.host:200}")
    protected int maxConnectionsPerHost;

    /**
     * The read preference for MongoDB.
     * The default value is secondaryPreferred.
     */
    @Value("${mongodb.read.preference:secondaryPreferred}")
    protected String readPreference;

    /**
     * The packages to map for Morphia.
     * The default value is org.eclipse.ecsp.
     */
    @Value("${morphia.map.packages:#{org.eclipse.ecsp}}")
    protected String[] mapPackages;

    /**
     * The server selection timeout in milliseconds for MongoDB.
     * The default value is 30000 milliseconds.
     */
    @Value("${mongodb.server.selection.timeout:30000}")
    protected int serverSelectionTimeout;

    /**
     * The fully qualified names of Morphia converters.
     * The default value is null.
     */
    @Value("${morphia.converters.fqn:#{null}}")
    protected String morphiaConverters;

    /**
     * The name of the Morphia convention.
     * The default value is null.
     */
    @Value("${morphia.convention:#{null}}")
    protected String morphiaConventionName;

    /**
     * Indicates if taggable read preference is enabled for MongoDB.
     * The default value is false.
     */
    @Value("${mongodb.taggable.read.preference.enabled:false}")
    protected boolean taggableReadPreferenceEnabled;

    /**
     * The read preference tag for MongoDB.
     * The default value is primary_region.
     */
    @Value("${mongodb.read.preference.tag:primary_region}")
    protected String readPreferenceTag;

    /**
     * Indicates if the health monitor for MongoDB is enabled.
     * The default value is true.
     */
    @Value("${health.mongo.monitor.enabled:true}")
    protected boolean healthMonitorEnabled;

    /**
     * Indicates if a restart is needed on failure for MongoDB.
     * The default value is true.
     */
    @Value("${health.mongo.needs.restart.on.failure:true}")
    protected boolean needsRestartOnFailure;

    /**
     * The connection string for CosmosDB.
     * The default value is empty.
     */
    @Value("${" + PropertyNames.COSMOS_DB_CONNECTION_STRING + ":}")
    protected String cosmosDbConnectionString;

    /**
     * The name of the CosmosDB database.
     * The default value is empty.
     */
    @Value("${" + PropertyNames.COSMOSDB_NAME + ":}")
    protected String cosmosdbName;

    /**
     * The minimum size of the MongoDB connection pool.
     * The default value is empty.
     */
    @Value("${mongodb.pool.min.size:}")
    protected Integer poolMinSize;

    /**
     * The maximum connection life time in milliseconds for MongoDB.
     * The default value is empty.
     */
    @Value("${mongodb.max.connection.life.time.ms:}")
    protected Integer maxConnectionLifeTime;

    /**
     * The maximum connection idle time in milliseconds for MongoDB.
     * The default value is empty.
     */
    @Value("${mongodb.max.connection.idle.time.ms:}")
    protected Integer maxConnectionIdleTime;

    /**
     * The initial delay in milliseconds for MongoDB maintenance.
     * The default value is empty.
     */
    @Value("${mongodb.maintenance.initial.delay.ms:}")
    protected Integer maintenanceInitialDelay;

    /**
     * The frequency in milliseconds for MongoDB maintenance.
     * The default value is empty.
     */
    @Value("${mongodb.maintenance.frequency.ms:}")
    protected Integer maintenanceFrequency;

    /**
     * The host for DocumentDB.
     * The default value is empty.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_HOST + ":}")
    protected String docDbHost;

    /**
     * The port for DocumentDB.
     * The default value is 27017.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_PORT + ":27017}")
    protected int docDbPort;

    /**
     * The username for DocumentDB.
     * The default value is empty.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_USERNAME + ":}")
    protected String docDbUsername;

    /**
     * The password for DocumentDB.
     * The default value is empty.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_PASSWORD + ":}")
    protected String docDbPassword;

    /**
     * The name of the DocumentDB database.
     * The default value is empty.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_NAME + ":}")
    protected String docDbName;

    /**
     * The connection parameters for DocumentDB.
     * The default value is defined by PropertyNames.DOCUMENTDB_DEFAULT_CONNECTION_PARAMS.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_CONNECTION_PARAMS + ":" 
            + PropertyNames.DOCUMENTDB_DEFAULT_CONNECTION_PARAMS + "}")
    protected String docDbConnectionParams;

    /**
     * The authentication database for DocumentDB.
     * The default value is admin.
     */
    @Value("${" + PropertyNames.DOCUMENTDB_AUTH_DB + ":admin}")
    protected String docDbAuthDb;

    /**
     * Custom connection pool listener.
     */
    @Autowired
    private CustomConnectionPoolListener customConnectionPoolListener;

    /**
     * Type of NoSQL database.
     */
    protected NoSqlDatabaseType noSqlDatabaseType;

    /**
     * Mongo client.
     */
    protected volatile MongoClient mongoClient = null;

    /**
     * Morphia config.
     */
    protected MorphiaConfig morphiaConfig;

    /**
     * Morphia codec registry.
     */
    protected CodecRegistry codecRegistry;

    /**
     * List of server addresses.
     */
    protected List<ServerAddress> servers;

    /**
     * The discriminator key for Morphia.
     * The default value is defined by Constants.DISCRIMINATOR_KEY.
     */
    @Value("${morphia.discriminator.key:" + Constants.DISCRIMINATOR_KEY + "}")
    private String discriminatorKey;

    /**
     * Sets the health status of the MongoDB connection.
     * @param flag boolean value indicating the health status.
     */
    protected static void setHealthy(boolean flag) {
        healthy = flag;
    }

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
        LOGGER.info("Database selected via property " + PropertyNames.NO_SQL_DATABASE_TYPE + ":" 
                + noSqlDatabaseTypeStr + " is : " + noSqlDatabaseType.name());
    }

    /**
     * Gets the MongoClient instance.
     * @return MongoClient instance.
     */
    MongoClient getMongoClient() {
        return mongoClient;
    }

    /**
     * Returns the port which a client uses to connect to MongoDB server.
     * OverridingPort is for supporting integration tests.
     *
     * @return int representing the port number.
     */
    protected int getPort() {
        return (AbstractIgniteDAOMongoConfig.overridingPort != null) ? overridingPort : port;
    }

    /**
     * Returns the port which a client uses to connect to DocumentDB server.
     * OverridingPort is for supporting integration tests.
     *
     * @return int representing the port number.
     */
    protected int getDocDbPort() {
        return (AbstractIgniteDAOMongoConfig.overridingPort != null) ? overridingPort : docDbPort;
    }

    /**
    * This method is used for Building Morphia mapping options.
    *
    * @return : AdvancedDatastore
    */
    @Bean
    public Datastore mongoDatastore() {
        Datastore datastore = null;
        try {
            //creating code registry for custom codec-providers
            if (codecRegistry == null) {
                LOGGER.info("trying to fetch registered codecs from properties while creating mongo datastore bean");
                codecRegistry = getCodecRegistryFromProp();
            }

            LOGGER.info("Building Morphia config. Property discovery enabled via FIELDS, "
                    + "with discriminator key as : {}", Constants.DISCRIMINATOR_KEY);
            if (StringUtils.isNotEmpty(morphiaConventionName)) {
                LOGGER.warn("morphia.convention property is not supported in Morphia 2.5.x."
                        + " Register conventions via ServiceLoader instead.");
            }
            this.morphiaConfig = new ManualMorphiaConfig()
                    .database(getDatastoreDbName())
                    .propertyDiscovery(MapperOptions.PropertyDiscovery.FIELDS)
                    .discriminatorKey(discriminatorKey)
                    .discriminator(DiscriminatorFunction.className())
                    .packages(Arrays.asList(mapPackages));
            datastore = getDatastore();
            
            long startTime = System.currentTimeMillis();
            // From morphia 2.0 UTC will be available by default
            LOGGER.info("Morphia DataStorage : {}", 
                    datastore.getMapper().getConfig().dateStorage().getZone().getId());
            long endTimeMorpia = System.currentTimeMillis();
            LOGGER.info("Connection time taken from Morphia in millisecs : {}", (endTimeMorpia - startTime));
        } catch (RuntimeException e) {
            if (null != mongoClient) {
                mongoClient.close();
            }
            LOGGER.error(FAILED_TO_INITIALIZE_MONGO_CONNECTION, e);
            throw new MongoConnectionException(FAILED_TO_INITIALIZE_MONGO_CONNECTION, e);
        }
        LOGGER.debug("Returning DataStore  hashcode {} ", datastore.hashCode());
        LOGGER.debug("Returning DataStore mongoclient hashcode   {} ", mongoClient.hashCode());
        return datastore;
    }

    /**
     * Gets the MongoDB credentials.
     * @return the MongoCredential
     */
    protected MongoCredential getCredentials() {
        if (noSqlDatabaseType == NoSqlDatabaseType.DOCUMENTDB) {
            return MongoCredential.createCredential(docDbUsername, docDbAuthDb,
                    docDbPassword.toCharArray());
        }
        return MongoCredential.createCredential(username, authDb, password.toCharArray());
    }

    /**
     * Validates the configuration properties for MongoDB and CosmosDB.
     * Throws IllegalArgumentException if any required property is missing.
     */
    protected void validate() {

        List<String> inValidConfAttributes = new ArrayList<>();
        
        if (noSqlDatabaseType == NoSqlDatabaseType.MONGODB) {
            validateMongoDbAttributes(inValidConfAttributes);
        } else if (noSqlDatabaseType == NoSqlDatabaseType.COSMOSDB) {
            validateCosmosDbAttributes(inValidConfAttributes);
        } else if (noSqlDatabaseType == NoSqlDatabaseType.DOCUMENTDB) {
            validateDocumentDbAttributes(inValidConfAttributes);
        }
        if (!inValidConfAttributes.isEmpty()) {
            throw new IllegalArgumentException("Missing connection properties: "
                    + inValidConfAttributes.toString());
        }
    }

    /**
     * Validates the configuration properties for DocumentDB.
     *
     * @param inValidConfAttributes : List of String
     */
    private void validateDocumentDbAttributes(List<String> inValidConfAttributes) {
        if (StringUtils.isBlank(docDbHost)) {
            inValidConfAttributes.add("documentdb.host");
        }
        if (docDbPort == 0) {
            inValidConfAttributes.add("documentdb.port");
        }
        if (StringUtils.isBlank(docDbUsername)) {
            inValidConfAttributes.add("documentdb.username");
        }
        if (StringUtils.isBlank(docDbPassword)) {
            inValidConfAttributes.add("documentdb.password");
        }
        if (StringUtils.isBlank(docDbAuthDb)) {
            inValidConfAttributes.add("documentdb.auth.db");
        }
    }

    /**
     * Validates the configuration properties for CosmosDB.
     *
     * @param inValidConfAttributes : List of String
     */
    protected void validateCosmosDbAttributes(List<String> inValidConfAttributes) {
        
        if (StringUtils.isBlank(cosmosDbConnectionString)) {
            inValidConfAttributes.add(PropertyNames.COSMOS_DB_CONNECTION_STRING);
        }
        if (StringUtils.isBlank(cosmosdbName)) {
            inValidConfAttributes.add(PropertyNames.COSMOSDB_NAME);
        }
    }

    /**
     * Validates the configuration properties for MongoDB.
     *
     * @param inValidConfAttributes : List of String
     */
    protected void validateMongoDbAttributes(List<String> inValidConfAttributes) {
        
        if (StringUtils.isBlank(hosts)) {
            inValidConfAttributes.add("mongodb.hosts");
        }
        if (port == 0) {
            inValidConfAttributes.add("mongodb.port");
        }
        if (StringUtils.isBlank(username)) {
            inValidConfAttributes.add("mongodb.username");
        }
        if (StringUtils.isBlank(password)) {
            inValidConfAttributes.add("mongodb.password");
        }
        if (StringUtils.isBlank(authDb)) {
            inValidConfAttributes.add("mongodb.auth.db}");
        }
        if (StringUtils.isBlank(dbName)) {
            inValidConfAttributes.add("mongodb.name");
        }
        if (maxWaitTime == 0) {
            inValidConfAttributes.add("mongodb.max.wait.time.ms");
        }
        if (connectionTimeout == 0) {
            inValidConfAttributes.add("mongodb.connection.timeout.ms");
        }
        if (socketTimeout == 0) {
            inValidConfAttributes.add("mongodb.socket.timeout.ms");
        }
        if (StringUtils.isBlank(readPreference)) {
            inValidConfAttributes.add("mongodb.read.preference");
        }
        if (ArrayUtils.isEmpty(mapPackages)) {
            inValidConfAttributes.add("morphia.map.packages");
        }
        if (serverSelectionTimeout == 0) {
            inValidConfAttributes.add("mongodb.server.selection.timeout");
        }
        if (StringUtils.isBlank(readPreferenceTag)) {
            inValidConfAttributes.add("mongodb.read.preference.tag");
        }
    }

    /**
     * Returns the Datastore instance.
     *
     * @return Datastore instance.
     */
    protected abstract Datastore getDatastore();

    /**
     * Maps the packages to the datastore.
     * @param datastore Datastore instance.
     */
    protected void mapPackagesToDatastore(Datastore datastore) {
        // Packages are now configured via MorphiaConfig.packages() at datastore creation time.
        // This method is retained for API compatibility only.
    }

    /**
     * Returns the health status of the MongoDB connection.
     *
     * @return boolean value indicating the health status.
     */
    @Override
    public boolean isEnabled() {
        return healthMonitorEnabled;
    }

    /**
     * Returns the health status of the MongoDB connection.
     *
     * @return boolean value indicating the health status.
     */
    @Override
    public String metricName() {
        return MONGO_HEALTH_GUAGE;
    }

    /**
     * Returns the health status of the MongoDB connection.
     *
     * @return boolean value indicating the health status.
     */
    @Override
    public String monitorName() {
        return MONGO_HEALTH_MONITOR;
    }

    /**
     * Returns the health status of the MongoDB connection.
     *
     * @return boolean value indicating the health status.
     */
    @Override
    public boolean needsRestartOnFailure() {
        return needsRestartOnFailure;
    }

    /**
     * Returns the health status of the MongoDB connection.
     *
     * @return boolean value indicating the health status.
     */
    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    /**
     * Creates the MongoClientSettings.Builder instance.
     *
     * @return MongoClientSettings.Builder instance.
     */
    protected MongoClientSettings.Builder createMongoClientSettingsBuilder() {
        if (noSqlDatabaseType == NoSqlDatabaseType.MONGODB) {
            String[] hostNames = hosts.split(",");
            servers = new ArrayList<>();
            for (String host : hostNames) {
                host = host.trim();
                if (!StringUtils.isEmpty(host)) {
                    ServerAddress serverAddress = new ServerAddress(host, getPort());
                    servers.add(serverAddress);
                }
            }
            LOGGER.debug("Initializing MongoClient with servers={}", servers);
        }
        if (codecRegistry == null) {
            LOGGER.info("loading codec registry from properties while creating mongo client setting builder");
            codecRegistry = getCodecRegistryFromProp();
        }

        MongoClientSettings.Builder mongoClientSettingsBuilder = getMongoClientBuilder();

        if (codecRegistry != null) {
            LOGGER.debug("The CodeRegistry registered for mongo operations {}", codecRegistry.toString());
            mongoClientSettingsBuilder.codecRegistry(
                    CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), codecRegistry));
        } else {
            LOGGER.debug("No CodecRegistry is provided");
            mongoClientSettingsBuilder.codecRegistry(
                    CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry()));
        }

        return mongoClientSettingsBuilder;
    }

    /**
     * Creates the MongoClientSettings.Builder instance.
     *
     * @return MongoClientSettings.Builder instance.
     */
    private MongoClientSettings.Builder getMongoClientBuilder() {
        MongoClientSettings.Builder mongoClientSettingsBuilder = MongoClientSettings.builder();
        switch (noSqlDatabaseType) {
            case COSMOSDB:
                applyClientSettingsForCosmosDB(mongoClientSettingsBuilder);
                break;
            case DOCUMENTDB:
                applyClientSettingsForDocumentDB(mongoClientSettingsBuilder);
                break;
            case MONGODB:
            default:
                applyClientSettingsForMongoDB(mongoClientSettingsBuilder);
                break;
        }
        mongoClientSettingsBuilder.applyToConnectionPoolSettings(
            builder -> builder.addConnectionPoolListener(customConnectionPoolListener)
        );
        return mongoClientSettingsBuilder;
    }

    /**
     * Applies the client settings for DocumentDB.
     *
     * @param mongoClientSettingsBuilder MongoClientSettings.Builder instance.
     */
    private void applyClientSettingsForDocumentDB(MongoClientSettings.Builder mongoClientSettingsBuilder) {
        String documentDbConnectionString = getConnectionStringForDocumentDb();
        ConnectionString connectionString = new ConnectionString(documentDbConnectionString);
        mongoClientSettingsBuilder.applyConnectionString(connectionString);
        LOGGER.info("Mongo client settings applied for DocumentDB");
    }

    /**
     * Constructs the connection string for DocumentDB.
     *
     * @return : String representing the connection string.
     */
    private String getConnectionStringForDocumentDb() {
        StringBuilder sb = new StringBuilder(Constants.MONGODB_PREFIX);
        sb.append(docDbHost).append(Constants.COLON).append(getDocDbPort())
                .append(Constants.FRONT_SLASH).append(docDbName)
                .append(Constants.QUESTION_MARK)
                .append(docDbConnectionParams);
        LOGGER.info("Document db connection string is : {}", sb);
        return sb.toString();
    }

    /**
     * Applies the client settings for CosmosDB.
     *
     * @param mongoClientSettingsBuilder MongoClientSettings.Builder instance.
     */
    private void applyClientSettingsForCosmosDB(MongoClientSettings.Builder mongoClientSettingsBuilder) {
        ConnectionString connectionString = new ConnectionString(cosmosDbConnectionString);
        mongoClientSettingsBuilder.applyConnectionString(connectionString);
        LOGGER.info("Mongo client settings applied for CosmosDB"); 
    }

    /**
     * Applies the client settings for MongoDB.
     *
     * @param mongoClientSettingsBuilder MongoClientSettings.Builder instance.
     */
    private void applyClientSettingsForMongoDB(MongoClientSettings.Builder mongoClientSettingsBuilder) {
        mongoClientSettingsBuilder.applyToConnectionPoolSettings(builder -> {
            builder.maxConnecting(maxConnectionsPerHost);
            builder.maxWaitTime(maxWaitTime, TimeUnit.MILLISECONDS);
            if (isNotNull(poolMaxSize)) {
                builder.maxSize(poolMaxSize);
            }
            if (isNotNull(poolMinSize)) {
                builder.minSize(poolMinSize);
            }
            if (isNotNull(maxConnectionIdleTime)) {
                builder.maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS);
            }
            if (isNotNull(maxConnectionLifeTime)) {
                builder.maxConnectionLifeTime(maxConnectionLifeTime, TimeUnit.MILLISECONDS);
            }
            if (isNotNull(maintenanceFrequency)) {
                builder.maintenanceFrequency(maintenanceFrequency, TimeUnit.MILLISECONDS);
            }
            if (isNotNull(maintenanceInitialDelay)) {
                builder.maintenanceInitialDelay(maintenanceInitialDelay, TimeUnit.MILLISECONDS);
            }
        }).applyToSocketSettings(builder -> {
            builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(socketTimeout, TimeUnit.MILLISECONDS);
        }).applyToClusterSettings(builder -> {
            builder.serverSelectionTimeout(serverSelectionTimeout, TimeUnit.MILLISECONDS);
            builder.hosts(servers);
        });
        if (taggableReadPreferenceEnabled) {
            mongoClientSettingsBuilder
                .readPreference(ReadPreference.secondaryPreferred(new TagSet(new Tag(readPreferenceTag, "true"))));
        } else {
            mongoClientSettingsBuilder
                .readPreference(MongoReadPreference.getEnum(readPreference).getReadPreference());
        }
        LOGGER.info("Mongo client settings applied for MongoDB");
    }

    /**
     * Returns the codec registry from the properties.
     * @return CodecRegistry instance.
     */
    private CodecRegistry getCodecRegistryFromProp() {
        List<Codec<?>> codecList = new ArrayList<>();
        if (morphiaConverters != null) {
            for (String converter : morphiaConverters.split(",")) {
                try {
                    codecList.add(((Codec<?>) Class.forName(converter).getDeclaredConstructor().newInstance()));
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                        | InvocationTargetException | NoSuchMethodException e) {
                    LOGGER.error("Exception occurred while getting the codec converters "
                        + "from properties. Exception is {} ", e);
                }
            }
            codecRegistry = CodecRegistries.fromCodecs(codecList);
            LOGGER.debug("The morphiaConverters are {},Codec list is {}, and codecRegistry is {} "
                    + "while creating MongoClientSettingsBuilder", morphiaConverters, codecList, codecRegistry);
        }
        return codecRegistry;
    }

    /**
     * Returns the health status of the MongoDB connection.
     * @return boolean value indicating the health status.
     */
    private boolean isNotNull(Integer value) {
        return (value != null);
    }

    /**
     * Returns the log target information.
     * @return String representing the log target.
     */
    protected String logTarget() {
        switch (noSqlDatabaseType) {
            case MONGODB:
                return "with servers=" + servers;
            case DOCUMENTDB:
                return "with host=" + docDbHost + ":" + docDbPort;
            default:
                return "";
        }
    }

    /**
     * Returns the database name for the datastore.
     * @return String representing the database name.
     */
    protected String getDatastoreDbName() {
        switch (noSqlDatabaseType) {
            case COSMOSDB:
                return cosmosdbName;
            case DOCUMENTDB:
                return docDbName;
            case MONGODB:
            default:
                return dbName;
        }
    }
}
