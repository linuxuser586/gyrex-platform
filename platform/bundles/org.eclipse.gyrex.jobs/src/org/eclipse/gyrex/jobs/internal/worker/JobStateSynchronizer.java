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
import org.eclipse.gyrex.jobs.internal.manager.JobManagerImpl;
import org.eclipse.gyrex.jobs.manager.IJobManager;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;

import org.slf4j.MDC;

/**
 * Synchronizes Gyrex Job state with Eclipse Jobs state.
 */
public final class JobStateSynchronizer implements IJobChangeListener, IJobStateWatch {

	private static final String MDC_KEY_CONTEXT_PATH = "gyrex.contextPath";
	private static final String MDC_KEY_JOB_ID = "gyrex.jobId";

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
			getJobManager().setJobState(getJobId(), JobState.ABORTING, JobState.NONE, null);
		}
	}

	private void clearMdc() {
		MDC.remove(MDC_KEY_JOB_ID);
		MDC.remove(MDC_KEY_CONTEXT_PATH);
	}

	@Override
	public void done(final IJobChangeEvent event) {
		// clear the MDC
		clearMdc();

		// update job state
		getJobManager().setJobState(getJobId(), null, JobState.NONE, null);

		// update job with result
		getJobManager().setResult(getJobId(), event.getResult(), System.currentTimeMillis());
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
		// set job state running
		getJobManager().setJobState(getJobId(), JobState.WAITING, JobState.RUNNING, this);

		// setup MDC
		setupMdc();
	}

	@Override
	public void scheduled(final IJobChangeEvent event) {
		// we're not interested at the moment - job state is waiting
	}

	private void setupMdc() {
		MDC.put(MDC_KEY_JOB_ID, getJobId());
		MDC.put(MDC_KEY_CONTEXT_PATH, getRuntimeContext().getContextPath().toString());
	}

	@Override
	public void sleeping(final IJobChangeEvent event) {
		// we're not interested at the moment - job state is running
	}
}
