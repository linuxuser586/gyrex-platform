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
package org.eclipse.gyrex.preferences.internal;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ZooKeeperPreferenceConnector implements IConnectionMonitor {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperPreferenceConnector.class);

	private final ZooKeeperBasedPreferences rootNode;

	/**
	 * Creates a new instance.
	 */
	ZooKeeperPreferenceConnector(final ZooKeeperBasedPreferences rootNode) {
		this.rootNode = rootNode;
	}

	@Override
	public void connected(final ZooKeeperGate gate) {
		// no-op because nodes will re-connect themselves lazy
	}

	@Override
	public void disconnected(final ZooKeeperGate gate) {
		// set dis-connected
		if (rootNode != null) {
			try {
				rootNode.disconnectTree();
			} catch (final Exception e) {
				// ignore (maybe already disconnected)
			}
		}

		LOG.info("De-activated ZooKeeper preferences.");
	}
}