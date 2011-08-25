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
package org.eclipse.gyrex.jobs.schedules;

import java.util.Map;

import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.manager.IJobManager;

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
	 * Returns the id of the schedule entry.
	 * 
	 * @return the schedule entry id
	 */
	String getId();

	/**
	 * Returns the id that will be used when generating the {@link IJob job}.
	 * <p>
	 * The generated id will consist of the {@link #getId() entry id} prefixed
	 * with the {@link ISchedule#getId() schedule id}. This allows to track
	 * execution of the job through the regular {@link IJobManager} API.
	 * </p>
	 * 
	 * @return the {@link IJob#getId() job id}
	 */
	String getJobId();

	/**
	 * Returns the parameter for the {@link IJob job}.
	 * 
	 * @return the job parameter
	 */
	Map<String, String> getJobParameter();

	/**
	 * Returns the id of the job type
	 * 
	 * @return the job type id
	 */
	String getJobTypeId();

}
