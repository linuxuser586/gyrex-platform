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
package org.eclipse.gyrex.jobs.schedules.manager;

import java.util.TimeZone;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;

/**
 * A modifiable {@link ISchedule schedule}.
 */
public interface IScheduleWorkingCopy extends ISchedule {

	/**
	 * Creates a new {@link IScheduleEntryWorkingCopy schedule entry} which is
	 * {@link IScheduleEntry#isEnabled() enabled} per default.
	 * <p>
	 * Note, if a schedule entry if the same id already exists, it will be
	 * overridden by the new one.
	 * </p>
	 * 
	 * @param entryId
	 * @return an modifiable {@link IScheduleEntryWorkingCopy schedule entry}
	 * @throws IllegalStateException
	 *             if a schedule entry with the specified id already exists in
	 *             the schedule or the schedule is still
	 *             {@link ISchedule#isEnabled() enabled}
	 * @throws IllegalArgumentException
	 *             if the specified id is not {@link IdHelper#isValidId(String)
	 *             valid}
	 */
	IScheduleEntryWorkingCopy createEntry(String entryId) throws IllegalStateException, IllegalArgumentException;

	/**
	 * Returns the schedule entry with the specified id
	 * 
	 * @param entryId
	 * @return the schedule entry with the specified id
	 * @throws IllegalArgumentException
	 *             if the specified id is not {@link IdHelper#isValidId(String)
	 *             valid}
	 * @throws IllegalStateException
	 *             if no schedule entry with the specified id could be found
	 */
	IScheduleEntryWorkingCopy getEntry(String entryId) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Removes a {@link IScheduleEntry schedule entry} with the specified id
	 * from schedule
	 * <p>
	 * The schedule must not be enabled for this action
	 * 
	 * @param entryId
	 * @throws IllegalArgumentException
	 *             if no schedule entry with the specified id could be found
	 * @throws IllegalStateException
	 *             if the schedule is {@link ISchedule#isEnabled() enabled} or
	 *             the schedule is not initialized properly
	 */
	void removeEntry(String entryId) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Enables/disables the schedule
	 * 
	 * @param enabled
	 */
	void setEnabled(boolean enabled);

	/**
	 * Sets the time zone of the schedule. If no time zone is set
	 * <p>
	 * The default time zone is <code>UTC</code> (also often referred as
	 * <code>GMT</code>)
	 * 
	 * @param timeZone
	 */
	void setTimeZone(TimeZone timeZone);

}
