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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.scheduler;

import java.text.ParseException;
import java.util.List;

import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

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

	private static final Logger LOG = LoggerFactory.getLogger(Schedule.class);

	public static String asQuartzCronExpression(final String cronExpression) {
		// quartz allows for seconds but Unix cron does not support this
		// thus, we always force the second to be '0'
		return "0 " + cronExpression;
	}

	private final String scheduleStoreStorageKey;
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
	}

	synchronized void activateEngine() {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Activating schedule {}...", scheduleStoreStorageKey);
		}
		if (null != quartzScheduler) {
			return;
		}

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
			if (null == quartzScheduler) {
				throw new SchedulerException("Unabled to retrieve created scheduler from Quartz SchedulerRepository. It looks like the creation failed but the Quartz framework did not report it as such!");
			}

			// TODO add support for calendars (we likely should support global calendars)

			// refresh the schedule
			refreshSchedule();

			// start
			quartzScheduler.start();

			// log success message
			LOG.info("Activated schedule {}.", getScheduleStoreStorageKey());

		} catch (final SchedulerException e) {
			LOG.error("Unable to activate Quarz scheduler. {}", ExceptionUtils.getRootCauseMessage(e));

			// cleanup
			quietShutdown();
		}

	}

	synchronized void deactivateEngine() {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Deactivating Quartz engine {}...", getScheduleStoreStorageKey());
		}
		quietShutdown();
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
		if (ScheduleImpl.ENABLED.equals(event.getKey())) {
			final boolean activate = StringUtils.equals(Boolean.toString(Boolean.TRUE), (String) event.getNewValue());
			try {
				if (activate && !isActive()) {
					activateEngine();
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
			// check if now is a good time to activate it
			if (!isActive()) {
				try {
					// (note: do NOT reload schedule data in cace more changes are in flight)
					final ScheduleImpl schedule = ensureScheduleData(Boolean.FALSE);
					if (schedule.isEnabled()) {
						activateEngine();
					}
				} catch (final Exception e) {
					// ignore; still not ready
					quietShutdown();
				}
			}
		}
	}

	private synchronized void quietShutdown() {
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

		// populate with configured jobs
		// (note, it's important that we pass false here in order to prevent the #sync call on the preference node)
		// (otherwise a in-flight preference change event will be reverted)
		// FIXME: this is another case where the preference store is problematic
		final List<IScheduleEntry> entries = ensureScheduleData(Boolean.FALSE).getEntries();
		for (final IScheduleEntry entry : entries) {
			final JobDetail detail = new JobDetail(entry.getId(), SchedulingJob.class);
			detail.getJobDataMap().putAll(entry.getJobParameter());
			detail.getJobDataMap().put(SchedulingJob.PROP_JOB_TYPE_ID, entry.getJobTypeId());
			detail.getJobDataMap().put(SchedulingJob.PROP_JOB_ID, entry.getJobId());
			detail.getJobDataMap().put(SchedulingJob.PROP_ENABLED, entry.isEnabled());
			detail.getJobDataMap().put(SchedulingJob.PROP_JOB_CONTEXT_PATH, ensureScheduleData(Boolean.FALSE).getContextPath().toString());
			detail.getJobDataMap().put(SchedulingJob.PROP_SCHEDULE_ID, ensureScheduleData(Boolean.FALSE).getId());
			detail.getJobDataMap().put(SchedulingJob.PROP_SCHEDULE_ENTRY_ID, entry.getId());

			final String cronExpression = entry.getCronExpression();
			final CronTrigger trigger = new CronTrigger(entry.getId());
			trigger.setTimeZone(ensureScheduleData(Boolean.FALSE).getTimeZone());
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

		if (!ScheduleStore.getSchedulesNode().nodeExists(scheduleStoreStorageKey)) {
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
			return;
		}

		// check if enabled
		if (schedule.isEnabled()) {
			activateEngine();
		} else {
			if (JobsDebug.schedulerEngine) {
				LOG.debug("Schedule {} is disabled.", getScheduleStoreStorageKey());
			}
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
		}
	}
}
