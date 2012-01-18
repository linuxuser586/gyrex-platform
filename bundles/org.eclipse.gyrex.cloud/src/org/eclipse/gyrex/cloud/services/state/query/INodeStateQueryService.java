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
package org.eclipse.gyrex.cloud.services.state.query;

import java.util.List;

/**
 * A service for querying the cloud for node state information.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface INodeStateQueryService {

	/**
	 * Retrieves all {@link INodeStateInfo node state} for a specific node id.
	 * 
	 * @param nodeId
	 *            the id of the node
	 * @return an unmodifiable list of {@link INodeStateInfo}
	 */
	List<INodeStateInfo> findByNodeId(String nodeId) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Retrieves all {@link INodeStateInfo node state} for a specific service
	 * pid.
	 * 
	 * @param servicePid
	 *            the service pid
	 * @return an unmodifiable list of {@link INodeStateInfo}
	 */
	List<INodeStateInfo> findByServicePid(String servicePid) throws IllegalArgumentException, IllegalStateException;
}
