/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
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

import java.io.IOException;
import java.util.Collection;

import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;
import org.eclipse.gyrex.persistence.storage.Repository;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 */
public class SolrRepository extends Repository {

	private final SolrServer solrServer;

	SolrRepository(final String repositoryId, final EmbeddedSolrRepositoryType repositoryType, final SolrServer solrServer) throws IllegalArgumentException {
		super(repositoryId, repositoryType, new SolrRepositoryMetrics("org.eclipse.gyrex.persistence.solr.repository." + repositoryId + repositoryId + ".metrics", "open", "repository created"));
		this.solrServer = solrServer;
	}

	public UpdateResponse add(final Collection<SolrInputDocument> docs) {
		try {
			return getSolrServer().add(docs);
		} catch (final SolrServerException e) {
			getSolrRepositoryMetrics().recordException("add()", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (final IOException e) {
			getSolrRepositoryMetrics().recordException("add()", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public UpdateResponse add(final Collection<SolrInputDocument> docs, final int commitWithi) {
		try {
			final UpdateRequest req = new UpdateRequest();
			req.add(docs);
			req.setCommitWithin(commitWithi); // TODO: should be configurable
			return req.process(getSolrServer());
		} catch (final SolrServerException e) {
			getSolrRepositoryMetrics().recordException("add()", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (final IOException e) {
			getSolrRepositoryMetrics().recordException("add()", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public UpdateResponse add(final SolrInputDocument doc) {
		try {
			return getSolrServer().add(doc);
		} catch (final SolrServerException e) {
			getSolrRepositoryMetrics().recordException("add()", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (final IOException e) {
			getSolrRepositoryMetrics().recordException("add()", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public UpdateResponse commit() {
		return commit(false, false);
	}

	public UpdateResponse commit(final boolean waitFlush, final boolean waitSearcher) {
		try {
			return getSolrServer().commit(waitFlush, waitSearcher);
		} catch (final SolrServerException e) {
			getSolrRepositoryMetrics().recordException("commit()", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (final IOException e) {
			getSolrRepositoryMetrics().recordException("commit()", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.persistence.storage.Repository#doClose()
	 */
	@Override
	protected void doClose() {
		getSolrRepositoryMetrics().setClosed("doClose called");
	}

	/**
	 * Returns the repository metrics.
	 * 
	 * @return the repository metrics
	 */
	public SolrRepositoryMetrics getSolrRepositoryMetrics() {
		return (SolrRepositoryMetrics) getMetrics();
	}

	/**
	 * Returns the SolrServer object.
	 * 
	 * @return the SolrServer
	 */
	protected SolrServer getSolrServer() {
		return solrServer;
	}

	public UpdateResponse optimize() {
		return optimize(false, false);
	}

	public UpdateResponse optimize(final boolean waitFlush, final boolean waitSearcher) {
		try {
			return getSolrServer().optimize(waitFlush, waitSearcher);
		} catch (final SolrServerException e) {
			getSolrRepositoryMetrics().recordException("optimize()", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (final IOException e) {
			getSolrRepositoryMetrics().recordException("optimize()", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public QueryResponse query(final SolrQuery solrQuery) {
		final String query = solrQuery.toString();
		final ThroughputMetric queryThroughputMetric = getSolrRepositoryMetrics().getQueryThroughputMetric();
		final long requestStarted = queryThroughputMetric.requestStarted();
		try {
			// TODO: limit should be configurable
			try {
				final int urlLengthLimit = 2000;
				if (query.length() > urlLengthLimit) {
					return getSolrServer().query(solrQuery, SolrRequest.METHOD.POST);
				} else {
					return getSolrServer().query(solrQuery, SolrRequest.METHOD.GET);
				}
			} finally {
				queryThroughputMetric.requestFinished(0, System.currentTimeMillis() - requestStarted);
			}
		} catch (final SolrServerException e) {
			queryThroughputMetric.requestFailed();
			getSolrRepositoryMetrics().recordException(query, e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
