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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=346996
 *******************************************************************************/
package org.eclipse.gyrex.jobs.manager;

import java.util.Collection;
import java.util.Map;

import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;
import org.eclipse.gyrex.cloud.services.locking.ILockService;
import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.history.IJobHistory;
import org.eclipse.gyrex.jobs.service.IJobService;
import org.eclipse.gyrex.jobs.spi.storage.IJobHistoryStorage;

import org.eclipse.core.runtime.jobs.Job;

/**
 * A manager for creating, configuring, queuing and monitoring {@link IJob
 * distributed jobs} in Gyrex.
 * <p>
 * A job manager can be obtained for a specific {@link IRuntimeContext context}
 * either from the context (via {@link IRuntimeContext#get(Class)} using
 * {@code IJobManager.class}) or from the {@link IJobService}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @see IJobService
 */
public interface IJobManager {

	/** the default queue for queuing jobs */
	String DEFAULT_QUEUE = "gyrex.jobs.queue.default";

	/**
	 * Job parameter which specifies the id of an {@link IExclusiveLock
	 * exclusive lock} that must be acquired by the worker engine prior to
	 * running this job.
	 * <p>
	 * The lock will be held as long as the job is running and released when it
	 * finishes. An attempt will be made to cancel the job if the lock is lost
	 * during execution of the job.
	 * </p>
	 * <p>
	 * Note, the id will be passed to {@link ILockService} as specified without
	 * any further modifications. If clients want to use context-specific locks
	 * they are required to add a context identifier (eg. consistent hash of
	 * context path) to the specified id themselves.
	 * </p>
	 */
	String LOCK_ID = "gyrex.jobs.lockId";

	/**
	 * Cancels the job with the specified id.
	 * <p>
	 * If the job's state is currently {@link JobState#RUNNING} or
	 * {@link JobState#WAITING} it will changed to {@link JobState#ABORTING}.
	 * The worker engine will call {@link Job#cancel()} for any running job
	 * instance asynchronously.
	 * </p>
	 * <p>
	 * When the job finishes execution (either by reacting to
	 * {@link Job#cancel()}, by a regular finish or by an error) the job status
	 * will be set to {@link JobState#NONE}. Thus, clients that want to reliably
	 * known then a canceled job is no longer active in the cloud need to
	 * monitor the job state to change from {@link JobState#ABORTING} to
	 * {@link JobState#NONE}.
	 * </p>
	 * <p>
	 * This method is a no-op if the job state is neither
	 * {@link JobState#RUNNING} nor {@link JobState#WAITING}.
	 * </p>
	 * <p>
	 * A trigger may be specified in order to record in the history who or what
	 * triggered the cancellation of the job. If trigger is <code>null</code> a
	 * default string will be used. Callers may specify an empty string in order
	 * to suppress generation of the default string.
	 * </p>
	 * 
	 * @param jobId
	 *            the id of the job to cancel
	 * @param trigger
	 *            any free text that will be saved in the job history and
	 *            describes who or what triggered cancellation of the job (may
	 *            be <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if the job id is invalid
	 * @throws IllegalStateException
	 *             if a job with the specified id does not exists
	 */
	void cancelJob(String jobId, String trigger) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Creates a {@link IJob job} of the specified type using the given id and
	 * parameters.
	 * 
	 * @param jobTypeId
	 *            the job type identifier
	 * @param jobId
	 *            the job identifier
	 * @param parameter
	 *            the job parameter (may be <code>null</code>)
	 * @return the created job
	 * @throws IllegalStateException
	 *             if a job with the given jobId already exists
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @see IdHelper#isValidId(String)
	 */
	IJob createJob(String jobTypeId, String jobId, Map<String, String> parameter) throws IllegalStateException, IllegalArgumentException;

