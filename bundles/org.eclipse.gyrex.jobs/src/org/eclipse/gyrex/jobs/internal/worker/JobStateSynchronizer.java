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
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.worker;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;
import org.eclipse.gyrex.cloud.services.locking.ILockService;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.manager.IJobStateWatch;
import org.eclipse.gyrex.jobs.internal.manager.JobImpl;
import org.eclipse.gyrex.jobs.internal.manager.JobManagerImpl;
import org.eclipse.gyrex.jobs.manager.IJobManager;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.exception.ExceptionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes Gyrex Job state with Eclipse Jobs state.
 */
public final class JobStateSynchronizer implements IJobChangeListener, IJobStateWatch, ILockMonitor<IExclusiveLock> {

	private static final Logger LOG = LoggerFactory.getLogger(JobStateSynchronizer.class);

	private final Job realJob;
	private final JobContext jobContext;
	private final JobInfo info;

	private IExclusiveLock lock;
	private long startTimestamp;
	private String oldThreadName;

	public JobStateSynchronizer(final Job realJob, final JobContext jobContext, final JobInfo info) {
		// just remember variable; never hook any listeners here
		this.realJob = realJob;
		this.jobContext = jobContext;
		this.info = info;
	}

	@Override
	public void aboutToRun(final IJobChangeEvent event) {
		try {
			// setup MDC so that any output is routed properly
			JobLogHelper.setupMdc(jobContext);

			// check if job should be executed or was canceled meanwhile
			final JobManagerImpl jobManager = getJobManager();

			final String jobId = getJobId();
			final IJob job = jobManager.getJob(jobId);

			// if the job state is not waiting, we cancel it
			if ((job.getState() != JobState.WAITING)) {
				// cancel Eclipse Job
				cancelRealJob();
				// abort execution now (so that no lock will be acquired)
				// note, we do not cleanup any state here because
				// the Eclipse Jobs API will actually call #done
				// (with no result) after we return
				return;
			}

			// check if a lock should be acquired
			final String lockId = getJobParameter().get(IJobManager.LOCK_ID);
			if (null != lockId) {
				if (!acquireLock(lockId)) {
					LOG.warn("Failed acquiring lock {} for job {}. Job execution will be delayed.", lockId, getJobId());

					// cancel any running job
					cancelRealJob();

					// again, as stated previously, we rely on the Eclipse Jobs API
					// calling #done for proper state clean-up

					// only re-schedule if thread wasn't interrupted
					if (!Thread.interrupted()) {
						// (note, scheduling the job will trigger #scheduled again after triggering #done)
						realJob.schedule(10000L);
					}
				}
			}
		} finally {
			// clear the MDC (must be set again in #running)
			JobLogHelper.clearMdc();
		}
	}

	private synchronized boolean acquireLock(final String lockId) {
		if ((null != lock) && lock.isValid()) {
			if (JobsDebug.workerEngine) {
				LOG.debug("Reusing acquired lock ({}) for job {}...", lock, getJobId());
			}
			return true; // consider success
		}

		if (JobsDebug.workerEngine) {
			LOG.debug("Acquiring lock {} for job {}...", lockId, getJobId());
		}
		try {
			lock = getLockService().acquireExclusiveLock(lockId, this, 2000L);
			return true; // got lock, ok to proceed
		} catch (final InterruptedException e) {
			// set interrupted flag
			Thread.currentThread().interrupt();
			return false; // interrupted
		} catch (final TimeoutException e) {
			return false; // timeout
		} catch (final Exception e) {
			LOG.error("Exception while acquiring lock {} for job {}. {}", new Object[] { lockId, getJobId(), ExceptionUtils.getRootCauseMessage(e), e });
			// consider not successful
			return false;
		}
	}

	@Override
	public void awake(final IJobChangeEvent event) {
		// nothing to do (for now)
	}

	private void cancelRealJob() {
		// cancel Eclipse job
		if (realJob.cancel()) {
			// log message
			LOG.info("Job {} has been canceled.", getJobId());
		} else {
			// log message
			LOG.info("Job {} cancelation has been requested. However, it is currently running and thus may not respond to cancelation.", getJobId());
		}
	}

	@Override
	public void done(final IJobChangeEvent event) {
		try {
			// setup MDC so that any output is routed properly
			JobLogHelper.setupMdc(jobContext);

			// release any held lock
			if (null != lock) {
				releaseLock();
			}

			// set the job inactive
			// (note, it was set active when it was scheduled)
			getJobManager().setInactive(getJobId());

			// update job state (but do not expect a current state, may be RUNNING or ABORTING)
			updateJobState(null, JobState.NONE, null, System.currentTimeMillis());

			// update job with result
			getJobManager().setResult(getJobId(), getJobParameter(), event.getResult(), System.currentTimeMillis(), startTimestamp, info.getQueueTrigger(), info.getQueueTimestamp());
		} catch (final Exception e) {
			LOG.error("Error updating job {} (with result {}): {}", new Object[] { getJobId(), event.getResult(), ExceptionUtils.getRootCauseMessage(e), e });
		} finally {
			// reset thread name
			if (oldThreadName != null) {
				try {
					Thread.currentThread().setName(oldThreadName);
				} catch (final Exception e) {
					// ignored
				}
				oldThreadName = null;
			}

			// clear the MDC (as the last thing to do)
			JobLogHelper.clearMdc();
		}
	}

	private String getJobId() {
		return jobContext.getJobId();
	}

	JobManagerImpl getJobManager() {
		return (JobManagerImpl) getRuntimeContext().get(IJobManager.class);
	}

	private Map<String, String> getJobParameter() {
		return jobContext.getParameter();
	}

