/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.manager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hung processing detection for jobs.
 * <p>
 * This class uses ephemeral nodes in ZooKeeper in order to detect hung Jobs due
 * to dying worker engines or broken jobs. Ephemeral nodes have that benefit
 * that they are maintained and destroyed automatically by ZooKeeper if a worker
 * engine dies.
 * </p>
 */
public class JobHungDetectionHelper {

	private static final Logger LOG = LoggerFactory.getLogger(JobHungDetectionHelper.class);
	private static final IPath ACTIVE_JOBS = IZooKeeperLayout.PATH_JOBS_ROOT.append("active");

	/**
	 * Returns the list of active jobs.
	 * 
	 * @param watcher
	 *            if not <code>null</code> and the call is sucessfull the watch
	 *            will be registered with ZooKeeper
	 * @return the timestamp (or -1 if the job is not active)
	 * @throws IllegalStateException
	 */
	public static List<String> getActiveJobs(final ZooKeeperMonitor watcher) throws IllegalStateException {
		try {
			return ZooKeeperGate.get().readChildrenNames(ACTIVE_JOBS, watcher, null);
		} catch (final NoNodeException e) {
			try {
				ZooKeeperGate.get().exists(ACTIVE_JOBS, watcher);
			} catch (final Exception e2) {
				throw new IllegalStateException("Unable to read job run info!", e2);
			}
			return Collections.emptyList();
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to read job run info!", e);
		}
	}

	/**
	 * @return the number of active jobs
	 * @throws IllegalStateException
	 */
	public static int getNumberOfActiveJobs() throws IllegalStateException {
		try {
			return ZooKeeperGate.get().readChildrenNames(ACTIVE_JOBS, null).size();
		} catch (final NoNodeException e) {
			return 0;
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to read job run info!", e);
		}
	}

	/**
	 * Returns the processing node id of the active job.
	 * 
	 * @param jobStorageKey
	 * @return the timestamp (or -1 if the job is not active)
	 * @throws IllegalStateException
	 */
	public static String getProcessingNodeId(final String jobStorageKey, final Stat stat) throws IllegalStateException {
		try {
			return ZooKeeperGate.get().readRecord(ACTIVE_JOBS.append(jobStorageKey), (String) null, stat);
		} catch (final NoNodeException e) {
			// good
			return null;
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to read job run info!", e);
		}
	}

	/**
	 * Indicates if the specified job is active.
	 * 
	 * @param jobStorageKey
	 * @return <code>true</code> if active, <code>false</code> otherwise
	 * @throws IllegalStateException
	 */
	public static boolean isActive(final String jobStorageKey) throws IllegalStateException {
		try {
			return ZooKeeperGate.get().exists(ACTIVE_JOBS.append(jobStorageKey));
		} catch (final NoNodeException e) {
			// good
			return false;
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to read job run info!", e);
		}
	}

	/**
	 * This helper detects if a job is stuck.
	 * 
	 * @param jobStorageKey
	 *            the job storage key
	 * @param job
	 *            the job to check
	 * @param logLongRunning
	 *            if warning and/or errors should be logged for jobs that have
	 *            been active for too long
	 * @return
	 */
	public static boolean isStuck(final String jobStorageKey, final JobImpl job, final boolean logLongRunning) {
		// jobs in state NONE are never stuck
		if (job.getState() == JobState.NONE)
			return false;

		// check that the job has been queued
		if (job.getLastQueued() < 0) {
			LOG.debug("Job {} (state {}) has never been queued! Assuming stuck.", job.getId(), job.getState());
			return true;
		}

		// get activity time
		final long timeActive = System.currentTimeMillis() - Math.max(job.getLastQueued(), job.getLastStart());

		// don't even care about jobs that are less than a minute active
		if (timeActive < 60000L) {
			LOG.debug("Job {} was active less then a minute ago! Assuming not stuck.", job.getId(), job.getState());
			return false;
		}

		// check if job is active in the system
		if (JobHungDetectionHelper.isActive(jobStorageKey)) {
			// job is still active in the system
			// verify that it's also marked active
			if (!job.isActive()) {
				LOG.debug("Job {} marked active in the system but not set active! Assuming stuck.", job.getId(), job.getState());
				return true;
			}

			// log a message if it has been active too long
			if (logLongRunning) {
				if (logLongRunning && (TimeUnit.MILLISECONDS.toHours(timeActive) > 12L)) {
					// it has been active for more than two hours
					LOG.error("Job {} has been active in the system for {} hours now. Please investigate.", job.getId(), TimeUnit.MILLISECONDS.toHours(timeActive));
				} else if (TimeUnit.MILLISECONDS.toHours(timeActive) > 2L) {
					// it has been active for more than two hours
					LOG.warn("Job {} has been active in the system for {} hours now.", job.getId(), TimeUnit.MILLISECONDS.toHours(timeActive));
				}
			}

			// active jobs are considered never stuck
			return false;
		}

		// check if the job is set active
		if (job.isActive()) {
			LOG.debug("Job {} (state {}) set active in the store but not marked active in the system! Assuming stuck.", job.getId(), job.getState());
			return true;
		}

		// at this point the job is not marked active but its state is not NONE (as checked above)
		// this is only ok for WAITING or ABORTING jobs still in a queue
		if ((job.getState() == JobState.WAITING) || (job.getState() == JobState.ABORTING)) {
			// TODO: we need to check if it's still in the QUEUE
			// for now we assume yes if it was queued less than 120 minutes ago
			final long timeInQueue = System.currentTimeMillis() - job.getLastQueued();
			if (TimeUnit.MILLISECONDS.toMinutes(timeInQueue) < 120L) {
				if (timeInQueue < 60000L) {
					LOG.debug("Job {} is {} (queued less than a minute ago). Assuming not stuck!", job.getId(), job.getState());
				} else {
					LOG.debug("Job {} is {} (queued {} minutes ago). Assuming not stuck!", new Object[] { job.getId(), job.getState(), TimeUnit.MILLISECONDS.toMinutes(timeInQueue) });
				}
				return false;
			}

			LOG.debug("Job {} (state {}) was queue {} minutes ago. Assuming stuck.", new Object[] { job.getId(), job.getState(), TimeUnit.MILLISECONDS.toMinutes(timeInQueue) });
			return true;
		}

		// at this point any job is considered stuck
		LOG.debug("Job {} is neither set active nor marked active in the system but its state is {}. Assuming stuck.", job.getId(), job.getState());
		return true;
	}

