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
package org.eclipse.gyrex.jobs.internal.scheduler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.manager.IJobManager;

import org.eclipse.core.runtime.Path;

import org.apache.commons.lang.StringUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 */
public class SchedulingJob implements Job {

	static final String INTERNAL_PROP_PREFIX = "gyrex.job.";

	public static final String PROP_JOB_ID = INTERNAL_PROP_PREFIX + "id";

	public static final String PROP_JOB_TYPE_ID = INTERNAL_PROP_PREFIX + "type";
	public static final String PROP_JOB_CONTEXT_PATH = INTERNAL_PROP_PREFIX + "contextPath";

	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final JobDataMap dataMap = context.getMergedJobDataMap();
		final String jobId = dataMap.getString(PROP_JOB_ID);
		final String jobTypeId = dataMap.getString(PROP_JOB_TYPE_ID);
		final String jobContextPath = dataMap.getString(PROP_JOB_CONTEXT_PATH);
		try {

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

			final IRuntimeContext runtimeContext = JobsActivator.getInstance().getService(IRuntimeContextRegistry.class).get(new Path(jobContextPath));
			if (null == runtimeContext) {
				throw new IllegalStateException(String.format("Context '%s' not found!", jobContextPath));
			}

			final IJobManager jobManager = runtimeContext.get(IJobManager.class);

			// create job if necessary
			if (jobManager.getJob(jobId) == null) {
				jobManager.createJob(jobTypeId, jobId, parameter);
			}

			final IQueueService queueService = JobsActivator.getInstance().getQueueService();

			final IQueue queue = queueService.getQueue(IJobManager.DEFAULT_QUEUE, null);
			if (queue == null) {
				queueService.createQueue(IJobManager.DEFAULT_QUEUE, null);
			}

			jobManager.queueJob(jobId, IJobManager.DEFAULT_QUEUE);
		} catch (final Exception e) {
			throw new JobExecutionException(String.format("Error queuing job '%s'. %s", jobId, e.getMessage()), e);
		}
	}
}