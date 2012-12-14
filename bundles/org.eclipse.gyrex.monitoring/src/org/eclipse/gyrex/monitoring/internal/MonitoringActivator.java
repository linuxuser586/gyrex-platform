/*******************************************************************************
 * Copyright (c) 2008, 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class MonitoringActivator extends BaseBundleActivator {

	/** plug-in id */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.monitoring";

	private MetricSetTracker metricSetTracker;
	private StatusTrackerWithMetric statusTracker;

	/**
	 * Creates a new instance.
	 */
	public MonitoringActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		// track metrics
		metricSetTracker = new MetricSetTracker(context);
		metricSetTracker.open();

		// track status
		statusTracker = new StatusTrackerWithMetric(context);
		statusTracker.open();
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		metricSetTracker.close();
		metricSetTracker = null;

		statusTracker.close();
		statusTracker = null;
	}
}
