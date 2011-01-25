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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
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

	public static void save(final ZooKeeperNodeInfo info, final boolean approved) throws IllegalStateException {
		final IPath path = approved ? IZooKeeperLayout.PATH_NODES_APPROVED : IZooKeeperLayout.PATH_NODES_PENDING;
		try {
			// create approved record
			ZooKeeperGate.get().writeRecord(path.append(info.getId()), CreateMode.PERSISTENT, info.toByteArray());
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Failed to save record for node %s! %s", info.getId(), ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	private final String nodeId;
	private final int version;
	private final boolean approved;
	private String name;
	private String location;
	private List<String> roles;

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
			final List<String> cloudRoles = new ArrayList<String>(roles.length);
			for (final String role : roles) {
				if (!cloudRoles.contains(role)) {
					cloudRoles.add(role);
				}
			}
			this.roles = cloudRoles;
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
	public List<String> getRoles() {
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

	/**
	 * Sets the location.
	 * 
	 * @param location
	 *            the location to set
	 */
	public void setLocation(final String location) {
		this.location = location;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Sets the roles.
	 * 
	 * @param roles
	 *            the roles to set
	 */
	public void setRoles(final List<String> roles) {
		this.roles = roles;
	}

	private byte[] toByteArray() throws IOException {
		final Properties nodeData = new Properties();
		if (StringUtils.isNotBlank(name)) {
			nodeData.setProperty("name", name);
		}
		if (StringUtils.isNotBlank(location)) {
			nodeData.setProperty("location", location);
		}
		if ((null != roles) && !roles.isEmpty()) {
			nodeData.setProperty("roles", StringUtils.join(roles, ','));
		}

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		nodeData.store(out, null);
		return out.toByteArray();
	}
}