	private static String myNodeId() {
		return JobsActivator.getInstance().getService(INodeEnvironment.class).getNodeId();
	}

	/**
	 * Sets the job active on <em>this</em> node.
	 * <p>
	 * Has no effect if the job is already active.
	 * </p>
	 * 
	 * @param jobStorageKey
	 * @throws IllegalStateException
	 */
	public static void setActive(final String jobStorageKey) throws IllegalStateException {
		final String myNodeId = myNodeId();
		final IPath path = ACTIVE_JOBS.append(jobStorageKey);
		int retries = 3;
		while (true) {
			try {
				// read processing node id
				final Stat stat = new Stat();
				final String processingNodeId = ZooKeeperGate.get().readRecord(path, (String) null, stat);
				if (StringUtils.isNotBlank(processingNodeId) && !StringUtils.equalsIgnoreCase(myNodeId, processingNodeId)) {
					LOG.warn("Job {} active on node {} but will now be moved to this node!", jobStorageKey, processingNodeId);
					ZooKeeperGate.get().writeRecord(path, myNodeId, stat.getVersion());
					return;
				} else if (StringUtils.equalsIgnoreCase(myNodeId, processingNodeId))
					// perfect
					return;

				if (JobsDebug.debug) {
					LOG.debug("Creating ephemeral node for job {}...", jobStorageKey);
				}
				ZooKeeperGate.get().createPath(path, CreateMode.EPHEMERAL, myNodeId);
			} catch (final NodeExistsException e) {
				// retry
				if (--retries == 0)
					throw new IllegalStateException("Unable to activate job!", e);
				continue;
			} catch (final BadVersionException e) {
				// retry
				if (--retries == 0)
					throw new IllegalStateException("Unable to activate job!", e);
				continue;
			} catch (final Exception e) {
				throw new IllegalStateException("Unable to set job state running!", e);
			}
		}
	}

	/**
	 * Sets the job to inactive, i.e. no longer actively processed.
	 * 
	 * @param jobStorageKey
	 * @throws IllegalStateException
	 */
	public static void setInactive(final String jobStorageKey) throws IllegalStateException {
		final IPath path = ACTIVE_JOBS.append(jobStorageKey);
		int retries = 3;
		while (true) {
			try {
				// read processing node id
				final Stat stat = new Stat();
				final String processingNodeId = ZooKeeperGate.get().readRecord(path, (String) null, stat);
				if (StringUtils.isNotBlank(processingNodeId) && !StringUtils.equalsIgnoreCase(myNodeId(), processingNodeId)) {
					LOG.warn("Job {} active on different node (node {})!", jobStorageKey, processingNodeId);
					return;
				}

				if (JobsDebug.debug) {
					LOG.debug("Removing ephemeral node for job {}.", jobStorageKey);
				}
				ZooKeeperGate.get().deletePath(path, stat.getVersion());
			} catch (final NoNodeException e) {
				// good
				return;
			} catch (final BadVersionException e) {
				// retry
				if (--retries == 0)
					throw new IllegalStateException("Unable to deactivate job!", e);
				continue;
			} catch (final Exception e) {
				throw new IllegalStateException("Unable to set job state stopped!", e);
			}
		}
	}

}
