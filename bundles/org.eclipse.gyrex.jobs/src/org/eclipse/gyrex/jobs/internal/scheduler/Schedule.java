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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.scheduler;

import java.text.ParseException;
import java.util.List;

import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A quarz schedule
 */
public class Schedule implements IPreferenceChangeListener {

	private class DeferredActivationJob extends Job {

		public DeferredActivationJob() {
			super("Deferred activation for schedule " + getScheduleStoreStorageKey());
			setSystem(true);
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			try {
				// Force re-load of schedule data
				final ScheduleImpl schedule = ensureScheduleData(Boolean.TRUE);
				if (schedule.isEnabled()) {
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					activateEngine();
				} else {
					deactivateEngine();
				}
				return Status.OK_STATUS;
			} catch (final RuntimeException e) {
				LOG.error("Error activating schedule {}: {}", new Object[] { getScheduleStoreStorageKey(), e.getMessage(), e });
				quietShutdown();
				return Status.CANCEL_STATUS;
			}
		}

	}

	private static final Logger LOG = LoggerFactory.getLogger(Schedule.class);
	private static final long DEFERRED_ACTIVATION_DELAY = 10000L; // 10 seconds

	public static String asQuartzCronExpression(final String cronExpression) {
		// quartz allows for seconds but Unix cron does not support this
		// thus, we always force the second to be '0'
		return "0 " + cronExpression;
	}

	private final DeferredActivationJob deferredActivationJob = new DeferredActivationJob();
	private final String scheduleStoreStorageKey;

	private final ScheduleMetrics metrics;
	private ServiceRegistration<MetricSet> metricsRegistration;
	private ScheduleImpl scheduleData;

	private org.quartz.Scheduler quartzScheduler;

	/**
	 * Creates a new instance.
	 * 
	 * @param scheduleStoreStorageKey
	 * @param scheduler
	 * @throws BackingStoreException
	 */
	public Schedule(final String scheduleStoreStorageKey, final Scheduler scheduler) throws Exception {
		this.scheduleStoreStorageKey = scheduleStoreStorageKey;
		metrics = new ScheduleMetrics(scheduleStoreStorageKey);
	}

	synchronized void activateEngine() {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Activating schedule {}...", scheduleStoreStorageKey);
		}
		if (null != quartzScheduler)
			return;

