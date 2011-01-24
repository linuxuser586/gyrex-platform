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
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.zk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.data.Stat;

/**
 * Information about a node stored in ZooKeeper.
 */
public class ZooKeeperNodeInfo {

	public static ZooKeeperNodeInfo load(final String nodeId, final boolean approved) throws IllegalStateException {
		final Stat stat = new Stat();
		final IPath path = approved ? IZooKeeperLayout.PATH_NODES_APPROVED : IZooKeeperLayout.PATH_NODES_PENDING;
		byte[] record = null;
		try {
			record = ZooKeeperGate.get().readRecord(path.append(nodeId), stat);
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Failed to read record for node %s! %s", nodeId, ExceptionUtils.getRootCauseMessage(e)), e);
		}
		if (record == null) {
			throw new IllegalStateException(String.format(approved ? "Approved record for node %s not found!" : "Pending record for node %s not found!", nodeId));
		}
		return new ZooKeeperNodeInfo(nodeId, approved, record, stat.getVersion());
	}

	private final String nodeId;
	private final int version;
	private final String name;
	private final String location;
	private final Set<String> roles;
	private final boolean approved;

	/**
	 * Creates a new instance.
	 */
	public ZooKeeperNodeInfo(final String nodeId, final boolean approved, final byte[] data, final int version) {
		if (nodeId == null) {
			throw new IllegalArgumentException("node id must not be null");
		}
		this.nodeId = nodeId;
		this.approved = approved;
		this.version = version;

		// parse
		final Properties properties = new Properties();
		if (data != null) {
			try {
				properties.load(new ByteArrayInputStream(data));
			} catch (final IOException e) {
				// this looks like invalid node data
				throw new IllegalArgumentException(String.format("Invalid node data for node %s. %s", nodeId, ExceptionUtils.getRootCauseMessage(e)), e);
			}
		}

		// name
		name = properties.getProperty("name");

		// location
		location = properties.getProperty("location", null);

		// roles
		final String[] roles = StringUtils.split(properties.getProperty("roles"), ',');
		if ((roles != null) && (roles.length > 0)) {
			final Set<String> cloudRoles = new HashSet<String>(roles.length);
			for (final String role : roles) {
				cloudRoles.add(role);
			}
			this.roles = Collections.unmodifiableSet(cloudRoles);
		} else {
			this.roles = Collections.emptySet();
		}
	}

	/**
	 * Returns the nodeId.
	 * 
	 * @return the nodeId
	 */
	public String getId() {
		return nodeId;
	}

	/**
	 * Returns the location.
	 * 
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Returns the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the roles.
	 * 
	 * @return the roles
	 */
	public Set<String> getRoles() {
		return roles;
	}

	/**
	 * Returns the version.
	 * 
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Returns the approved.
	 * 
	 * @return the approved
	 */
	public boolean isApproved() {
		return approved;
	}
}
