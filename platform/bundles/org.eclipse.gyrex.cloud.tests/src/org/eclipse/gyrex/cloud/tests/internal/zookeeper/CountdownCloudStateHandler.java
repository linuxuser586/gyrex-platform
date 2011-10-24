/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.tests.internal.zookeeper;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.events.ICloudEventConstants;
import org.eclipse.gyrex.cloud.tests.internal.CloudTestsActivator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountdownCloudStateHandler implements EventHandler {

	public static enum CloudState {
		ONLINE, OFFLINE, INTERRUPTED
	}

	private static final Logger LOG = LoggerFactory.getLogger(CountdownCloudStateHandler.class);
	private static final BlockingDeque<CloudState> deque = new LinkedBlockingDeque<CloudState>();

	private final ServiceRegistration<EventHandler> registration;

	private volatile CountDownLatch online;
	private volatile CountDownLatch offline;
	private volatile CountDownLatch interrupted;

	/**
	 * Creates a new instance.
	 */
	public CountdownCloudStateHandler() {
		// initializes latches
		reset();

		// register listener
		final BundleContext context = CloudTestsActivator.getInstance().getBundle().getBundleContext();
		final Hashtable<String, Object> properties = new Hashtable<String, Object>(1);
		properties.put(EventConstants.EVENT_TOPIC, Arrays.asList(ICloudEventConstants.TOPIC_NODE_ONLINE, ICloudEventConstants.TOPIC_NODE_OFFLINE, ICloudEventConstants.TOPIC_NODE_INTERRUPTED));
		registration = context.registerService(EventHandler.class, this, properties);
	}

	public void close() {
		registration.unregister();
	}

	@Override
	public void handleEvent(final Event event) {
		LOG.debug("Received cloud event: {}", event);
		if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_ONLINE)) {
			online.countDown();
		} else if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_OFFLINE)) {
			offline.countDown();
		} else if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_INTERRUPTED)) {
			interrupted.countDown();
		}
	}

	public void reset() {
		online = new CountDownLatch(1);
		offline = new CountDownLatch(1);
		interrupted = new CountDownLatch(1);
	}

	public void waitForInterrupted(final int timeout) throws InterruptedException, TimeoutException {
		if (!interrupted.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Did not became interrupted!");
		}
	}

	public void waitForOffline(final int timeout) throws InterruptedException, TimeoutException {
		if (!offline.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Did not became offline!");
		}
	}

	public void waitForOnline(final int timeout) throws InterruptedException, TimeoutException {
		if (!online.await(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("Did not became online!");
		}
	}
}
