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

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.jobs.internal.JobsDebug;

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
				engine.join();
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
