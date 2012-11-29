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
package org.eclipse.gyrex.cloud.environment;

import java.util.Set;

import org.eclipse.gyrex.server.Platform;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * A service which can be queried for further information of the node
 * environment.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface INodeEnvironment {

	/** the service name */
	String SERVICE_NAME = INodeEnvironment.class.getName();

	/**
	 * Returns the node id.
	 * <p>
	 * Each node has a unique node id which identifies a node in the cloud. The
	 * node id must be unique. A new one is generated for new instances that
	 * don't have an id yet.
	 * </p>
	 * 
	 * @return the node id
	 */
	String getNodeId();

	/**
	 * Returns the node tags.
	 * <p>
	 * A node can have tags assigned by operators. The system may use tags to
	 * associate further functionality with a node (eg. "web" for nodes serving
	 * web content, "tests" for test nodes)
	 * </p>
	 * 
	 * @return an unmodifiable set of node tags
	 */
	Set<String> getTags();

	/**
	 * Indicates if the node/instance the service runs on is operating
	 * standalone and not in a cloud.
	 * <p>
	 * When a node operates in standalone mode it is independent from a cloud.
	 * This is the default for new instances either in
	 * {@link Platform#inDevelopmentMode() development or production} mode. Any
	 * instance must be explicitly configured in order to join a cloud.
	 * </p>
	 * 
	 * @return <code>true</code> if the node does operates standalone,
	 *         <code>false</code> otherwise
	 */
	boolean inStandaloneMode();

	/**
	 * Indicates if the node/instance the service runs on is approved to operate
	 * in the cloud.
	 * <p>
	 * Nodes must be approved before they are allowed to operate in a cloud.
	 * Usually, this is done by an administrator after the node is online. In
	 * {@link Platform#inDevelopmentMode() development} when operating
	 * {@link #inStandaloneMode() standalone} an attempt is made to approve the
	 * node automatically.
	 * </p>
	 * 
	 * @return <code>true</code> if the node is approved, <code>false</code>
	 *         otherwise
	 */
	boolean isApproved();

	/**
	 * Performs matching of the specified filter string against a set of
	 * properties describing the node.
	 * <p>
	 * The filter will be created using
	 * {@link FrameworkUtil#createFilter(String)} and matching will be performed
	 * using {@link Filter#matches(java.util.Map)}. The map passed to
	 * {@link Filter#matches(java.util.Map)} will contain the following
	 * properties.
	 * </p>
	 * <ul>
	 * <li>id</li> - {@link #getNodeId() the node id}
	 * <li>name</li> - the node name (if set)
	 * <li>location</li> - the node location string (if set)
	 * <li>tag</li> - an array of strings containing {@link #getTags() the node
	 * tags}
	 * </ul>
	 * 
	 * @param filter
	 *            the filter string (may not be <code>null</code>)
	 * @return <code>true</code> if the filter matches, <code>false</code>
	 *         otherwise
	 * @throws InvalidSyntaxException
	 *             if the filter syntax is invalid
	 */
	boolean matches(String filter) throws InvalidSyntaxException;
}
