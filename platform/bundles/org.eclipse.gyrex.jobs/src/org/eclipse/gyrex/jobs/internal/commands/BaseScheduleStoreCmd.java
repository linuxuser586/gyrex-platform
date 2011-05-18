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

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;

import org.kohsuke.args4j.Argument;

public abstract class BaseScheduleStoreCmd extends Command {

	@Argument(index = 0, usage = "specify the schedule storage key", required = true, metaVar = "SCHEDULE")
	protected String scheduleStorageKey;

	/**
	 * Creates a new instance.
	 */
	public BaseScheduleStoreCmd(final String description) {
		super("<scheduleStorageKey> " + description);
	}

	@Override
	protected void doExecute() throws Exception {
		if (!IdHelper.isValidId(scheduleStorageKey)) {
			throw new IllegalArgumentException("invalid storage key");
		}
		final String externalId = ScheduleManagerImpl.getExternalId(scheduleStorageKey);
		doExecute(scheduleStorageKey, externalId);
	}

	protected abstract void doExecute(String storageId, String scheduleId) throws Exception;

}
