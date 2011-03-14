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
	private static final AtomicReference<CountDownLatch> stopSignalRef = new AtomicReference<CountDownLatch>(null);

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		if (JobsDebug.workerEngine) {
			LOG.debug("Starting worker engine application.");
		}

		// set stop signal
		final CountDownLatch stopSignal = new CountDownLatch(1);
		if (!stopSignalRef.compareAndSet(null, stopSignal)) {
			throw new IllegalStateException("Worker engine already running!");
		}

		try {
			// launch worker engine

			// signal running
			context.applicationRunning();

			// wait for stop
			try {
				stopSignal.await();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} catch (final Exception e) {
			LOG.error("Unable to start worker engine. Please check the log files.", e);
			return EXIT_ERROR;
		} finally {
			// done, now reset signal to allow further starts
			stopSignalRef.compareAndSet(stopSignal, null);
		}

		return EXIT_OK;
	}

	@Override
	public void stop() {
		final CountDownLatch stopSignal = stopSignalRef.get();
		if (stopSignal != null) {
			stopSignal.countDown();
		}
	}

}