	/**
	 * Returns the history for the specified job.
	 * <p>
	 * Storing and accessing the history for any job requires the availability
	 * of a {@link IJobHistoryStorage job history storage} in a specific
	 * context.
	 * </p>
	 * <p>
	 * If no storage is configured, no history will be persisted and an empty
	 * {@link IJobHistory result} will be returned.
	 * </p>
	 * 
	 * @param jobId
	 *            the id of the job
	 * @return the job history (never <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the job history cannot be read (either the job does not
	 *             exists or any system service is missing)
	 * @see IJobHistoryStorage
	 */
	IJobHistory getHistory(String jobId) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Returns the specified job.
	 * <p>
	 * Returns <code>null</code> if a job with the specified id does not exist.
	 * </p>
	 * <p>
	 * Note, the returned job represents a snapshot of the job definition at the
	 * time of calling this method. It may be changed concurrently on different
	 * machines at any point after this method returns. Those changes won't be
	 * reflected into the returned job instance.
	 * </p>
	 * 
	 * @param id
	 *            the id of the job
	 * @return the {@link IJob job} with the given id (maybe <code>null</code>
	 *         if it doesn't exist)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 */
	IJob getJob(String id) throws IllegalArgumentException;

	/**
	 * Returns a list of all defined jobs.
	 * 
	 * @return an unmodifiable collection of job ids (never <code>null</code>)
	 */
	Collection<String> getJobs();

	/**
	 * Returns a list of jobs with the specified job state.
	 * 
	 * @param state
	 *            the job state to filter
	 * @return an unmodifiable collection of job ids (never <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if the given status is invalid
	 */
	Collection<String> getJobsByState(JobState state) throws IllegalArgumentException;

	/**
	 * Queues a job to be executed by a worker engine using the specified set of
	 * parameter.
	 * <p>
	 * This method allows to update the parameter of a job and queuing it in an
	 * atomic way. The update of the job parameter will be done as specified in
	 * {@link #setJobParameter(String, Map)}. The queuing of the job will be
	 * done as specified in {@link #queueJob(String, String, String)}.
	 * </p>
	 * 
	 * @param jobId
	 *            the id of the job to queue
	 * @param parameter
	 *            the new job parameter to set prior to queuing the job (may be
	 *            <code>null</code>)
	 * @param queueId
	 *            the id of the queue to add the job to (may be
	 *            <code>null</code> for {@link #DEFAULT_QUEUE})
	 * @param trigger
	 *            any free text that will be saved in the job history and
	 *            describes who or what triggered queuing of the job (may be
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the job cannot be queued (either the job or the queue does
	 *             not exists or any system service is missing or it is already
	 *             running)
	 * @see #queueJob(String, String, String)
	 */
	void queueJob(String jobId, Map<String, String> parameter, String queueId, String trigger) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Queues a job to be executed by a worker engine.
	 * <p>
	 * The job will be added to the specified queue. If no queue id is provided,
	 * {@link #DEFAULT_QUEUE} will be used instead. It's the responsibility of a
	 * system operator to ensure that the specified queue exists and that the
	 * queue will be monitored by a set of workers. If no worker monitors a
	 * queue no job will ever be executed.
	 * </p>
	 * <p>
	 * A trigger may be specified in order to record in the history who or what
	 * triggered the execution of the job. If trigger is <code>null</code> a
	 * default string will be used. Callers may specify an empty string in order
	 * to suppress generation of the default string.
	 * </p>
	 * <p>
	 * This method will fail if the {@link IJob#getState() job state} is
	 * <strong>not</strong> {@link JobState#NONE}. Otherwise as a result of this
	 * operation the {@link IJob#getState() job state} will be set to
	 * {@link JobState#WAITING} once the job has been added to the queue.
	 * </p>
	 * 
	 * @param jobId
	 *            the id of the job to queue
	 * @param queueId
	 *            the id of the queue to add the job to (may be
	 *            <code>null</code> for {@link #DEFAULT_QUEUE})
	 * @param trigger
	 *            any free text that will be saved in the job history and
	 *            describes who or what triggered queuing of the job (may be
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the job cannot be queued (either the job or the queue does
	 *             not exists or any system service is missing or it is already
	 *             running)
	 */
	void queueJob(String jobId, String queueId, String trigger) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Removes a job explicitly from the system.
	 * <p>
	 * If the job state is <strong>not</strong> {@link JobState#NONE} it cannot
	 * be removed.
	 * </p>
	 * <p>
	 * This method does nothing if a job with the specified id does not exist.
	 * </p>
	 * 
	 * @param jobId
	 *            the id of the job to remove
	 * @throws IllegalArgumentException
	 *             if the job id is invalid
	 * @throws IllegalStateException
	 *             if the job cannot be removed (state is not
	 *             {@link JobState#NONE})
	 */
	void removeJob(String jobId) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Sets the parameter of a {@link IJob job}.
	 * <p>
	 * Any currently stored parameter will be completely replaced by the
	 * specified parameter.
	 * </p>
	 * 
	 * @param jobId
	 *            the job identifier
	 * @param parameter
	 *            the new job parameter to set
	 * @throws IllegalStateException
	 *             if a job with the given jobId does not exists or the
	 *             {@link IJob#getState() job state} is not
	 *             {@link JobState#NONE}
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @see IdHelper#isValidId(String)
	 */
	void setJobParameter(String jobId, Map<String, String> parameter) throws IllegalStateException, IllegalArgumentException;
}
