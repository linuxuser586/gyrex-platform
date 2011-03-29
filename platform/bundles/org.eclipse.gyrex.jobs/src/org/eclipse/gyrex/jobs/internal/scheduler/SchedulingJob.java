/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.scheduler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.cloud.services.queue.DuplicateQueueException;
import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.worker.JobInfo;
import org.eclipse.gyrex.jobs.internal.worker.WorkerEngine;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
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

	public static final String PROP_JOB_PROVIDER_ID = "gyrex.job.providerId";
	private static final Logger LOG = LoggerFactory.getLogger(SchedulingJob.class);

	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final JobDataMap dataMap = context.getMergedJobDataMap();
		final String providerId = dataMap.getString(PROP_JOB_PROVIDER_ID);

		final Map<String, String> parameter = new HashMap<String, String>();
		for (final Object keyObj : dataMap.keySet()) {
			if (!(keyObj instanceof String)) {
				continue;
			}
			final String key = (String) keyObj;
			if (!StringUtils.equals(PROP_JOB_PROVIDER_ID, key)) {
				final Object value = dataMap.get(key);
				if (value instanceof String) {
					parameter.put(key, (String) value);
				}
			}
		}

		try {
			final JobInfo jobInfo = new JobInfo(providerId, parameter);
			getQueue().sendMessage(JobInfo.asMessage(jobInfo));
		} catch (final Exception e) {
			if (JobsDebug.debug) {
				LOG.debug("Exceptions scheduling job {}.", new Object[] { context.getJobDetail().getFullName(), e });
			}
			LOG.error("Unable to schedule job {} ({}). {}", new Object[] { context.getJobDetail().getFullName(), providerId, ExceptionUtils.getRootCauseMessage(e) });
		}

	}

	/**
	 * Returns the queue for scheduling jobs.
	 * 
	 * @return
	 */
	protected IQueue getQueue() {
		final IQueueService queueService = JobsActivator.getInstance().getQueueService();
		IQueue queue = queueService.getQueue(WorkerEngine.DEFAULT_QUEUE, null);
		if (null != queue) {
			return queue;
		}

		// ensure the queue exists
		// TODO: implement queue handling (should like happen by operator)
		try {
			queue = queueService.createQueue(WorkerEngine.DEFAULT_QUEUE, null);
		} catch (final DuplicateQueueException e) {
			queue = queueService.getQueue(WorkerEngine.DEFAULT_QUEUE, null);
		}

		if (null == queue) {
			throw new IllegalStateException("no queue available for submitting jobs");
		}

		return queue;
	}

}
