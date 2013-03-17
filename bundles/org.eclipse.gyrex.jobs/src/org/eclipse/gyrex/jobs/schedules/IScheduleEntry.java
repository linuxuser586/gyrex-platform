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
package org.eclipse.gyrex.jobs.schedules;

import java.util.Collection;
import java.util.Map;

import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.manager.IJobManager;

/**
 * An entry in a {@link ISchedule} with run information for jobs.
 * <p>
 * Schedule entries define what job to execute, which parameters to use and
 * when. Multiple options are available for triggering jobs through schedule
 * entries.
 * </p>
 * <p>
 * A schedule entry can be triggered using a {@link #getCronExpression() cron
 * expression}. This allows to execute jobs regularly.
 * </p>
 * <p>
 * A schedule entry can be triggered {@link #getPrecedingEntries() when other
 * schedule entries} finish execution. This allows to build job chains.
 * </p>
 * <p>
 * When multiple options are set any option will be used to trigger job
 * executions.
 * </p>
 * <p>
 * When a schedule entry is executed, a job of {@link #getJobTypeId() the
 * specified type} will be submitted to a queue using the {@link IJobManager job
 * manager}. Thus, the {@link IJobManager job manager} rules of queueing apply,
 * i.e. a job will never be queued multiple times.
 * </p>
 */
public interface IScheduleEntry {

	/**
	 * Returns the <a href="http://en.wikipedia.org/wiki/Cron">cron
	 * expression</a> which defines when the job should be queued for execution.
	 * 
	 * @return a chron expression (may be <code>null</code> if not desired)
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
	 * Generates and returns the id that will be used when creating the
	 * {@link IJob job}.
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
	 * @return an unmodifiable map of job parameter
	 */
	Map<String, String> getJobParameter();

	/**
	 * Returns the id of the job type
	 * 
	 * @return the job type id
	 */
	String getJobTypeId();

	/**
	 * Returns a list of schedule entries this entry depends on.
	 * <p>
	 * Whenever any of the returned schedule entries finishes without errors a
	 * job defined by this entry will be executed.
	 * </p>
	 * <p>
	 * This allows to build execution chains (as an alternative to or in
	 * combination with the other scheduling mechanisms).
	 * </p>
	 * 
	 * @return an unmodifiable collection of schedule entry ids
	 * @since 1.2
	 */
	Collection<String> getPrecedingEntries();

	/**
	 * Returns the id of the queue that will be used for queuing the job defined
	 * by this entry.
	 * <p>
	 * May return <code>null</code> if no specific queue is defined for the
	 * entry.
	 * </p>
	 * 
	 * @return the queue id (may be <code>null</code>)
	 * @since 1.2
	 */
	String getQueueId();

	/**
	 * Returns the schedule this entry is belongs to.
	 * 
	 * @return the schedule (maybe <code>null</code> if the entry has been
	 *         removed from the schedule)
	 */
	ISchedule getSchedule();

	/**
	 * Returns whether a job is created and queued at the next time the schedule
	 * entry triggered for execution.
	 * 
	 * @return the state of the schedule entry
	 */
	boolean isEnabled();

}
