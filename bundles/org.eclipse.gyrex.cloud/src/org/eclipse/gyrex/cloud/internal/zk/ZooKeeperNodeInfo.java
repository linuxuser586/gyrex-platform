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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;

/**
 * Information about a node stored in ZooKeeper.
 */
public class ZooKeeperNodeInfo {

	private static final String PROP_TAGS = "tags";
	private static final String PROP_LOCATION = "location";
	private static final String PROP_NAME = "name";

	public static void approve(final String nodeId, String name, String location) throws Exception {
		final ZooKeeperGate zk = ZooKeeperGate.get();

		// read missing info from current pending record
		try {
			final ZooKeeperNodeInfo info = load(nodeId, false);
			if (name == null) {
				name = info.getName();
			}
			if (location == null) {
				location = info.getLocation();
			}
		} catch (final Exception e) {
			// ignore
		}

		// create new info
		final ZooKeeperNodeInfo nodeInfo = new ZooKeeperNodeInfo(nodeId, true, null, 0);
		nodeInfo.setName(name);
		nodeInfo.setLocation(location);

		// create approved record
		zk.createPath(IZooKeeperLayout.PATH_NODES_APPROVED.append(nodeId), CreateMode.PERSISTENT, nodeInfo.toByteArray());

		// delete pending record
		zk.deletePath(IZooKeeperLayout.PATH_NODES_PENDING.append(nodeId));
	}

	public static ZooKeeperNodeInfo load(final String nodeId, final boolean approved) throws IllegalStateException {
		final Stat stat = new Stat();
		final IPath path = approved ? IZooKeeperLayout.PATH_NODES_APPROVED : IZooKeeperLayout.PATH_NODES_PENDING;
		byte[] record = null;
		try {
			record = ZooKeeperGate.get().readRecord(path.append(nodeId), stat);
		} catch (final NoNodeException e) {
			throw new IllegalStateException(String.format(approved ? "Approved record for node %s not found!" : "Pending record for node %s not found!", nodeId));
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Failed to read record for node %s! %s", nodeId, ExceptionUtils.getRootCauseMessage(e)), e);
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
	private Set<String> tags;

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
		name = properties.getProperty(PROP_NAME);

		// location
		location = properties.getProperty(PROP_LOCATION, null);

		// tags
		final String[] tags = StringUtils.split(properties.getProperty(PROP_TAGS), ',');
		if ((tags != null) && (tags.length > 0)) {
			final Set<String> cloudTags = new HashSet<String>(tags.length);
			for (final String role : tags) {
				if (!cloudTags.contains(role)) {
					cloudTags.add(role);
				}
			}
			this.tags = cloudTags;
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
	public Set<String> getTags() {
		return tags;
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
	 * Sets the tags.
	 * 
	 * @param tags
	 *            the tags to set (maybe <code>null</code> to unset)
	 */
	public void setTags(final Set<String> tags) {
		this.tags = tags;
	}

	private byte[] toByteArray() throws IOException {
		final Properties nodeData = new Properties();
		if (StringUtils.isNotBlank(name)) {
			nodeData.setProperty(PROP_NAME, name);
		}
		if (StringUtils.isNotBlank(location)) {
			nodeData.setProperty(PROP_LOCATION, location);
		}
		if ((null != tags) && !tags.isEmpty()) {
			nodeData.setProperty(PROP_TAGS, StringUtils.join(tags, ','));
		}

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		nodeData.store(out, null);
		return out.toByteArray();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.NO_FIELD_NAMES_STYLE).append("id", nodeId).append("name", name).append("location", location).append("tags", tags).toString();
	}
}
