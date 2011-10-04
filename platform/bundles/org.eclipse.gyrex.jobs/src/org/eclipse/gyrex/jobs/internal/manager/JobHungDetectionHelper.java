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
package org.eclipse.gyrex.jobs.internal.manager;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;

import org.eclipse.core.runtime.IPath;

import org.apache.zookeeper.CreateMode;
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
@SuppressWarnings("restriction")
class JobHungDetectionHelper {

	private static final Logger LOG = LoggerFactory.getLogger(JobHungDetectionHelper.class);

	private static final IPath ACTIVE_JOBS = IZooKeeperLayout.PATH_GYREX_ROOT.append("jobs/active");

	/**
	 * Returns the timestamp since when a job is active.
	 * 
	 * @param jobStorageKey
	 * @return the timestamp (or -1 if the job is not active)
	 * @throws IllegalStateException
	 */
	public static long getActiveSince(final String jobStorageKey) throws IllegalStateException {
		try {
			final Stat stat = new Stat();
			ZooKeeperGate.get().readRecord(ACTIVE_JOBS.append(jobStorageKey), stat);
			return stat.getCtime();
		} catch (final NoNodeException e) {
			// good
			return -1;
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
	 * Sets the job active on <em>this</em> node.
	 * 
	 * @param jobStorageKey
	 * @throws IllegalStateException
	 */
	public static void setActive(final String jobStorageKey) throws IllegalStateException {
		try {
			if (JobsDebug.debug) {
				LOG.debug("Creating ephemeral node for job {}.", jobStorageKey);
			}
			ZooKeeperGate.get().createPath(ACTIVE_JOBS.append(jobStorageKey), CreateMode.EPHEMERAL, JobsActivator.getInstance().getService(INodeEnvironment.class).getNodeId());
		} catch (final NodeExistsException e) {
			// ignore for now (we might need to better handle this case)
			//throw new IllegalStateException(String.format("Job %s already active!", jobStorageKey), e);
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to set job state running!", e);
		}
	}

	/**
	 * Sets the job to inactive, i.e. no longer actively processed.
	 * 
	 * @param jobStorageKey
	 * @throws IllegalStateException
	 */
	public static void setInactive(final String jobStorageKey) throws IllegalStateException {
		try {
			if (JobsDebug.debug) {
				LOG.debug("Removing ephemeral node for job {}.", jobStorageKey);
			}
			ZooKeeperGate.get().deletePath(ACTIVE_JOBS.append(jobStorageKey));
		} catch (final NoNodeException e) {
			// good
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to set job state stopped!", e);
		}
	}
}
