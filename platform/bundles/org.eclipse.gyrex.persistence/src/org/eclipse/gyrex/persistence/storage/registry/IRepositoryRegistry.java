/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.persistence.storage.registry;

/**
 * The repository registry provides administrative access to repositories
 * defined in the system.
 * <p>
 * Although this interface is made available as an OSGi service, security
 * restrictions may be used to prevent global access to the
 * {@link IRepositoryRegistry}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRepositoryRegistry {

	/**
	 * Retrieves the list of repository properties for the specified repository.
	 * 
	 * @param repositoryId
	 *            the repository id
	 * @return
	 */
	//Map<String, String> getRepositoryProperties(String repositoryId);
}
