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

import org.eclipse.gyrex.persistence.solr.SolrServerRepository;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;

/**
 * {@link SolrServerRepository} based on {@link EmbeddedSolrServer}.
 */
public class SolrRepository extends SolrServerRepository {

	private final SolrServer solrServer;
	private final SolrServer solrServerForQuery;

	SolrRepository(final String repositoryId, final SolrRepositoryProvider repositoryType, final SolrServer solrServer) throws IllegalArgumentException {
		this(repositoryId, repositoryType, solrServer, null);
	}

	SolrRepository(final String repositoryId, final SolrRepositoryProvider repositoryType, final SolrServer solrServer, final SolrServer solrServerForQuery) throws IllegalArgumentException {
		super(repositoryId, repositoryType, new SolrRepositoryMetrics(createMetricsId(repositoryType, repositoryId), "open", "repository created"));
		this.solrServer = new SolrServerWithMetrics(solrServer, getSolrRepositoryMetrics());
		this.solrServerForQuery = (solrServerForQuery != null) && (solrServerForQuery != solrServer) ? new SolrServerWithMetrics(solrServerForQuery, getSolrRepositoryMetrics()) : this.solrServer;
	}

	@Override
	protected void doClose() {
		getSolrRepositoryMetrics().setClosed("doClose called");
	}

	public SolrRepositoryMetrics getSolrRepositoryMetrics() {
		return (SolrRepositoryMetrics) getMetrics();
	}

	@Override
	public SolrServer getSolrServer() throws IllegalStateException {
		if (isClosed()) {
			throw new IllegalStateException("repository '" + getRepositoryId() + "' closed");
		}
		return solrServer;
	}

	@Override
	public SolrServer getSolrServerOptimizedForQuery() throws IllegalStateException {
		if (isClosed()) {
			throw new IllegalStateException("repository '" + getRepositoryId() + "' closed");
		}
		return solrServerForQuery;
	}
}
