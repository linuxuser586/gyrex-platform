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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gyrex.monitoring.metrics.BaseMetric;
import org.eclipse.gyrex.monitoring.metrics.ErrorMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;
import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

public class SolrRepositoryMetrics extends MetricSet {

	private static BaseMetric[] createMetrics(final String initialStatus, final String initialStatusReason) {
		final List<BaseMetric> metrics = new ArrayList<BaseMetric>(6);
		metrics.add(new StatusMetric("status", initialStatus, initialStatusReason));
		metrics.add(new ErrorMetric("error", true)); /* IDX_ERROR */
		metrics.add(new ThroughputMetric("query.throughput")); /* IDX_THROUGHPUT_QUERY */
		metrics.add(new ThroughputMetric("update.throughput")); /* IDX_THROUGHPUT_UPDATE */
		metrics.add(new ThroughputMetric("admin.throughput")); /* IDX_THROUGHPUT_ADMIN */
		metrics.add(new ThroughputMetric("other.throughput")); /* IDX_THROUGHPUT_OTHER */
		return metrics.toArray(new BaseMetric[metrics.size()]);
	}

	private final StatusMetric statusMetric;
	private final ErrorMetric errorMetric;
	private final BaseMetric queryMetric;
	private final BaseMetric updateMetric;
	private final BaseMetric adminMetric;
	private final BaseMetric otherMetric;

	protected SolrRepositoryMetrics(final String id, final String repositoryId, final String initialStatus, final String initialStatusReason) {
		super(id, String.format("Metrics for repository %s", repositoryId), createMetrics(initialStatus, initialStatusReason));
		statusMetric = getMetric(0, StatusMetric.class);
		errorMetric = getMetric(1, ErrorMetric.class);

		queryMetric = getMetric(2, ThroughputMetric.class);
		updateMetric = getMetric(3, ThroughputMetric.class);
		adminMetric = getMetric(4, ThroughputMetric.class);
		otherMetric = getMetric(5, ThroughputMetric.class);

		/*@formatter:on*/
	}

	/**
	 * Returns the throughput metric for admin requests.
	 * 
	 * @return the throughput metric for admin requests
	 */
	public ThroughputMetric getAdminThroughputMetric() {
		return (ThroughputMetric) adminMetric;
	}

	private String getErrorDetails(final String requestInfo, final Exception exception) {
		final StrBuilder builder = new StrBuilder();

		builder.appendln("REQUEST");
		builder.appendln(requestInfo);
		builder.appendNewLine();

		builder.appendln("EXCEPTION");
		final String[] rootCauseStackTrace = ExceptionUtils.getRootCauseStackTrace(exception);
		for (final String line : rootCauseStackTrace) {
			builder.appendln(line);
		}

		return builder.toString();
	}

	/**
	 * Returns the errorMetric.
	 * 
	 * @return the errorMetric
	 */
	public ErrorMetric getErrorMetric() {
		return errorMetric;
	}

	/**
	 * Returns the throughput metric for all other requests.
	 * 
	 * @return the throughput metric for all other requests
	 */
	public ThroughputMetric getOtherThroughputMetric() {
		return (ThroughputMetric) otherMetric;
	}

	/**
	 * Returns the throughput metric for queries.
	 * 
	 * @return the throughput metric for queries
	 */
	public ThroughputMetric getQueryThroughputMetric() {
		return (ThroughputMetric) queryMetric;
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
	public ThroughputMetric getUpdateThroughputMetric() {
		return (ThroughputMetric) updateMetric;
	}

	/**
	 * Records an exception.
	 * 
	 * @param requestInfo
	 *            information about the request
	 * @param exception
	 *            the exception
	 */
	public void recordException(final String requestInfo, final Exception exception) {
		final String error = ExceptionUtils.getRootCauseMessage(exception);
		final String errorDetails = getErrorDetails(requestInfo, exception);
		getErrorMetric().setLastError(error, errorDetails);
	}

	public void setClosed(final String reason) {
		getStatusMetric().setStatus("closed", reason);
	}

}
