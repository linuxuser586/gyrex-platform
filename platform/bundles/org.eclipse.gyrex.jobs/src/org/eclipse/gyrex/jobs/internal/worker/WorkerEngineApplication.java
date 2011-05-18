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
package org.eclipse.gyrex.jobs.internal.worker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.jobs.internal.JobsDebug;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker engine application.
 */
public class WorkerEngineApplication implements IApplication {

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = Integer.valueOf(1);

	private static final Logger LOG = LoggerFactory.getLogger(WorkerEngineApplication.class);
	private static final AtomicReference<WorkerEngine> workerEngineRef = new AtomicReference<WorkerEngine>(null);
	private IApplicationContext runningContext;

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		if (JobsDebug.workerEngine) {
			LOG.debug("Starting worker engine application.");
		}

		// set stop signal
		final WorkerEngine workerEngine = new WorkerEngine();
		if (!workerEngineRef.compareAndSet(null, workerEngine)) {
			throw new IllegalStateException("Worker engine already running!");
		}

		try {
			// launch worker engine
			workerEngine.schedule();

			// signal running
			context.applicationRunning();

			// finish async
			runningContext = context;
			return IApplicationContext.EXIT_ASYNC_RESULT;
		} catch (final Exception e) {
			LOG.error("Unable to start worker engine. Please check the log files.", e);
			return EXIT_ERROR;
		}
	}

	@Override
	public void stop() {
		final IApplicationContext context = runningContext;
		if (context == null) {
			throw new IllegalStateException("not started");
		}

		final WorkerEngine engine = workerEngineRef.getAndSet(null);
		if (engine == null) {
			return;
		}

		if (JobsDebug.schedulerEngine) {
			LOG.debug("Stopping scheduler engine application...");
		}

		if (!engine.cancel()) {
			try {
				LOG.info("Waiting for worker engine to finish remaining work...");
				final CountDownLatch wait = new CountDownLatch(1);
				final Job job = new Job("Worker Engine Shutdown Job") {

					@Override
					protected IStatus run(final IProgressMonitor monitor) {
						try {
							if (engine.getState() == Job.RUNNING) {
								engine.join();
							}
						} catch (final InterruptedException e) {
							Thread.currentThread().interrupt();
						} finally {
							wait.countDown();
						}
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.setPriority(Job.SHORT);
				job.schedule();
				if (!wait.await(30, TimeUnit.SECONDS)) {
					LOG.warn("Time out waiting for worker engine to finish remaining work. A complete restart of the process may be required.");
				}
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (JobsDebug.schedulerEngine) {
			LOG.debug("Starting scheduler engine stopped.");
		}
		context.setResult(EXIT_OK, this);
	}

}
