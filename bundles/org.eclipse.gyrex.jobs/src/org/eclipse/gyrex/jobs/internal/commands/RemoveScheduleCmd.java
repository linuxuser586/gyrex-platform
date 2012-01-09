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

import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;

public class RemoveScheduleCmd extends BaseScheduleStoreCmd {

	/**
	 * Creates a new instance.
	 */
	public RemoveScheduleCmd() {
		super("- removes a schedule");
	}

	@Override
	protected void doExecute(final String storageId, final String scheduleId) throws Exception {
		ScheduleStore.remove(storageId, scheduleId);
		printf("Removed schedule %s!", scheduleId);
	}

}
