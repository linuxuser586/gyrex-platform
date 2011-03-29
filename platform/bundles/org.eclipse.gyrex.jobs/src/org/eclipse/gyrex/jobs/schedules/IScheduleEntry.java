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
public interface IScheduleEntry {

	/**
	 * The <a href="http://en.wikipedia.org/wiki/Cron">cron expression</a> which
	 * defines when the job should be queued for execution.
	 * 
	 * @return a chron expression
	 * @see http://en.wikipedia.org/wiki/Cron
	 */
	String getCronExpression();

	/**
	 * @return
	 */
	String getId();

	/**
	 * @return
	 */
	Map<String, String> getJobParameter();

	/**
	 * @return
	 */
	String getJobProviderId();

}
