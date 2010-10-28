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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.server.Platform;

import org.eclipse.osgi.util.NLS;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.CharSet;
import org.apache.commons.lang.StringUtils;

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
	 * @return
	 */
	private static String createNodeId() {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] nodeId = digest.digest(UUID.randomUUID().toString().getBytes(CharEncoding.US_ASCII));
			return new String(Hex.encodeHex(nodeId));
		} catch (final Exception e) {
			throw new IllegalStateException(NLS.bind("Unable to generate node id. {0}", e));
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
			final String newNodeId = createNodeId();

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
	private boolean approved;
	private String location;

	/**
	 * Creates a new instance.
	 */
	public NodeInfo() {
		nodeId = initializeNodeId();
		try {
			location = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (final UnknownHostException e) {
			location = nodeId + ": " + e.getMessage();
		}
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param data
	 * @throws Exception
	 */
	NodeInfo(final byte[] data) throws Exception {
		this();

		// TODO de-serialize data
//		ObjectInputStream in = null;
//		try {
//			in = new ObjectInputStream(new ByteArrayInputStream(data));
//
//			// location info
//			final String location = in.readUTF();
//
//			// server roles
//			final int numberOfRoles = in.readInt();
//			final String[] roles = new String[numberOfRoles];
//			for (int i = 0; i < numberOfRoles; i++) {
//				roles[i] = in.readUTF();
//			}
//		} catch (final IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//			IOUtils.closeQuietly(in);
//		}

		// this node is approved
		approved = true;
	}

	/**
	 * Returns a human-reable string of the node location.
	 * 
	 * @return the node location
	 */
	public String getLocation() {
		return location;
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
	 * Indicates if the node is an approved member.
	 * 
	 * @return <code>true</code> if the node membership is approved,
	 *         <code>false</code> otherwise
	 */
	public boolean isApproved() {
		return approved;
	}

}
