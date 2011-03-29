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

import java.util.Collection;

import org.eclipse.gyrex.common.identifiers.IdHelper;

/**
 * A manager for modifying schedules.
 * <p>
 * This interface is made available as an OSGi service.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
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
