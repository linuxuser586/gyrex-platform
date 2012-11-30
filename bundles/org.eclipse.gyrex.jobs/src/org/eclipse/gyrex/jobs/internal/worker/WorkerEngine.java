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
import java.util.NoSuchElementException;
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
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The worker engine pulls jobs from queues and executes them.
 */
public class WorkerEngine extends Job {

	private static final long DEFAULT_IDLE_SLEEP_TIME = TimeUnit.SECONDS.toMillis(20);
	private static final long DEFAULT_NON_IDLE_SLEEP_TIME = TimeUnit.SECONDS.toMillis(3);
	private static final long DEFAULT_MAX_SLEEP_TIME = TimeUnit.MINUTES.toMillis(5);

	private static final String NODE_WORKER_ENGINE = "workerEngine";
	private static final String PREF_KEY_SUSPENDED = "suspended";

	private static final Logger LOG = LoggerFactory.getLogger(WorkerEngine.class);

	private static Preferences getWorkerEnginePreferences() {
		return CloudScope.INSTANCE.getNode(JobsActivator.SYMBOLIC_NAME).node(NODE_WORKER_ENGINE);
	}

	public static void resume() throws BackingStoreException {
		final Preferences preferences = getWorkerEnginePreferences();
		preferences.remove(PREF_KEY_SUSPENDED);
		preferences.flush();
	}

	public static void suspend() throws BackingStoreException {
		final Preferences preferences = getWorkerEnginePreferences();
		preferences.putBoolean(PREF_KEY_SUSPENDED, true);
		preferences.flush();
	}

	private final WorkerEngineMetrics metrics;
	private final int maxConcurrentJobs;
	private final long idleSleepTime;
	private final long nonIdleSleepTime;

	private long engineSleepTime = DEFAULT_IDLE_SLEEP_TIME;

	private volatile int scheduledJobsCount;

	private final IJobChangeListener jobFinishedListener = new JobChangeAdapter() {
		@Override
		public void done(final IJobChangeEvent event) {
			// decrease scheduled jobs count
			scheduledJobsCount--;
			// make sure the values make sense
			if (scheduledJobsCount < 0) {
				scheduledJobsCount = 0;
			}
			// update metric
			metrics.getCapacity().channelFinished();
		};

		@Override
		public void scheduled(final IJobChangeEvent event) {
			// update metric
			metrics.getCapacity().channelStarted(0);
		}
	};

	/**
	 * Creates a new instance.
	 */
	public WorkerEngine(final WorkerEngineMetrics metrics) {
		super("Gyrex Worker Engine Job");
		this.metrics = metrics;
		setSystem(true);
		setPriority(LONG);
		idleSleepTime = Long.getLong("gyrex.jobs.workerEngine.idleSleepTime", DEFAULT_IDLE_SLEEP_TIME);
		nonIdleSleepTime = Long.getLong("gyrex.jobs.workerEngine.nonIdleSleepTime", DEFAULT_NON_IDLE_SLEEP_TIME);
		maxConcurrentJobs = Integer.getInteger("gyrex.jobs.workerEngine.maxConcurrentScheduledJobs", Runtime.getRuntime().availableProcessors() * 2);
	}

	private JobContext createContext(final JobInfo info) {
		final IRuntimeContextRegistry contextRegistry = JobsActivator.getInstance().getService(IRuntimeContextRegistry.class);
		final IRuntimeContext context = contextRegistry.get(info.getContextPath());
		if (null == context)
			throw new IllegalStateException(String.format("Context %s not available!", info.getContextPath().toString()));
		return new JobContext(context, info);
	}

	private Job createJob(final IQueue queue, final IMessage message, final JobInfo info, final JobContext jobContext) {
		try {
			// prepare MDC (bug 357183)
			JobLogHelper.setupMdc(jobContext);

			// create job instance
			return createJobInstance(info, jobContext);
		} catch (final LinkageError e) {
			handleCreateJobError(queue, message, info, jobContext, e);
			return null;
		} catch (final Exception e) {
			handleCreateJobError(queue, message, info, jobContext, e);
			return null;
		} finally {
			// clean MDC (will be prepared again once the job runs)
			JobLogHelper.clearMdc();
		}
	}

	private Job createJobInstance(final JobInfo info, final IJobContext jobContext) throws Exception {
		final JobProvider provider = JobsActivator.getInstance().getJobProviderRegistry().getProvider(info.getJobTypeId());
		if (null == provider)
			throw new IllegalStateException(String.format("Job type %s not available!", info.getJobTypeId()));

		final Job job = provider.createJob(info.getJobTypeId(), jobContext);
		if (null == job)
			throw new IllegalStateException(String.format("Provider %s did not create job of type %s!", provider.toString(), info.getJobTypeId()));
		return job;
	}

