/**
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=346996
 */
package org.eclipse.gyrex.jobs.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.jobs.internal.registry.JobProviderRegistry;

import org.osgi.framework.BundleContext;

/**
 * Bundle activator.
 */
public class JobsActivator extends BaseBundleActivator {

	/** SYMBOLIC_NAME */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.jobs";

	private static final AtomicReference<JobsActivator> instanceRef = new AtomicReference<JobsActivator>();

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static JobsActivator getInstance() {
		final JobsActivator activator = instanceRef.get();
		if (activator == null) {
			throw new IllegalStateException("inactive");
		}
		return activator;
	}

	private volatile IServiceProxy<IQueueService> queueServiceProxy;
	private volatile JobProviderRegistry jobProviderRegistry;

	/**
	 * Creates a new instance.
	 */
	public JobsActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);
		queueServiceProxy = getServiceHelper().trackService(IQueueService.class);
		jobProviderRegistry = new JobProviderRegistry(context);
		jobProviderRegistry.open();
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);

		queueServiceProxy = null;

		jobProviderRegistry.close();
		jobProviderRegistry = null;
	}

	@Override
	protected Class getDebugOptions() {
		return JobsDebug.class;
	}

	/**
	 * Returns the jobProviderRegistry.
	 * 
	 * @return the jobProviderRegistry
	 */
	public JobProviderRegistry getJobProviderRegistry() {
		final JobProviderRegistry providerRegistry = jobProviderRegistry;
		if (null == providerRegistry) {
			throw createBundleInactiveException();
		}
		return providerRegistry;
	}

	/**
	 * Returns the queueServiceProxy.
	 * 
	 * @return the queueServiceProxy
	 */
	public IQueueService getQueueService() {
		final IServiceProxy<IQueueService> proxy = queueServiceProxy;
		if (proxy == null) {
			createBundleInactiveException();
		}
		return proxy.getService();
	}

	public void removeService(final Object service) {
	}
}
