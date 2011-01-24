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
package org.eclipse.gyrex.cloud.admin;

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;

/**
 * A service which allows administration of a cloud.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICloudManager {

	IStatus approveNode(String nodeId);

	Collection<INodeDescriptor> getApprovedNodes();

	/**
	 * Returns a node specific interface for configuring the node with the
	 * specified id
	 * 
	 * @param nodeId
	 *            the node id
	 * @return the node configurer
	 */
	INodeConfigurer getNodeConfigurer(String nodeId);

	Collection<INodeDescriptor> getPendingNodes();

	IStatus retireNode(String nodeId);

}