	private void discardJobMessage(final IQueue queue, final IMessage message) {
		try {
			if (!queue.deleteMessage(message)) {
				// log warning
				LOG.warn("Unable to remove job message {}. Job might be processed again.", message);
			}
		} catch (final NoSuchElementException ignored) {
			// not too bad
		} catch (final Exception e) {
			LOG.error("Error removing job message {}: {}", new Object[] { message, ExceptionUtils.getRootCauseMessage(e), e });
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

	private void handleCreateJobError(final IQueue queue, final IMessage message, final JobInfo info, final JobContext jobContext, final Throwable e) {
		// log error
		LOG.error("Error creating job {}: {}. Job won't be executed.", new Object[] { message, ExceptionUtils.getRootCauseMessage(e), e });

		// discard message
		discardJobMessage(queue, message);

		// set job status
		try {
			final JobManagerImpl jobManager = (JobManagerImpl) jobContext.getContext().get(IJobManager.class);
			jobManager.setResult(info.getJobId(), jobContext.getParameter(), new Status(IStatus.ERROR, JobsActivator.SYMBOLIC_NAME, String.format("Error creating job: %s", e.getMessage()), e), System.currentTimeMillis());
		} catch (final Exception jobManagerException) {
			LOG.error("Error updating job result for job {}: {}", new Object[] { info.getJobId(), ExceptionUtils.getRootCauseMessage(jobManagerException) });
		}
	}

	private JobInfo parseJobInfo(final IQueue queue, final IMessage message) {
		try {
			return JobInfo.parse(message);
		} catch (final IOException e) {
			// log error
			LOG.error("Invalid job info in message {}: {}", new Object[] { message, ExceptionUtils.getRootCauseMessage(e), e });

			// discard message
			discardJobMessage(queue, message);
		}
		return null;
	}

	/**
	 * Fetches the next job from a queue and schedules it for execution.
	 * 
	 * @return <code>true</code> if additional jobs might be available in the
	 *         queue, <code>false</code> if the queue is empty
	 */
	private boolean processNextJobFromQueue() {
		final IQueue queue = getDefaultQueue(); // TODO how to get messages from other queues?
		if (queue == null)
			throw new IllegalStateException("No queue available for reading scheduled jobs to execute. Please check engine configuration.");

		// don't process any jobs if we are over the limit
		if (scheduledJobsCount > maxConcurrentJobs) {
			// we return false here in order to allow this node to breath a bit
			if (JobsDebug.workerEngine) {
				LOG.debug("There are currently {} jobs scheduled. Won't schedule more at this time.", scheduledJobsCount);
			}
			metrics.getCapacity().channelDenied();
			metrics.setStatus("EXHAUSTED", "capacity limit reached");
			return false;
		}

		metrics.setStatus("PROCESSING", "processing next job from queue");

		// set receive timeout
		final Map<String, Object> requestProperties = new HashMap<String, Object>(1);
		requestProperties.put(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT, getReceiveTimeout());

		// receive message
		final List<IMessage> messages = queue.receiveMessages(1, requestProperties);
		if (messages.isEmpty())
			// no more messages, abort
			return false;
		final IMessage message = messages.get(0);

		// read job info
		final JobInfo info = parseJobInfo(queue, message);
		if (null == info)
			// continue with next message
			return true;

		// create context
		final JobContext jobContext = createContext(info);

		// create job
		final Job job = createJob(queue, message, info, jobContext);
		if (null == job)
			// continue with next message
			return true;

		// add state synchronizer and finish listener
		final JobStateSynchronizer stateSynchronizer = new JobStateSynchronizer(job, jobContext);
		job.addJobChangeListener(stateSynchronizer);
		job.addJobChangeListener(jobFinishedListener);

		// mark the job active before removing from queue (bug 360402)
		// (this will ensure that it's set active by JobStateSynchronizer)
		stateSynchronizer.setJobActive();

		// delete from queue and schedule if successful
		// (note, we intentionally only catch NoSuchElementException here)
		try {
			if (!queue.deleteMessage(message)) {
				// abort job
				stateSynchronizer.setJobInactive();

				// someone else might already processed it
				// just continue with next available message
				return true;
			}
		} catch (final NoSuchElementException e) {
			// abort job
			stateSynchronizer.setJobInactive();

			// not too bad
			// someone else might already processed it
			// just continue with next available message
			return true;
		}

		// only at this point we allowed the job to proceed
		job.schedule();

		// increment count
		scheduledJobsCount++;

		// done, but return "true" to indicate that the queue might still contain jobs
		return true;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			// check if suspended
			if (getWorkerEnginePreferences().getBoolean(PREF_KEY_SUSPENDED, false)) {
				if (JobsDebug.workerEngine) {
					LOG.debug("Worker engine is suspended.");
				}
				metrics.setStatus("SUSPENDED", "suspended in preferences");
				return Status.CANCEL_STATUS;
			}

			// update metric
			metrics.getCapacity().setChannelsCapacity(maxConcurrentJobs);

			// process next job from queue
			final boolean moreJobsAvailable = processNextJobFromQueue();

			// reset sleep time
			// even if there are still jobs in the queue we don't just go on processing them
			// instead we wait a few seconds to let other worker engines pick up jobs from the queue
			// otherwise this node might just over-schedule itself
			// TODO: there is room for improving the distribution of jobs among worker engines with a better algorithm
			engineSleepTime = moreJobsAvailable ? nonIdleSleepTime : idleSleepTime;

			// done
			if (!monitor.isCanceled()) {
				metrics.setStatus("SLEEPING", "expected sleep time: " + engineSleepTime);
				return Status.OK_STATUS;
			} else {
				metrics.setStatus("CANCELED", "monitor is canceled");
				return Status.CANCEL_STATUS;
			}
		} catch (final Exception e) {
			// implement a back-off sleeping time (max 5 min)
			engineSleepTime = Math.min(engineSleepTime * 2, DEFAULT_MAX_SLEEP_TIME);

			// log error
			LOG.error("Unable to process queued jobs. Please verify the system is setup properly. {}", new Object[] { ExceptionUtils.getRootCauseMessage(e), e });

			// indicate error through cancel status
			metrics.setStatus("ERROR", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
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
