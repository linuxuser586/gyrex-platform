/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.server.Platform;

import org.eclipse.osgi.util.NLS;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.CharSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * Information about this node.
 * <p>
 * In Gyrex each node is identified by a unique node id. This id is read from a
 * file in the instance location (see {@link #getNodeIdFile()}). If it doesn't
 * exist, a new node id is generated and written to that file.
 * </p>
 */
public class NodeInfo {

	/** file name of node id file */
	private static final String NODE_ID_FILENAME = "nodeId";

	/** char set of allowed chars */
	private static final CharSet ALLOWED_NODE_ID_CHARS = CharSet.getInstance(new String[] { "a-z", "0-9", ".", "-", "_" });

	/** verified node id */
	private static final AtomicReference<String> verifiedNodeId = new AtomicReference<String>();

	/**
	 * Generates a node id based on {@link UUID#randomUUID()}.
	 * 
	 * @return the generated node id
	 */
	private static String generateNodeId() {
		return UUID.randomUUID().toString();
	}

	private static String getDefaultLocationInfo(final String nodeId) {
		try {
			return InetAddress.getLocalHost().getCanonicalHostName();
		} catch (final UnknownHostException e) {
			return nodeId + ": " + e.getMessage();
		}
	}

	/**
	 * Returns the instance node id file.
	 * 
	 * @return the instance node id file
	 */
	private static File getNodeIdFile() {
		return Platform.getStateLocation(CloudActivator.getInstance().getBundle()).append(NODE_ID_FILENAME).toFile();
	}

	/**
	 * Read node id from instance location or generate a new one (and save it
	 * persistently).
	 */
	private static String initializeNodeId() {
		try {
			// re-use verified id
			final String nodeId = verifiedNodeId.get();
			if (nodeId != null) {
				return nodeId;
			}

			// get file
			final File nodeIdFile = getNodeIdFile();
			if (nodeIdFile.exists() && nodeIdFile.isFile()) {
				final String rawNodeId = StringUtils.trimToNull(FileUtils.readFileToString(nodeIdFile, CharEncoding.US_ASCII));

				// verify id
				if (isValidNodeId(rawNodeId)) {
					verifiedNodeId.compareAndSet(null, rawNodeId);
					return verifiedNodeId.get();
				}
			}

			// generate new node id
			final String newNodeId = generateNodeId();

			// write to file
			if (verifiedNodeId.compareAndSet(null, newNodeId)) {
				FileUtils.writeStringToFile(nodeIdFile, newNodeId, CharEncoding.US_ASCII);
			}

			// done
			return verifiedNodeId.get();
		} catch (final IOException e) {
			throw new IllegalStateException(NLS.bind("Unable to initialize node id. {0}", e.getMessage()), e);
		}
	}

	/**
	 * Validates a node id
	 * 
	 * @param rawNodeId
	 * @return <code>true</code> if valid, <code>false</code> otherwise
	 */
	private static boolean isValidNodeId(final String rawNodeId) {
		// not null or blank
		if (StringUtils.isBlank(rawNodeId)) {
			return false;
		}

		// scan for invalid chars
		for (final char c : rawNodeId.toCharArray()) {
			if (!ALLOWED_NODE_ID_CHARS.contains(c)) {
				return false;
			}
		}

		// ok
		return true;
	}

	private final String nodeId;
	private final boolean approved;
	private final String location;
	private final String name;
	private final Set<String> roles;

	/**
	 * Creates a new instance.
	 */
	public NodeInfo() {
		nodeId = initializeNodeId();
		location = getDefaultLocationInfo(nodeId);
		name = "";
		roles = Collections.emptySet();
		approved = false;
	}

	/**
	 * Creates a new instance using the specified instance data.
	 * 
	 * @param data
	 *            the instance data
	 * @throws Exception
	 *             if an error occurred parsing the instance data
	 */
	NodeInfo(final byte[] data) throws Exception {
		nodeId = initializeNodeId();

		// parse
		final Properties properties = new Properties();
		if (data != null) {
			properties.load(new ByteArrayInputStream(data));
		}

		// name
		name = properties.getProperty("name");

		// location
		location = properties.getProperty("location", getDefaultLocationInfo(nodeId));

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

		// this node is approved
		approved = true;
	};

	/**
	 * Returns a human-reable string of the node location.
	 * 
	 * @return the node location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Returns the name.
	 * 
	 * @return the name (defaults to {@link #getNodeId() node id} if the name is
	 *         not set)
	 */
	public String getName() {
		return name != null ? name : nodeId;
	}

	/**
	 * Returns the unique identifier of this node.
	 * 
	 * @return the unique node identifier
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * Returns a collection of roles assigned to the node.
	 * 
	 * @return an unmodifiable collection of assigned roles
	 */
	public Collection<String> getRoles() {
		return roles;
	}

	/**
	 * Indicates if the node is an approved member.
	 * 
	 * @return <code>true</code> if the node membership is approved,
	 *         <code>false</code> otherwise
	 */
	public boolean isApproved() {
		return approved;
	}

	@Override
	public String toString() {
		final StrBuilder info = new StrBuilder();
		info.append(getName()).append(" (");
		info.append(location);
		info.append(", ");
		info.append(approved ? "APPROVED" : "PENDING");
		info.append(", ");
		info.append(nodeId);
		info.append(")");
		return info.toString();
	}
}
