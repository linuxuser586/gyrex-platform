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
package org.eclipse.gyrex.cloud.internal.zk;

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.NodeInfo;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.preferences.IPreferencesService;

/**
 * ZooKeeper Gate configuration.
 */
public class ZooKeeperGateConfig {

	private static final String PREF_KEY_DEFAULT_CLIENT_CONNECT_STRING = "zookeeper/defaultClientConnectString";
	private static final String PREF_KEY_PREFIX_CLIENT_CONNECT_STRINGS = "zookeeper/clientConnectStrings/";

	private static final String PREF_KEY_DEFAULT_CLIENT_TIMEOUT = "zookeeper/defaultClientTimeout";
	private static final String PREF_KEY_PREFIX_CLIENT_TIMEOUTS = "zookeeper/clientTimeouts/";

	private static String getDefaultConnectString() {
		if (Platform.inDevelopmentMode()) {
			return "localhost:2181";
		}
		return null;
	}

	private final String nodeId;

	private final String nodeLocation;
	private String connectString;
	private int sessionTimeout;

	public ZooKeeperGateConfig(final NodeInfo info) {
		nodeId = info.getNodeId();
		nodeLocation = info.getLocation();
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
		final IPreferencesService preferenceService = CloudActivator.getInstance().getPreferenceService();

		// check for node specific string
		String connectionString = preferenceService.getString(CloudActivator.SYMBOLIC_NAME, PREF_KEY_PREFIX_CLIENT_CONNECT_STRINGS.concat(nodeId), null, null);
		if (connectionString != null) {
			return connectionString;
		}

		// check for location specific string
		connectionString = preferenceService.getString(CloudActivator.SYMBOLIC_NAME, PREF_KEY_PREFIX_CLIENT_CONNECT_STRINGS.concat(nodeLocation), null, null);
		if (connectionString != null) {
			return connectionString;
		}

		// fallback to default
		return preferenceService.getString(CloudActivator.SYMBOLIC_NAME, PREF_KEY_DEFAULT_CLIENT_CONNECT_STRING, getDefaultConnectString(), null);
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
		final IPreferencesService preferenceService = CloudActivator.getInstance().getPreferenceService();

		// check for node specific string
		int timeout = preferenceService.getInt(CloudActivator.SYMBOLIC_NAME, PREF_KEY_PREFIX_CLIENT_TIMEOUTS.concat(nodeId), 0, null);
		if (timeout > 0) {
			return timeout;
		}

		// check for location specific string
		timeout = preferenceService.getInt(CloudActivator.SYMBOLIC_NAME, PREF_KEY_PREFIX_CLIENT_TIMEOUTS.concat(nodeLocation), 0, null);
		if (timeout > 0) {
			return timeout;
		}

		// fallback to default
		return preferenceService.getInt(CloudActivator.SYMBOLIC_NAME, PREF_KEY_DEFAULT_CLIENT_TIMEOUT, 10000, null);
	}

	public void readFromPreferences() {
		// connect string
		connectString = getConnectStringFromPreferences();
		if (connectString == null) {
			throw new IllegalStateException("Connect string not configured for node " + nodeId);
		}

		// timeout
		sessionTimeout = getSessionTimeoutFromPreferences();
		if (sessionTimeout < 5000) {
			throw new IllegalStateException("Session timeout too low for node " + nodeId);
		}
	}
}
