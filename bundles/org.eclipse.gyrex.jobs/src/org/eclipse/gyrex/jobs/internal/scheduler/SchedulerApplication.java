/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.scheduler;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.common.internal.applications.BaseApplication;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.util.WaitForJobToFinishJob;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.eclipse.core.runtime.jobs.Job;

import org.osgi.framework.ServiceRegistration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler engine application of scheduling {@link ISchedule schedules}.
 */
public class SchedulerApplication extends BaseApplication {

	private static final Logger LOG = LoggerFactory.getLogger(SchedulerApplication.class);
	private Scheduler scheduler;

	private SchedulerApplicationMetrics metrics;
	private ServiceRegistration<MetricSet> metricsRegistration;

	@Override
	protected void doCleanup() {
		final Scheduler scheduler = this.scheduler;
		if (null != scheduler) {
			this.scheduler = null;
			scheduler.cancel();
		}
	}

	@Override
	protected void doStart(@SuppressWarnings("rawtypes") final Map arguments) throws Exception {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Starting scheduler engine application.");
		}

		// register metrics
		metrics = new SchedulerApplicationMetrics();
		metricsRegistration = JobsActivator.registerMetrics(metrics);

		metrics.setApplicationStatus("STARTING", "application started");

		// create & start scheduler engine
		scheduler = new Scheduler(metrics);
		scheduler.schedule();

		metrics.setApplicationStatus("STARTED", "scheduler activated");
	}

	@Override
	protected Object doStop() {
		final Scheduler scheduler = this.scheduler;
		if (null == scheduler)
			return EXIT_OK;

		if (JobsDebug.schedulerEngine) {
			LOG.debug("Stopping scheduler engine application...");
		}

		final ServiceRegistration<MetricSet> metricsRegistration = this.metricsRegistration;
		final SchedulerApplicationMetrics metrics = this.metrics;
		metrics.setApplicationStatus("STOPPING", "application shutdown");

		// unset
		this.scheduler = null;
		this.metricsRegistration = null;
		this.metrics = null;

		// cancel
		try {

			if (!scheduler.cancel()) {
				try {
					final int timeoutInSeconds = 60;
					LOG.info("Waiting up to {}s for scheduler engine to finish...", timeoutInSeconds);
					final CountDownLatch wait = new CountDownLatch(1);
					final Job job = new WaitForJobToFinishJob("Scheduler Engine Shutdown Job", scheduler, wait);
					job.schedule();
					if (!wait.await(timeoutInSeconds, TimeUnit.SECONDS)) {
						LOG.warn("Time out waiting for scheduler engine to shutdown. A complete restart (or shutdown) of the current process is recommended.");
						return EXIT_ERROR;
					}
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		} finally {
			if (metricsRegistration != null) {
				try {
					metricsRegistration.unregister();
				} catch (final IllegalStateException e) {
					// ignore
				}
			}
		}

		if (JobsDebug.schedulerEngine) {
			LOG.debug("Scheduler engine application engine stopped.");
		}

		return EXIT_OK;
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}

}
