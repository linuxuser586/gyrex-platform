/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
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

import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.http.jetty.internal.HttpJettyActivator;
import org.eclipse.gyrex.monitoring.metrics.ErrorMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;
import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Metrics for {@link ApplicationHandler}
 */
public class ApplicationHandlerMetrics extends MetricSet {

	private final StatusMetric statusMetric;
	private final ThroughputMetric requestsMetric;
	private final ErrorMetric errorsMetric;

	/**
	 * Creates a new instance.
	 */
	protected ApplicationHandlerMetrics(final String applicationId) {
		super(String.format(HttpJettyActivator.SYMBOLIC_NAME + ".handler.application.%s.metric", applicationId), String.format("Metrics for Jetty Server requests handled by Gyrex application '%s'.", applicationId), new StatusMetric("status", "created", "not initialized"), new ThroughputMetric("requests", TimeUnit.NANOSECONDS), new ErrorMetric("errors", 5));
		statusMetric = getMetric(0, StatusMetric.class);
		requestsMetric = getMetric(1, ThroughputMetric.class);
		errorsMetric = getMetric(2, ErrorMetric.class);
	}

	public void error(final int status, final String reason) {
		errorsMetric.setLastError("Error " + status, StringUtils.trimToEmpty(reason));
	}

	public void error(final String message, final Throwable t) {
		errorsMetric.setLastError(message, ExceptionUtils.getFullStackTrace(t));
	}

	public ThroughputMetric getRequestsMetric() {
		return requestsMetric;
	}

	public void setStatus(final String status, final String reasons) {
		statusMetric.setStatus(status, reasons);
	}
}
