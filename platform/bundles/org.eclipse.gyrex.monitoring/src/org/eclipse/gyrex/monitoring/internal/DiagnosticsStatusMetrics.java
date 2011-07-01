/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.internal;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;

/**
 *
 */
public class DiagnosticsStatusMetrics extends MetricSet {

	public static final String ID = MonitoringActivator.SYMBOLIC_NAME + ".diagnostics.status";
	private final StatusMetric statusMetric;

	/**
	 * Creates a new instance.
	 */
	public DiagnosticsStatusMetrics() {
		super(ID, "Collection of diagnostics status metrics", new StatusMetric("status", "unknown", "not initialized"));
		statusMetric = getMetric(0, StatusMetric.class);
	}

	/**
	 * @param status
	 * @param statusChangeReason
	 * @see org.eclipse.gyrex.monitoring.metrics.StatusMetric#setStatus(java.lang.String,
	 *      java.lang.String)
	 */
	public void setStatus(final String status, final String statusChangeReason) {
		statusMetric.setStatus(status, statusChangeReason);
	}

}
