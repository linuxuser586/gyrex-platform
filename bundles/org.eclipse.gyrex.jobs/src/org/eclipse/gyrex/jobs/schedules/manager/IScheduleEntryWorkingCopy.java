/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
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

import java.util.Map;

import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;

/**
 * A modifiable extension of {@link IScheduleEntry}.
 * <p>
 * A working copy is used for modifying schedule entries. Any modification must
 * be made persistent using
 * {@link IScheduleManager#updateSchedule(IScheduleWorkingCopy)}
 * </p>
 */
public interface IScheduleEntryWorkingCopy extends IScheduleEntry {

	/**
	 * Sets/updates the required field cronExpression to the schedule entry
	 * <p>
	 * Changes take effect, when the schedule entry is triggered for execution
	 * the next time.
	 * 
	 * @param cronExpression
	 * @see http://en.wikipedia.org/wiki/Cron
	 * @throws IllegalArgumentException
	 *             when the given cronExpression is invalid
	 */
	void setCronExpression(String cronExpression) throws IllegalArgumentException;

	/**
	 * Sets the schedule entry enabled/disabled. If the schedule entry is
	 * disabled, no job will be created and queued when the schedule entry is
	 * triggered to be executed regardless the enclosing schedule is running
	 * 
	 * @param enabled
	 */
	void setEnabled(boolean enabled);

	/**
	 * Sets/updates the {@link IScheduleEntry#getJobParameter() job parameter}
	 * of the schedule entry.
	 * <p>
	 * Changes take effect, when the schedule entry is triggered for execution
	 * the next time.
	 * </p>
	 * 
	 * @param parameter
	 *            the parameter to set (maybe <code>null</code> to unset all
	 *            parameter)
	 */
	void setJobParameter(Map<String, String> jobParameterMap);

	/**
	 * Sets/updates the required field jobTypeId to the schedule entry
	 * <p>
	 * Changes take effect, when the schedule entry is triggered for execution
	 * the next time.
	 * </p>
	 * 
	 * @param jobTypeId
	 */
	void setJobTypeId(String jobTypeId);

	/**
	 * Sets/unsets the schedule entries this entry depends on.
	 * <p>
	 * This allows to build up execution chains of schedule entries. Each time
	 * any of the specified schedule entries are executed and finish without
	 * errors this schedule entry will be triggered too.
	 * </p>
	 * 
	 * @param scheduleEntryIds
	 *            array of schedule entries this entry depends on
	 * @throws IllegalArgumentException
	 *             if one (or multiple) of the specified ids is invalid or may
	 *             cause a recursion
	 * @since 1.2
	 */
	void setPrecedingEntries(String... scheduleEntryIds) throws IllegalArgumentException;

	/**
	 * Sets/unsets the id of the queue to use.
	 * 
	 * @param queueId
	 *            the queue id
	 * @throws IllegalArgumentException
	 *             if the queue id is not <code>null</code> but invalid
	 * @since 1.2
	 */
	void setQueueId(String queueId) throws IllegalArgumentException;

}
