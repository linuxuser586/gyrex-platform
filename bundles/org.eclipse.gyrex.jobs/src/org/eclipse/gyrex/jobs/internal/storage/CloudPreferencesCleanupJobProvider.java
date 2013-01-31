/*******************************************************************************
 * Copyright (c) 2013 <enter-company-name-here> and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.storage;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.IJobContext;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class CloudPreferencesCleanupJobProvider extends JobProvider {

	public static final String CLEANUP_JOB_TYPE = "org.eclipse.gyrex.jobs.system.cleanup.cloud";
	public static final String CLEANUP_JOB_ID = "org.eclipse.gyrex.jobs.system.cleanup.cloud";

	private static final Logger LOG = LoggerFactory.getLogger(CloudPreferencesCleanupJobProvider.class);

	private static AtomicLong lastCleanup = new AtomicLong();

	static void triggerCleanUp() {
		final long last = lastCleanup.get();
		if ((System.currentTimeMillis() - last) > TimeUnit.HOURS.toMillis(3)) {
			if (lastCleanup.compareAndSet(last, System.currentTimeMillis())) {
				try {
					final IRuntimeContext context = JobsActivator.getInstance().getService(IRuntimeContextRegistry.class).get(Path.ROOT);
					if (context == null)
						throw new IllegalStateException("Root context not available!");
					final IJobManager jobManager = context.get(IJobManager.class);
					if (jobManager == null)
						throw new IllegalStateException("Job manager not available!");
					final IJob cleanupJob = jobManager.getJob(CLEANUP_JOB_ID);
					if ((cleanupJob == null) || (cleanupJob.getState() == JobState.NONE)) {
						jobManager.queueJob(CLEANUP_JOB_TYPE, CLEANUP_JOB_ID, null, null, "automatic cleanup triggered by " + JobsActivator.getInstance().getService(INodeEnvironment.class).getNodeId());
					}
				} catch (final Exception e) {
					LOG.warn("Unable to queue job data cloud cleanup. Staring locally. {}", e.getMessage(), e);
					new CloudPreferencesCleanupJob().schedule();
				}
			}
		}
	}

	public CloudPreferencesCleanupJobProvider() {
		super(Arrays.asList(CLEANUP_JOB_TYPE));
	}

	@Override
	public Job createJob(final String typeId, final IJobContext context) throws Exception {
		if (!CLEANUP_JOB_TYPE.equals(typeId))
			return null;

		final CloudPreferencesCleanupJob cleanupJob = new CloudPreferencesCleanupJob();
		final String maxDaysSinceLastRunParam = context.getParameter().get("maxDaysSinceLastRun");
		if (StringUtils.isNotBlank(maxDaysSinceLastRunParam)) {
			final int maxDaysSinceLastRun = NumberUtils.toInt(maxDaysSinceLastRunParam, 0);
			if (maxDaysSinceLastRun <= 0)
				throw new IllegalArgumentException("Invalid value for parameter 'maxDaysSinceLastRun'. Please set it to a positiv integer greater than zero!");
			cleanupJob.setMaxDaysSinceLastRun(maxDaysSinceLastRun);
		}

		return cleanupJob;
	}

}
