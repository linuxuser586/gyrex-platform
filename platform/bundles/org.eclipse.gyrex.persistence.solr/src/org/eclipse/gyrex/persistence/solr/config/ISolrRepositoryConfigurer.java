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
package org.eclipse.gyrex.persistence.solr.config;

import java.util.Collection;

import org.eclipse.gyrex.persistence.solr.ISolrRepositoryConstants;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Adapter for {@link IRepositoryDefinition} which allows a convenient
 * configuration of Solr repositories created by the default
 * {@link ISolrRepositoryConstants#PROVIDER_ID Solr repository provider}.
 * <p>
 * Clients that wish to obtain an instance of this class simply need to invoke
 * {@link IRepositoryDefinition#getAdapter(Class)} using this class as argument.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISolrRepositoryConfigurer {

	/**
	 * Adds a new collection to the repository.
	 * <p>
	 * This method has no effect if a collection of the same name is already
	 * defined.
	 * </p>
	 * 
	 * @param collection
	 *            the collection to add
	 * @param serverType
	 *            the server type
	 * @throws IllegalArgumentException
	 *             if any of the parameters is invalid
	 */
	void addCollection(String collection, SolrServerType serverType) throws IllegalArgumentException;

	/**
	 * Calls {@link IRepositoryPreferences#flush()}.
	 */
	void flush() throws BackingStoreException;

	/**
	 * Returns the list of configured collections.
	 * 
	 * @return an unmodifiable collection of configured collections
	 */
	Collection<String> getCollections();

	/**
	 * Removes a collection from the repository.
	 * 
	 * @param collection
	 *            the collection to remove
	 */
	void removeCollection(String collection);

}
