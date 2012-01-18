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
package org.eclipse.gyrex.cloud.services.state;

import org.eclipse.gyrex.cloud.services.state.query.INodeStateQueryService;

import org.osgi.framework.Constants;

/**
 * A service to publish state information of the local node environment.
 * <p>
 * This service allows the local node to publish information into the cloud. It
 * can then be queried from any node using the {@link INodeStateQueryService
 * query service}.
 * </p>
 * <p>
 * Node state is typically state information that is valuable as long as the
 * node is active. It's usually coupled to the life-cycle of the node as well as
 * to the life-cycle of some other component. Internally, Gyrex ensures that any
 * node information is made available when a cloud connection is established and
 * synchronized whenever the service is modified.
 * </p>
 * <p>
 * In order to publish node state information it must be registered with Gyrex
 * by registering it as OSGi service using {@link #SERVICE_NAME this class name}
 * . The {@link Constants#SERVICE_PID service pid} will be used to uniquely
 * identify the service within the cloud and to query node state information for
 * a particular service. All service properties will be published to the cloud.
 * Any service property will be converted to a String value.
 * </p>
 * <p>
 * This interface must be implemented by clients providing node state.
 * </p>
 */
public interface INodeState {

	/** the service name */
	String SERVICE_NAME = "org.eclipse.gyrex.cloud.environment.NodeState";
}
