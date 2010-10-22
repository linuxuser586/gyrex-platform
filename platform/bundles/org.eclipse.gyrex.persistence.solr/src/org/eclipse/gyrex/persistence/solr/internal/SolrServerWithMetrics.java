/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;

/**
 * A {@link SolrServer} implementation collecting metrics.
 */
public class SolrServerWithMetrics extends SolrServer {

	/** serialVersionUID */
	private static final long serialVersionUID = 7409277302681086835L;

	private final SolrServer server;

	private final SolrRepositoryMetrics metrics;

	/**
	 * Creates a new instance.
	 */
	public SolrServerWithMetrics(final SolrServer server, final SolrRepositoryMetrics metrics) {
		this.server = server;
		this.metrics = metrics;
	}

	private ThroughputMetric getRequestMetric(final SolrRequest request) {
		if (request instanceof QueryRequest) {
			return metrics.getQueryThroughputMetric();
		} else if (request instanceof AbstractUpdateRequest) {
			return metrics.getUpdateThroughputMetric();
		} else if (request instanceof CoreAdminRequest) {
			return metrics.getAdminThroughputMetric();
		} else {
			return metrics.getOtherThroughputMetric();
		}
	}

	private void recordException(final SolrRequest request, final Exception e) {
		final StringBuilder requestInfo = new StringBuilder();
		requestInfo.append(request.getClass().getSimpleName());
		requestInfo.append('[');
		requestInfo.append(request.getMethod());
		requestInfo.append(' ');
		requestInfo.append(request.getPath());
		requestInfo.append(' ');
		requestInfo.append(request.getParams().toNamedList());
		requestInfo.append(']');
		metrics.recordException(requestInfo.toString(), e);
	}

	@Override
	public NamedList<Object> request(final SolrRequest request) throws SolrServerException, IOException {
		final ThroughputMetric requestMetric = getRequestMetric(request);
		final long requestStarted = requestMetric.requestStarted();
		try {
			final NamedList<Object> result = server.request(request);
			requestMetric.requestFinished(result.size(), System.currentTimeMillis() - requestStarted);
			return result;
		} catch (final IOException e) {
			requestMetric.requestFailed();
			recordException(request, e);
			throw e;
		} catch (final SolrServerException e) {
			requestMetric.requestFailed();
			recordException(request, e);
			throw e;
		}
	}

}
