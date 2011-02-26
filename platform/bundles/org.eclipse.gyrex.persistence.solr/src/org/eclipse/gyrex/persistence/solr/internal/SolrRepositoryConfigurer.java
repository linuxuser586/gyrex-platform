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
package org.eclipse.gyrex.persistence.solr.internal;

import org.eclipse.gyrex.persistence.solr.config.ISolrRepositoryConfigurer;
import org.eclipse.gyrex.persistence.solr.config.SolrServerType;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;

import org.osgi.service.prefs.BackingStoreException;

/**
 * {@link ISolrRepositoryConfigurer} impl
 */
public class SolrRepositoryConfigurer implements ISolrRepositoryConfigurer {

	private final IRepositoryDefinition repositoryDefinition;

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryDefinition
	 */
	public SolrRepositoryConfigurer(final IRepositoryDefinition repositoryDefinition) {
		this.repositoryDefinition = repositoryDefinition;
	}

	@Override
	public void flush() throws BackingStoreException {
		repositoryDefinition.getRepositoryPreferences().flush();
	}

	@Override
	public void setProperties(final SolrServerType serverType, final String serverUrl) throws IllegalArgumentException {
		repositoryDefinition.getRepositoryPreferences().put(SolrRepositoryProvider.PREF_KEY_SERVER_TYPE, serverType.toString(), false);
		if (serverUrl != null) {
			repositoryDefinition.getRepositoryPreferences().put(SolrRepositoryProvider.PREF_KEY_SERVER_URL, serverUrl, false);
		} else {
			repositoryDefinition.getRepositoryPreferences().remove(SolrRepositoryProvider.PREF_KEY_SERVER_URL);
		}
	}
}
