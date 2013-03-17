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
package org.eclipse.gyrex.jobs.internal.commands;

import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleEntryWorkingCopy;

import org.kohsuke.args4j.Argument;

public class CreateScheduleEntryCmd extends BaseScheduleStoreCmd {

	@Argument(index = 1, usage = "the id for the entry to add", required = true, metaVar = "ID")
	String entryId;

	@Argument(index = 2, usage = "the job type identifier", required = true, metaVar = "JOBTYPE")
	String jobTypeId;

	/**
	 * Creates a new instance.
	 */
	public CreateScheduleEntryCmd() {
		super("<entryId> <cronExpression> <jobTypeId> - Creates a schedule entry");
	}

	@Override
	protected void doExecute(final String storageId, final String scheduleId) throws Exception {
		final ScheduleImpl schedule = ScheduleStore.load(storageId, scheduleId, true);

		if (schedule.isEnabled()) {
			printf("Schedule %s is enabled, please disable first!", scheduleId);
			return;
		}

		if (null == JobsActivator.getInstance().getJobProviderRegistry().getProvider(jobTypeId))
			throw new IllegalArgumentException(String.format("no provider for job type %s found", jobTypeId));

		final IScheduleEntryWorkingCopy entry = schedule.createEntry(entryId);

		entry.setJobTypeId(jobTypeId);
		entry.setEnabled(false);

		ScheduleStore.flush(storageId, schedule);
		printf("Entry added to schedule %s!", scheduleId);
	}

}