	private ILockService getLockService() {
		return JobsActivator.getInstance().getService(ILockService.class);
	}

	private IRuntimeContext getRuntimeContext() {
		return jobContext.getContext();
	}

	boolean isJobMarkedAborting() {
		try {
			return getJobManager().getJob(getJobId()).getState() == JobState.ABORTING;
		} catch (final Exception e) {
			LOG.warn("Unable read job state ({}): {}", new Object[] { getJobId(), ExceptionUtils.getRootCauseMessage(e), e });
			return false;
		}
	}

	@Override
	public void jobStateChanged(final String jobId) {
		final IJob job = getJobManager().getJob(getJobId());
		final JobState state = job.getState();

		// handle aborting changes
		if (state == JobState.ABORTING) {
			// cancel Eclipse Job
			cancelRealJob();

			// again, as stated previously, we rely on the Eclipse Jobs API
			// calling #done for proper state clean-up; this is also very
			// important in this special case because the job state change
			// watch might be triggered while a job modification is in progress
			// (on the same or different machine); thus any job state changes
			// must be performed asynchronously
		}
	}

	@Override
	public void lockAcquired(final IExclusiveLock lock) {
		// empty
	}

	@Override
	public void lockLost(final IExclusiveLock lock) {
		LOG.warn("Lost lock {} for job {}. An attempt will be made to cancel a running job instance.", lock.getId(), getJobId());

		// abort the job
		cancelRealJob();

		// again, as stated previously, we rely on the Eclipse Jobs API
		// calling #done for proper state clean-up
	}

	@Override
	public void lockReleased(final IExclusiveLock lock) {
		// empty
	}

	@Override
	public void lockSuspended(final IExclusiveLock lock) {
		// we let the job continue
	}

	private synchronized void releaseLock() {
		if (null != lock) {
			if (JobsDebug.workerEngine) {
				LOG.debug("Releasing lock {} for job {}...", lock.getId(), getJobId());
			}
			try {
				lock.release();
			} catch (final Exception e) {
				// ignored
			}
			lock = null;
		}
	}

	@Override
	public void running(final IJobChangeEvent event) {
		try {
			// setup MDC first (in order to have proper MDC in case of exceptions)
			// however, we don't clear it in this method so that the Job implementation also benefits from the MDC
			JobLogHelper.setupMdc(jobContext);

			// update thread name with job id
			try {
				oldThreadName = Thread.currentThread().getName();
				Thread.currentThread().setName(String.format("%s [%s])", oldThreadName, getJobId()));
			} catch (final Exception e) {
				// ignore
			}

			// remember start time
			startTimestamp = System.currentTimeMillis();

			// set job state running
			updateJobState(JobState.WAITING, JobState.RUNNING, this, startTimestamp);
		} catch (final Exception e) {
			LOG.error("Error updating job state {} (with result {}): {}", new Object[] { getJobId(), event.getResult(), ExceptionUtils.getRootCauseMessage(e), e });
		}
	}

	@Override
	public void scheduled(final IJobChangeEvent event) {
		try {
			// setup MDC so that any output is routed properly
			JobLogHelper.setupMdc(jobContext);

			// set the job active
			getJobManager().setActive(getJobId());

			// update job state (it is now WAITING)
			updateJobState(null, JobState.WAITING, null, System.currentTimeMillis());
		} catch (final Exception e) {
			LOG.error("Error updating job {}: {}", new Object[] { getJobId(), ExceptionUtils.getRootCauseMessage(e), e });
		} finally {
			// clear the MDC (as the last thing to do)
			JobLogHelper.clearMdc();
		}
	}

	void setJobAborted() {
		// update job state (but do not expect a current state, may be ABORTING)
		updateJobState(JobState.ABORTING, JobState.NONE, null, System.currentTimeMillis());
	}

	public boolean setJobActive() {
		try {
			// log message
			if (JobsDebug.workerEngine) {
				LOG.debug("Activating job {}...", getJobId());
			}

			// set the job active
			// (this is used by the worker engine to indicate processing is ready)
			getJobManager().setActive(getJobId());

			// success
			return true;
		} catch (final Exception e) {
			LOG.warn("Unable to set job active ({}): {}", new Object[] { getJobId(), ExceptionUtils.getRootCauseMessage(e), e });
			return false;
		}
	}

	public void setJobInactive() {
		try {
			// log message
			if (JobsDebug.workerEngine) {
				LOG.debug("Inactivating job {}...", getJobId());
			}

			// set the job in-active
			getJobManager().setInactive(getJobId());
		} catch (final Exception e) {
			LOG.warn("Unable to set job inactive ({}): {}", new Object[] { getJobId(), ExceptionUtils.getRootCauseMessage(e), e });
		}
	}

	@Override
	public void sleeping(final IJobChangeEvent event) {
		// nothing to do (for now)
	}

	private void updateJobState(final JobState expected, final JobState state, final IJobStateWatch jobStateWatch, final long stateTimestamp) {
		try {
			if (!getJobManager().setJobState(getJobId(), expected, state, jobStateWatch, stateTimestamp)) {
				final JobImpl job = getJobManager().getJob(getJobId());
				if (null != job) {
					if (job.getState() != state) {
						LOG.error("Unable to update job {} from {} to {}. The jobs current state is {}.", new Object[] { getJobId(), null != expected ? "state " + expected : "any state", state, job.getState() });
					}
				}
			}
		} catch (final Exception e) {
			LOG.error("Error updating job {} from {} to {}: {}", new Object[] { getJobId(), null != expected ? "state " + expected : "any state", state, ExceptionUtils.getRootCauseMessage(e), e });
		}
	}
}
