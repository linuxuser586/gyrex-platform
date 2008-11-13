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
package org.eclipse.cloudfree.persistence.solr.internal;

import java.io.IOException;
import java.util.Collection;


import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.eclipse.cloudfree.persistence.storage.Repository;

/**
 * 
 */
public class SolrRepository extends Repository {

	private final SolrServer solrServer;

	SolrRepository(final String repositoryId, final EmbeddedSolrRepositoryType repositoryType, final SolrServer solrServer) throws IllegalArgumentException {
		super(repositoryId, repositoryType, new SolrRepositoryMetrics("org.eclipse.cloudfree.persistence.solr.repository." + repositoryId + repositoryId + ".metrics", "not initialized", "repository created"));
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

	public UpdateResponse commit() {
		try {
			return getSolrServer().commit(false, false);
		} catch (final SolrServerException e) {
			getSolrRepositoryMetrics().recordException("committ()", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (final IOException e) {
			getSolrRepositoryMetrics().recordException("committ()", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.persistence.storage.Repository#doClose()
	 */
	@Override
	protected void doClose() {
		getSolrRepositoryMetrics().setClosed("doClose called");
	}

	private SolrRepositoryMetrics getSolrRepositoryMetrics() {
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

	public QueryResponse query(final SolrQuery solrQuery) {
		final String query = solrQuery.toString();
		try {
			// TODO: limit should be configurable
			final int urlLengthLimit = 2000;
			if (query.length() > urlLengthLimit) {
				return getSolrServer().query(solrQuery, SolrRequest.METHOD.POST);
			} else {
				return getSolrServer().query(solrQuery, SolrRequest.METHOD.GET);
			}
		} catch (final SolrServerException e) {
			getSolrRepositoryMetrics().recordException(query, e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
