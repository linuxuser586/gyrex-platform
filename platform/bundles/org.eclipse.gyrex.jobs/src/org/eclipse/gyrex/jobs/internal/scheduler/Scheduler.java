/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
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
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;
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

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 */
	public Scheduler() {
		super("Gyrex Scheduler");
		setSystem(true);
		setPriority(SHORT);
	}

	@Override
	public void added(final NodeChangeEvent event) {
		try {
			addSchedule(event.getChild());
		} catch (final Exception e) {
			LOG.error("Unable to start schedule {}. {}", event.getChild().name(), ExceptionUtils.getRootCauseMessage(e));
		}
	}

	private void addSchedule(final Preferences scheduleNode) throws Exception {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Adding schedule {}...", scheduleNode.name());
		}

		final Schedule schedule = new Schedule(scheduleNode, this);
		if (null == schedulesById.putIfAbsent(schedule.getId(), schedule)) {
			schedule.start();
		}
	}

	private IStatus doRun(final IProgressMonitor monitor) {
		IExclusiveLock schedulerEngineLock = null;
		try {
			// get schedule lock first
			// this ensures that there is at most one schedule
			// engine active in the whole cloud
			if (JobsDebug.schedulerEngine) {
				LOG.debug("Waiting for global scheduler engine lock.");
			}
			final ILockService lockService = JobsActivator.getInstance().getService(ILockService.class);
			try {
				schedulerEngineLock = lockService.acquireExclusiveLock(SCHEDULER_LOCK, null, 0L);
			} catch (final TimeoutException e) {
				// timeout waiting for lock
				// in theory, this should not happen because
				// we wait forever
			}

			// check for cancellation
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			// setup the schedule listeners
			final IEclipsePreferences schedulesNode = ScheduleManagerImpl.getSchedulesNode();
			schedulesNode.addNodeChangeListener(this);

			// hook with all existing schedules
			final String[] childrenNames = schedulesNode.childrenNames();
			for (final String scheduleId : childrenNames) {
				try {
					addSchedule(schedulesNode.node(scheduleId));
				} catch (final Exception e) {
					LOG.error("Unable to start schedule {}. {}", scheduleId, ExceptionUtils.getRootCauseMessage(e));
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
			LOG.warn("Unable to check for schedules. System does not seem to be ready. {}", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return Status.CANCEL_STATUS;
		} catch (final BackingStoreException e) {
			LOG.error("Error reading schedules. {}", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
		} finally {
			try {
				// remove listener
				try {
					ScheduleManagerImpl.getSchedulesNode().removeNodeChangeListener(this);
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
		if (null == schedule) {
			return;
		}

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
