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

import java.util.Map;

import org.osgi.framework.Constants;

/**
 * State information published by a node.
 */
public interface INodeStateInfo {

	/**
	 * Returns the id of the node which published the information.
	 * 
	 * @return
	 */
	String getNodeId();

	/**
	 * Returns the {@link Constants#SERVICE_PID pid} of the service which
	 * published the information.
	 * 
	 * @return
	 */
	String getServicePid();

	/**
	 * Returns the published data.
	 * 
	 * @return an unmodifiable map of the published data
	 */
	Map<String, String> getStateData();

}
