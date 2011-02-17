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
package org.eclipse.gyrex.persistence.solr.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
	public void addCollection(final String collection) throws IllegalArgumentException {
		repositoryDefinition.getRepositoryPreferences().put("collections/" + collection + "/" + SolrRepositoryProvider.PREF_KEY_SERVER_TYPE, SolrServerType.EMBEDDED.toString(), false);
	}

	@Override
	public void addCollection(final String collection, final SolrServerType serverType, final String serverUrl) throws IllegalArgumentException {
		repositoryDefinition.getRepositoryPreferences().put("collections/" + collection + "/" + SolrRepositoryProvider.PREF_KEY_SERVER_TYPE, serverType.toString(), false);
		repositoryDefinition.getRepositoryPreferences().put("collections/" + collection + "/" + SolrRepositoryProvider.PREF_KEY_SERVER_URL, serverUrl.toString(), false);
	}

	@Override
	public void flush() throws BackingStoreException {
		repositoryDefinition.getRepositoryPreferences().flush();
	}

	@Override
	public Collection<String> getCollections() {
		try {
			return Collections.unmodifiableList(Arrays.asList(repositoryDefinition.getRepositoryPreferences().getChildrenNames("collections")));
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Unable to read configuration of repository %s. %s", repositoryDefinition.getRepositoryId(), e.getMessage()));
		}
	}

	@Override
	public void removeCollection(final String collection) {
		repositoryDefinition.getRepositoryPreferences().remove("collections/" + collection + "/" + SolrRepositoryProvider.PREF_KEY_SERVER_TYPE);

	}

}
