/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.worker;

import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.monitoring.metrics.CapacityMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;

/**
 * Metrics for {@link Scheduler}.
 */
public class WorkerEngineMetrics extends MetricSet {

	private final StatusMetric status;
	private final CapacityMetric capacity;

	public WorkerEngineMetrics() {
		super(JobsActivator.SYMBOLIC_NAME + ".worker.engine.metric", "Metrics for worker engine.", new StatusMetric("status", "created", "not initialized"), new CapacityMetric("jobs", 0));
		status = getMetric(0, StatusMetric.class);
		capacity = getMetric(1, CapacityMetric.class);
	}

	public CapacityMetric getCapacity() {
		return capacity;
	}

	public void setStatus(final String status, final String changeReason) {
		this.status.setStatus(status, changeReason);
	}

}
