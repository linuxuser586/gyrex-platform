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
package org.eclipse.gyrex.jobs.schedules.manager;

import java.util.Map;

import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;

/**
 * An entry in a {@link ISchedule} with run information for jobs.
 */
public interface IScheduleEntryWorkingCopy extends IScheduleEntry {

	/**
	 * @param cronExpression
	 */
	void setCronExpression(String cronExpression) throws IllegalArgumentException;

	/**
	 * @param parameter
	 */
	void setJobParameter(Map<String, String> jobParameterMap);

	/**
	 * @param jobProviderId
	 */
	void setJobTypeId(String jobProviderId);

}
