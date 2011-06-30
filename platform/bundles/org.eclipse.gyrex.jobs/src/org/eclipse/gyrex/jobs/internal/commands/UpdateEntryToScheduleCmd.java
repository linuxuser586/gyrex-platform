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
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.jobs.internal.schedules.ScheduleImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleEntryWorkingCopy;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;

public class UpdateEntryToScheduleCmd extends BaseScheduleStoreCmd {

	@Argument(index = 1, usage = "the entry id", required = true, metaVar = "ID")
	String entryId;

	@Argument(index = 2, usage = "parameter key", required = true, metaVar = "STRING")
	String jobParamKey;

	@Argument(index = 3, usage = "parameter value", required = false, metaVar = "STRING")
	String jobParamValue;

	/**
	 * Creates a new instance.
	 */
	public UpdateEntryToScheduleCmd() {
		super("<entryId> <jobParamKey> [<jobParamValue>] - Sets (or removes) an entry job paramater");
	}

	@Override
	protected void doExecute(final String storageId, final String scheduleId) throws Exception {
		final ScheduleImpl schedule = ScheduleStore.load(storageId, scheduleId, true);

		if (schedule.isEnabled()) {
			printf("Schedule %s is enabled, please disable first!", scheduleId);
			return;
		}

		final IScheduleEntryWorkingCopy entry = schedule.getEntry(entryId);

		final Map<String, String> parameter = new HashMap<String, String>(entry.getJobParameter());
		if (StringUtils.isBlank(jobParamValue)) {
			parameter.remove(jobParamKey);
		} else {
			parameter.put(jobParamKey, jobParamValue);
		}

		entry.setJobParameter(parameter);

		ScheduleStore.flush(storageId, schedule);
		printf("Updated schedule %s entry %s!", scheduleId, entryId);
	}

}
