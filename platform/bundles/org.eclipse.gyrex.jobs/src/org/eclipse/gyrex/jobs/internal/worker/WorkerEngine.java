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
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;

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

	private Job createJob(final JobInfo info) {
		// TODO Auto-generated method stub
		return null;
	}

	private IStatus doExecuteScheduledJob(final IProgressMonitor monitor) {
		try {
			final IQueue queue = getQueue();
			if (queue == null) {
				LOG.warn("No queue available for reading scheduled jobs to execute. Please check engine configuration.");
				return Status.CANCEL_STATUS;
			}

			// set receive timeout
			final Map<String, Object> requestProperties = new HashMap<String, Object>(1);
			requestProperties.put(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT, getReceiveTimeout());

			// receive message
			final List<IMessage> messages = queue.receiveMessages(1, requestProperties);
			if (messages.isEmpty()) {
				return Status.OK_STATUS;
			}
			final IMessage message = messages.get(0);

			try {
				// read job info
				final JobInfo info = JobInfo.parse(message);

				// create job
				final Job job = createJob(info);

				// delete from queue
				if (queue.deleteMessage(message)) {
					job.schedule();
				}

			} catch (final IOException e) {
				LOG.warn("Invalid job info in message {}: {}", message, ExceptionUtils.getRootCauseMessage(e));

				// move message to the failure queue
				if (queue.deleteMessage(message)) {
					IQueue queueForFailedJobs = null;
					try {
						queueForFailedJobs = getQueueForFailedJobs();
						queueForFailedJobs.sendMessage(message.getBody());
					} catch (final Exception e2) {
						LOG.error("Failed moving job message {} to failure queue {}. Job is lost. {}", new Object[] { message, queueForFailedJobs, ExceptionUtils.getRootCauseMessage(e2) });
					}
				} else {
					LOG.error("Unable to delete job message. {}", message);
				}
				return Status.CANCEL_STATUS;
			}

		} catch (final IllegalStateException e) {
			LOG.warn("Unable to check queue for new jobs. System does not seem to be ready. {}", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
		}

		return Status.OK_STATUS;
	}

	/**
	 * Returns the queue with scheduled jobs.
	 * 
	 * @return
	 */
	protected IQueue getQueue() {
		final IQueueService queueService = JobsActivator.getInstance().getQueueService();
		return queueService.getQueue("gyrex.jobs.scheduled", null);
	}

	/**
	 * Returns the queue with scheduled jobs.
	 * 
	 * @return
	 */
	protected IQueue getQueueForFailedJobs() {
		final IQueueService queueService = JobsActivator.getInstance().getQueueService();
		return queueService.getQueue("gyrex.jobs.failed", null);
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

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			final IStatus status = doExecuteScheduledJob(monitor);
			if (!status.isOK()) {
				// implement a back-off sleeping time (max 5 min)
				engineSleepTime = Math.min(engineSleepTime * 2, MAX_SLEEP_TIME);
			} else {
				// reset sleep time
				engineSleepTime = INITIAL_SLEEP_TIME;
			}
			return status;
		} finally {
			// reschedule
			if (JobsDebug.workerEngine) {
				LOG.debug("Rescheduling worker engine to run again in {} seconds", TimeUnit.MILLISECONDS.toSeconds(engineSleepTime));
			}
			schedule(engineSleepTime);
		}
	}

}
