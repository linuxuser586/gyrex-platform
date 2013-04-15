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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;
import org.eclipse.gyrex.cloud.services.locking.ILockService;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.RandomUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The scheduler is responsible for scheduling
 */
public class Scheduler extends Job implements INodeChangeListener {

	private static final String SCHEDULER_LOCK = JobsActivator.SYMBOLIC_NAME + ".scheduler";
	private static final long INITIAL_SLEEP_TIME = TimeUnit.SECONDS.toMillis(30);
	private static final long MAX_SLEEP_TIME = TimeUnit.MINUTES.toMillis(5);

	private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

	private long engineSleepTime = INITIAL_SLEEP_TIME;
	private final ConcurrentMap<String, Schedule> schedulesById = new ConcurrentHashMap<String, Schedule>();
	private final SchedulerApplicationMetrics metrics;

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 */
	public Scheduler(final SchedulerApplicationMetrics metrics) {
		super("Gyrex Scheduler");
		this.metrics = metrics;
		setSystem(true);
		setPriority(SHORT);
		addJobChangeListener(new IJobChangeListener() {

			@Override
			public void aboutToRun(final IJobChangeEvent event) {
				metrics.setStatus("ABOUTTORUN", "job event");
			}

			@Override
			public void awake(final IJobChangeEvent event) {
				metrics.setStatus("AWAKE", "job event");
			}

			@Override
			public void done(final IJobChangeEvent event) {
				metrics.setStatus("DONE", "job event");
			}

			@Override
			public void running(final IJobChangeEvent event) {
				metrics.setStatus("RUNNING", "job event");
			}

			@Override
			public void scheduled(final IJobChangeEvent event) {
				metrics.setStatus("SCHEDULED", "job event");
			}

			@Override
			public void sleeping(final IJobChangeEvent event) {
				metrics.setStatus("SLEEPING", "job event");
			}
		});
	}

	@Override
	public void added(final NodeChangeEvent event) {
		try {
			addSchedule(event.getChild().name());
		} catch (final Exception e) {
			LOG.error("Unable to start schedule {}. {}", new Object[] { event.getChild().name(), ExceptionUtils.getRootCauseMessage(e), e });
		}
	}

	private void addSchedule(final String scheduleStoreStorageKey) throws Exception {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Adding schedule {}...", scheduleStoreStorageKey);
		}

		final Schedule schedule = new Schedule(scheduleStoreStorageKey, this);
		if (null == schedulesById.putIfAbsent(scheduleStoreStorageKey, schedule)) {
			schedule.start();
		}
	}

	private IStatus doRun(final IProgressMonitor monitor) {
		IExclusiveLock schedulerEngineLock = null;
		try {
			// get scheduler lock first
			// this ensures that there is at most one scheduler
			// engine is active in the whole cloud
			if (JobsDebug.schedulerEngine) {
				LOG.debug("Waiting for global scheduler engine lock.");
			}
			final ILockService lockService = JobsActivator.getInstance().getService(ILockService.class);
			while (schedulerEngineLock == null) {
				// check for cancellation
				if (monitor.isCanceled())
					throw new OperationCanceledException();

				metrics.setStatus("WAITINGFORLOCK", "lock acquire loop");

				// try to acquire lock
				// (note, we cannot wait forever because we must check for cancelation regularly)
				// (however, checking very often is too expensive; we need to make a tradeoff here)
				// (randomizing might be a good strategy here; modifying the time here should also cause updates to the shutdown timeout in SchedulerApplication)
				try {
					schedulerEngineLock = lockService.acquireExclusiveLock(SCHEDULER_LOCK, null, 10000 + RandomUtils.nextInt(50000));
				} catch (final TimeoutException e) {
					// timeout waiting for lock
					// we simply keep on going as long as we aren't canceled
					Thread.yield();
				}
			}

			// check for cancellation
			if (monitor.isCanceled())
				throw new OperationCanceledException();

			metrics.setStatus("LOCKACQUIRED", "lock acquire loop");

			// setup the schedule listeners
			final IEclipsePreferences schedulesNode = ScheduleStore.getSchedulesNode();
			schedulesNode.addNodeChangeListener(this);

			// hook with all existing schedules
			for (final String scheduleStoreStorageKey : ScheduleStore.getSchedules()) {
				try {
					addSchedule(scheduleStoreStorageKey);
				} catch (final Exception e) {
					LOG.error("Unable to start schedule {}. {}", scheduleStoreStorageKey, ExceptionUtils.getRootCauseMessage(e));
				}
			}

			// spin the loop while we are good to go
			while (schedulerEngineLock.isValid() && !monitor.isCanceled()) {
				Thread.sleep(1000L);
			}

			if (JobsDebug.schedulerEngine) {
				LOG.debug("Scheduler engine canceled. Shutting down.");
			}
		} catch (final IllegalStateException e) {
			metrics.setStatus("ERROR", ExceptionUtils.getRootCauseMessage(e));
			LOG.warn("Unable to check for schedules. System does not seem to be ready. {}", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
		} catch (final InterruptedException e) {
			metrics.setStatus("INTERRUPTED", ExceptionUtils.getRootCauseMessage(e));
			Thread.currentThread().interrupt();
			return Status.CANCEL_STATUS;
		} catch (final BackingStoreException e) {
			metrics.setStatus("ERROR", ExceptionUtils.getRootCauseMessage(e));
			LOG.error("Error reading schedules. {}", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
		} finally {
			try {
				// remove listener
				try {
					ScheduleStore.getSchedulesNode().removeNodeChangeListener(this);
				} catch (final Exception e) {
					// might already be going down
				}

				// bring down all schedules
				final Collection<Schedule> schedules = schedulesById.values();
				for (final Schedule schedule : schedules) {
					try {
						schedule.stop();
					} catch (final Exception e) {
						// ignore
					}
				}
				schedulesById.clear();
			} finally {
				// release lock
				if (null != schedulerEngineLock) {
					try {
						schedulerEngineLock.release();
					} catch (final Exception e) {
						// ignore
					}
				}
			}
		}

		return Status.OK_STATUS;
	}

	@Override
	public void removed(final NodeChangeEvent event) {
		try {
			removeSchedule(event.getChild().name());
		} catch (final Exception e) {
			LOG.error("Unable to stop schedule {}. {}", event.getChild().name(), ExceptionUtils.getRootCauseMessage(e));
		}
	}

	private void removeSchedule(final String id) {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Removing schedule {}...", id);
		}

		final Schedule schedule = schedulesById.remove(id);
		if (null == schedule)
			return;

		schedule.stop();
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			final IStatus status = doRun(monitor);
			if (!status.isOK()) {
				// implement a back-off sleeping time (max 5 min)
				engineSleepTime = Math.min(engineSleepTime * 2, MAX_SLEEP_TIME);
			} else {
				// reset sleep time
				engineSleepTime = INITIAL_SLEEP_TIME;
			}
			return status;
		} finally {
			// reschedule if not canceled
			if (!monitor.isCanceled()) {
				if (JobsDebug.schedulerEngine) {
					LOG.debug("Rescheduling scheduler engine to run again in {} seconds", TimeUnit.MILLISECONDS.toSeconds(engineSleepTime));
				}
				schedule(engineSleepTime);
			}
		}
	}

}
