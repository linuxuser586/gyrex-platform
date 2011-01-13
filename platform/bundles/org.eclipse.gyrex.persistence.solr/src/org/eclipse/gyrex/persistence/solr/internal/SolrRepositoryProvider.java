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
 *******************************************************************************/
package org.eclipse.gyrex.persistence.solr.internal;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.monitoring.metrics.BaseMetric;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOG = LoggerFactory.getLogger(SolrRepositoryProvider.class);

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

	private Map<String, SolrServer[]> createServers(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		String[] collections;
		try {
			collections = repositoryPreferences.getChildrenNames("collections");
		} catch (final Exception e) {
			LOG.error("Error reading configuration of repository {}.", new Object[] { repositoryId, e });
			throw new IllegalStateException(String.format("Unable to read configuration of repository %s. %s", repositoryId, e.getMessage()));
		}
		if (collections.length == 0) {
			throw new IllegalStateException(String.format("Repository %s not configured. No collections defined!", repositoryId));
		}
		final Map<String, SolrServer[]> servers = new HashMap<String, SolrServer[]>(collections.length);
		for (final String collection : collections) {
			if (!BaseMetric.isValidId(collection)) {
				throw new IllegalStateException(String.format("Repository %s not configured correctly. Invalid chars detected in collection name '%2'. Please use only ASCII lower- and uppercase letters a-z and A-Z and/or numbers 0-9", repositoryId, collection));
			}
			final String typeStr = repositoryPreferences.get("collections/" + collection + "/" + PREF_KEY_SERVER_TYPE, null);
			final SolrServerType serverType = typeStr == null ? SolrServerType.EMBEDDED : SolrServerType.valueOf(typeStr);
			switch (serverType) {
				case EMBEDDED:
					final SolrServer embeddedServer = getEmbeddedServer(repositoryId, collection);
					servers.put(collection, new SolrServer[] { embeddedServer, embeddedServer });
					break;

				case REMOTE:
					final String urlString = repositoryPreferences.get("collections/" + collection + "/" + PREF_KEY_SERVER_URL, null);
					try {
						servers.put(collection, new SolrServer[] { new CommonsHttpSolrServer(urlString), createReadServer(urlString) });
					} catch (final MalformedURLException e) {
						throw new IllegalStateException(String.format("Repository %s not configured correctly. Server URL '%s' is invalid for collection %s. %s", repositoryId, urlString, collection, e.getMessage()));
					}
					break;

				default:
					throw new IllegalStateException(String.format("Repository %s not configured correctly. Unsupported server type %s for collection %s", repositoryId, typeStr, collection));
			}
		}
		return servers;
	}

	private SolrServer getEmbeddedServer(final String repositoryId, final String collection) {
		// compute the core name
		final String coreName = SolrActivator.getEmbeddedSolrCoreName(repositoryId, collection);
		// check core
		final SolrCore core = coreContainer.getCore(coreName);
		if (null == core) {
			throw new IllegalStateException("Solr core '" + coreName + "' not found");
		}
		core.close();
		return new EmbeddedSolrServer(coreContainer, coreName);
	}

}
