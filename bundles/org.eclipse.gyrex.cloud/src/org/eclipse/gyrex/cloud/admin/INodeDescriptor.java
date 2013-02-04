/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
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

import java.util.Set;

/**
 * Public node information.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.o
 */
public interface INodeDescriptor {

	/**
	 * Returns the node identifier.
	 * 
	 * @return the node id
	 */
	String getId();

	/**
	 * Returns the node location.
	 * 
	 * @return the node location
	 */
	String getLocation();

	/**
	 * Returns the node name.
	 * 
	 * @return the node name
	 */
	String getName();

	/**
	 * Returns the node tags.
	 * 
	 * @return the node tags
	 */
	Set<String> getTags();

	/**
	 * Indicates if the node is approved.
	 * 
	 * @return <code>true</code> if approved, <code>false</code> otherwise
	 */
	boolean isApproved();
}
