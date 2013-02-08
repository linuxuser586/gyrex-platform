/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.NodeInfo;
import org.eclipse.gyrex.server.Platform;

import org.apache.commons.lang.math.NumberUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeper Gate configuration.
 */
public class ZooKeeperGateConfig {

	/** default session timeout (in ms) */
	private static final int DEFAULT_SESSION_TIMEOUT = 30000;

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperGateConfig.class);

	public static final String PREF_NODE_ZOOKEEPER = "zookeeper";
	public static final String PREF_KEY_CLIENT_CONNECT_STRING = "clientConnectString";
	public static final String PREF_KEY_CLIENT_TIMEOUT = "clientTimeout";

	private static String getDefaultConnectString() {
		final String connectString = System.getProperty("gyrex.zookeeper.connectString");
		if (null != connectString)
			return connectString;
		if (Platform.inDevelopmentMode())
			return "localhost:2181";
		return null;
	}

	private final String nodeId;

	private String connectString;

	private int sessionTimeout = DEFAULT_SESSION_TIMEOUT;

	public ZooKeeperGateConfig(final NodeInfo info) {
		nodeId = info.getNodeId();
	}

	/**
	 * Returns the connectString.
	 * 
	 * @return the connectString
	 */
	public String getConnectString() {
		return connectString;
	}

	/**
	 * Reads the connect string from the preferences.
	 * 
	 * @return the connect string
	 */
	private String getConnectStringFromPreferences() {
		// check for node specific string
		return CloudActivator.getInstance().getPreferenceService().getString(CloudActivator.SYMBOLIC_NAME, PREF_NODE_ZOOKEEPER + "/" + PREF_KEY_CLIENT_CONNECT_STRING, getDefaultConnectString(), null);
	}

	private int getDefaultSessionTimeout() {
		final String sessionTimeoutValue = System.getProperty("gyrex.zookeeper.sessionTimeout");
		final int sessionTimeout = NumberUtils.toInt(sessionTimeoutValue, DEFAULT_SESSION_TIMEOUT);
		if (sessionTimeout > 5000)
			return sessionTimeout;
		if (null != sessionTimeoutValue) {
			LOG.warn("ZooKeeper session timeout of {}ms (parsed from '{}') is too low. Using default {}ms.", sessionTimeout, sessionTimeoutValue, DEFAULT_SESSION_TIMEOUT);
		} else {
			LOG.warn("ZooKeeper session timeout of {}ms is too low. Using default {}ms.", sessionTimeout, DEFAULT_SESSION_TIMEOUT);
		}
		return DEFAULT_SESSION_TIMEOUT;
	}

	/**
	 * Returns the sessionTimeout.
	 * 
	 * @return the sessionTimeout
	 */
	public int getSessionTimeout() {
		return sessionTimeout;
	}

	/**
	 * Reads the connect string from the preferences.
	 * 
	 * @return the connect string
	 */
	private int getSessionTimeoutFromPreferences() {
		// check for node specific string
		return CloudActivator.getInstance().getPreferenceService().getInt(CloudActivator.SYMBOLIC_NAME, PREF_NODE_ZOOKEEPER + "/" + PREF_KEY_CLIENT_TIMEOUT, getDefaultSessionTimeout(), null);
	}

	public void readFromPreferences() {
		// connect string
		connectString = getConnectStringFromPreferences();
		if (connectString == null)
			throw new IllegalStateException("Connect string not configured for node " + nodeId);

		// timeout
		sessionTimeout = getSessionTimeoutFromPreferences();
		if (sessionTimeout < 5000)
			throw new IllegalStateException("Session timeout too low for node " + nodeId);
	}

	/**
	 * Sets the connectString.
	 * 
	 * @param connectString
	 *            the connectString to set
	 */
	protected void setConnectString(final String connectString) {
		this.connectString = connectString;
	}

	/**
	 * Sets the sessionTimeout.
	 * 
	 * @param sessionTimeout
	 *            the sessionTimeout to set
	 */
	protected void setSessionTimeout(final int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(connectString).append(", sessionTimeout=").append(sessionTimeout).append("]");
		return builder.toString();
	}
}
