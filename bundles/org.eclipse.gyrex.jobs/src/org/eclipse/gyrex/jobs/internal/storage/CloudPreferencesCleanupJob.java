/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht, Mike Tschierschke - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.storage;

import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.internal.manager.JobHungDetectionHelper;
import org.eclipse.gyrex.jobs.internal.manager.JobImpl;
import org.eclipse.gyrex.jobs.internal.manager.JobManagerImpl;
import org.eclipse.gyrex.jobs.internal.util.ContextHashUtil;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clean-up of old job entries
 */
public final class CloudPreferencesCleanupJob extends Job {

	private static final Logger LOG = LoggerFactory.getLogger(CloudPreferencesCleanupJob.class);
	private long maxAge;

	public CloudPreferencesCleanupJob() {
		super("Gyrex Job Cloud Garbage Collector");
		setSystem(true);
		setPriority(LONG);

		// initialize max age
		// (backwards compatibility; must be done in job)
		setMaxDaysSinceLastRun(Integer.getInteger("gyrex.jobs.cleanup.maxDaysSinceLastRun", 14));
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		final IEclipsePreferences jobsNode = CloudPreferncesJobStorage.getJobsNode();
		final IEclipsePreferences jobsHistoryNode = CloudPreferncesJobHistoryStorage.getJobsHistoryNode();
		try {
			// ensure the preference tree is current (bug 360402)
			LOG.info("Refreshing job definitions...");
			jobsNode.sync();

			LOG.info("Cleaning up old job definitions older than {} days...", TimeUnit.MILLISECONDS.toDays(maxAge));
			final long now = System.currentTimeMillis();
			final String[] childrenNames = jobsNode.childrenNames();
			for (final String internalId : childrenNames) {
				final String externalId = ContextHashUtil.getExternalId(internalId);
				JobImpl job = CloudPreferncesJobStorage.readJob(externalId, jobsNode.node(internalId));

				// acquire lock (see bug 363423)
				final IExclusiveLock lock = JobManagerImpl.acquireLock(job);
				try {
					// re-read job in lock
					job = CloudPreferncesJobStorage.readJob(externalId, jobsNode.node(internalId));

					// fix hung jobs
					if (JobHungDetectionHelper.isStuck(internalId, job, true)) {
						LOG.info("Resetting job {} stuck in state {} (queued {} minutes and started {} minutes ago).", new Object[] { job.getId(), job.getState(), TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - job.getLastQueued()), TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - job.getLastStart()) });

						// set inactive
						final Preferences jobNode = jobsNode.node(internalId);
						jobNode.put(CloudPreferncesJobStorage.PROPERTY_STATUS, JobState.NONE.name());
						jobNode.remove(CloudPreferncesJobStorage.PROPERTY_ACTIVE);
						jobNode.flush();
						job = CloudPreferncesJobStorage.readJob(externalId, jobsNode.node(internalId));
					}

					// remove only jobs in state NONE
					if (job.getState() != JobState.NONE) {
						if (JobsDebug.cleanup) {
							LOG.debug("Skipping active job {}...", job.getId());
						}
						continue;
					}

					// remove only jobs not older then maxAge
					final long jobAge = now - Math.max(job.getLastResultTimestamp(), Math.max(job.getLastQueued(), job.getLastStart()));
					if (jobAge < maxAge) {
						if (JobsDebug.cleanup) {
							LOG.debug("Skipping too young job {} (age {} days)...", job.getId(), TimeUnit.MILLISECONDS.toDays(jobAge));
						}
						continue;
					}

					LOG.info("Removing job {}.", externalId);
					if (jobsHistoryNode.nodeExists(internalId)) {
						jobsHistoryNode.node(internalId).removeNode();
						jobsHistoryNode.flush();
					}
					if (jobsNode.nodeExists(internalId)) {
						jobsNode.node(internalId).removeNode();
						jobsNode.flush();
					}
				} finally {
					lock.release();
				}
			}
			LOG.info("Finished clean-up of old job definitions.");
		} catch (final Exception e) {
			LOG.warn("Unable to clean-up old job definitions. {}", ExceptionUtils.getRootCauseMessage(e));
			return new Status(IStatus.ERROR, JobsActivator.SYMBOLIC_NAME, String.format("Unable to clean-up old job definitions. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
		return Status.OK_STATUS;
	}

	public void setMaxDaysSinceLastRun(final int maxDaysSinceLastRun) {
		if (maxDaysSinceLastRun > 0) {
			maxAge = TimeUnit.DAYS.toMillis(maxDaysSinceLastRun);
		} else {
			maxAge = Long.MAX_VALUE;
		}
	}
}