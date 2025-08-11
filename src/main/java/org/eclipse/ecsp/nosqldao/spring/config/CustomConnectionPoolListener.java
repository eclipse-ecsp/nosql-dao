package org.eclipse.ecsp.nosqldao.spring.config;

import com.mongodb.event.ConnectionPoolListener;
import com.mongodb.event.ConnectionPoolOpenedEvent;
import com.mongodb.event.ConnectionPoolClosedEvent;
import com.mongodb.event.ConnectionCheckedOutEvent;
import com.mongodb.event.ConnectionCheckedInEvent;
import com.mongodb.event.ConnectionAddedEvent;
import com.mongodb.event.ConnectionRemovedEvent;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

public class CustomConnectionPoolListener implements ConnectionPoolListener {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(CustomConnectionPoolListener.class);

    @Override
    public void connectionPoolOpened(final ConnectionPoolOpenedEvent event) {
        LOGGER.debug("Opened Connection Pool Event:{}",event);
    }

    @Override
    public void connectionPoolClosed(final ConnectionPoolClosedEvent event) {
        LOGGER.debug("Closed Connection Pool Event:{}",event);
    }

    @Override
    public void connectionCheckedOut(final ConnectionCheckedOutEvent event) {
        LOGGER.debug("Connection checked out from Pool Event:{}",event);
    }

    @Override
    public void connectionCheckedIn(final ConnectionCheckedInEvent event) {
        LOGGER.debug("Connection checked in from Pool Event:{}",event);
    }

    @Override
    public void connectionAdded(final ConnectionAddedEvent event) {
        LOGGER.debug("Connection Added Event:{}",event);
    }

    @Override
    public void connectionRemoved(final ConnectionRemovedEvent event) {
        LOGGER.debug("Connection Removed Event:{}",event);
    }

}
