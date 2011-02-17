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
 *     Mike Tschierschke - API rework (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=337184)
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
	 * Adds a new collection to the repository. The collection is linked to an
	 * embedded solr server with default settings.
	 * <p>
	 * This method has no effect if a collection of the same name is already
	 * defined.
	 * </p>
	 * 
	 * @param collection
	 *            the collection to add
	 * @throws IllegalArgumentException
	 *             if any of the parameters is invalid
	 */
	void addCollection(String collection) throws IllegalArgumentException;

	/**
	 * Adds a new collection to the repository. The collection is linked to an
	 * solr server with the given type and the specified url.
	 * <p>
	 * This method has no effect if a collection of the same name is already
	 * defined.
	 * </p>
	 * 
	 * @param collection
	 *            the collection to add
	 * @param serverType
	 *            the server type
	 * @param serverUrl
	 *            the server url
	 * @throws IllegalArgumentException
	 *             if any of the parameters is invalid
	 */
	void addCollection(String collection, SolrServerType serverType, String serverUrl) throws IllegalArgumentException;

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
