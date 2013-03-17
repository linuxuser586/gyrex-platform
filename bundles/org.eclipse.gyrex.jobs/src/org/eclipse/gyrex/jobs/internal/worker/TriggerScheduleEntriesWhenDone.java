/*******************************************************************************
 * Copyright (c) 2013 <enter-company-name-here> and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.worker;

import org.eclipse.gyrex.jobs.internal.scheduler.SchedulingJob;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleEntryImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleImpl;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleManager;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.apache.commons.lang.exception.ExceptionUtils;

import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A job listeners which triggers dependent schedule entries.
 */
public final class TriggerScheduleEntriesWhenDone extends JobChangeAdapter {
	private final String[] scheduleInfo;
	private final JobContext jobContext;

	private static final Logger LOG = LoggerFactory.getLogger(TriggerScheduleEntriesWhenDone.class);

	public TriggerScheduleEntriesWhenDone(final String[] scheduleInfo, final JobContext jobContext) {
		this.scheduleInfo = scheduleInfo;
		this.jobContext = jobContext;
	}

	@Override
	public void done(final IJobChangeEvent event) {
		final IStatus result = event.getResult();
		if (result.isOK() || result.matches(IStatus.WARNING | IStatus.INFO)) {
			triggerDependentJobsAfterSuccess();
		}
	}

	private void triggerDependentJobsAfterSuccess() {
		if (scheduleInfo.length <= 2) {
			LOG.debug("No dependent jobs to trigger after run.");
			return;
		}

		final IScheduleManager scheduleManager = jobContext.getContext().get(IScheduleManager.class);
		if (scheduleManager == null) {
			LOG.debug("Schedule manager not available.");
			return;
		}

		final ScheduleImpl schedule = (ScheduleImpl) scheduleManager.getSchedule(scheduleInfo[0]);
		if (schedule == null) {
			LOG.debug("Schedule {} removed.", scheduleInfo[0]);
			return;
		}

		final ScheduleEntryImpl entry = schedule.getEntry(scheduleInfo[1]);
		if (entry == null) {
			LOG.debug("Schedule entry {} removed from schedule {}.", scheduleInfo[1], scheduleInfo[0]);
			return;
		}

		// 2.. -> entries to schedule
		for (int i = 2; i < scheduleInfo.length; i++) {
			final ScheduleEntryImpl entryToRun = schedule.getEntry(scheduleInfo[i]);
			if (entryToRun != null) {
				LOG.debug("Trigger dependent entry {}.", entryToRun.getId());
				try {
					final JobDataMap dataMap = new JobDataMap();
					SchedulingJob.populateJobDataMap(dataMap, entryToRun, schedule);
					SchedulingJob.queueJob(dataMap);
				} catch (final Exception e) {
					LOG.error("Unable to schedule entry {}. {}", entryToRun.getId(), ExceptionUtils.getRootCauseMessage(e), e);
				}
			} else {
				LOG.debug("Dependent entry {} not found in schedule {}.", scheduleInfo[i], schedule.getId());
			}
		}
	}
}