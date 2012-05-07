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
			LOG.debug("Activating schedule {}...", getScheduleStoreStorageKey());
		}
		if (null != quartzScheduler) {
			return;
		}

		try {
			// make sure that Quartz does not check for updates
			System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");

			// create scheduler
			final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
			final SimpleThreadPool threadPool = new SimpleThreadPool(1, Thread.NORM_PRIORITY);
			threadPool.setInstanceId(getScheduleStoreStorageKey());
			threadPool.setInstanceName(getScheduleStoreStorageKey());
			final JobStore jobStore = new RAMJobStore();
			factory.createScheduler(getScheduleStoreStorageKey(), getScheduleStoreStorageKey(), threadPool, jobStore);
			quartzScheduler = factory.getScheduler(getScheduleStoreStorageKey());

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

	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		if (ScheduleImpl.ENABLED.equals(event.getKey())) {
			final boolean activate = StringUtils.equals(Boolean.toString(Boolean.TRUE), (String) event.getNewValue());
			try {
				if (activate) {
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
		}
	}

	private synchronized void quietShutdown() {
		if (null != quartzScheduler) {
			try {
				quartzScheduler.shutdown();

				// log success message
				LOG.info("Deactivated schedule {}.", getScheduleStoreStorageKey());
			} catch (final Exception ignored) {
				// ignore
			}
			try {
				SchedulerRepository.getInstance().remove(getScheduleStoreStorageKey());
				if (JobsDebug.schedulerEngine) {
					LOG.debug("Successful removal of Quartz engine {} from scheduler repo.", getScheduleStoreStorageKey());
				}
			} catch (final Exception ignored) {
				// ignore
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
			// put all parameter
			detail.getJobDataMap().putAll(entry.getJobParameter());
			// put type id
			detail.getJobDataMap().put(SchedulingJob.PROP_JOB_TYPE_ID, entry.getJobTypeId());
			// put job id
			detail.getJobDataMap().put(SchedulingJob.PROP_JOB_ID, entry.getJobId());
			// put state
			detail.getJobDataMap().put(SchedulingJob.PROP_ENABLED, entry.isEnabled());
			// put context path
			detail.getJobDataMap().put(SchedulingJob.PROP_JOB_CONTEXT_PATH, ensureScheduleData(Boolean.FALSE).getContextPath().toString());
			// put schedule details
			detail.getJobDataMap().put(SchedulingJob.PROP_SCHEDULE_ID, ensureScheduleData(Boolean.FALSE).getId());
			detail.getJobDataMap().put(SchedulingJob.PROP_SCHEDULE_ENTRY_ID, entry.getId());

			final String cronExpression = entry.getCronExpression();
			final CronTrigger trigger = new CronTrigger(entry.getId());
			trigger.setTimeZone(ensureScheduleData(Boolean.FALSE).getTimeZone());
			try {
				trigger.setCronExpression(asQuartzCronExpression(cronExpression));
			} catch (final ParseException e) {
				LOG.error("Unable to schedule entry {}. Invalid cron expression. {}", entry, ExceptionUtils.getRootCauseMessage(e));
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

		// check if enabled
		// (note, use ensureScheduleData to populate with fresh data)
		if (ensureScheduleData(Boolean.TRUE).isEnabled()) {
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
