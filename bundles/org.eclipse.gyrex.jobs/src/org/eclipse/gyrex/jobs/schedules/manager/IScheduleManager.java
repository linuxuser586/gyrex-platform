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

import java.util.Collection;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.service.IScheduleService;

/**
 * A manager for scheduling {@link IJob jobs} at recuring intervals.
 * <p>
 * A schedule manager can be obtained for a specific {@link IRuntimeContext
 * context} either from the context (via {@link IRuntimeContext#get(Class)}
 * using {@code IScheduleManager}) or from the {@link IScheduleService}.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @see IScheduleService
 */
public interface IScheduleManager {

	/**
	 * Creates a new schedule using the specified id.
	 * <p>
	 * Every newly created schedule is disabled by default. It must be
	 * explicitly enabled in order to become active.
	 * </p>
	 * 
	 * @param id
	 *            the schedule id (must be {@link IdHelper#isValidId(String) a
	 *            valid id})
	 * @return a working copy for modifying the schedule
	 */
	IScheduleWorkingCopy createSchedule(String id);

	/**
	 * Creates a working copy for modifying a schedule.
	 * 
	 * @param id
	 *            the id of the schedule to modify
	 * @return the created working copy
	 */
	IScheduleWorkingCopy createWorkingCopy(String id);

	/**
	 * Returns the schedule using the specified id.
	 * 
	 * @param id
	 *            the schedule id
	 * @return the schedule (maybe <code>null</code> if no such schedule exist)
	 */
	ISchedule getSchedule(String id);

	/**
	 * Returns a list of schedules
	 * 
	 * @return
	 */
	Collection<String> getSchedules();

	/**
	 * Removes a schedule
	 * 
	 * @param id
	 */
	void removeSchedule(String id);

	/**
	 * Updates a schedule. Once a schedule is updated all modifications will be
	 * applied.
	 * 
	 * @param copy
	 */
	void updateSchedule(IScheduleWorkingCopy copy);

}
