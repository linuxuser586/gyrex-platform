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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gyrex.monitoring.metrics.BaseMetric;
import org.eclipse.gyrex.monitoring.metrics.ErrorMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;
import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;

import org.eclipse.osgi.util.NLS;

public class SolrRepositoryMetrics extends MetricSet {

	private static final int IDX_ERROR = 0;
	private static final int IDX_THROUGHPUT_QUERY = 1;
	private static final int IDX_THROUGHPUT_UPDATE = 2;
	private static final int IDX_THROUGHPUT_ADMIN = 3;
	private static final int IDX_THROUGHPUT_OTHER = 4;

	private static BaseMetric[] createMetrics(final String initialStatus, final String initialStatusReason, final Collection<String> collections) {
		final List<BaseMetric> metrics = new ArrayList<BaseMetric>(2 + collections.size());
		metrics.add(new StatusMetric("status", initialStatus, initialStatusReason));
		metrics.add(new ErrorMetric("error", false));
		for (final String collection : collections) {
			// collection name is checked externally, we assume that it is valid for metrics ids at this point
			metrics.add(new ErrorMetric(collection + ".error", true)); /* IDX_ERROR */
			metrics.add(new ThroughputMetric(collection + ".query.throughput")); /* IDX_THROUGHPUT_QUERY */
			metrics.add(new ThroughputMetric(collection + ".update.throughput")); /* IDX_THROUGHPUT_UPDATE */
			metrics.add(new ThroughputMetric(collection + ".admin.throughput")); /* IDX_THROUGHPUT_ADMIN */
			metrics.add(new ThroughputMetric(collection + ".other.throughput")); /* IDX_THROUGHPUT_OTHER */
		}
		return metrics.toArray(new BaseMetric[metrics.size()]);
	}

	private static String getError(final Exception e) {
		return NLS.bind("[{0}] {1}", e.getClass().getSimpleName(), e.getMessage());
	}

	private static String getErrorDetails(final String requestInfo, final Exception e) {
		final StringWriter errorDetailsStringWriter = new StringWriter(1024);
		final PrintWriter errorDetailsWriter = new PrintWriter(errorDetailsStringWriter);
		errorDetailsWriter.append("[request] ").append(requestInfo);
		errorDetailsWriter.println();

		errorDetailsWriter.append("[").append(e.getClass().getSimpleName()).append("]  ").append(e.getMessage());
		errorDetailsWriter.println();

		Throwable cause = e.getCause();
		while ((null != cause) && (cause != e)) {
			errorDetailsWriter.append("caused by [").append(cause.getClass().getSimpleName()).append("]  ").append(cause.getMessage());
			errorDetailsWriter.println();
			cause = e.getCause();
		}

		errorDetailsWriter.flush();
		return errorDetailsStringWriter.toString();
	}

	private final StatusMetric statusMetric;
	private final ErrorMetric errorMetric;
	private final Map<String, BaseMetric[]> serverMetricsByCollection;

	protected SolrRepositoryMetrics(final String id, final String repositoryId, final String initialStatus, final String initialStatusReason, final Collection<String> collections) {
		super(id, String.format("Metrics for repository %s", repositoryId), createMetrics(initialStatus, initialStatusReason, collections));
		statusMetric = getMetric(0, StatusMetric.class);
		errorMetric = getMetric(1, ErrorMetric.class);

		serverMetricsByCollection = new HashMap<String, BaseMetric[]>(collections.size());
		int idx = 1;
		for (final String collection : collections) {
			/*@formatter:off*/
			serverMetricsByCollection.put(collection, new BaseMetric[] {
					getMetric(++idx, ErrorMetric.class),
					getMetric(++idx, ThroughputMetric.class),
					getMetric(++idx, ThroughputMetric.class),
					getMetric(++idx, ThroughputMetric.class),
					getMetric(++idx, ThroughputMetric.class)
			});
			/*@formatter:on*/
		}
	}

	/**
	 * Returns the throughput metric for admin requests.
	 * 
	 * @return the throughput metric for admin requests
	 */
	public ThroughputMetric getAdminThroughputMetric(final String collection) {
		return (ThroughputMetric) serverMetricsByCollection.get(collection)[IDX_THROUGHPUT_ADMIN];
	}

	private ErrorMetric getErrorMetric(final String collection) {
		return ((ErrorMetric) serverMetricsByCollection.get(collection)[IDX_ERROR]);
	}

	/**
	 * Returns the errorMetric.
	 * 
	 * @return the errorMetric
	 */
	public ErrorMetric getErrorMetricGlobal() {
		return errorMetric;
	}

	/**
	 * Returns the throughput metric for all other requests.
	 * 
	 * @return the throughput metric for all other requests
	 */
	public ThroughputMetric getOtherThroughputMetric(final String collection) {
		return (ThroughputMetric) serverMetricsByCollection.get(collection)[IDX_THROUGHPUT_OTHER];
	}

	/**
	 * Returns the throughput metric for queries.
	 * 
	 * @return the throughput metric for queries
	 */
	public ThroughputMetric getQueryThroughputMetric(final String collection) {
		return (ThroughputMetric) serverMetricsByCollection.get(collection)[IDX_THROUGHPUT_QUERY];
	}

	/**
	 * Returns the pool status metric.
	 * 
	 * @return the pool status metric
	 */
	public StatusMetric getStatusMetric() {
		return statusMetric;
	}

	/**
	 * Returns the throughput metric for update requests.
	 * 
	 * @return the throughput metric for update requests
	 */
	public ThroughputMetric getUpdateThroughputMetric(final String collection) {
		return (ThroughputMetric) serverMetricsByCollection.get(collection)[IDX_THROUGHPUT_UPDATE];
	}

	/**
	 * Records an exception.
	 * 
	 * @param requestInfo
	 *            information about the request
	 * @param exception
	 *            the exception
	 */
	public void recordException(final String collection, final String requestInfo, final Exception exception) {
		final String error = getError(exception);
		final String errorDetails = getErrorDetails(requestInfo, exception);
		getErrorMetricGlobal().setLastError(error, errorDetails);
		getErrorMetric(collection).setLastError(error, errorDetails);
	}

	public void setClosed(final String reason) {
		getStatusMetric().setStatus("closed", reason);
	}

}
