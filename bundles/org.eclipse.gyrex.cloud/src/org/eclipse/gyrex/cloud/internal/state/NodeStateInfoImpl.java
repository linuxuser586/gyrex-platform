/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.state;

import java.util.Map;

import org.eclipse.gyrex.cloud.services.state.query.INodeStateInfo;

/**
 * {@link INodeStateInfo} implementation.
 */
public class NodeStateInfoImpl implements INodeStateInfo {

	/** servicePid */
	private final String servicePid;
	/** nodeId */
	private final String nodeId;
	/** map */
	private final Map<String, String> data;

	/**
	 * Creates a new instance.
	 * 
	 * @param servicePid
	 * @param nodeId
	 * @param data
	 */
	NodeStateInfoImpl(final String servicePid, final String nodeId, final Map<String, String> data) {
		this.servicePid = servicePid;
		this.nodeId = nodeId;
		this.data = data;
	}

	@Override
	public String getNodeId() {
		return nodeId;
	}

	@Override
	public String getServicePid() {
		return servicePid;
	}

	@Override
	public Map<String, String> getStateData() {
		return data;
	}
}