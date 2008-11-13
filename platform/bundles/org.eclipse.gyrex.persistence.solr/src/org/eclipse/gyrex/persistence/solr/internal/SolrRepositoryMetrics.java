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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;


import org.apache.solr.client.solrj.SolrServerException;
import org.eclipse.cloudfree.monitoring.metrics.BaseMetric;
import org.eclipse.cloudfree.monitoring.metrics.ErrorMetric;
import org.eclipse.cloudfree.monitoring.metrics.MetricSet;
import org.eclipse.cloudfree.monitoring.metrics.StatusMetric;

/**
 * 
 */
public class SolrRepositoryMetrics extends MetricSet {

	private static String getError(final IOException ioException) {
		return MessageFormat.format("[IOException] {0}", ioException.getMessage());
	}

	private static String getError(final SolrServerException sqlException) {
		return MessageFormat.format("[SolrServerException] {0}", sqlException.getMessage());
	}

	private static String getErrorDetails(final String query, final IOException ioException) {
		final StringWriter errorDetailsStringWriter = new StringWriter(1024);
		final PrintWriter errorDetailsWriter = new PrintWriter(errorDetailsStringWriter);
		errorDetailsWriter.append("[query] ").append(query);
		errorDetailsWriter.println();

		errorDetailsWriter.append("[IOException]  ").append(ioException.getMessage());//.append("; ");
		errorDetailsWriter.println();

		errorDetailsWriter.flush();
		return errorDetailsStringWriter.toString();
	}

	private static String getErrorDetails(final String query, final SolrServerException solrServerException) {
		final StringWriter errorDetailsStringWriter = new StringWriter(1024);
		final PrintWriter errorDetailsWriter = new PrintWriter(errorDetailsStringWriter);
		errorDetailsWriter.append("[query] ").append(query);
		errorDetailsWriter.println();

		errorDetailsWriter.append("[SolrServerException]  ").append(solrServerException.getMessage());//.append("; ");
		errorDetailsWriter.println();

		final Throwable rootCause = solrServerException.getRootCause();
		if ((null != rootCause) && (rootCause != solrServerException)) {
			errorDetailsWriter.append("[").append(rootCause.getClass().getSimpleName()).append("]  ").append(rootCause.getMessage());//.append("; ");
			errorDetailsWriter.println();
		}

		errorDetailsWriter.flush();
		return errorDetailsStringWriter.toString();
	}

	private final StatusMetric statusMetric;

	private final ErrorMetric errorMetric;

	protected SolrRepositoryMetrics(final String id, final String initialStatus, final String initialStatusReason) {
		super(id, new BaseMetric[] { new StatusMetric(id + ".status", initialStatus, initialStatusReason), new ErrorMetric(id + ".error", true) });
		statusMetric = getMetric(0, StatusMetric.class);
		errorMetric = getMetric(1, ErrorMetric.class);
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
	 * Returns the pool status metric.
	 * 
	 * @return the pool status metric
	 */
	public StatusMetric getStatusMetric() {
		return statusMetric;
	}

	/**
	 * Records a {@link IOException}
	 * 
	 * @param e
	 */
	public void recordException(final String query, final IOException ioException) {
		final String error = getError(ioException);
		final String errorDetails = getErrorDetails(query, ioException);
		getErrorMetric().setLastError(error, errorDetails);
	}

	/**
	 * Records a {@link SolrServerException}
	 * 
	 * @param e
	 */
	public void recordException(final String query, final SolrServerException solrServerException) {
		final String error = getError(solrServerException);
		final String errorDetails = getErrorDetails(query, solrServerException);
		getErrorMetric().setLastError(error, errorDetails);
	}

	public void setClosed(final String reason) {
		getStatusMetric().setStatus("closed", reason);
	}

}
