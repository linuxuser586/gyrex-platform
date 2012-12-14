/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;

import org.eclipse.core.runtime.IStatus;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 *
 */
public class DiagnosticsStatusMetrics extends MetricSet {

	public static final String ID = MonitoringActivator.SYMBOLIC_NAME + ".diagnostics.status";

	static String getFormattedMessage(final IStatus[] children, final int ident) {
		final StrBuilder builder = new StrBuilder();
		for (final IStatus child : children) {
			builder.appendSeparator(SystemUtils.LINE_SEPARATOR);
			builder.appendPadding(ident, ' ');
			builder.append(getSeverityText(child.getSeverity())).append(": ");
			builder.append(child.getMessage());
			if (child.getCode() != 0) {
				builder.append(" (code ").append(child.getCode()).append(")");
			}
			if (child.isMultiStatus()) {
				builder.appendNewLine();
				builder.append(getFormattedMessage(child.getChildren(), ident + 2));
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
				return "UNKNOWN";
		}
	}

	private final StatusMetric statusMetric;

	public DiagnosticsStatusMetrics() {
		super(ID, "Collection of diagnostics status metrics", new StatusMetric("status", "unknown", "not initialized"));
		statusMetric = getMetric(0, StatusMetric.class);
	}

	public void setStatus(final IStatus systemStatus) {
		statusMetric.setStatus(getSeverityText(systemStatus.getSeverity()), getFormattedMessage(systemStatus.getChildren(), 0));
	}
}
