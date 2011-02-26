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
 *     Mike Tschierschke - rework of the SolrRepository concept (https://bugs.eclipse.org/bugs/show_bug.cgi?id=337404)
 *******************************************************************************/
package org.eclipse.gyrex.persistence.solr.config;

import org.eclipse.gyrex.persistence.solr.ISolrRepositoryConstants;
import org.eclipse.gyrex.persistence.solr.SolrServerRepository;
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
	 * Calls {@link IRepositoryPreferences#flush()}.
	 */
	void flush() throws BackingStoreException;

	/**
	 * Set's the required properties for a new {@link SolrServerRepository} to
	 * create.
	 * 
	 * @param serverType
	 *            the server type
	 * @param serverUrl
	 *            the server url - should be <code>null</code> for
	 *            {@link SolrServerType#EMBEDDED}
	 * @throws IllegalArgumentException
	 *             if any of the parameters is invalid
	 */
	void setProperties(SolrServerType serverType, String serverUrl) throws IllegalArgumentException;
}
