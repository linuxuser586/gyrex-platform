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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.scheduler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.manager.JobImpl;
import org.eclipse.gyrex.jobs.internal.manager.JobManagerImpl;
import org.eclipse.gyrex.jobs.internal.worker.JobLogHelper;
import org.eclipse.gyrex.jobs.manager.IJobManager;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.apache.commons.lang.StringUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SchedulingJob implements Job {

	private static final Logger LOG = LoggerFactory.getLogger(SchedulingJob.class);

	static final String INTERNAL_PROP_PREFIX = "gyrex.job.";
	public static final String PROP_JOB_ID = INTERNAL_PROP_PREFIX + "id";
	public static final String PROP_JOB_TYPE_ID = INTERNAL_PROP_PREFIX + "type";
	public static final String PROP_JOB_CONTEXT_PATH = INTERNAL_PROP_PREFIX + "contextPath";
	public static final String PROP_SCHEDULE_ID = INTERNAL_PROP_PREFIX + "scheduleId";
	public static final String PROP_SCHEDULE_ENTRY_ID = INTERNAL_PROP_PREFIX + "scheduleEntryId";
	public static final String PROP_ENABLED = INTERNAL_PROP_PREFIX + "enabled";

	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final JobDataMap dataMap = context.getMergedJobDataMap();
		final String jobId = dataMap.getString(PROP_JOB_ID);
		final String jobTypeId = dataMap.getString(PROP_JOB_TYPE_ID);
		final String jobContextPath = dataMap.getString(PROP_JOB_CONTEXT_PATH);
		final String scheduleId = dataMap.getString(PROP_SCHEDULE_ID);
		final String scheduleEntryId = dataMap.getString(PROP_SCHEDULE_ENTRY_ID);
		final Boolean enabled = dataMap.containsKey(PROP_ENABLED) ? dataMap.getBooleanValue(PROP_ENABLED) : null;

		if ((null != enabled) && !enabled) {
			LOG.debug(String.format("Skipping execution of job '%s' - entry is not enabled", jobId));
			return;
		}

		try {
			// parse path
			final IPath contextPath = new Path(jobContextPath);

			// setup MDC
			JobLogHelper.setupMdc(jobId, contextPath);

			// populate map
			final Map<String, String> parameter = new HashMap<String, String>();
			for (final Object keyObj : dataMap.keySet()) {
				if (!(keyObj instanceof String)) {
					continue;
				}
				final String key = (String) keyObj;
				if (!StringUtils.startsWith(key, INTERNAL_PROP_PREFIX)) {
					final Object value = dataMap.get(key);
					if (value instanceof String) {
						parameter.put(key, (String) value);
					}
				}
			}

			// get context
			final IRuntimeContext runtimeContext = JobsActivator.getInstance().getService(IRuntimeContextRegistry.class).get(contextPath);
			if (null == runtimeContext) {
				LOG.error("Unable to find context (using path {}) for job {}.", jobContextPath, jobId);
				return;
			}

			// get job manager
			final IJobManager jobManager = runtimeContext.get(IJobManager.class);
			if (!(jobManager instanceof JobManagerImpl)) {
				LOG.error("Invalid job manager ({}). Please verify the system is setup properly.", jobManager);
				return;
			}
			final JobManagerImpl jobManagerImpl = (JobManagerImpl) jobManager;

			// create job if necessary
			JobImpl job = jobManagerImpl.getJob(jobId);
			if (job == null) {
				job = jobManagerImpl.createJob(jobTypeId, jobId, parameter);
			}

			// check that job state is NONE (and it's not stuck)
			if ((job.getState() != JobState.NONE) && !jobManagerImpl.isStuck(job)) {
				LOG.warn("Job {} (type {}) cannot be queued because it is already active in the system (current state {}).", new Object[] { job.getId(), job.getTypeId(), job.getState() });
				return;
			}

			// check that queue exists
			final IQueueService queueService = JobsActivator.getInstance().getQueueService();
			IQueue queue = queueService.getQueue(IJobManager.DEFAULT_QUEUE, null);
			if (queue == null) {
				queue = queueService.createQueue(IJobManager.DEFAULT_QUEUE, null);
			}

			// queue job
			jobManager.queueJob(jobId, parameter, queue.getId(), String.format("Schedule '%s' entry '%s'.", scheduleId, scheduleEntryId));
		} catch (final Exception e) {
			throw new JobExecutionException(String.format("Error queuing job '%s'. %s", jobId, e.getMessage()), e);
		} finally {
			// clear MDC
			JobLogHelper.clearMdc();
		}
	}
}