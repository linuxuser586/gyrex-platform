/*******************************************************************************
 * Copyright (c) 2012 <enter-company-name-here> and others.
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

import java.util.Hashtable;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.eclipse.core.runtime.MultiStatus;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * A {@link StatusTracker} which exposes the system status as an overall metric.
 */
public class StatusTrackerWithMetric extends StatusTracker {

	private final DiagnosticsStatusMetrics diagnosticsStatusMetrics = new DiagnosticsStatusMetrics();
	private ServiceRegistration<MetricSet> metricSetRegistration;

	public StatusTrackerWithMetric(final BundleContext context) {
		super(context);
	}

	@Override
	public void close() {
		// close
		super.close();
		// unregister metric
		if (null != metricSetRegistration) {
			try {
				metricSetRegistration.unregister();
			} catch (final Exception e) {
				// ignore
			}
			metricSetRegistration = null;
		}
	}

	@Override
	public void open(final boolean trackAllServices) {
		// open
		super.open(trackAllServices);
		// register metric
		final Hashtable<String, Object> properties = new Hashtable<String, Object>(3);
		properties.put(Constants.SERVICE_VENDOR, "Eclipse Gyrex");
		properties.put(Constants.SERVICE_DESCRIPTION, diagnosticsStatusMetrics.getDescription());
		properties.put(Constants.SERVICE_PID, diagnosticsStatusMetrics.getId());
		metricSetRegistration = context.registerService(MetricSet.class, diagnosticsStatusMetrics, properties);
	}

	@Override
	protected void setSystemStatus(final MultiStatus systemStatus) {
		diagnosticsStatusMetrics.setStatus(systemStatus);
		super.setSystemStatus(systemStatus);
	}
}
