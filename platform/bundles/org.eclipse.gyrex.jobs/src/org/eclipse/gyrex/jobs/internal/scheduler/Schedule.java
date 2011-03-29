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

import java.text.ParseException;
import java.util.Collection;

import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleImpl;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

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
 *
 */
public class Schedule implements IPreferenceChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(Schedule.class);

	public static String asQuartzCronExpression(final String cronExpression) {
		// quartz allows for seconds but Unix cron does not support this
		// thus, we always force the second to be '0'
		return "0 " + cronExpression;
	}

	private final ScheduleImpl scheduleData;
	private org.quartz.Scheduler quartzScheduler;

	/**
	 * Creates a new instance.
	 * 
	 * @param scheduleNode
	 * @param scheduler
	 * @throws BackingStoreException
	 */
	public Schedule(final Preferences scheduleNode, final Scheduler scheduler) throws Exception {
		scheduleData = new ScheduleImpl(scheduleNode);
	}

	void activateEngine() {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Activating schedule {}...", getId());
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
			threadPool.setInstanceId(getId());
			threadPool.setInstanceName(getId());
			final JobStore jobStore = new RAMJobStore();
			factory.createScheduler(getId(), getId(), threadPool, jobStore);
			quartzScheduler = factory.getScheduler(getId());

			// TODO add support for calendars (we likely should support global calendars)

			// refresh the schedule
			refreshSchedule();

			// start
			quartzScheduler.start();

			// log success message
			LOG.info("Activated schedule {}.", getId());

		} catch (final SchedulerException e) {
			LOG.error("Unable to activate Quarz scheduler. {}", ExceptionUtils.getRootCauseMessage(e));

			// cleanup
			quietShutdown();
		}

	}

	void deactivateEngine() {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Deactivating Quartz engine {}...", getId());
		}
		quietShutdown();
	}

	public String getId() {
		return scheduleData.getId();
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		if (ScheduleImpl.ENABLED.equals(event.getKey())) {
			try {
				scheduleData.load();
				if (scheduleData.isEnabled()) {
					activateEngine();
				} else {
					deactivateEngine();
				}
			} catch (final BackingStoreException e) {
				LOG.error("Unable to update entry {}. {}", event.getNode().name(), ExceptionUtils.getRootCauseMessage(e));
				quietShutdown();
			}
		}
	}

	private void quietShutdown() {
		if (null != quartzScheduler) {
			try {
				quartzScheduler.shutdown();

				// log success message
				LOG.info("Deactivated schedule {}.", getId());
			} catch (final Exception ignored) {
				// ignore
			}
			try {
				SchedulerRepository.getInstance().remove(getId());
				if (JobsDebug.schedulerEngine) {
					LOG.debug("Successful removal of Quartz engine {} from scheduler repo.", getId());
				}
			} catch (final Exception ignored) {
				// ignore
			}
			quartzScheduler = null;
		}
	}

	private void refreshSchedule() throws SchedulerException {

		// delete all existing jobs
		final String[] jobGroupNames = quartzScheduler.getJobGroupNames();
		if (null != jobGroupNames) {
			for (final String groupName : jobGroupNames) {
				for (final String jobName : quartzScheduler.getJobNames(groupName)) {
					if (JobsDebug.schedulerEngine) {
						LOG.debug("Removing job {} from Quartz engine {}...", jobName, getId());
					}
					quartzScheduler.deleteJob(jobName, groupName);
				}
			}
		}

		// populate with configured jobs
		final Collection<IScheduleEntry> entries = scheduleData.getEntries();
		for (final IScheduleEntry entry : entries) {
			final JobDetail detail = new JobDetail(entry.getId(), SchedulingJob.class);
			// put all parameter
			detail.getJobDataMap().putAll(entry.getJobParameter());
			// put provider id
			detail.getJobDataMap().put(SchedulingJob.PROP_JOB_PROVIDER_ID, entry.getJobProviderId());

			final String cronExpression = entry.getCronExpression();
			final CronTrigger trigger = new CronTrigger(entry.getId());
			trigger.setTimeZone(scheduleData.getTimeZone());
			try {
				trigger.setCronExpression(asQuartzCronExpression(cronExpression));
			} catch (final ParseException e) {
				LOG.error("Unable to schedule entry {}. Invalid cron expression. {}", entry, ExceptionUtils.getRootCauseMessage(e));
			}

			if (JobsDebug.schedulerEngine) {
				LOG.debug("Adding job {} to Quartz engine {}...", entry, getId());
			}
			quartzScheduler.scheduleJob(detail, trigger);
		}
	}

	/**
	 * Starts the schedule
	 */
	public void start() throws Exception {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Starting schedule {}...", getId());
		}

		scheduleData.getPreferenceNode().addPreferenceChangeListener(this);
		scheduleData.load();
		if (scheduleData.isEnabled()) {
			activateEngine();
		} else {
			if (JobsDebug.schedulerEngine) {
				LOG.debug("Schedule {} is disabled.", getId());
			}
		}
	}

	/**
	 * Stops the schedule
	 */
	public void stop() {
		if (JobsDebug.schedulerEngine) {
			LOG.debug("Stopping schedule {}...", getId());
		}

		// bring down engine
		deactivateEngine();

		try {
			scheduleData.getPreferenceNode().removePreferenceChangeListener(this);
		} catch (final Exception e) {
			// might have been removed
		}
	}

}
