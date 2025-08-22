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
import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.AdvancedDatastore;
import dev.morphia.Morphia;
import org.eclipse.ecsp.nosqldao.NoSqlDatabaseType;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This class provides methods for IgniteDAOMongoConfiguration with properties
 * extending AbstractIgniteDAOMongoConfig class.
 *
 */
@Conditional(MongoPropsCondition.class)
@Configuration
@ComponentScan(basePackages = { "org.eclipse.ecsp.nosqldao" })
public class IgniteDAOMongoConfigWithProps extends AbstractIgniteDAOMongoConfig {

    /**
     * Logger.
     */
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(IgniteDAOMongoConfigWithProps.class);

    /**
     * PropertyEnabledDatastoreInvocationHandler.
     */
    private volatile PropertyEnabledDatastoreInvocationHandler peInvocationHandler =
            new PropertyEnabledDatastoreInvocationHandler();

    /**
     * Retrieves the AdvancedDatastore instance, creating a new MongoClient if necessary.
     *
     * @return an instance of AdvancedDatastore
     */
    @SuppressWarnings("removal")
    @Override
    protected AdvancedDatastore getDatastore() {
        mongoClient = createMongoClient();
        AdvancedDatastore ads = (AdvancedDatastore) Morphia.createDatastore(mongoClient, 
                getDatastoreDbName(), mapperOptions);
        mapPackagesToDatastore(ads);
        peInvocationHandler.setDatastore(ads);
        return (AdvancedDatastore) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[] { AdvancedDatastore.class }, peInvocationHandler);
    }

    /**
     * Creates and returns a new instance of MongoClient with the configured settings.
     *
     * @return a new MongoClient instance
     */
    private MongoClient createMongoClient() {
        validate();
        MongoClient newMongoClient = null;
        MongoClientSettings mongoClientSettings = null;
        MongoClientSettings.Builder mongoClientSettingsBuilder = createMongoClientSettingsBuilder();
        if (noSqlDatabaseType != NoSqlDatabaseType.COSMOSDB) {
            mongoClientSettingsBuilder.credential(getCredentials());
        }
        LOGGER.info("Initializing MongoClient {}", logTarget());
        try {
            mongoClientSettings = mongoClientSettingsBuilder.build();
            long startTime = System.currentTimeMillis();
            newMongoClient = MongoClients.create(mongoClientSettings);
            long endTime = System.currentTimeMillis();
            setHealthy(true);
            LOGGER.info("Initialized mongo client {} and time taken in millisec is: {}", 
                logTarget(), endTime - startTime);
        } catch (Exception e) {
            if (null != newMongoClient) {
                newMongoClient.close();
            }
            setHealthy(false);
            LOGGER.error("Failed to initialize database connection", e);
            throw new MongoConnectionException("Failed to initialize database connection", e);
        }
        return newMongoClient;
    }

    /**
     * Checks the health status of the MongoDB client.
     *
     * @param forceToRecreateClient if true, forces the recreation of the MongoDB client if it is not healthy
     * @return true if the MongoDB client is healthy, false otherwise
     */
    @Override
    @SuppressWarnings("removal")
    public boolean isHealthy(boolean forceToRecreateClient) {

        if (forceToRecreateClient && (!healthy || mongoClient == null)) {
            AdvancedDatastore ads = (AdvancedDatastore) Morphia.createDatastore(
                createMongoClient(), getDatastoreDbName(), mapperOptions);
            mapPackagesToDatastore(ads);
            peInvocationHandler.setDatastore(ads);
            setHealthy(true);
        }
        return healthy;
    }

    /**
     * InvocationHandler implementation for handling datastore operations with properties.
     */
    private class PropertyEnabledDatastoreInvocationHandler implements InvocationHandler {

        /**
         * AdvancedDatastore instance.
         */
        @SuppressWarnings("removal")
        private volatile AdvancedDatastore datastore;

        /**
         * Invokes the specified method on the AdvancedDatastore instance.
         * Handles cases for -
         * error code 11 - UserNotFound,
         * error code 13 - Unauthorized,
         * error code 31 - RoleNotFound,
         * error code 32 - RolesNotRelated,
         * error code 33 - PrivilegeNotFound.
         *
         * @param proxy the proxy object
         * @param method the method to invoke
         * @param args the arguments to pass to the method
         * @return the result of the method invocation
         * @throws Throwable if an error occurs while invoking the method
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            LOGGER.trace("ProxyInvoke:Invoking data store {} with mongo client {} ",
                    datastore.hashCode(), mongoClient.hashCode());
            try {
                Object result = method.invoke(datastore, args);
                setHealthy(true);
                return result;
            } catch (Exception ex) {
                Throwable targetException = ex.getCause();
                if (targetException instanceof MongoException exception) {
                    int code = exception.getCode();
                    if (exception instanceof MongoClientException || exception instanceof MongoSocketException
                            || code == NumericConstants.ELEVEN || code == NumericConstants.THIRTEEN
                            || code == NumericConstants.THIRTY_ONE || code == NumericConstants.THIRTY_TWO
                            || code == NumericConstants.THIRTY_THREE) {
                        setHealthy(false);
                    }
                }
                throw targetException;

            }
        }

        /**
         * Sets the AdvancedDatastore instance.
         *
         * @param datastore the AdvancedDatastore instance to set
         */
        @SuppressWarnings("removal")
        private void setDatastore(AdvancedDatastore datastore) {
            this.datastore = datastore;
        }
    }
}