/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
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
 *
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
	public void addCollection(final String collection, final SolrServerType serverType) throws IllegalArgumentException {
		repositoryDefinition.getRepositoryPreferences().put("collections/" + collection + "/" + SolrRepositoryProvider.PREF_KEY_SERVER_TYPE, serverType.toString(), false);
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
