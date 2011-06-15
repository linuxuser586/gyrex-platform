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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.worker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.services.queue.IMessage;
import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.cloud.services.queue.IQueueServiceProperties;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.IJobContext;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.manager.JobManagerImpl;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The worker engine pulls jobs from queues and executes them.
 */
public class WorkerEngine extends Job {

	private static final long INITIAL_SLEEP_TIME = TimeUnit.SECONDS.toMillis(30);
	private static final long MAX_SLEEP_TIME = TimeUnit.MINUTES.toMillis(5);

	private static final Logger LOG = LoggerFactory.getLogger(WorkerEngine.class);

	private long engineSleepTime = INITIAL_SLEEP_TIME;

	/**
	 * Creates a new instance.
	 */
	public WorkerEngine() {
		super("Gyrex Worker Engine Job");
		setSystem(true);
		setPriority(LONG);
	}

	private JobContext createContext(final JobInfo info) {
		final IRuntimeContextRegistry contextRegistry = JobsActivator.getInstance().getService(IRuntimeContextRegistry.class);
		final IRuntimeContext context = contextRegistry.get(info.getContextPath());
		if (null == context) {
			throw new IllegalStateException(String.format("Context %s not available!", info.getContextPath().toString()));
		}
		return new JobContext(context, info);
	}

	private Job createJob(final IQueue queue, final IMessage message, final JobInfo info, final JobContext jobContext) {
		try {
			return createJobInstance(info, jobContext);
		} catch (final Exception e) {
			// log warning
			LOG.warn("Error creating job {}: {}", message, ExceptionUtils.getRootCauseMessage(e));

			// discard message
			queue.deleteMessage(message);

			// set job status
			try {
				final JobManagerImpl jobManager = (JobManagerImpl) jobContext.getContext().get(IJobManager.class);
				jobManager.setResult(info.getJobId(), new Status(IStatus.ERROR, JobsActivator.SYMBOLIC_NAME, String.format("Error creating job: %s", e.getMessage()), e), System.currentTimeMillis());
			} catch (final Exception jobManagerException) {
				LOG.warn("Error updating job result for job {}: {}", info.getJobId(), ExceptionUtils.getRootCauseMessage(jobManagerException));
			}

			return null;
		}
	}

	private Job createJobInstance(final JobInfo info, final IJobContext jobContext) throws Exception {
		final JobProvider provider = JobsActivator.getInstance().getJobProviderRegistry().getProvider(info.getJobTypeId());
		if (null == provider) {
			throw new IllegalStateException(String.format("Job type %s not available!", info.getJobTypeId()));
		}

		final Job job = provider.createJob(info.getJobTypeId(), jobContext);
		if (null == job) {
			throw new IllegalStateException(String.format("Provider %s did not create job of type %s!", provider.toString(), info.getJobTypeId()));
		}
		return job;
	}

	private IStatus doRun(final IProgressMonitor monitor) {
		try {
			final IQueue queue = getDefaultQueue(); // TODO how to get messages from other queues?
			if (queue == null) {
				LOG.warn("No queue available for reading scheduled jobs to execute. Please check engine configuration.");
				return Status.CANCEL_STATUS;
			}

			// set receive timeout
			final Map<String, Object> requestProperties = new HashMap<String, Object>(1);
			requestProperties.put(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT, getReceiveTimeout());

			// process as long as we have messages in the queue
			while (true) {

				// receive message
				final List<IMessage> messages = queue.receiveMessages(1, requestProperties);
				if (messages.isEmpty()) {
					// no more messages, abort
					return Status.OK_STATUS;
				}
				final IMessage message = messages.get(0);

				// read job info
				final JobInfo info = parseJobInfo(queue, message);
				if (null == info) {
					// continue with next message
					continue;
				}

				// create context
				final JobContext jobContext = createContext(info);

				// create job
				final Job job = createJob(queue, message, info, jobContext);
				if (null == job) {
					// continue with next message
					continue;
				}

				// add state synchronizer
				job.addJobChangeListener(new JobStateSynchronizer(job, jobContext));

				// delete from queue and schedule if successful
				if (queue.deleteMessage(message)) {
					job.schedule();
				} else {
					// someone else might already processed it
					// just continue with next available message
				}
			}

		} catch (final IllegalStateException e) {
			LOG.warn("Unable to check queue for new jobs. System does not seem to be ready. {}", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
		}
	}

	/**
	 * Returns the queue with scheduled jobs.
	 * 
	 * @return
	 */
	protected IQueue getDefaultQueue() {
		final IQueueService queueService = JobsActivator.getInstance().getQueueService();

		final IQueue queue = queueService.getQueue(IJobManager.DEFAULT_QUEUE, null);
		if (null == queue) {
			queueService.createQueue(IJobManager.DEFAULT_QUEUE, null);
		}

		return queueService.getQueue(IJobManager.DEFAULT_QUEUE, null);
	}

	/**
	 * Returns the receive timeout as specified by
	 * {@link IQueueServiceProperties#MESSAGE_RECEIVE_TIMEOUT}.
	 * <p>
	 * The default implementation returns 1 minute.
	 * </p>
	 * 
	 * @return the receive timeout
	 */
	protected long getReceiveTimeout() {
		return TimeUnit.MINUTES.toMillis(1);
	}

	private JobInfo parseJobInfo(final IQueue queue, final IMessage message) {
		try {
			return JobInfo.parse(message);
		} catch (final IOException e) {
			// log warning
			LOG.warn("Invalid job info in message {}: {}", message, ExceptionUtils.getRootCauseMessage(e));
			// discard message
			queue.deleteMessage(message);
		}
		return null;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			final IStatus status = doRun(monitor);
			if (!status.isOK()) {
				// implement a back-off sleeping time (max 5 min)
				engineSleepTime = Math.min(engineSleepTime * 2, MAX_SLEEP_TIME);
			} else {
				// reset sleep time
				engineSleepTime = INITIAL_SLEEP_TIME;
			}
			return status;
		} finally {
			// reschedule if not canceled
			if (!monitor.isCanceled()) {
				if (JobsDebug.workerEngine) {
					LOG.debug("Rescheduling worker engine to run again in {} seconds", TimeUnit.MILLISECONDS.toSeconds(engineSleepTime));
				}
				schedule(engineSleepTime);
			}
		}
	}
}
