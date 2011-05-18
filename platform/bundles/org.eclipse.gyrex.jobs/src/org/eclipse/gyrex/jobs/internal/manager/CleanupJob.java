/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.manager;

import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.jobs.JobState;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clean-up of old job entries
 */
final class CleanupJob extends Job {

	static class MutexRule implements ISchedulingRule {

		private final Object object;

		public MutexRule(final Object object) {
			this.object = object;
		}

		public boolean contains(final ISchedulingRule rule) {
			return rule == this;
		}

		public boolean isConflicting(final ISchedulingRule rule) {
			if (rule instanceof CleanupJob.MutexRule) {
				return object.equals(((CleanupJob.MutexRule) rule).object);
			}
			return false;
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(CleanupJob.class);

	private static final long MAX_JOB_AGE = TimeUnit.DAYS.toMillis(2);

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public CleanupJob() {
		super("Gyrex Job API Garbage Collector");
		setSystem(true);
		setPriority(LONG);
		setRule(new MutexRule(CleanupJob.class));
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		final IEclipsePreferences jobsNode = JobManagerImpl.getJobsNode();
		try {
			LOG.info("Cleaning up of old job definitions.");
			final long now = System.currentTimeMillis();
			final String[] childrenNames = jobsNode.childrenNames();
			for (final String internalId : childrenNames) {
				final String externalId = JobManagerImpl.getExternalId(internalId);
				final JobImpl job = JobManagerImpl.readJob(externalId, jobsNode.node(internalId));
				if ((job.getState() != JobState.NONE) || ((now - job.getLastResultTimestamp()) < MAX_JOB_AGE)) {
					continue;
				}

				// note, we do not use the exclusive lock here
				// thus, the operation might be unsafe
				// TODO: investigate, add tests and fix

				LOG.info("Removing job {}.", externalId);
				if (jobsNode.nodeExists(internalId)) {
					jobsNode.node(internalId).removeNode();
					jobsNode.flush();
				}

				final IEclipsePreferences statesNode = JobManagerImpl.getStatesNode();
				for (final JobState state : JobState.values()) {
					final Preferences stateNode = statesNode.node(state.name());
					if (null != stateNode.get(internalId, null)) {
						stateNode.remove(internalId);
						stateNode.flush();
					}
				}
			}
			LOG.info("Finished clean-up of old job definitions.");
		} catch (final BackingStoreException e) {
			LOG.warn("Unable to clean-up old job definitions. {}", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}
}