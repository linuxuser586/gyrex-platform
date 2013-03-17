/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
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
package org.eclipse.gyrex.jobs.internal.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;
import org.eclipse.gyrex.cloud.services.locking.ILockService;
import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.history.IJobHistory;
import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.storage.CloudPreferncesJobHistoryStorage;
import org.eclipse.gyrex.jobs.internal.storage.CloudPreferncesJobStorage;
import org.eclipse.gyrex.jobs.internal.util.ContextHashUtil;
import org.eclipse.gyrex.jobs.internal.worker.JobInfo;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.spi.storage.IJobHistoryStorage;
import org.eclipse.gyrex.jobs.spi.storage.JobHistoryEntryStorable;
import org.eclipse.gyrex.preferences.ModificationConflictException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a {@link IJobManager} and {@link IJobHistoryManager} that
 * persists the job data in the {@link Preferences}
 */
public class JobManagerImpl implements IJobManager {

	/**
	 * One-time state change trigger.
	 */
	private static class StateWatchListener implements IPreferenceChangeListener {
		private final String jobId;
		private final JobState state;
		private final IJobStateWatch stateWatch;

		public StateWatchListener(final String jobId, final JobState state, final IJobStateWatch stateWatch) {
			this.jobId = jobId;
			this.state = state;
			this.stateWatch = stateWatch;
		}

