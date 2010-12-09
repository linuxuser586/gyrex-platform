/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.internal.mbeans;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;

/**
 * {@link MetricSetJmxMBean} implementation.
 */
public class MetricSetJmx implements MetricSetJmxMBean {
	private final MetricSet metricSet;

	/**
	 * Creates a new instance.
	 */
	public MetricSetJmx(final MetricSet metricSet) {
		this.metricSet = metricSet;
	}

	@Override
	public String getId() {
		return metricSet.getId();
	}

}
