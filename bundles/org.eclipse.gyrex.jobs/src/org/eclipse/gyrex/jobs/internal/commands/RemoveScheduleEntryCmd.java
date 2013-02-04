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

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;

import org.kohsuke.args4j.Argument;

public class RemoveScheduleEntryCmd extends BaseScheduleStoreCmd {

	@Argument(index = 1, usage = "the entry id", required = true, metaVar = "ID")
	String entryId;

	/**
	 * Creates a new instance.
	 */
	public RemoveScheduleEntryCmd() {
		super("<entryId> - Removes a schedule entry");
	}

	@Override
	protected void doExecute(final String storageId, final String scheduleId) throws Exception {
		final ScheduleImpl schedule = ScheduleStore.load(storageId, scheduleId, true);

		if (schedule.isEnabled()) {
			printf("Schedule %s is enabled, please disable first!", scheduleId);
			return;
		}

		if (!IdHelper.isValidId(entryId)) {
			throw new IllegalArgumentException("invalid entry id");
		}

		schedule.removeEntry(entryId);

		ScheduleStore.flush(storageId, schedule);
		printf("Removed entry %s in schedule %s !", entryId, scheduleId);
	}

}
