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

/**
 * This class provides constants for Property Names.
 *
 */
public abstract class PropertyNames {

    /**
     * MongoDB host property name.
     */
    public static final String MONGODB_HOST = "mongodb.host";

    /**
     * MongoDB port property name.
     */
    public static final String MONGODB_PORT = "mongodb.port";

    /**
     * MongoDB username property name.
     */
    public static final String MONGODB_USERNAME = "mongodb.username";

    /**
     * MongoDB password property name.
     */
    public static final String MONGODB_PASSSWORD = "mongodb.password";

    /**
     * MongoDB authentication database property name.
     */
    public static final String MONGODB_AUTH_DB = "mongodb.auth.db";

    /**
     * MongoDB database name property name.
     */
    public static final String MONGODB_DBNAME = "mongodb.name";

    /**
     * MongoDB pool maximum size property name.
     */
    public static final String MONGODB_POOL_MAX_SIZE = "mongodb.pool.max.size";

    /**
     * MongoDB maximum wait time in milliseconds property name.
     */
    public static final String MONGODB_MAX_WAIT_TIME_MS = "mongodb.max.wait.time.ms";

    /**
     * MongoDB connection timeout in milliseconds property name.
     */
    public static final String MONGODB_CONNECTION_TIMEOUT_MS = "mongodb.connection.timeout.ms";

    /**
     * MongoDB socket timeout in milliseconds property name.
     */
    public static final String MONGODB_SOCKET_TIMEOUT_MS = "mongodb.socket.timeout.ms";

    /**
     * MongoDB maximum connections per host property name.
     */
    public static final String MONGODB_MAX_CONNECTIONS_PER_HOST = "mongodb.max.connections.per.host";

    /**
     * MongoDB read preference property name.
     */
    public static final String MONGODB_READ_PREFERENCE = "mongodb.read.preference";

    /**
     * Morphia map packages property name.
     */
    public static final String MORPHIA_MAP_MACKAGES = "morphia.map.packages";

    /**
     * Default MongoDB host.
     */
    public static final String DEFAULT_MONGODB_HOST = "localhost";

    /**
     * Default MongoDB port.
     */
    public static final String DEFAULT_MONGODB_PORT = "27017";

    /**
     * Default MongoDB username.
     */
    public static final String DEFAULT_MONGODB_USERNAME = "admin";

    /**
     * Default MongoDB password.
     */
    public static final String DEFAULT_MONGODB_PASSSWORD = "password";

    /**
     * Default MongoDB authentication database.
     */
    public static final String DEFAULT_MONGODB_AUTH_DB = "admin";

    /**
     * Default MongoDB database name.
     */
    public static final String DEFAULT_MONGODB_DBNAME = "testDB";

    /**
     * Default MongoDB pool maximum size.
     */
    public static final String DEFAULT_MONGODB_POOL_MAX_SIZE = "10";

    /**
     * Default MongoDB maximum wait time in milliseconds.
     */
    public static final String DEFAULT_MONGODB_MAX_WAIT_TIME_MS = "60000";

    /**
     * Default MongoDB connection timeout in milliseconds.
     */
    public static final String DEFAULT_MONGODB_CONNECTION_TIMEOUT_MS = "30000";

    /**
     * Default MongoDB socket timeout in milliseconds.
     */
    public static final String DEFAULT_MONGODB_SOCKET_TIMEOUT_MS = "30000";

    /**
     * Default MongoDB maximum connections per host.
     */
    public static final String DEFAULT_MONGODB_MAX_CONNECTIONS_PER_HOST = "5";

    /**
     * Default MongoDB read preference.
     */
    public static final String DEFAULT_MONGODB_READ_PREFERENCE = "secondaryPreferred";

    /**
     * Sharded property name.
     */
    public static final String SHARDED = "sharded";

    /**
     * Indexed property name.
     */
    public static final String INDEXED = "indexed";

    /**
     * Mongo diagnostic reporter enabled property name.
     */
    public static final String MONGO_DIAGNOSTIC_REPORTER_ENABLED = "mongo.diagnostic.reporter.enabled";

    /**
     * Prometheus metrics enabled property name.
     */
    public static final String ENABLE_PROMETHEUS = "metrics.prometheus.enabled";

    /**
     * DAO metrics enabled property name.
     */
    public static final String DAO_METRICS_ENABLED = "metrics.dao.enabled";

    /**
     * Cosmos DB connection string property name.
     */
    public static final String COSMOS_DB_CONNECTION_STRING = "cosmos.db.connection.string";

    /**
     * NoSQL database type property name.
     */
    public static final String NO_SQL_DATABASE_TYPE = "no.sql.database.type";

    /**
     * CosmosDB name property name.
     */
    public static final String COSMOSDB_NAME = "cosmosdb.name";

    /**
     * DocumentDB connection string property name.
     */
    public static final String DOCUMENT_DB_CONNECTION_STRING = "document.db.connection.string"; ;


    /**
     * NoSQL database type for DocumentDB property name.
     */
    public static final String DOCUMENTDB_NAME = "documentdb.name";
    public static final String DOCUMENTDB_TLS_ENABLED = "documentdb.tls.enabled";
    public static final String DOCUMENTDB_AUTH_MECHANISM = "documentdb.auth.mechanism";
    public static final String DOCUMENTDB_RETRY_WRITES = "documentdb.retry.writes";
    public static final String DOCUMENTDB_READ_PREFFRENCE = "documentdb.read.preference";
    public static final String DOCUMENTDB_PORT= "documentdb.port";
    public static final String DOCUMENTDB_HOST= "documentdb.host";
    public static final String DOCUMENTDB_USERNAME= "documentdb.username";
    public static final String DOCUMENTDB_PASSWORD= "documentdb.password";


    /**
     * Private constructor to prevent instantiation.
     */
    private PropertyNames() {
         // private constructor
    }

}
