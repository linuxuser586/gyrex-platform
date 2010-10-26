/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.solr.internal;

import java.net.MalformedURLException;

import org.eclipse.gyrex.persistence.solr.ISolrRepositoryConstants;
import org.eclipse.gyrex.persistence.solr.SolrServerRepository;
import org.eclipse.gyrex.persistence.solr.SolrServerType;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;

/**
 * A repository provider for Solr repositories.
 */
@SuppressWarnings("restriction")
public class SolrRepositoryProvider extends RepositoryProvider {

	private final CoreContainer coreContainer;

	/**
	 * Creates a new instance.
	 * 
	 * @param coreContainer
	 * @param adminCore
	 * @param repositoryTypeId
	 */
	public SolrRepositoryProvider(final CoreContainer coreContainer) {
		super(ISolrRepositoryConstants.PROVIDER_ID, SolrServerRepository.class);
		this.coreContainer = coreContainer;
	}

	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		final IEclipsePreferences prefs = repositoryPreferences.getPreferences();
		final String typeStr = prefs.get(ISolrRepositoryConstants.PREF_KEY_SERVER_TYPE, null);
		final SolrServerType serverType = typeStr == null ? SolrServerType.EMBEDDED : SolrServerType.valueOf(typeStr);
		switch (serverType) {
			case EMBEDDED:
				return new SolrRepository(repositoryId, this, getEmbeddedServer(repositoryId));

			case REMOTE:
				return getRemoteServer(repositoryId, repositoryPreferences);
		}
		throw new IllegalStateException(NLS.bind("unsupported server type {0} for repository {1}", typeStr, repositoryId));
	}

	private SolrServer getEmbeddedServer(final String repositoryId) {
		// we simple use the repository id as the core name
		final SolrCore core = coreContainer.getCore(repositoryId);
		if (null == core) {
			throw new IllegalStateException("no Solr '" + repositoryId + "' not found");
		}
		core.close();
		return new EmbeddedSolrServer(coreContainer, repositoryId);
	}

	private SolrRepository getRemoteServer(final String repositoryId, final IRepositoryPreferences preferences) {
		final String urlString = preferences.getPreferences().get(ISolrRepositoryConstants.PREF_KEY_SERVER_URL, "http://localhost:8983/solr/");
		try {
			final CommonsHttpSolrServer solrServer = new CommonsHttpSolrServer(urlString);
			final CommonsHttpSolrServer solrServerForRead = new CommonsHttpSolrServer(urlString);
			solrServerForRead.setConnectionTimeout(1000);
			solrServerForRead.setConnectionManagerTimeout(1000l);
			solrServerForRead.setSoTimeout(5000);
			return new SolrRepository(repositoryId, this, solrServer, solrServerForRead);
		} catch (final MalformedURLException e) {
			throw new IllegalStateException(NLS.bind("invalid url ({0}) configured for repository {1}: {2}", new Object[] { urlString, repositoryId, e.getMessage() }));
		}
	}

}
