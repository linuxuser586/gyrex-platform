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

import org.apache.commons.lang.text.StrBuilder;

/**
 * A tracker for {@link IStatus}.
 */
public class StatusTracker extends ServiceTracker<IStatus, IStatus> {

	static String getFormattedMessage(final IStatus status, final int ident) {
		final StrBuilder builder = new StrBuilder();
		builder.appendPadding(ident, ' ');
		switch (status.getSeverity()) {
			case IStatus.CANCEL:
				builder.append("(ABORT) ");
				break;
			case IStatus.ERROR:
				builder.append("(ERROR) ");
				break;
			case IStatus.WARNING:
				builder.append("(WARNING) ");
				break;
			case IStatus.INFO:
				builder.append("(INFO) ");
				break;
			case IStatus.OK:
				builder.append("(OK) ");
				break;
		}
		builder.append(status.getMessage());
		if (status.getCode() != 0) {
			builder.append(" [error code ").append(status.getCode()).append("]");
		}
		if (status.isMultiStatus()) {
			final IStatus[] children = status.getChildren();
			for (final IStatus child : children) {
				builder.appendNewLine();
				builder.append(getFormattedMessage(child, ident + 2));
			}
		}
		return builder.toString();
	}

	private static String getSeverityText(final int severity) {
		switch (severity) {
			case IStatus.OK:
				return "OK";
			case IStatus.INFO:
				return "INFO";
			case IStatus.WARNING:
				return "WARNING";
			case IStatus.ERROR:
				return "ERROR";
			case IStatus.CANCEL:
				return "CANCEL";
			default:
				return "unknown";
		}
	}

	private final DiagnosticsStatusMetrics diagnosticsStatusMetrics;

	private ServiceRegistration<MetricSet> metricSetRegistration;

	private final CopyOnWriteArrayList<IStatus> statusList = new CopyOnWriteArrayList<IStatus>();

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public StatusTracker(final BundleContext context) {
		super(context, IStatus.class, null);
		diagnosticsStatusMetrics = new DiagnosticsStatusMetrics();
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
		// unregister metric
		synchronized (this) {
			if (null != metricSetRegistration) {
				try {
					metricSetRegistration.unregister();
				} catch (final Exception e) {
					// ignore
				}
				metricSetRegistration = null;
			}
		}
		// close
		super.close();
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
		synchronized (this) {
			if (null == metricSetRegistration) {
				final Hashtable<String, Object> properties = new Hashtable<String, Object>(3);
				properties.put(Constants.SERVICE_VENDOR, "Eclipse Gyrex");
				properties.put(Constants.SERVICE_DESCRIPTION, diagnosticsStatusMetrics.getDescription());
				properties.put(Constants.SERVICE_PID, diagnosticsStatusMetrics.getId());
				metricSetRegistration = context.registerService(MetricSet.class, diagnosticsStatusMetrics, properties);
			}
		}
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

		diagnosticsStatusMetrics.setStatus(getSeverityText(systemStatus.getSeverity()), getFormattedMessage(systemStatus, 0));
	}

}
