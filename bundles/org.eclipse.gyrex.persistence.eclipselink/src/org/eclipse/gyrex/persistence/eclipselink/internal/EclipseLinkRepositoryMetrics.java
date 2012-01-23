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
package org.eclipse.gyrex.persistence.eclipselink.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gyrex.monitoring.metrics.BaseMetric;
import org.eclipse.gyrex.monitoring.metrics.ErrorMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

/**
 *
 */
public class EclipseLinkRepositoryMetrics extends MetricSet {

	private static BaseMetric[] createMetrics(final String initialStatus, final String initialStatusReason) {
		final List<BaseMetric> metrics = new ArrayList<BaseMetric>(6);
		metrics.add(new StatusMetric("status", initialStatus, initialStatusReason));
		metrics.add(new ErrorMetric("error", 5)); /* IDX_ERROR */
		return metrics.toArray(new BaseMetric[metrics.size()]);
	}

	private static Map<String, String> createProperties(final String repositoryId, final RepositoryProvider repositoryProvider) {
		final HashMap<String, String> properties = new HashMap<String, String>(4);
		properties.put("repository.id", repositoryId);
		properties.put("repository.provider.id", repositoryProvider.getProviderId());
		return properties;
	}

	private final StatusMetric statusMetric;
	private final ErrorMetric errorMetric;

	EclipseLinkRepositoryMetrics(final String id, final String repositoryId, final RepositoryProvider repositoryProvider, final String initialStatus, final String initialStatusReason) {
		super(id, String.format("Repository metrics for EclipseLink repository '%s'", repositoryId), createProperties(repositoryId, repositoryProvider), createMetrics(initialStatus, initialStatusReason));
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
	 * Returns the statusMetric.
	 * 
	 * @return the statusMetric
	 */
	public StatusMetric getStatusMetric() {
		return statusMetric;
	}

	public void setClosed(final String reason) {
		getStatusMetric().setStatus("closed", reason);
	}

}
