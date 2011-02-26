/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mike Tschierschke - rework of the SolrRepository concept (https://bugs.eclipse.org/bugs/show_bug.cgi?id=337404)
 *******************************************************************************/
package org.eclipse.gyrex.persistence.solr.internal;

import java.net.MalformedURLException;

import org.eclipse.gyrex.persistence.solr.ISolrRepositoryConstants;
import org.eclipse.gyrex.persistence.solr.SolrServerRepository;
import org.eclipse.gyrex.persistence.solr.config.SolrServerType;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

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

	/**
	 * preference key for {@link SolrServerType server type} setting (value
	 * <code>serverType</code>)
	 */
	public static final String PREF_KEY_SERVER_TYPE = "serverType";

	/**
	 * preference key for a Solr base URL to be used with
	 * {@link SolrServerType#REMOTE remote} Solr servers (value
	 * <code>serverUrl</code>)
	 */
	public static final String PREF_KEY_SERVER_URL = "serverUrl";

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

	private SolrServer createReadServer(final String urlString) throws MalformedURLException {
		final CommonsHttpSolrServer solrServerForRead = new CommonsHttpSolrServer(urlString);
		solrServerForRead.setConnectionTimeout(1000);
		solrServerForRead.setConnectionManagerTimeout(1000l);
		solrServerForRead.setSoTimeout(5000);
		return solrServerForRead;
	}

	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		return new SolrRepository(repositoryId, this, createServers(repositoryId, repositoryPreferences));
	}

	private SolrServer[] createServers(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {

		final SolrServer[] servers;
		final String typeStr = repositoryPreferences.get(PREF_KEY_SERVER_TYPE, null);
		final SolrServerType serverType = typeStr == null ? SolrServerType.EMBEDDED : SolrServerType.valueOf(typeStr);
		switch (serverType) {
			case EMBEDDED:
				final SolrServer embeddedServer = getEmbeddedServer(repositoryId);
				servers = new SolrServer[] { embeddedServer, embeddedServer };
				break;

			case REMOTE:
				final String urlString = repositoryPreferences.get(PREF_KEY_SERVER_URL, null);
				try {
					servers = new SolrServer[] { new CommonsHttpSolrServer(urlString), createReadServer(urlString) };
				} catch (final MalformedURLException e) {
					throw new IllegalStateException(String.format("Repository %s not configured correctly. Server URL '%s' is invalid.  %s", repositoryId, urlString, e.getMessage()));
				}
				break;

			default:
				throw new IllegalStateException(String.format("Repository %s not configured correctly. Unsupported server type %s", repositoryId, typeStr));
		}
		return servers;
	}

	private SolrServer getEmbeddedServer(final String repositoryId) {
		// compute the core name
		final String coreName = SolrActivator.getEmbeddedSolrCoreName(repositoryId);
		// check core
		final SolrCore core = coreContainer.getCore(coreName);
		if (null == core) {
			throw new IllegalStateException("Solr core '" + coreName + "' not found");
		}
		core.close();
		return new EmbeddedSolrServer(coreContainer, coreName);
	}

}
