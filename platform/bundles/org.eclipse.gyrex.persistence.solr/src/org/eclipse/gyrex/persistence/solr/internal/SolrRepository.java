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

import org.eclipse.gyrex.persistence.solr.SolrServerRepository;

import org.eclipse.osgi.util.NLS;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;

/**
 * {@link SolrServerRepository} based on {@link EmbeddedSolrServer}.
 */
public class SolrRepository extends SolrServerRepository {

	static final int IDX_WRITE_SERVER = 0;
	static final int IDX_READ_SERVER = 1;

	private final SolrServer[] solrServers;

	SolrRepository(final String repositoryId, final SolrRepositoryProvider repositoryType, final SolrServer[] solrServers) throws IllegalArgumentException {
		super(repositoryId, repositoryType, new SolrRepositoryMetrics(createMetricsId(repositoryType, repositoryId), repositoryId, "open", "repository created"));

		final SolrRepositoryMetrics metrics = getSolrRepositoryMetrics();
		final SolrServerWithMetrics writeServer = new SolrServerWithMetrics(solrServers[IDX_WRITE_SERVER], metrics);
		final SolrServerWithMetrics readServer = null != solrServers[IDX_READ_SERVER] ? new SolrServerWithMetrics(solrServers[IDX_READ_SERVER], metrics) : writeServer;
		this.solrServers = new SolrServer[] { writeServer, readServer };

	}

	@Override
	protected void doClose() {
		getSolrRepositoryMetrics().setClosed("doClose called");
	}

	private SolrServer[] getServers() {
		if (solrServers == null) {
			throw new IllegalArgumentException(NLS.bind("Repository {1} isn't configured properly. No solr server found", getRepositoryId()));
		}
		return solrServers;
	}

	public SolrRepositoryMetrics getSolrRepositoryMetrics() {
		return (SolrRepositoryMetrics) getMetrics();
	}

	@Override
	public SolrServer getSolrServer() throws IllegalStateException {
		if (isClosed()) {
			throw new IllegalStateException("repository '" + getRepositoryId() + "' closed");
		}
		return getServers()[IDX_WRITE_SERVER];
	}

	@Override
	public SolrServer getSolrServerOptimizedForQuery() throws IllegalStateException {
		if (isClosed()) {
			throw new IllegalStateException("repository '" + getRepositoryId() + "' closed");
		}
		return getServers()[IDX_READ_SERVER];
	}
}
