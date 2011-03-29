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
 */
package org.eclipse.gyrex.jobs.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.jobs.internal.registry.JobProviderRegistry;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;
import org.eclipse.gyrex.jobs.schedules.IScheduleManager;

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
	private volatile ScheduleManagerImpl scheduleManager;
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

		jobProviderRegistry = new JobProviderRegistry();
		jobProviderRegistry.activate();

		scheduleManager = new ScheduleManagerImpl();
		getServiceHelper().registerService(IScheduleManager.class, scheduleManager, "Eclipse Gyrex", "Gyrex Schedule Manager", null, null);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);

		queueServiceProxy = null;

		scheduleManager = null;

		jobProviderRegistry.deactivate();
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

	/**
	 * Returns the scheduleManager.
	 * 
	 * @return the scheduleManager
	 */
	public ScheduleManagerImpl getScheduleManager() {
		final ScheduleManagerImpl manager = scheduleManager;
		if (null == manager) {
			throw createBundleInactiveException();
		}
		return manager;
	}
}
