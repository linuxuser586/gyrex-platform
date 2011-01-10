/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *	   Mike Tschierschke - customization for rap based admin ui,
 *     						added method getRepositoryDefinition

 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.registry;

import java.util.Collection;

import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

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
	 * Creates a repository of the specified type.
	 * 
	 * @param repositoryId
	 *            the repository id (may not be <code>null</code>)
	 * @param repositoryProviderId
	 *            the {@link RepositoryProvider#getProviderId() repository
	 *            provider id} (may not be <code>null</code>)
	 * @return the repository definition
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if a repository already exists or an error occurred accessing
	 *             the underlying repository store
	 */
	IRepositoryDefinition createRepository(String repositoryId, String repositoryProviderId) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Retrieves the repository definition for the specified repository.
	 * 
	 * @param repositoryId
	 *            the repository id
	 * @return the repository definition (maybe <code>null</code> if the
	 *         repository does not exist)
	 * @throws IllegalStateException
	 *             if an error occurred accessing the underlying repository
	 *             store
	 */
	public IRepositoryDefinition getRepositoryDefinition(String repositoryId) throws IllegalStateException;

	/**
	 * Returns a list of all repository ids.
	 * 
	 * @return an <b>unmodifiable</b> collection wit the ids of all registered
	 *         repositories (may be empty but never <code>null</code>)
	 * @throws IllegalStateException
	 *             if an error occurred accessing the underlying repository
	 *             store
	 */
	public Collection<String> getRepositoryIds() throws IllegalStateException;

	/**
	 * Removes a repository.
	 * 
	 * @param repositoryId
	 *            the id of the repository to undefine (may not be
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if an error occurred accessing the underlying repository
	 *             store
	 */
	void removeRepository(String repositoryId) throws IllegalArgumentException, IllegalStateException;
}
