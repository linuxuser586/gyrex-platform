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
package org.eclipse.gyrex.http.jetty.internal.app;

import org.eclipse.gyrex.http.jetty.internal.HttpJettyActivator;
import org.eclipse.gyrex.monitoring.metrics.CapacityMetric;
import org.eclipse.gyrex.monitoring.metrics.ErrorMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;
import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Metrics for {@link ApplicationHandlerCollection}
 */
public class ApplicationHandlerCollectionMetrics extends MetricSet {

	private final StatusMetric statusMetric;
	private final ThroughputMetric requestsMetric;
	private final ErrorMetric errorsMetric;
	private final CapacityMetric applicationsMetric;

	/**
	 * Creates a new instance.
	 */
	protected ApplicationHandlerCollectionMetrics() {
		super(HttpJettyActivator.SYMBOLIC_NAME + ".handler.applications", "Metrics for Jetty Server requests handled by Gyrex applications.", new StatusMetric("status", "created", "not initialized"), new ThroughputMetric("requests"), new ErrorMetric("errors", false), new CapacityMetric("applications", -1));
		statusMetric = getMetric(0, StatusMetric.class);
		requestsMetric = getMetric(1, ThroughputMetric.class);
		errorsMetric = getMetric(2, ErrorMetric.class);
		applicationsMetric = getMetric(3, CapacityMetric.class);
	}

	public void error(final int status, final String reason) {
		errorsMetric.setLastError("Error " + status, StringUtils.trimToEmpty(reason));
	}

	public void error(final String message, final Exception e) {
		errorsMetric.setLastError(message, ExceptionUtils.getFullStackTrace(e));
	}

	public CapacityMetric getApplicationsMetric() {
		return applicationsMetric;
	}

	/**
	 * Returns the requestsMetric.
	 * 
	 * @return the requestsMetric
	 */
	public ThroughputMetric getRequestsMetric() {
		return requestsMetric;
	}

	public void setStatus(final String status, final String reasons) {
		statusMetric.setStatus(status, reasons);
	}
}
