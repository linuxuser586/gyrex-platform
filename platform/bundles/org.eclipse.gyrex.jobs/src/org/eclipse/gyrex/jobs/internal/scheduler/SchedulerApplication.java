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
package org.eclipse.gyrex.jobs.internal.scheduler;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.schedules.ISchedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler engine application of scheduling {@link ISchedule schedules}.
 */
public class SchedulerApplication implements IApplication {

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = Integer.valueOf(1);

	private static final Logger LOG = LoggerFactory.getLogger(SchedulerApplication.class);
	private static final AtomicReference<Scheduler> schedulerRef = new AtomicReference<Scheduler>(null);
	private IApplicationContext runningContext;

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Starting scheduler engine application.");
		}

		// set stop signal
		final Scheduler scheduler = new Scheduler();
		if (!schedulerRef.compareAndSet(null, scheduler)) {
			throw new IllegalStateException("Scheduler engine already running!");
		}

		try {
			// start scheduler engine
			scheduler.schedule();

			// signal running
			context.applicationRunning();

			// finish async
			runningContext = context;
			return IApplicationContext.EXIT_ASYNC_RESULT;
		} catch (final Exception e) {
			LOG.error("Unable to start scheduler engine. Please check the log files.", e);
			return EXIT_ERROR;
		}
	}

	@Override
	public void stop() {
		final IApplicationContext context = runningContext;
		if (context == null) {
			throw new IllegalStateException("not started");
		}

		final Scheduler scheduler = schedulerRef.getAndSet(null);
		if (scheduler == null) {
			return;
		}

		scheduler.cancel();
		context.setResult(EXIT_OK, this);
	}

}
