/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
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

import java.util.Hashtable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A tracker for {@link IStatus}.
 */
public class StatusTracker extends ServiceTracker<IStatus, IStatus> {

	private final DiagnosticsStatusMetrics diagnosticsStatusMetrics = new DiagnosticsStatusMetrics();
	private final CopyOnWriteArrayList<IStatus> statusList = new CopyOnWriteArrayList<IStatus>();

	private ServiceRegistration<MetricSet> metricSetRegistration;

	public StatusTracker(final BundleContext context) {
		super(context, IStatus.class, null);
	}

	@Override
	public IStatus addingService(final ServiceReference<IStatus> reference) {
		final IStatus status = super.addingService(reference);
		if (status != null) {
			statusList.add(status);
			updateStatus();
		}
		return status;
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
	public void modifiedService(final ServiceReference<IStatus> reference, final IStatus service) {
		updateStatus();
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
	public void removedService(final ServiceReference<IStatus> reference, final IStatus service) {
		statusList.remove(service);
		updateStatus();
		super.removedService(reference, service);
	}

	private void updateStatus() {
		// create multi status
		final MultiStatus systemStatus = new MultiStatus(MonitoringActivator.SYMBOLIC_NAME, 0, "System Status", null);
		for (final IStatus status : statusList) {
			systemStatus.add(status);
		}

		diagnosticsStatusMetrics.setStatus(systemStatus);
	}

}
