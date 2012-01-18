/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.zk;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Interface with constants to centralize ZooKeeper layout definitions.
 * <p>
 * Note, the Gyrex ZooKeeper layout is not API. Although public readable (and
 * modifiable) by anyone with appropriate permissions to access a ZooKeeper
 * ensemble, the layout must not be changed directly.
 * </p>
 * 
 * <pre>
 *   /gyrex     ... root
 *     /nodes   ... information about cloud membership
 *     /prefs   ... platform preferences
 * </pre>
 */
public interface IZooKeeperLayout {

	/** root path for all Gyrex information stored in ZooKeeper */
	public static final IPath PATH_GYREX_ROOT = new Path("gyrex").makeAbsolute();

	/** root path for cloud membership information stored in ZooKeeper */
	public static final IPath PATH_NODE_ROOT = PATH_GYREX_ROOT.append("nodes").makeAbsolute();

	/** path with ephemeral records for each node */
	public static final IPath PATH_NODES_ALL = PATH_NODE_ROOT.append("all").makeAbsolute();

	/** path with ephemeral records for each online node (approved only) */
	public static final IPath PATH_NODES_ONLINE = PATH_NODE_ROOT.append("online").makeAbsolute();

	/**
	 * path with persistent records for each node which is an approved cloud
	 * member
	 */
	public static final IPath PATH_NODES_APPROVED = PATH_NODE_ROOT.append("approved").makeAbsolute();

	/**
	 * path with persistent records for each node awaiting cloud membership
	 * approval
	 */
	public static final IPath PATH_NODES_PENDING = PATH_NODE_ROOT.append("pending").makeAbsolute();

	/** path with metric records for each node */
	public static final IPath PATH_NODES_METRICS = PATH_NODE_ROOT.append("metrics").makeAbsolute();

	/** path with node state for each node */
	public static final IPath PATH_NODES_STATE_BY_NODE_ID = PATH_NODE_ROOT.append("state/byNodeId").makeAbsolute();

	/** path with node state for each service pid */
	public static final IPath PATH_NODES_STATE_BY_SERVICE_PID = PATH_NODE_ROOT.append("state/byServicePid").makeAbsolute();

	/** root path for platform preferences stored in ZooKeeper */
	public static final IPath PATH_PREFERENCES_ROOT = PATH_GYREX_ROOT.append("prefs").makeAbsolute();

	/** root path for queues stored in ZooKeeper */
	public static final IPath PATH_QUEUES_ROOT = PATH_GYREX_ROOT.append("queues").makeAbsolute();

	/** root path for locks stored in ZooKeeper */
	public static final IPath PATH_LOCKS_ROOT = PATH_GYREX_ROOT.append("locks").makeAbsolute();

	/** root path for locks stored in ZooKeeper */
	public static final IPath PATH_LOCKS_EXCLUSIVE = PATH_LOCKS_ROOT.append("exclusive").makeAbsolute();

	/** root path for locks stored in ZooKeeper */
	public static final IPath PATH_LOCKS_DURABLE = PATH_LOCKS_ROOT.append("durable").makeAbsolute();
}