		try {
			// make sure that Quartz does not check for updates
			System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");

			// prepare scheduler 
			final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
			final SimpleThreadPool threadPool = new SimpleThreadPool(1, Thread.NORM_PRIORITY);
			threadPool.setInstanceId(scheduleStoreStorageKey);
			threadPool.setInstanceName(scheduleStoreStorageKey);
			final JobStore jobStore = new RAMJobStore();

			// create scheduler
			// (make sure that only a single thread manipulates the SchedulerRepository)
			final SchedulerRepository repository = SchedulerRepository.getInstance();
			synchronized (repository) {
				factory.createScheduler(scheduleStoreStorageKey, scheduleStoreStorageKey, threadPool, jobStore);
				quartzScheduler = factory.getScheduler(scheduleStoreStorageKey);
				if (null == quartzScheduler) {
					quartzScheduler = repository.lookup(scheduleStoreStorageKey);
				}
			}

			// double check to ensure that Quartz did not fail
			if (null == quartzScheduler)
				throw new SchedulerException("Unabled to retrieve created scheduler from Quartz SchedulerRepository. It looks like the creation failed but the Quartz framework did not report it as such!");

			// TODO add support for calendars (we likely should support global calendars)

			// refresh the schedule
			refreshSchedule();

			// start
			quartzScheduler.start();
			metrics.setStatus("QUARTZRUNNING", "Quartz schedule started successfully");

			// log success message
			LOG.info("Activated schedule {}.", getScheduleStoreStorageKey());
		} catch (final SchedulerException e) {
			LOG.error("Unable to activate Quarz scheduler. {}", ExceptionUtils.getRootCauseMessage(e));
			metrics.error("error activating schedule", e);

			// cleanup
			quietShutdown();
		}

	}

	synchronized void deactivateEngine() {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Deactivating Quartz engine {}...", getScheduleStoreStorageKey());
		}
		quietShutdown();
		metrics.setStatus("DEACTIVATED", "schedule deactivated");
	}

	/**
	 * Returns the scheduleData.
	 * 
	 * @return the scheduleData
	 */
	private ScheduleImpl ensureScheduleData(final boolean forceReload) {
		if ((null == scheduleData) || forceReload) {
			try {
				return scheduleData = ScheduleStore.load(scheduleStoreStorageKey, ScheduleManagerImpl.getExternalId(scheduleStoreStorageKey), true);
			} catch (final Exception e) {
				throw new IllegalStateException(String.format("Unable to load schedule '%s'. %s", scheduleStoreStorageKey, e.getMessage()), e);
			}
		}

		return scheduleData;
	}

	public String getScheduleStoreStorageKey() {
		return scheduleStoreStorageKey;
	}

	synchronized boolean isActive() {
		try {
			return (null != quartzScheduler) && quartzScheduler.isStarted();
		} catch (final SchedulerException e) {
			return false;
		}
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		// cancel any ongoing activation
		deferredActivationJob.cancel();

		if (ScheduleImpl.ENABLED.equals(event.getKey())) {
			final boolean activate = StringUtils.equals(Boolean.toString(Boolean.TRUE), (String) event.getNewValue());
			try {
				if (activate) {
					deferredActivationJob.schedule(DEFERRED_ACTIVATION_DELAY);
				} else {
					deactivateEngine();
				}
			} catch (final Exception e) {
				if (activate) {
					LOG.error("Error activating schedule '{}'. {}", new Object[] { event.getNode().name(), ExceptionUtils.getRootCauseMessage(e), e });
				} else {
					LOG.error("Error deactivating schedule '{}'. {}", new Object[] { event.getNode().name(), ExceptionUtils.getRootCauseMessage(e), e });
				}
				quietShutdown();
			}
		} else {
			// a schedule might have been incomplete previously and not activated properly;
			// check if now is a good time to activate it and try to activate it
			if (!isActive()) {
				try {
					deferredActivationJob.schedule(DEFERRED_ACTIVATION_DELAY);
				} catch (final Exception e) {
					// ignore; still not ready
					quietShutdown();
				}
			}
		}
	}

	private synchronized void quietShutdown() {
		// cancel any ongoing activation
		deferredActivationJob.cancel();

		// make sure that there is no Quartz schedule left in the Quartz scheduler repo
		final SchedulerRepository repository = SchedulerRepository.getInstance();
		if (null == quartzScheduler) {
			synchronized (repository) {
				quartzScheduler = repository.lookup(scheduleStoreStorageKey);
			}
		}

		// shutdown Quartz scheduler
		if (null != quartzScheduler) {
			try {
				// remove from SchedulerRepository
				boolean removed;
				synchronized (repository) {
					removed = repository.remove(scheduleStoreStorageKey);
				}
				if (!removed) {
					LOG.error("Quartz eninge for schedule {} could not be removed from the Quartz scheduler repository. Please monitor the process memory and scheduling closely for anomalies. A restart of the node may be necessary.", scheduleStoreStorageKey);
				} else if (JobsDebug.schedulerEngine) {
					LOG.debug("Successful removal of Quartz engine {} from scheduler repo.", scheduleStoreStorageKey);
				}
			} catch (final Exception ignored) {
				// ignore
			} finally {
				try {
					// shutdown
					quartzScheduler.shutdown();
					metrics.setStatus("QUARTZSTOPPED", "quiet shutdown triggered");

					// log success message
					LOG.info("Deactivated schedule {}.", scheduleStoreStorageKey);
				} catch (final Exception ignored) {
					// ignore
				}
			}
			quartzScheduler = null;
		}
	}

	private synchronized void refreshSchedule() throws SchedulerException {

		// delete all existing jobs
		final String[] jobGroupNames = quartzScheduler.getJobGroupNames();
		if (null != jobGroupNames) {
			for (final String groupName : jobGroupNames) {
				for (final String jobName : quartzScheduler.getJobNames(groupName)) {
					if (JobsDebug.schedulerEngine) {
						LOG.debug("Removing job {} from Quartz engine {}...", jobName, getScheduleStoreStorageKey());
					}
					quartzScheduler.deleteJob(jobName, groupName);
				}
			}
		}

		// get configured jobs
		// (note, it's important that we pass false here to ensureScheduleData in order to prevent the #sync call on the preference node)
		// (otherwise a in-flight preference change event will be reverted)
		// FIXME: this is another case where the preference store is problematic
		final ScheduleImpl schedule = ensureScheduleData(false);
		final List<IScheduleEntry> entries = schedule.getEntries();

		// schedule entries with cron expression if available
		for (final IScheduleEntry entry : entries) {
			final String cronExpression = entry.getCronExpression();
			if (StringUtils.isBlank(cronExpression)) {
				continue;
			}

			final JobDetail detail = new JobDetail(entry.getId(), SchedulingJob.class);
			SchedulingJob.populateJobDataMap(detail.getJobDataMap(), entry, schedule);

			final CronTrigger trigger = new CronTrigger(entry.getId());
			trigger.setTimeZone(schedule.getTimeZone());
			try {
				trigger.setCronExpression(asQuartzCronExpression(cronExpression));
			} catch (final ParseException e) {
				LOG.error("Unable to schedule entry {}. Invalid cron expression. {}", entry, ExceptionUtils.getRootCauseMessage(e));
				continue;
			}

			if (JobsDebug.schedulerEngine) {
				LOG.debug("Adding job {} to Quartz engine {}...", entry, getScheduleStoreStorageKey());
			}
			quartzScheduler.scheduleJob(detail, trigger);
		}
	}

	/**
	 * Starts the schedule
	 */
	public void start() throws Exception {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Starting schedule {}...", getScheduleStoreStorageKey());
		}

		metricsRegistration = JobsActivator.registerMetrics(metrics);

		if (!ScheduleStore.getSchedulesNode().nodeExists(scheduleStoreStorageKey)) {
			metrics.setStatus("NOTFOUND", "not found during start");
			throw new IllegalStateException(String.format("Schedule '%s' not found", scheduleStoreStorageKey));
		}

		// get preference node
		final IEclipsePreferences node = (IEclipsePreferences) ScheduleStore.getSchedulesNode().node(scheduleStoreStorageKey);

		// add listener
		node.addPreferenceChangeListener(this);

		// load the schedule
		// (note, use ensureScheduleData to populate with fresh data)
		ScheduleImpl schedule;
		try {
			schedule = ensureScheduleData(Boolean.TRUE);
		} catch (final IllegalStateException e) {
			// the schedule might be in an incomplete "in-creation" phase
			// don't activate the engine right now and wait for the complete creation
			if (JobsDebug.schedulerEngine) {
				LOG.debug("Schedule {} will not be activated due to loading errors. It looks like its creation is still in progress.", scheduleStoreStorageKey, e);
			}
			metrics.setStatus("ERROR", "exception during start");
			metrics.error("loading error", e);
			return;
		}

		// check if enabled
		if (schedule.isEnabled()) {
			metrics.setStatus("ACTIVATING", "schedule started");
			activateEngine();
		} else {
			if (JobsDebug.schedulerEngine) {
				LOG.debug("Schedule {} is disabled.", getScheduleStoreStorageKey());
			}
			metrics.setStatus("DISABLED", "schedule is disabled");
		}
	}

	/**
	 * Stops the schedule
	 */
	public void stop() {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Stopping schedule {}...", getScheduleStoreStorageKey());
		}

		// bring down engine
		deactivateEngine();

		try {
			if (ScheduleStore.getSchedulesNode().nodeExists(scheduleStoreStorageKey)) {
				// get preference node
				final IEclipsePreferences node = (IEclipsePreferences) ScheduleStore.getSchedulesNode().node(scheduleStoreStorageKey);

				// remove listener
				node.removePreferenceChangeListener(this);
			}
		} catch (final Exception e) {
			// might have been removed
		} finally {
			final ServiceRegistration<MetricSet> registration = metricsRegistration;
			if (registration != null) {
				try {
					registration.unregister();
				} catch (final IllegalStateException e) {
					// ignore
				} finally {
					metricsRegistration = null;
				}
			}
		}

		metrics.setStatus("STOPPED", "schedule stopped");
	}
}
