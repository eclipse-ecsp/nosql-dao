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

import com.mongodb.event.ConnectionCheckOutFailedEvent;
import com.mongodb.event.ConnectionCheckedInEvent;
import com.mongodb.event.ConnectionCheckedOutEvent;
import com.mongodb.event.ConnectionClosedEvent;
import com.mongodb.event.ConnectionCreatedEvent;
import com.mongodb.event.ConnectionPoolClearedEvent;
import com.mongodb.event.ConnectionPoolClosedEvent;
import com.mongodb.event.ConnectionPoolCreatedEvent;
import com.mongodb.event.ConnectionPoolListener;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Spring-managed MongoDB connection pool listener that logs key pool and
 * connection lifecycle events. This implementation is stateless and intended
 * for observability and troubleshooting of the MongoDB driver connection pool.
 * <p>
 * Typical usage wires this component into the MongoClientSettings builder so
 * that events are emitted by the driver and captured by the application logs.
 * </p>
 */
@Component
public class CustomConnectionPoolListener implements ConnectionPoolListener {

    /**
     * Logger for emitting connection pool and connection lifecycle diagnostics.
     */
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(CustomConnectionPoolListener.class);

    /**
     * Invoked when a new connection pool is created by the MongoDB driver.
     *
     * @param event details about the created pool (e.g., settings, server ID)
     */
    @Override
    public void connectionPoolCreated(final ConnectionPoolCreatedEvent event) {
        LOGGER.info("Connection pool created: {}", event);
    }

    /**
     * Invoked when a connection pool is closed and its resources are released.
     *
     * @param event details about the closed pool
     */
    @Override
    public void connectionPoolClosed(final ConnectionPoolClosedEvent event) {
        LOGGER.info("Connection pool closed: {}", event);
    }

    /**
     * Invoked when a connection is successfully checked out from the pool.
     *
     * @param event details about the checked out connection
     */
    @Override
    public void connectionCheckedOut(final ConnectionCheckedOutEvent event) {
        LOGGER.trace("Connection checked out: {}", event);
    }

    /**
     * Invoked when a connection is returned to the pool (checked in).
     *
     * @param event details about the checked in connection
     */
    @Override
    public void connectionCheckedIn(final ConnectionCheckedInEvent event) {
        LOGGER.trace("Connection checked in: {}", event);
    }

    /**
     * Invoked when a new physical connection to the server is established.
     *
     * @param event details about the created connection
     */
    @Override
    public void connectionCreated(final ConnectionCreatedEvent event) {
        LOGGER.trace("Connection created: {}", event);
    }

    /**
     * Invoked when a connection is closed.
     *
     * @param event details about the closed connection and reason
     */
    @Override
    public void connectionClosed(final ConnectionClosedEvent event) {
        LOGGER.trace("Connection closed: {}", event);
    }

    /**
     * Invoked when a connection checkout attempt fails.
     *
     * @param event details including the failure reason
     */
    @Override
    public void connectionCheckOutFailed(final ConnectionCheckOutFailedEvent event) {
        LOGGER.warn("Connection checkout failed: reason={}, event={}", event.getReason(), event);
    }

    /**
     * Invoked when the connection pool is cleared, typically due to server
     * state changes or error conditions.
     *
     * @param event details about the clearing action
     */
    @Override
    public void connectionPoolCleared(final ConnectionPoolClearedEvent event) {
        LOGGER.info("Connection pool cleared: {}", event);
    }

}
