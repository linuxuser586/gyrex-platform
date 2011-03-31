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
package org.eclipse.gyrex.boot.tests.internal;

import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;

import org.eclipse.gyrex.cloud.events.ICloudEventConstants;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class Activator extends BaseBundleActivator {

	private static final String SYMBOLIC_NAME = "org.eclipse.gyrex.boot.tests";
	private static volatile Activator instance;

	static final CountDownLatch cloudOnlineWatch = new CountDownLatch(1);

	private static final EventHandler cloudOnlineHandler = new EventHandler() {

		@Override
		public void handleEvent(final Event event) {
			cloudOnlineWatch.countDown();
		}
	};

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static Activator getInstance() {
		final Activator activator = instance;
		if (activator == null) {
			throw new IllegalStateException("inactive");
		}
		return activator;
	}

	/**
	 * Creates a new instance.
	 */
	public Activator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;

		final Hashtable<String, Object> properties = new Hashtable<String, Object>(1);
		properties.put(EventConstants.EVENT_TOPIC, ICloudEventConstants.TOPIC_NODE_ONLINE);
		context.registerService(EventHandler.class, cloudOnlineHandler, properties);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
	}

}
