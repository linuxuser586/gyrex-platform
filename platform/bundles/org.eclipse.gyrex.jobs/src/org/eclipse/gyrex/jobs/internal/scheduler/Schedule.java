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

	private org.quartz.Scheduler quarzScheduler;

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
		if (null != quarzScheduler) {
			return;
		}

		try {
			// create scheduler
			final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
			factory.createVolatileScheduler(1);
			quarzScheduler = factory.getScheduler(getId());

			// TODO add support for calendars (we likely should support global calendars)

			// refresh the schedule
			refreshSchedule();

			// start
			quarzScheduler.start();

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
		if (null != quarzScheduler) {
			try {
				quarzScheduler.shutdown();

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
			quarzScheduler = null;
		}
	}

	private void refreshSchedule() throws SchedulerException {

		// delete all existing jobs
		for (final String groupName : quarzScheduler.getJobGroupNames()) {
			for (final String jobName : quarzScheduler.getJobNames(groupName)) {
				if (JobsDebug.schedulerEngine) {
					LOG.debug("Removing job {} from Quartz engine {}...", jobName, getId());
				}
				quarzScheduler.deleteJob(jobName, groupName);
			}
		}

		// populate with configured jobs
		final Collection<IScheduleEntry> entries = scheduleData.getEntries();
		for (final IScheduleEntry entry : entries) {
			final JobDetail detail = new JobDetail(entry.getId(), SchedulingJob.class);
			detail.getJobDataMap().put(SchedulingJob.PROP_JOB_PROVIDER_ID, entry.getJobProviderId());
			detail.getJobDataMap().putAll(entry.getJobParameter());

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
			quarzScheduler.scheduleJob(detail, trigger);
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
