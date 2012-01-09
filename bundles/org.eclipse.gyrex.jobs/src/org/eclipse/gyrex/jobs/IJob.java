/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Mike Tschierschke - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs;

import java.util.Map;

import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.provider.JobProvider;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleManager;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A distributed job in Gyrex.
 * <p>
 * This interface defines a Gyrex job. In contrast to an {@link Job Eclipse Job}
 * a Gyrex job is not the actually job implementation that does the work but a
 * definition of a specific {@link Job Eclipse Job} run configuration.
 * </p>
 * <p>
 * The Gyrex Job API extends the {@link Job Eclipse Job API} with capabilities
 * of defining, queuing and executing jobs across a set of nodes within a cloud.
 * {@link Job Eclipse Jobs} do the actual work. They implement the business
 * logic of a job. A {@link JobProvider} is responsible for creating {@link Job}
 * instances which will then be executed by a worker engine. The worker engine
 * retrieves the information which jobs should be executed from {@link IQueue
 * queues}.
 * </p>
 * <p>
 * Like {@link Job Eclipse Jobs} Gyrex jobs also have a state that indicates
 * what they are currently doing. When created, jobs start with a state value of
 * {@link JobState#NONE}. When a job is queued to be run, it moves into the
 * {@link JobState#WAITING} state. The state value stays
 * {@link JobState#WAITING} when a job is fetched from the queue by the worker
 * engine and schedule on a worker node to be executed. When a
 * {@link JobState#WAITING} or {@link JobState#RUNNING} job is canceled its
 * state value will change to {@link JobState#ABORTING}. When a job starts
 * running on a worker node, it moves into the {@link JobState#RUNNING} state.
 * When execution finishes (either normally or through cancelation), the state
 * changes back to {@link JobState#NONE}.
 * </p>
 * <p>
 * The Gyrex {@link IJobManager} is responsible for creating, managing and
 * queuing {@link IJob job definitions}. The Gyrex {@link IScheduleManager} may
 * be used for creating schedules of recuring jobs.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @see JobProvider
 * @see IJobManager
 * @see IScheduleManager
 */
public interface IJob {

	/**
	 * @return the id of the job
	 */
	String getId();

	/**
	 * Returns the milliseconds from the Java epoch of
	 * <code>1970-01-01T00:00:00Z</code> when the job was last cancelled.
	 * 
	 * @return the milliseconds from the Java epoch of
	 *         <code>1970-01-01T00:00:00Z</code> when the job was last
	 *         cancelled, or <code>-1</code> if the job was never cancelled
	 *         until now
	 */
	long getLastCancelled();

	/**
	 * Returns the trigger specified when the the job was last canceled.
	 * 
	 * @return the trigger specified when the the job was last canceled or
	 *         <code>null</code> if the job has not been canceled until now
	 */
	String getLastCancelledTrigger();

	/**
	 * Returns the milliseconds from the Java epoch of
	 * <code>1970-01-01T00:00:00Z</code> when the job was last queued.
	 * 
	 * @return the milliseconds from the Java epoch of
	 *         <code>1970-01-01T00:00:00Z</code> when the job was last queued,
	 *         or <code>-1</code> if the job was never queued until now
	 */
	long getLastQueued();

	/**
	 * Returns the trigger specified when the the job was last queued.
	 * 
	 * @return the trigger specified when the the job was last queued or
	 *         <code>null</code> if the job has not been queued until now
	 */
	String getLastQueuedTrigger();

	/**
	 * Returns the last result of the job execution.
	 * 
	 * @return the last result (maybe <code>null</code>)
	 */
	public IStatus getLastResult();

	/**
	 * Returns the milliseconds from the Java epoch of
	 * <code>1970-01-01T00:00:00Z</code> when the job was last started.
	 * 
	 * @return the milliseconds from the Java epoch of
	 *         <code>1970-01-01T00:00:00Z</code> when the job was last started,
	 *         or <code>-1</code> if the job never started until now
	 */
	long getLastStart();

	/**
	 * Returns the milliseconds from the Java epoch of
	 * <code>1970-01-01T00:00:00Z</code> when the job last finished
	 * successfully.
	 * 
	 * @return the milliseconds from the Java epoch of
	 *         <code>1970-01-01T00:00:00Z</code> of the last successful finish
	 *         (execution finished with {@link IStatus#OK}) of the job, or
	 *         <code>-1</code> if the job never finished it's execution until
	 *         now
	 */
	long getLastSuccessfulFinish();

	/**
	 * A map of all parameter defined for the job.
	 * <p>
	 * If not parameters are defined, an empty map will be returned.
	 * </p>
	 * 
	 * @return an unmodifiable map of parameter for the job (never
	 *         <code>null</code>)
	 */
	Map<String, String> getParameter();

	/**
	 * Returns the state of the job. Result will be one of:
	 * <ul>
	 * <li>{@link JobState#RUNNING} - if the job is currently running.</li>
	 * <li>{@link JobState#WAITING} - if the job is waiting to be run.</li>
	 * <li>{@link JobState#ABORTING} - if the job is aborting.</li>
	 * <li>{@link JobState#NONE} - in all other cases.</li>
	 * </ul>
	 * <p>
	 * Note that job state is inherently volatile, and in most cases clients
	 * cannot rely on the result of this method being valid by the time the
	 * result is obtained. For example, if <tt>getState</tt> returns
	 * <tt>RUNNING</tt>, the job may have actually completed by the time the
	 * <tt>getState</tt> method returns. All clients can infer from invoking
	 * this method is that the job was recently in the returned state.
	 * 
	 * @return the job state
	 * @throws IllegalStateException
	 *             if the status is <code>null</code>
	 */
	JobState getState() throws IllegalStateException;

	/**
	 * Returns the type identifier of the job.
	 * 
	 * @return the jobs type (never <code>null</code>)
	 */
	String getTypeId();

}
