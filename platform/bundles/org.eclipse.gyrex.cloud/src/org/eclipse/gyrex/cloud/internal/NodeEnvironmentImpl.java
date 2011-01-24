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
package org.eclipse.gyrex.cloud.internal;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateConfig;

import org.eclipse.core.runtime.preferences.InstanceScope;

import org.osgi.service.prefs.Preferences;

/**
 * {@link INodeEnvironment} implementation.
 */
public class NodeEnvironmentImpl implements INodeEnvironment {

	@Override
	public String getNodeId() {
		return new NodeInfo().getNodeId();
	}

	@Override
	public boolean inStandaloneMode() {
		// check if the node exists
		final Preferences preferences = new InstanceScope().getNode(CloudActivator.SYMBOLIC_NAME).node(ZooKeeperGateConfig.PREF_NODE_ZOOKEEPER);
		return preferences.get(ZooKeeperGateConfig.PREF_KEY_CLIENT_CONNECT_STRING, null) == null;
	}

}