		@Override
		public void preferenceChange(final PreferenceChangeEvent event) {
			if (!CloudPreferncesJobStorage.PROPERTY_STATUS.equals(event.getKey()))
				return;

			final JobState newState = JobState.toState(event.getNewValue());
			if (newState != state) {
				try {
					stateWatch.jobStateChanged(jobId);
				} finally {
					((IEclipsePreferences) event.getNode()).removePreferenceChangeListener(this);
				}
			}
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(JobManagerImpl.class);

	private static final long DEFAULT_MODIFY_LOCK_TIMEOUT = 3000L;
	private static final long modifyLockTimeout = Long.getLong("gyrex.jobs.modifyLock.timeout", DEFAULT_MODIFY_LOCK_TIMEOUT);

	private static final int storageMaxNumberOfRetries = Integer.getInteger("gyrex.jobs.storage.retryAttempts", 3);
	private static final long storageRetryDelay = Math.max(Long.getLong("gyrex.jobs.storage.retryDelay", 150L), 50L);

	static final IJobHistory EMPTY_HISTORY = new IJobHistory() {
		@Override
		public Collection<IJobHistoryEntry> getEntries() {
			return Collections.emptyList();
		}

		@Override
		public String toString() {
			return "No history available.";
		};
	};

	public static IExclusiveLock acquireLock(final JobImpl job) {
		final String lockId = "gyrex.jobs.modify.".concat(job.getStorageKey());
		if (JobsDebug.jobLocks) {
			LOG.debug("Requesting lock {} for job {}", new Object[] { lockId, job.getId(), new Exception("Call Stack") });
		}
		try {
			return JobsActivator.getInstance().getService(ILockService.class).acquireExclusiveLock(lockId, null, modifyLockTimeout);
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Unable to get job modify lock. %s", e.getMessage()), e);
		}
	}

	public static void cancel(JobImpl job, final String trigger) {
		IExclusiveLock jobLock = null;
		final String jobId = job.getId();
		try {
			// get job modification lock
			jobLock = acquireLock(job);

			// re-read job status (inside lock)
			job = getJob(jobId, job.getStorageKey());
			if (null == job)
				throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

			// re-check job status (inside lock)
			if ((job.getState() != JobState.WAITING) && (job.getState() != JobState.RUNNING))
				// no-op
				return;

			try {
				// set state
				setJobState(job, JobState.ABORTING, jobLock);

				// add cancellation note
				setJobCancelled(job, System.currentTimeMillis(), null != trigger ? trigger : findCaller(), jobLock);
			} catch (final BackingStoreException e) {
				throw new IllegalStateException(String.format("Error canceling job %s. %s", jobId, e.getMessage()), e);
			}
		} finally {
			releaseLock(jobLock, jobId);
		}
	}

	private static String findCaller() {
		final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		if (trace.length == 0)
			return StringUtils.EMPTY;

		// find first _none_ jobs API call
		for (int i = 0; i < trace.length; i++) {
			if (StringUtils.startsWith(trace[i].getClassName(), JobManagerImpl.class.getName())) {
				continue;
			}
			return trace[i].toString();
		}

		return StringUtils.EMPTY;
	}

	public static JobImpl getJob(final String jobId, final String storageKey) throws IllegalStateException {
		try {
			// don't create node if it doesn't exist
			if (!CloudPreferncesJobStorage.getJobsNode().nodeExists(storageKey))
				return null;

			// read job
			return CloudPreferncesJobStorage.readJob(jobId, CloudPreferncesJobStorage.getJobsNode().node(storageKey));
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Error reading job data. %s", e.getMessage()), e);
		}
	}

	private static void releaseLock(final IExclusiveLock jobLock, final String jobId) {
		if (null != jobLock) {
			if (JobsDebug.jobLocks) {
				LOG.debug("Releasing lock {} for job {}", new Object[] { jobLock.getId(), jobId });
			}
			try {
				jobLock.release();
			} catch (final Exception e) {
				// ignore
			}
		}
	}

	/**
	 * records cancellation time and trigger
	 */
	private static void setJobCancelled(final JobImpl job, final long timestamp, final String trigger, final IExclusiveLock lock) throws BackingStoreException {
		if ((null == lock) || !lock.isValid())
			throw new IllegalStateException(String.format("Unable to update job %s due to missing or lost job lock!", job.getId()));

		if (!CloudPreferncesJobStorage.getJobsNode().nodeExists(job.getStorageKey()))
			// don't update if removed
			return;

		// update job node
		final Preferences jobNode = CloudPreferncesJobStorage.getJobsNode().node(job.getStorageKey());
		jobNode.putLong(CloudPreferncesJobStorage.PROPERTY_LAST_CANCELLED, timestamp);
		jobNode.put(CloudPreferncesJobStorage.PROPERTY_LAST_CANCELLED_TRIGGER, trigger);
		jobNode.flush();
	}

	private static void setJobState(final JobImpl job, final JobState state, final IExclusiveLock lock) throws BackingStoreException {
		if (null == state)
			throw new IllegalArgumentException("job state must not be null");

		if ((null == lock) || !lock.isValid())
			throw new IllegalStateException(String.format("Unable to update job state of job %s to %s due to missing or lost job lock!", job.getId(), state.toString()));

		if (!CloudPreferncesJobStorage.getJobsNode().nodeExists(job.getStorageKey()))
			// don't update if removed
			return;

		final Preferences jobNode = CloudPreferncesJobStorage.getJobsNode().node(job.getStorageKey());
		if (StringUtils.equals(jobNode.get(CloudPreferncesJobStorage.PROPERTY_STATUS, null), state.name()))
			// don't update if not different
			return;

		// update job node
		// (note, this might trigger any watches immediately)
		jobNode.put(CloudPreferncesJobStorage.PROPERTY_STATUS, state.name());
		jobNode.flush();

		// update job
		job.setStatus(state);
	}

	private final IRuntimeContext context;
	private final ContextHashUtil contextHash;

	/**
	 * Creates a new instance.
	 */
	@Inject
	public JobManagerImpl(final IRuntimeContext context) {
		this.context = context;
		contextHash = new ContextHashUtil(context);
	}

	@Override
	public void cancelJob(final String jobId, final String trigger) throws IllegalStateException {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		final JobImpl job = getJob(jobId);
		if (null == job)
			throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

		// check job status
		if ((job.getState() != JobState.WAITING) && (job.getState() != JobState.RUNNING))
			// no-op
			return;

		cancel(job, trigger);
	}

	@Override
	public JobImpl createJob(final String jobTypeId, final String jobId, final Map<String, String> parameter) {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		if (!IdHelper.isValidId(jobTypeId))
			throw new IllegalArgumentException(String.format("Invalid type id '%s'", jobTypeId));

		IExclusiveLock jobLock = null;
		try {
			final String internalId = toInternalId(jobId);
			if (CloudPreferncesJobStorage.getJobsNode().nodeExists(internalId))
				throw new IllegalStateException(String.format("Job '%s' is already stored", jobId));

			// create preference node
			final Preferences node = CloudPreferncesJobStorage.getJobsNode().node(internalId);
			node.put(CloudPreferncesJobStorage.PROPERTY_TYPE, jobTypeId);
			node.flush();

			// read job
			JobImpl job = CloudPreferncesJobStorage.readJob(jobId, node);

			// acquire lock
			jobLock = acquireLock(job);

			// make sure node is in sync
			syncJobNode(jobId);

			// re-read job status (inside lock)
			job = getJob(jobId);
			if (null == job)
				throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

			// update job parameter
			setJobParameter(job, parameter, jobLock);

			// set initial state
			setJobState(job, JobState.NONE, jobLock);

			// trigger possible clean-up
			CloudPreferncesJobStorage.mayTriggerCleanup();

			// re-read job (this time with parameter)
			return CloudPreferncesJobStorage.readJob(jobId, node);
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Error creating job data. %s", e.getMessage()), e);
		} finally {
			releaseLock(jobLock, jobId);
		}
	}

	private void doQueueJob(final String jobTypeId, final String jobId, Map<String, String> parameter, final String queueId, final String trigger, final String scheduleInfo) {
		JobImpl job = getJob(jobId);

		// if no job type is given, we are not allowed to create a job 
		if ((null == job) && (jobTypeId == null))
			throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

		// verify the queue exists
		final IQueue queue = JobsActivator.getInstance().getQueueService().getQueue(null != queueId ? queueId : DEFAULT_QUEUE, null);
		if (null == queue)
			throw new IllegalStateException(String.format("Queue %s does not exist!", queueId));

		IExclusiveLock jobLock = null;
		try {
			// create job if necessary
			if (job == null) {
				try {
					final String internalId = toInternalId(jobId);
					if (CloudPreferncesJobStorage.getJobsNode().nodeExists(internalId))
						throw new IllegalStateException(String.format("Job '%s' is already stored", jobId));

					// create preference node
					final Preferences node = CloudPreferncesJobStorage.getJobsNode().node(internalId);
					node.put(CloudPreferncesJobStorage.PROPERTY_TYPE, jobTypeId);
					node.flush();

					// read job
					job = CloudPreferncesJobStorage.readJob(jobId, node);
				} catch (final BackingStoreException e) {
					throw new IllegalStateException(String.format("Error creating job data. %s", e.getMessage()), e);
				}
			}

			// acquire lock
			jobLock = acquireLock(job);

			// refresh job info
			syncJobNode(jobId);

			// re-read job status (inside lock)
			job = getJob(jobId);
			if (null == job)
				throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

			// check the job state (inside lock) but ignore stuck jobs
			if (job.getState() != JobState.NONE) {
				if (!isStuck(job))
					throw new IllegalStateException(String.format("Job %s cannot be queued because of a job state conflict (expected %s, got %s)!", jobId, JobState.NONE.toString(), job.getState().toString()));
				else {
					LOG.warn("Job {} is in state {}. However, it's assumed stuck. The queue request will reset the state!", job.getId(), job.getState());
				}
			}

			try {
				// determine job parameter to use
				if (null != parameter) {
					// update job definition with parameter
					setJobParameter(job, parameter, jobLock);
				} else {
					// use parameter from job definition
					parameter = job.getParameter();
				}

				// set job state
				setJobState(job, JobState.WAITING, jobLock);

				// collect queue info
				final String queueTrigger = null != trigger ? trigger : findCaller();
				final long queueTimestamp = System.currentTimeMillis();

				// add to queue
				queue.sendMessage(JobInfo.asMessage(new JobInfo(job.getTypeId(), jobId, context.getContextPath(), parameter, queueTrigger, queueTimestamp, scheduleInfo)));

				// set queued time
				try {
					setJobQueued(job, queueTimestamp, queueTrigger, jobLock);
				} catch (final Exception e) {
					// we must not fail at this point, the job has been queued already
					LOG.warn("Unable to set job queue time for job {}: {}", jobId, ExceptionUtils.getRootCauseMessage(e));
				}
			} catch (final Exception e) {
				// try to reset the job state
				try {
					setJobState(job, JobState.NONE, jobLock);
				} catch (final Exception resetException) {
					LOG.error("Unable to reset job state for job {}: {}", jobId, ExceptionUtils.getRootCauseMessage(e));
				}
				throw new IllegalStateException(String.format("Error queuing job %s. %s", jobId, e.getMessage()), e);
			}
		} finally {
			releaseLock(jobLock, jobId);
		}

		// trigger possible clean-up
		CloudPreferncesJobStorage.mayTriggerCleanup();
	}

	private <T> T executeWithRetry(final Callable<T> c) throws Exception {
		int attempt = 0;
		while (true) {
			try {
				return c.call();
			} catch (final ModificationConflictException e) {
				// try up to max retries
				if (++attempt > storageMaxNumberOfRetries)
					throw e;

				// wait a bit
				Thread.sleep(storageRetryDelay);
			}
		}
	}

	@Override
	public IJobHistory getHistory(final String jobId) throws IllegalStateException {
		final JobImpl job = getJob(jobId);
		if (null == job)
			throw new IllegalStateException(String.format("Job '%s' does not exist.", jobId));

		final IJobHistoryStorage storage = context.get(IJobHistoryStorage.class);
		if (storage == null)
			return EMPTY_HISTORY;

		try {
			final int count = storage.count(jobId);
			if (count > 0)
				return new StorageBackedJobHistory(jobId, storage);
		} catch (final Exception e) {
			throw new IllegalStateException("Error reading history", e);
		}

		return EMPTY_HISTORY;
	}

	@Override
	public JobImpl getJob(final String jobId) {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		return getJob(jobId, toInternalId(jobId));
	}

	@Override
	public Collection<String> getJobs() {
		try {
			final String[] storageIds = CloudPreferncesJobStorage.getJobsNode().childrenNames();
			final List<String> jobIds = new ArrayList<String>(storageIds.length);
			for (final String internalId : storageIds) {
				if (contextHash.isInternalId(internalId)) {
					jobIds.add(toExternalId(internalId));
				}
			}
			return Collections.unmodifiableCollection(jobIds);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error reading job data. %s", e.getMessage()), e);
		}
	}

	@Override
	public Collection<String> getJobsByState(final JobState state) {
		if (null == state)
			throw new IllegalArgumentException("Status must not be null");

		try {
			final String[] storageIds = CloudPreferncesJobStorage.getJobsNode().childrenNames();
			final List<String> jobIds = new ArrayList<String>(storageIds.length);
			for (final String internalId : storageIds) {
				if (contextHash.isInternalId(internalId) && StringUtils.equals(CloudPreferncesJobStorage.getJobsNode().node(internalId).get(CloudPreferncesJobStorage.PROPERTY_STATUS, null), state.name())) {
					jobIds.add(toExternalId(internalId));
				}
			}
			return Collections.unmodifiableCollection(jobIds);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error reading job data. %s", e.getMessage()), e);
		}
	}

	public boolean isStuck(final JobImpl job) {
		// just check but don't log a warning
		return JobHungDetectionHelper.isStuck(toInternalId(job.getId()), job, false);
	}

	@Override
	public void queueJob(final String jobId, final Map<String, String> parameter, final String queueId, final String trigger) throws IllegalArgumentException, IllegalStateException {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		doQueueJob(null, jobId, parameter, queueId, trigger, null);
	}

	@Override
	public void queueJob(final String jobTypeId, final String jobId, final Map<String, String> parameter, final String queueId, final String trigger) throws IllegalArgumentException, IllegalStateException {
		if (!IdHelper.isValidId(jobTypeId))
			throw new IllegalArgumentException(String.format("Invalid job type id '%s'", jobTypeId));
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		doQueueJob(jobTypeId, jobId, parameter, queueId, trigger, null);
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void queueJob(final String jobTypeId, final String jobId, final Map<String, String> parameter, final String queueId, final String trigger, final String scheduleInfo) throws IllegalArgumentException, IllegalStateException {
		if (!IdHelper.isValidId(jobTypeId))
			throw new IllegalArgumentException(String.format("Invalid job type id '%s'", jobTypeId));
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		doQueueJob(jobTypeId, jobId, parameter, queueId, trigger, scheduleInfo);
	}

	@Override
	public void queueJob(final String jobId, final String queueId, final String trigger) {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		doQueueJob(null, jobId, null, queueId, trigger, null);
	}

	@Override
	public void removeJob(final String jobId) {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		final JobImpl job = getJob(jobId);
		if (null == job)
			return;

		// check the job state
		if (job.getState() != JobState.NONE)
			throw new IllegalStateException(String.format("Job %s cannot be removed because of a job state conflict (expected %s, got %s)!", jobId, JobState.NONE.toString(), job.getState().toString()));

		IExclusiveLock jobLock = null;
		try {
			// get job modification lock
			jobLock = acquireLock(job);

			// remove from preferences
			final String internalId = toInternalId(jobId);
			final IEclipsePreferences jobsNode = CloudPreferncesJobStorage.getJobsNode();
			executeWithRetry(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					if (jobsNode.nodeExists(internalId)) {
						jobsNode.node(internalId).removeNode();
						jobsNode.flush();
					}
					return null;
				}
			});
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Error removing job %s. %s", jobId, e.getMessage()), e);
		} finally {
			releaseLock(jobLock, jobId);
		}
	}

