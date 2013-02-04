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
package org.eclipse.gyrex.jobs.internal.worker;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.common.internal.applications.BaseApplication;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.util.WaitForJobToFinishJob;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.eclipse.core.runtime.jobs.Job;

import org.osgi.framework.ServiceRegistration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker engine application.
 */
public class WorkerEngineApplication extends BaseApplication {

	private static final Logger LOG = LoggerFactory.getLogger(WorkerEngineApplication.class);

	private final WorkerEngineMetrics metrics = new WorkerEngineMetrics();
	private WorkerEngine workerEngine;
	private ServiceRegistration<MetricSet> metricsRegistration;

	@Override
	protected void doCleanup() {
		final WorkerEngine engine = workerEngine;
		if (null != engine) {
			workerEngine = null;
			engine.cancel();
		}
	}

	@Override
	protected void doStart(@SuppressWarnings("rawtypes") final Map arguments) throws Exception {
		if (JobsDebug.workerEngine) {
			LOG.debug("Starting worker engine application.");
		}

		metricsRegistration = JobsActivator.registerMetrics(metrics);

		// create & launch worker engine
		workerEngine = new WorkerEngine(metrics);
		workerEngine.schedule();
	}

	@Override
	protected Object doStop() {
		final WorkerEngine engine = workerEngine;
		if (null == engine)
			return EXIT_OK;

		if (JobsDebug.workerEngine) {
			LOG.debug("Stopping worker engine application...");
		}

		// unset
		workerEngine = null;

		// cancel
		if (!engine.cancel()) {
			try {
				final int timeoutInSeconds = 30;
				LOG.info("Waiting {}s for worker engine to finish remaining work gracefully...", timeoutInSeconds);
				final CountDownLatch wait = new CountDownLatch(1);
				final Job job = new WaitForJobToFinishJob("Worker Engine Shutdown Job", engine, wait);
				job.schedule();
				if (!wait.await(timeoutInSeconds, TimeUnit.SECONDS)) {
					LOG.warn("Time out waiting for worker engine to finish remaining work. A complete restart (or shutdown) of the current process is recommended.");
					return EXIT_ERROR;
				}
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (JobsDebug.workerEngine) {
			LOG.debug("Worker engine application engine stopped.");
		}

		if (metricsRegistration != null) {
			metricsRegistration.unregister();
			metricsRegistration = null;
		}

		return EXIT_OK;
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}
}
