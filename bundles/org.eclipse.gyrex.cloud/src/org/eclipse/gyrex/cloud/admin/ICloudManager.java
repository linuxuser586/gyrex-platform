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
import java.util.Set;

import org.eclipse.gyrex.cloud.admin.node.INodeManager;
import org.eclipse.gyrex.cloud.environment.INodeEnvironment;

import org.eclipse.core.runtime.IStatus;

/**
 * A service which allows administration of a cloud.
 * <p>
 * It's exposed as API in order to allow external management of cloud nodes.
 * However, some limitations apply. This package represents an administration
 * API which is tightly coupled to an internal technology. As such, it may
 * evolve quicker than usual APIs and may not follow the <a
 * href="http://wiki.eclipse.org/Version_Numbering" target="_blank">Eclipse
 * version guidelines</a>.
 * </p>
 * <p>
 * Clients using this API should inform the Gyrex development team through it's
 * preferred channels (eg. development mailing). They should also define a more
 * strict package version range (eg. <code>[1.0.0,1.1.0)</code>) when importing
 * this package (or any other sub-package).
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICloudManager {

	void addNodeListener(INodeListener nodeListener);

	IStatus approveNode(String nodeId);

	Collection<INodeDescriptor> getApprovedNodes();

	INodeEnvironment getLocalInfo();

	/**
	 * Returns a node specific interface for configuring the node with the
	 * specified id
	 * 
	 * @param nodeId
	 *            the node id
	 * @return the node configurer
	 */
	INodeConfigurer getNodeConfigurer(String nodeId);

	/**
	 * Returns a node specific interface for managing the node with the
	 * specified id
	 * 
	 * @param nodeId
	 *            the node id
	 * @return the node manager
	 */
	INodeManager getNodeManager(String nodeId);

	/**
	 * A set of node id which are online in the system.
	 * <p>
	 * Note, the returned set may be modifiable. However, modifications do not
	 * reflect back into the system. The returned set is a snapshot at the time
	 * of invoking this method. It may already be invalid when this method
	 * returns.
	 * </p>
	 * 
	 * @return a set of node ids (never <code>null</code>)
	 */
	Set<String> getOnlineNodes();

	Collection<INodeDescriptor> getPendingNodes();

	void removeNodeListener(INodeListener nodeListener);

	IStatus retireNode(String nodeId);

}
