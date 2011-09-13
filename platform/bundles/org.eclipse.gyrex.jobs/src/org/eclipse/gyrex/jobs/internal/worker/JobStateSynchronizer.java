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
package org.eclipse.gyrex.jobs.internal.worker;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.JobState;
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
public final class JobStateSynchronizer implements IJobChangeListener, IJobStateWatch {

	private static final Logger LOG = LoggerFactory.getLogger(JobStateSynchronizer.class);

	private final Job realJob;
	private final JobContext jobContext;

	public JobStateSynchronizer(final Job realJob, final JobContext jobContext) {
		this.realJob = realJob;
		this.jobContext = jobContext;
	}

	@Override
	public void aboutToRun(final IJobChangeEvent event) {
		// check if job should be executed or was canceled meanwhile
		final JobManagerImpl jobManager = getJobManager();

		final String jobId = getJobId();
		final IJob job = jobManager.getJob(jobId);

		// if the job state is not waiting, we cancel it
		if ((job.getState() != JobState.WAITING)) {
			cancelRealJob();
		}
	}

	@Override
	public void awake(final IJobChangeEvent event) {
		// we're not interested at the moment - job state is running
	}

	private void cancelRealJob() {
		// cancel Eclipse job
		if (realJob.cancel()) {
			// update job if cancellation was successful
			updateJobState(JobState.ABORTING, JobState.NONE, null);
		}
	}

	@Override
	public void done(final IJobChangeEvent event) {
		try {
			// update job state
			updateJobState(null, JobState.NONE, null);

			// update job with result
			getJobManager().setResult(getJobId(), event.getResult(), System.currentTimeMillis());
		} catch (final Exception e) {
			LOG.error("Error updating job {} (with result {}): {}", new Object[] { getJobId(), event.getResult(), ExceptionUtils.getRootCauseMessage(e), e });
		} finally {
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

	private IRuntimeContext getRuntimeContext() {
		return jobContext.getContext();
	}

	@Override
	public void jobStateChanged(final String jobId) {
		final IJob job = getJobManager().getJob(getJobId());
		final JobState state = job.getState();
		if (state == JobState.ABORTING) {
			cancelRealJob();
		}
	}

	@Override
	public void running(final IJobChangeEvent event) {
		try {
			// setup MDC first (in order to have proper MDC in case of exceptions)
			JobLogHelper.setupMdc(jobContext);

			// set job state running
			updateJobState(JobState.WAITING, JobState.RUNNING, this);
		} catch (final Exception e) {
			LOG.error("Error updating job state {} (with result {}): {}", new Object[] { getJobId(), event.getResult(), ExceptionUtils.getRootCauseMessage(e), e });
		}
	}

	@Override
	public void scheduled(final IJobChangeEvent event) {
		// we're not interested at the moment - job state is waiting
	}

	@Override
	public void sleeping(final IJobChangeEvent event) {
		// we're not interested at the moment - job state is running
	}

	private void updateJobState(final JobState expected, final JobState state, final IJobStateWatch jobStateWatch) {
		try {
			if (!getJobManager().setJobState(getJobId(), expected, state, jobStateWatch)) {
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