	/**
	 * Marks a job active in the system.
	 * <p>
	 * This must be called by the worker engine to indicate that a job left the
	 * queue and is now scheduled locally and/or running.
	 * </p>
	 * 
	 * @param jobId
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void setActive(final String jobId) throws Exception {
		if (JobsDebug.debug) {
			LOG.debug("Marking job {} active.", jobId);
		}
		JobHungDetectionHelper.setActive(toInternalId(jobId));
		final Preferences jobNode = CloudPreferncesJobStorage.getJobsNode().node(toInternalId(jobId));
		executeWithRetry(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				jobNode.sync();
				jobNode.putBoolean(CloudPreferncesJobStorage.PROPERTY_ACTIVE, true);
				jobNode.flush();
				return null;
			}
		});
	}

	/**
	 * Marks a job inactive in the system.
	 * <p>
	 * This must be called by the worker engine to indicate that a job finished
	 * running locally (either successful or canceled).
	 * </p>
	 * 
	 * @param jobId
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void setInactive(final String jobId) throws Exception {
		if (JobsDebug.debug) {
			LOG.debug("Marking job {} inactive.", jobId);
		}
		JobHungDetectionHelper.setInactive(toInternalId(jobId));
		final Preferences jobNode = CloudPreferncesJobStorage.getJobsNode().node(toInternalId(jobId));

		executeWithRetry(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				jobNode.sync();
				jobNode.remove(CloudPreferncesJobStorage.PROPERTY_ACTIVE);
				jobNode.flush();
				return null;
			}
		});
	}

	private void setJobParameter(final JobImpl job, final Map<String, String> parameter, final IExclusiveLock lock) throws BackingStoreException {
		if ((null == lock) || !lock.isValid())
			throw new IllegalStateException(String.format("Unable to update parameter of job %s due to missing or lost job lock!", job.getId()));

		final String internalId = toInternalId(job.getId());
		if (!CloudPreferncesJobStorage.getJobsNode().nodeExists(internalId))
			// don't update if removed
			return;

		final Preferences jobNode = CloudPreferncesJobStorage.getJobsNode().node(internalId);
		if ((null != parameter) && !parameter.isEmpty()) {
			final Preferences paramNode = jobNode.node(CloudPreferncesJobStorage.NODE_PARAMETER);
			// remove obsolete keys
			for (final String key : paramNode.keys()) {
				if (StringUtils.isBlank(parameter.get(key))) {
					paramNode.remove(key);
				}
			}
			// add updated/new parameter
			for (final Entry<String, String> entry : parameter.entrySet()) {
				if (StringUtils.isNotBlank(entry.getValue())) {
					paramNode.put(entry.getKey(), entry.getValue());
				}
			}
		} else {
			if (jobNode.nodeExists(CloudPreferncesJobStorage.NODE_PARAMETER)) {
				jobNode.node(CloudPreferncesJobStorage.NODE_PARAMETER).removeNode();
			}
		}

		// flush
		jobNode.flush();

		// update job (create a copy to prevent modifications from outside)
		job.setParameter(parameter != null ? new HashMap<String, String>(parameter) : null);
	}

	@Override
	public void setJobParameter(final String jobId, final Map<String, String> parameter) throws IllegalStateException, IllegalArgumentException {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));
		if (null == parameter)
			throw new IllegalArgumentException("parameter must not be null");

		JobImpl job = getJob(jobId);
		if (null == job)
			throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

		// check the job state (but ignore stuck jobs)
		if ((job.getState() != JobState.NONE) && !isStuck(job))
			throw new IllegalStateException(String.format("Job %s cannot be updated because of a job state conflict (expected %s, got %s)!", jobId, JobState.NONE.toString(), job.getState().toString()));

		IExclusiveLock jobLock = null;
		try {
			// acquire lock
			jobLock = acquireLock(job);

			// re-read job status (inside lock)
			job = getJob(jobId);
			if (null == job)
				throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

			// re-check the job state (inside lock) but ignore stuck jobs
			if (job.getState() != JobState.NONE) {
				if (!isStuck(job))
					throw new IllegalStateException(String.format("Job %s cannot be updated because of a job state conflict (expected %s, got %s)!", jobId, JobState.NONE.toString(), job.getState().toString()));
				else {
					LOG.warn("Job {} is in state {}. However, it's assumed stuck. The updated will reset the state!", job.getId(), job.getState());
				}
			}

			// update job parameter
			setJobParameter(job, parameter, jobLock);

			// reset state
			setJobState(job, JobState.NONE, jobLock);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error updating job parameter. %s", e.getMessage()), e);
		} finally {
			releaseLock(jobLock, jobId);
		}
	}

	/**
	 * records queuing time and trigger.
	 */
	private void setJobQueued(final IJob job, final long timestamp, final String trigger, final IExclusiveLock lock) throws BackingStoreException {
		if ((null == lock) || !lock.isValid())
			throw new IllegalStateException(String.format("Unable to update job %s due to missing or lost job lock!", job.getId()));

		final String internalId = toInternalId(job.getId());
		if (!CloudPreferncesJobStorage.getJobsNode().nodeExists(internalId))
			// don't update if removed
			return;

		// update job node
		final Preferences jobNode = CloudPreferncesJobStorage.getJobsNode().node(internalId);
		jobNode.putLong(CloudPreferncesJobStorage.PROPERTY_LAST_QUEUED, timestamp);
		jobNode.put(CloudPreferncesJobStorage.PROPERTY_LAST_QUEUED_TRIGGER, trigger);
		jobNode.flush();
	}

