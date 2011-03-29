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

import java.util.Map;

/**
 * An entry in a {@link ISchedule} with run information for jobs.
 */
public interface IScheduleEntryWorkingCopy extends IScheduleEntry {

	/**
	 * @param cronExpression
	 */
	void setCronExpression(String cronExpression);

	/**
	 * @param parameter
	 */
	void setJobParameter(Map<String, String> jobParameterMap);

	/**
	 * @param jobProviderId
	 */
	void setJobProviderId(String jobProviderId);

}
