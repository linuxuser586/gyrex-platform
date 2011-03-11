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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateConfig;

import org.eclipse.core.runtime.preferences.InstanceScope;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.prefs.Preferences;

/**
 * {@link INodeEnvironment} implementation.
 */
public class NodeEnvironmentImpl implements INodeEnvironment {

	@Override
	public String getNodeId() {
		final NodeInfo nodeInfo = CloudState.getNodeInfo();
		if (nodeInfo == null) {
			return new NodeInfo().getNodeId();
		}
		return nodeInfo.getNodeId();
	}

	private Map<String, Object> getNodeProperties() {
		final Map<String, Object> nodeProperties = new HashMap<String, Object>(2);
		nodeProperties.put("id", getNodeId());
		final Set<String> tags = getTags();
		if (!tags.isEmpty()) {
			nodeProperties.put("tag", tags.toArray(new String[tags.size()]));
		}
		return nodeProperties;
	}

	@Override
	public Set<String> getTags() {
		final NodeInfo nodeInfo = CloudState.getNodeInfo();
		if (nodeInfo == null) {
			return Collections.emptySet();
		}
		return nodeInfo.getTags();
	}

	@Override
	public boolean inStandaloneMode() {
		// TODO duplicated in NodeConfigurer
		final Preferences preferences = InstanceScope.INSTANCE.getNode(CloudActivator.SYMBOLIC_NAME).node(ZooKeeperGateConfig.PREF_NODE_ZOOKEEPER);
		return preferences.get(ZooKeeperGateConfig.PREF_KEY_CLIENT_CONNECT_STRING, null) == null;
	}

	@Override
	public boolean isApproved() {
		final NodeInfo nodeInfo = CloudState.getNodeInfo();
		if (nodeInfo == null) {
			return false;
		}
		return nodeInfo.isApproved();
	}

	@Override
	public boolean matches(final String filter) throws InvalidSyntaxException {
		return FrameworkUtil.createFilter(filter).matches(getNodeProperties());
	}
}