	private void setJobResult(final JobImpl job, final Map<String, String> parameter, final IStatus result, final long resultTimestamp, final String queueTrigger, final long queueTimestamp, final IExclusiveLock lock) throws BackingStoreException {
		if ((null == lock) || !lock.isValid())
			throw new IllegalStateException(String.format("Unable to update job result of job %s due to missing or lost job lock!", job.getId()));
		if (null == result)
			throw new IllegalStateException(String.format("Unable to update job result of job %s due to missing result!", job.getId()));

		final String internalId = toInternalId(job.getId());
		if (!CloudPreferncesJobStorage.getJobsNode().nodeExists(internalId))
			// don't update if removed
			return;

		// update job node
		final Preferences jobNode = CloudPreferncesJobStorage.getJobsNode().node(internalId);
		jobNode.putLong(CloudPreferncesJobStorage.PROPERTY_LAST_RESULT, resultTimestamp);
		jobNode.put(CloudPreferncesJobStorage.PROPERTY_LAST_RESULT_MESSAGE, StringUtils.left(CloudPreferncesJobHistoryStorage.getFormattedMessage(result, 0), CloudPreferncesJobHistoryStorage.MAX_RESULT_MESSAGE_SIZE));
		jobNode.putInt(CloudPreferncesJobStorage.PROPERTY_LAST_RESULT_SEVERITY, result.getSeverity());
		if (!result.matches(IStatus.CANCEL | IStatus.ERROR)) {
			// every run that does not result in ERROR or CANCEL is considered successful
			jobNode.putLong(CloudPreferncesJobStorage.PROPERTY_LAST_SUCCESSFUL_FINISH, resultTimestamp);
		}
		jobNode.flush();

		// save history
		final IJobHistoryStorage storage = context.get(IJobHistoryStorage.class);
		if (storage != null) {
			final JobHistoryEntryStorable storable = new JobHistoryEntryStorable();
			storable.setResult(result);
			storable.setTimestamp(resultTimestamp);
			storable.setParameter(parameter);
			storable.setQueuedTrigger(queueTrigger);
			// only pass cancellation trigger to history if it makes sense
			// FIXME: is logic is brittle, we need to make it part of the job message the queued and started the job
			if ((job.getLastCancelled() > job.getLastQueued()) && (job.getLastQueued() < resultTimestamp)) {
				storable.setCancelledTrigger(job.getLastCancelledTrigger());
			}

			try {
				storage.add(job.getId(), storable);
			} catch (final Exception e) {
				LOG.error("Error persisting job history for job '{}' (context {}). {}", job.getId(), context.getContextPath(), ExceptionUtils.getRootCauseMessage(e), e);
			}
		}
	}

