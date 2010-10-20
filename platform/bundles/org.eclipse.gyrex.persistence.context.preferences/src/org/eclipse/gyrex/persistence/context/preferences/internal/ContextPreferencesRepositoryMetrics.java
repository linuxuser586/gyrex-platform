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
package org.eclipse.gyrex.persistence.context.preferences.internal;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;

/**
 * Metrics for {@link ContextPreferencesRepositoryImpl}.
 */
public class ContextPreferencesRepositoryMetrics extends MetricSet {

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param metrics
	 */
	protected ContextPreferencesRepositoryMetrics(final String id) {
		super(id, new ThroughputMetric(id.concat(".reads")), new ThroughputMetric(id.concat(".writes")));
	}

}
