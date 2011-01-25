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

import java.util.Set;

import org.eclipse.core.runtime.IStatus;

/**
 * The node specific cloud configuration.
 */
public interface INodeConfigurer {

	/**
	 * Configures the cloud connection of this node.
	 * <p>
	 * As part of this method a cloud connection test will be performed.
	 * </p>
	 * 
	 * @param connectString
	 *            the connection string (maybe <code>null</code> to reset)
	 * @return a status indicating the result
	 */
	IStatus configureConnection(String connectString);

	/**
	 * Assigns the specified roles to the node.
	 * 
	 * @param tags
	 *            the roles to assign (maybe <code>null</code> to reset)
	 * @return a status indicating the result
	 */
	IStatus setTags(Set<String> tags);

}
