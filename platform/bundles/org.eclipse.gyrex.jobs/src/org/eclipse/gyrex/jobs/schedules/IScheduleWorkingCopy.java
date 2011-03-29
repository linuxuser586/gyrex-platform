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
package org.eclipse.gyrex.jobs.schedules;

import java.util.TimeZone;

/**
 * A modifiable {@link ISchedule schedule}.
 */
public interface IScheduleWorkingCopy extends ISchedule {

	IScheduleEntryWorkingCopy createEntry(String entryId);

	IScheduleEntryWorkingCopy getEntry(String entryId);

	/**
	 * @param b
	 */
	void setEnabled(boolean enabled);

	/**
	 * @param timeZone
	 */
	void setTimeZone(TimeZone timeZone);

}
