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

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;

/**
 * A repository provider for Solr repositories.
 */
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

	/**
	 * preference key for a Solr base URL to be used with
	 * {@link SolrServerType#REMOTE remote} Solr servers (value
	 * <code>serverUrl</code>)
	 */
	public static final String PREF_KEY_SERVER_READ_URLS = "serverReadUrls";

	/**
	 * Creates a new instance.
	 * 
	 * @param coreContainer
	 * @param adminCore
	 * @param repositoryTypeId
	 */
	public SolrRepositoryProvider() {
		super(ISolrRepositoryConstants.PROVIDER_ID, SolrServerRepository.class);
	}

	private SolrServer createLoadBalancedReadServer(final String[] readUrls) throws MalformedURLException {
		// need to set some better defaults (to mimic what's in CommonsHttpSolrServer)
		final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		connectionManager.getParams().setDefaultMaxConnectionsPerHost(200);
		connectionManager.getParams().setMaxTotalConnections(200 * readUrls.length);

		// create load balancing server
		final LBHttpSolrServer solrServerForRead = new LBHttpSolrServer(new HttpClient(connectionManager), readUrls);
		solrServerForRead.setConnectionTimeout(1000);
		solrServerForRead.setConnectionManagerTimeout(1000);
		solrServerForRead.setSoTimeout(5000);
		return solrServerForRead;
	}

	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		return new SolrRepository(repositoryId, this, createServers(repositoryId, repositoryPreferences));
	}

	private SolrServer[] createServers(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		final String typeStr = repositoryPreferences.get(PREF_KEY_SERVER_TYPE, null);
		final SolrServerType serverType = typeStr == null ? SolrServerType.EMBEDDED : SolrServerType.valueOf(typeStr);
		switch (serverType) {
			case EMBEDDED:
				final SolrServer embeddedServer = getEmbeddedServer(repositoryId);
				return new SolrServer[] { embeddedServer, embeddedServer };

			case REMOTE:
				final String masterUrlString = repositoryPreferences.get(PREF_KEY_SERVER_URL, null);
				try {
					// master server first
					final CommonsHttpSolrServer masterServer = new CommonsHttpSolrServer(masterUrlString);

					// read servers
					final SolrServer readServer;
					final String[] readUrlKeys = repositoryPreferences.getKeys(PREF_KEY_SERVER_READ_URLS);
					if ((null == readUrlKeys) || (readUrlKeys.length == 0)) {
						readServer = createSingleReadServer(masterUrlString);
					} else if (readUrlKeys.length == 1) {
						readServer = createSingleReadServer(repositoryPreferences.get(PREF_KEY_SERVER_READ_URLS + "//" + readUrlKeys[0], null));
					} else {
						// need to convert positions to URLs
						final String[] urls = new String[readUrlKeys.length];
						try {
							for (int i = 0; i < readUrlKeys.length; i++) {
								final int pos = NumberUtils.toInt(readUrlKeys[i], -1);
								urls[pos] = repositoryPreferences.get(PREF_KEY_SERVER_READ_URLS + "//" + readUrlKeys[0], null);
							}
						} catch (final IndexOutOfBoundsException e) {
							throw new IllegalStateException(String.format("Unable to read replica urls for repository %s. %s", repositoryId, e.getMessage()), e);
						}
						readServer = createLoadBalancedReadServer(urls);
					}

					// done
					return new SolrServer[] { masterServer, readServer };
				} catch (final MalformedURLException e) {
					throw new IllegalStateException(String.format("Repository %s not configured correctly. Server URL '%s' is invalid.  %s", repositoryId, masterUrlString, e.getMessage()));
				} catch (final BackingStoreException e) {
					throw new IllegalStateException(String.format("Unable to read repository settings for repository %s. %s", repositoryId, e.getMessage()), e);
				}

			default:
				throw new IllegalStateException(String.format("Repository %s not configured correctly. Unsupported server type %s", repositoryId, typeStr));
		}
	}

	private SolrServer createSingleReadServer(final String urlString) throws MalformedURLException {
		final CommonsHttpSolrServer solrServerForRead = new CommonsHttpSolrServer(urlString);
		solrServerForRead.setConnectionTimeout(1000);
		solrServerForRead.setConnectionManagerTimeout(1000l);
		solrServerForRead.setSoTimeout(5000);
		return solrServerForRead;
	}

	private SolrServer getEmbeddedServer(final String repositoryId) {
		// compute the core name
		final String coreName = SolrActivator.getEmbeddedSolrCoreName(repositoryId);
		// check core
		final CoreContainer coreContainer = SolrActivator.getInstance().getEmbeddedCoreContainer();
		final SolrCore core = coreContainer.getCore(coreName);
		if (null == core) {
			throw new IllegalStateException("Solr core '" + coreName + "' not found");
		}
		core.close();
		return new EmbeddedSolrServer(coreContainer, coreName);
	}

}
