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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

/**
 * ZooKeeper server configuration.
 */
public class ZooKeeperServerConfig extends QuorumPeerConfig {

	public static final String PREF_KEY_CLIENT_PORT = "zookeeper/server/clientPort";
	public static final String PREF_KEY_CLIENT_PORT_ADDRESS = "zookeeper/clientPortAddress";
	public static final String PREF_KEY_TICKTIME = "zookeeper/ticktime";

	/**
	 * Creates a new instance.
	 */
	public ZooKeeperServerConfig() {
		// initialize defaults
		final IPath zkBase = Platform.getInstanceLocation().append("zookeeper");
		dataDir = zkBase.toString();
		dataLogDir = zkBase.append("logs").toString();
	}

	public void readFromPreferences() throws ConfigException {
		final IPreferencesService preferenceService = CloudActivator.getInstance().getPreferenceService();

		// port/address to listen for client connections
		final int clientPort = preferenceService.getInt(CloudActivator.SYMBOLIC_NAME, PREF_KEY_CLIENT_PORT, 2181, null);
		final String clientPortBindAddress = preferenceService.getString(CloudActivator.SYMBOLIC_NAME, PREF_KEY_CLIENT_PORT_ADDRESS, null, null);
		if (clientPortBindAddress != null) {
			try {
				clientPortAddress = new InetSocketAddress(InetAddress.getByName(clientPortBindAddress), clientPort);
			} catch (final UnknownHostException e) {
				throw new ConfigException("Invalid clientPortAddress hostname. " + e.getMessage());
			}
		} else {
			clientPortAddress = new InetSocketAddress(clientPort);
		}

		// tick time
		tickTime = preferenceService.getInt(CloudActivator.SYMBOLIC_NAME, PREF_KEY_TICKTIME, 2000, null);
	}
}
