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

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public abstract class BaseScheduleStoreCmd extends Command {

	@Argument(index = 0, usage = "specify the schedule storage key", required = true, metaVar = "SCHEDULE")
	protected String scheduleStorageKey;

	@Option(name = "--process-all", aliases = { "-all" }, usage = "process all matching schedules instead of only the specific one")
	protected boolean isFilter;

	/**
	 * Creates a new instance.
	 */
	public BaseScheduleStoreCmd(final String description) {
		super("<scheduleStorageKey> " + description);
	}

	@Override
	protected void doExecute() throws Exception {
		// process all matching once
		if (isFilter) {
			final String[] schedules = ScheduleStore.getSchedules();
			for (final String schedule : schedules) {
				if (StringUtils.containsIgnoreCase(schedule, scheduleStorageKey)) {
					doExecute(schedule, ScheduleManagerImpl.getExternalId(schedule));
				}
			}
		}

		// just single one
		if (!IdHelper.isValidId(scheduleStorageKey)) {
			throw new IllegalArgumentException("invalid storage key");
		}
		doExecute(scheduleStorageKey, ScheduleManagerImpl.getExternalId(scheduleStorageKey));
	}

	protected abstract void doExecute(String storageId, String scheduleId) throws Exception;

}
