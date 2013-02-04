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
import org.eclipse.gyrex.monitoring.metrics.ErrorMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.monitoring.metrics.StatusMetric;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Metrics for {@link Scheduler}.
 */
public class ScheduleMetrics extends MetricSet {

	private final StatusMetric status;
	private final ErrorMetric lastError;

	public ScheduleMetrics(final String scheduleStoreStorageKey) {
		super(String.format(JobsActivator.SYMBOLIC_NAME + ".scheduler.schedule.%s.metric", scheduleStoreStorageKey), "Metrics for Quartz based schedule.", new StatusMetric("status", "created", "not initialized"), new ErrorMetric("lastError", 0));
		status = getMetric(0, StatusMetric.class);
		lastError = getMetric(1, ErrorMetric.class);
	}

	public void error(final String message, final Throwable t) {
		lastError.setLastError(message, ExceptionUtils.getFullStackTrace(t));
	}

	public void setStatus(final String status, final String changeReason) {
		this.status.setStatus(status, changeReason);
	}

}
