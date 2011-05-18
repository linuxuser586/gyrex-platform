/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.commands;

import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleEntryWorkingCopy;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.args4j.Argument;

public class AddEntryToScheduleCmd extends BaseScheduleStoreCmd {

	@Argument(index = 1, usage = "the id for the entry to add", required = true, metaVar = "ID")
	String entryId;

	@Argument(index = 2, usage = "a cron expression", required = true, metaVar = "EXPR")
	String cronExpression;

	@Argument(index = 3, usage = "the job type identifier", required = true, metaVar = "JOBTYPE")
	String jobTypeId;

	/**
	 * Creates a new instance.
	 */
	public AddEntryToScheduleCmd() {
		super("<entryId> <cronExpression> <jobTypeId> - Adds an entry to a schedule");
	}

	@Override
	protected void doExecute(final String storageId, final String scheduleId) throws Exception {
		final ScheduleImpl schedule = ScheduleStore.load(storageId, scheduleId, true);

		if (schedule.isEnabled()) {
			printf("Schedule %s is enabled, please disable first!", scheduleId);
			return;
		}

		if (null == JobsActivator.getInstance().getJobProviderRegistry().getProvider(jobTypeId)) {
			printf("ERROR: no provider for job type %s found", jobTypeId);
			return;
		}

		final IScheduleEntryWorkingCopy entry = schedule.createEntry(entryId);

		entry.setJobTypeId(jobTypeId);
		try {
			entry.setCronExpression(cronExpression);
		} catch (final Exception e) {
			printf("ERROR: invalid cron expression, please see http://en.wikipedia.org/wiki/Cron#CRON_expression");
			printf("       %s", ExceptionUtils.getRootCauseMessage(e));
			return;
		}

		ScheduleStore.flush(storageId, schedule);
		printf("Entry added to schedule %s!", scheduleId);
	}

}
