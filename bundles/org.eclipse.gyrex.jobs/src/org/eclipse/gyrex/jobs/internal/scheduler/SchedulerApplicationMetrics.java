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
package org.eclipse.gyrex.jobs.internal.scheduler;

import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;

/**
 * Metrics for {@link Scheduler}.
 */
public class SchedulerApplicationMetrics extends MetricSet {

	private final StatusMetric status;
	private final StatusMetric applicationStatus;

	public SchedulerApplicationMetrics() {
		super(JobsActivator.SYMBOLIC_NAME + ".scheduler.metric", "Metrics for scheduler application.", new StatusMetric("status", "created", "not initialized"), new StatusMetric("applicationStatus", "created", "not initialized"));
		status = getMetric(0, StatusMetric.class);
		applicationStatus = getMetric(1, StatusMetric.class);
	}

	public void setApplicationStatus(final String status, final String changeReason) {
		applicationStatus.setStatus(status, changeReason);
	}

	public void setStatus(final String status, final String changeReason) {
		this.status.setStatus(status, changeReason);
	}

}
