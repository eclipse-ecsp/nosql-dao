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
 * This Class provides Constants.
 *
 */
public abstract class Constants {

    /**
     * The name of the latency histogram.
     */
    public static final String LATENCY_HISTO_NAME = "dao_request_processing_duration_ms";

    /**
     * The name of the request gauge.
     */
    public static final String REQ_GAUGE_NAME = "dao_request_total";

    /**
     * The name of the request counter.
     */
    public static final String REQ_COUNTER_NAME = "dao_request_processing_rate";

    /**
     * The help string for the latency histogram.
     */
    public static final String LATENCY_HISTO_HELP_STR = "Request processing duration per operation in ms";

    /**
     * The help string for the request gauge.
     */
    public static final String REQ_GAUGE_HELP_STR = "Total number of requests processed";

    /**
     * The help string for the request counter.
     */
    public static final String REQ_COUNTER_HELP_STR = "Rate of processing of requests";

    /**
     * The service constant.
     */
    public static final String SVC = "svc";

    /**
     * The label for the operation type.
     */
    public static final String OPERATION_TYPE_LABEL = "op_type";

    /**
     * The operation type for find.
     */
    public static final String OPERATION_TYPE_FIND = "find";

    /**
     * The operation type for find query.
     */
    public static final String OPERATION_TYPE_FIND_QUERY = "find_query";

    /**
     * The operation type for save.
     */
    public static final String OPERATION_TYPE_SAVE = "save";

    /**
     * The operation type for upsert.
     */
    public static final String OPERATION_TYPE_UPSERT = "upsert";

    /**
     * The operation type for delete.
     */
    public static final String OPERATION_TYPE_DELETE = "delete";

    /**
     * The operation type for delete query.
     */
    public static final String OPERATION_TYPE_DELETE_QUERY = "delete_query";

    /**
     * The operation type for count all.
     */
    public static final String OPERATION_TYPE_COUNT_ALL = "count_all";

    /**
     * The operation type for count query.
     */
    public static final String OPERATION_TYPE_COUNT_QUERY = "count_query";

    /**
     * The label for the full query.
     */
    public static final String FULL_QUERY_LABEL = "full_query";

    /**
     * The constant for "na" full query.
     */
    public static final String FULL_QUERY_NA = "na";

    /**
     * The value string constant.
     */
    public static final String VALUE_STRING = "value";

    /**
     * The label for the entity.
     */
    public static final String ENTITY_LABEL = "entity";

    /**
     * The label for pagination.
     */
    public static final String PAGINATION_LABEL = "pagination";

    /**
     * The operation type for update query.
     */
    public static final String OPERATION_TYPE_UPDATE_QUERY = "update_query";

    /**
     * The operation type for find all.
     */
    public static final String OPERATION_TYPE_FIND_ALL = "find_all";

    /**
     * The operation type for find distinct.
     */
    public static final String OPERATION_TYPE_FIND_DISTINCT = "find_distinct";

    /**
     * The constant for the ID filter.
     */
    public static final String ID_FILTER_CONSTANT = "_id";

    /**
     * The discriminator key constant.
     */
    public static final String DISCRIMINATOR_KEY = "className";
    /**
     * The discriminator value constant.
     */
    public static final String COLON = ":";
    /**
     * The discriminator value constant.
     */
    public static final String AT_THE_RATE = "@";
    /**
     * The front slash constant.
     */
    public static final String FRONT_SLASH = "/";
    /**
     * The question mark constant.
     */
    public static final String QUESTION_MARK = "?";
    /**
     * The ampersand constant.
     */
    public static final String AND = "&";
    /**
     * The equals constant.
     */
    public static final String EQUALS = "=";
    /**
     * The tls constant.
     */
    public static final String TLS = "tls";
    /**
     * The authenticationMechanism constant.
     */
    public static final String AUTHMECHANISM = "authenticationMechanism";

    public static final String DEFAULT_AUTHMECHANISM = "SCRAM-SHA-1";

    /**
     * The retryWrites constant.
     */
    public static final String RETRY_WRITES = "retryWrites";
    /**
     * The readPreference constant.
     */
    public static final String READ_PREFERENCE = "readpreference";

    /**
     * The connectTimeoutMS constant.
     */
    public static final String CONN_TIME_OUT = "connectTimeoutMS";

    /**
     * The socketTimeoutMS constant.
     */
    public static final String SOCKET_TIME_OUT = "socketTimeoutMS";

    /**
     * The maxWaitTimeMS constant.
     */
    public static final String MAX_CONN_IDLE_TIME = "maxConnectionIdleTime";
    /**
     * The maxWaitTimeMS constant.
     */
    public static final String MIN_CONN_PER_HOST = "minConnectionsPerHost";
    /**
     * The maxWaitTimeMS constant.
     */
    public static final String MAX_CONN_PER_HOST = "maxConnectionsPerHost";
    /**
     * The writeConcern constant.
     */
    public static final String WRITE_CONCERN = "writeConcern";
    /**
     * The readConcern constant.
     */
    public static final String READ_CONCERN = "readConcern";
    /**
     * The retryReads constant.
     */
    public static final String RETRY_READS = "retryReads";


    /**
     * Private constructor.
     */
    private Constants() {
        // private constructor
    }

}