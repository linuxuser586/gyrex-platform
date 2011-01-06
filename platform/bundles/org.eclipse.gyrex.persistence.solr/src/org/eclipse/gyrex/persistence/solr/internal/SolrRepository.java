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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.gyrex.persistence.solr.SolrServerRepository;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;

/**
 * {@link SolrServerRepository} based on {@link EmbeddedSolrServer}.
 */
public class SolrRepository extends SolrServerRepository {

	static final int IDX_WRITE_SERVER = 0;
	static final int IDX_READ_SERVER = 1;

	private final Map<String, SolrServer[]> serversByCollection;
	private final String defaultCollection;

	SolrRepository(final String repositoryId, final SolrRepositoryProvider repositoryType, final Map<String, SolrServer[]> serversByCollection, final String defaultCollection) throws IllegalArgumentException {
		super(repositoryId, repositoryType, new SolrRepositoryMetrics(createMetricsId(repositoryType, repositoryId), repositoryId, "open", "repository created", serversByCollection.keySet()));
		if (StringUtils.isBlank(defaultCollection)) {
			throw new IllegalArgumentException("invalid default collection: " + defaultCollection);
		}
		this.defaultCollection = defaultCollection;

		// wrap servers to collect metrics
		this.serversByCollection = new HashMap<String, SolrServer[]>(serversByCollection.size());
		final SolrRepositoryMetrics metrics = getSolrRepositoryMetrics();
		for (final Entry<String, SolrServer[]> entry : serversByCollection.entrySet()) {
			final String collection = entry.getKey();
			final SolrServerWithMetrics writeServer = new SolrServerWithMetrics(entry.getValue()[IDX_WRITE_SERVER], collection, metrics);
			final SolrServerWithMetrics readServer = null != entry.getValue()[IDX_READ_SERVER] ? new SolrServerWithMetrics(entry.getValue()[IDX_READ_SERVER], collection, metrics) : writeServer;
			this.serversByCollection.put(collection, new SolrServer[] { writeServer, readServer });
		}
	}

	@Override
	protected void doClose() {
		getSolrRepositoryMetrics().setClosed("doClose called");
	}

	public SolrRepositoryMetrics getSolrRepositoryMetrics() {
		return (SolrRepositoryMetrics) getMetrics();
	}

	@Override
	public SolrServer getSolrServer(final String key) throws IllegalStateException, IllegalArgumentException {
		if (isClosed()) {
			throw new IllegalStateException("repository '" + getRepositoryId() + "' closed");
		}
		return serversByCollection.get(sanitizeCollection(key))[IDX_WRITE_SERVER];
	}

	@Override
	public SolrServer getSolrServerOptimizedForQuery(final String key) throws IllegalStateException, IllegalArgumentException {
		if (isClosed()) {
			throw new IllegalStateException("repository '" + getRepositoryId() + "' closed");
		}
		return serversByCollection.get(sanitizeCollection(key))[IDX_READ_SERVER];
	}

	private String sanitizeCollection(final String key) {
		return !StringUtils.isEmpty(key) ? key : defaultCollection;
	}
}
