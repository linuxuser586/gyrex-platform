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

import java.util.Collection;

import org.eclipse.gyrex.persistence.solr.config.ISolrRepositoryConfigurer;
import org.eclipse.gyrex.persistence.solr.config.SolrServerType;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;

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

	@Override
	public void setUrls(final String masterUrl, final Collection<String> replicaUrls) throws IllegalArgumentException {
		if (StringUtils.isBlank(masterUrl)) {
			throw new IllegalArgumentException("master url must not be null");
		}
		if ((null == replicaUrls) || replicaUrls.isEmpty()) {
			throw new IllegalArgumentException("at least on replica url must be specified");
		}

		// set server type
		repositoryDefinition.getRepositoryPreferences().put(SolrRepositoryProvider.PREF_KEY_SERVER_TYPE, SolrServerType.REMOTE.toString(), false);

		// store master url
		repositoryDefinition.getRepositoryPreferences().put(SolrRepositoryProvider.PREF_KEY_SERVER_URL, masterUrl, false);

		// remove all existing replica urls and set new once
		int pos = 0;
		repositoryDefinition.getRepositoryPreferences().remove(SolrRepositoryProvider.PREF_KEY_SERVER_READ_URLS);
		for (final String replicaUrl : replicaUrls) {
			if (StringUtils.isBlank(replicaUrl)) {
				throw new IllegalArgumentException("invalid blank replica url found ");
			}
			repositoryDefinition.getRepositoryPreferences().put(SolrRepositoryProvider.PREF_KEY_SERVER_READ_URLS + "//" + pos, replicaUrl, false);
			pos++;
		}
	}
}