	private void setJobStartTime(final IJob job, final long startTimestamp, final IExclusiveLock lock) throws BackingStoreException {
		if ((null == lock) || !lock.isValid())
			throw new IllegalStateException(String.format("Unable to update job %s due to missing or lost job lock!", job.getId()));

		final String internalId = toInternalId(job.getId());
		if (!CloudPreferncesJobStorage.getJobsNode().nodeExists(internalId))
			// don't update if removed
			return;

		// update job node
		final Preferences jobNode = CloudPreferncesJobStorage.getJobsNode().node(internalId);
		jobNode.putLong(CloudPreferncesJobStorage.PROPERTY_LAST_START, startTimestamp);
		jobNode.flush();
	}

	/**
	 * Updates the state in an atomic way.
	 * <p>
	 * When the job state was changes successfully a watch will be registered
	 * which will be informed in case of the next state change
	 * </p>
	 * 
	 * @param jobId
	 *            the job id
	 * @param expected
	 *            the expected state
	 * @param state
	 *            the new state
	 * @param stateWatch
	 *            a watch to add
	 * @return <code>true</code> if and only if the job state was changed to the
	 *         given state successfully
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the job does not exist or an error occured updating the
	 *             job
	 */
	public boolean setJobState(final String jobId, final JobState expected, final JobState state, final IJobStateWatch stateWatch) throws IllegalArgumentException, IllegalStateException {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));
		if (null == state)
			throw new IllegalArgumentException("Job state must not be null");

