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

/**
 * Public node information.
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

}
