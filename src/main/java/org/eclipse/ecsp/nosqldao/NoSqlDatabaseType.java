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

package org.eclipse.ecsp.nosqldao;

import org.eclipse.ecsp.nosqldao.mongodb.UnsupportedDatabaseTypeException;

/**
 * Enumeration for types of NoSQL databases supported.
 */
public enum NoSqlDatabaseType {
    /**
     * MongoDB database type.
     */
    MONGODB("mongoDB"),
    /**
     * CosmosDB database type.
     */
    COSMOSDB("cosmosDB"),
    /**
     * DocumentDB database type.
     */
    DOCUMENTDB("documentDB");

    /**
     * The database type.
     */
    private String databaseType;

    /**
     * Constructor for NoSqlDatabaseType.
     *
     * @param databaseType : databaseType
     */
    private NoSqlDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    /**
     * Method to get enum of instance NoSqlDatabaseType.
     *                                                    
     * @param dbType : dbType
     * @return NoSqlDatabaseType
     */
    public static NoSqlDatabaseType getEnum(String dbType) {
        NoSqlDatabaseType dbTypeEnum = null;
        switch (dbType) {
            case "mongoDB":
                dbTypeEnum = NoSqlDatabaseType.MONGODB;
                break;
            case "cosmosDB":
                dbTypeEnum = NoSqlDatabaseType.COSMOSDB;
                break;
            case "documentDB":
                dbTypeEnum = NoSqlDatabaseType.DOCUMENTDB;
                break;
            default:
                throw new UnsupportedDatabaseTypeException("Unsupported database type : " + dbType);
        }
        return dbTypeEnum;
    }
    
    @Override
    public String toString() {
        return databaseType;
    }

}