		JobImpl job = getJob(jobId);
		if (null == job)
			throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

		IExclusiveLock jobLock = null;
		try {
			// get job modification lock
			jobLock = acquireLock(job);

			// make sure node is in sync
			syncJobNode(jobId);

			// re-read job status (inside lock)
			job = getJob(jobId);
			if (null == job)
				throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

			// check job status (inside lock) but ignore for stuck jobs
			if ((null != expected) && (job.getState() != expected)) {
				if (!isStuck(job))
					// no-op
					return false;
				else {
					LOG.warn("Job {} is in state {} which doesn't match the expected state {}. However, it's assumed stuck. Thus, the status will be reset to {}!", new Object[] { job.getId(), job.getState(), expected, state });
				}
			}

			try {
				// set state
				setJobState(job, state, jobLock);

				// update last run time if new state is RUNNING
				if (state == JobState.RUNNING) {
					// set start time
					setJobStartTime(job, System.currentTimeMillis(), jobLock);
				}

				// add watch
				if (null != stateWatch) {
					((IEclipsePreferences) CloudPreferncesJobStorage.getJobsNode().node(toInternalId(jobId))).addPreferenceChangeListener(new StateWatchListener(jobId, state, stateWatch));
				}

				// report update success
				return true;
			} catch (final BackingStoreException e) {
				throw new IllegalStateException(String.format("Error updating state of job %s to %s. %s", jobId, state.name(), e.getMessage()), e);
			}
		} finally {
			releaseLock(jobLock, jobId);
		}
	}

	public void setResult(final String jobId, final Map<String, String> parameter, final IStatus result, final long resultTimestamp, final String queueTrigger, final long queueTimestamp) {
		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException(String.format("Invalid id '%s'", jobId));

		JobImpl job = getJob(jobId);
		if (null == job)
			throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

		IExclusiveLock jobLock = null;
		try {
			// get job modification lock
			jobLock = acquireLock(job);

			// refresh job info
			syncJobNode(jobId);

			try {
				// make sure node is in sync
				syncJobNode(jobId);

				// re-read job status (inside lock)
				job = getJob(jobId);
				if (null == job)
					throw new IllegalStateException(String.format("Job %s does not exist!", jobId));

				// set state
				setJobState(job, JobState.NONE, jobLock);

				// set result
				setJobResult(job, parameter, result, resultTimestamp, queueTrigger, queueTimestamp, jobLock);
			} catch (final BackingStoreException e) {
				throw new IllegalStateException(String.format("Error setting result of job %s. %s", jobId, e.getMessage()), e);
			}
		} finally {
			releaseLock(jobLock, jobId);
		}
	}

	private void syncJobNode(final String jobId) {
		try {
			executeWithRetry(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					CloudPreferncesJobStorage.getJobsNode().node(toInternalId(jobId)).sync();
					return null;
				}
			});
		} catch (final Exception e) {
			LOG.warn("Exception refreshing job {}. Available job data might be stale.", toInternalId(jobId), e);
		}
	}

	private String toExternalId(final String internalId) {
		return contextHash.toExternalId(internalId);
	}

	private String toInternalId(final String id) {
		return contextHash.toInternalId(id);
	}

}
