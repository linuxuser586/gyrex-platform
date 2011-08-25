/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Mike Tschierschke - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.manager;

import java.util.Collections;
import java.util.Map;

import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.internal.JobsActivator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.commons.lang.time.DateFormatUtils;

/**
 * Simple implementation of a {@link IJob} without any business logic.
 */
public class JobImpl implements IJob {

	private String id;
	private String typeId;
	private Map<String, String> parameter;

	private long lastStart;
	private long lastSuccessfulFinish;
	private JobState state;

	private long lastResultTimestamp;
	private IStatus lastResult;

	/**
	 * Creates a new instance.
	 */
	public JobImpl() {
		lastStart = lastSuccessfulFinish = -1;
	}

	/**
	 * Returns the jobId.
	 * 
	 * @return the jobId
	 */
	public String getId() {
		return id;
	}

	@Override
	public IStatus getLastResult() {
		return lastResult;
	}

	/**
	 * Returns the lastResultTimestamp.
	 * 
	 * @return the lastResultTimestamp
	 */
	public long getLastResultTimestamp() {
		return lastResultTimestamp;
	}

	/**
	 * Returns the lastStart.
	 * 
	 * @return the lastStart
	 */
	public long getLastStart() {
		return lastStart;
	}

	/**
	 * Returns the lastSuccessfullFinish.
	 * 
	 * @return the lastSuccessfullFinish
	 */
	public long getLastSuccessfulFinish() {
		return lastSuccessfulFinish;
	}

	/**
	 * Returns the jobParameter.
	 * 
	 * @return the jobParameter
	 */
	public Map<String, String> getParameter() {
		final Map<String, String> map = parameter;
		if (null == map) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Returns the jobStatus.
	 * 
	 * @return the jobStatus
	 */
	public JobState getState() {
		final JobState jobState = state;
		if (null == jobState) {
			return JobState.NONE;
		}

		return state;
	}

	/**
	 * Returns the jobTypeId.
	 * 
	 * @return the jobTypeId
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * Sets the jobId.
	 * 
	 * @param id
	 *            the jobId to set
	 */
	public void setId(final String id) {
		this.id = id;
	}

	public void setLastResult(final long timestamp, final int severity, final String message) {
		lastResultTimestamp = timestamp;
		try {
			lastResult = new Status(severity, JobsActivator.SYMBOLIC_NAME, message);
		} catch (final Exception e) {
			lastResult = new Status(IStatus.CANCEL, JobsActivator.SYMBOLIC_NAME, String.format("Error reading result. %s", e.getMessage()));
		}
	}

	/**
	 * Sets the lastStart.
	 * 
	 * @param lastStart
	 *            the lastStart to set
	 */
	public void setLastStart(final long lastStart) {
		this.lastStart = lastStart;
	}

	/**
	 * Sets the lastSuccessfullFinish.
	 * 
	 * @param lastSuccessfullFinish
	 *            the lastSuccessfullFinish to set
	 */
	public void setLastSuccessfulFinish(final long lastSuccessfullFinish) {
		lastSuccessfulFinish = lastSuccessfullFinish;
	}

	/**
	 * Sets the jobParameter.
	 * 
	 * @param parameter
	 *            the jobParameter to set
	 */
	public void setParameter(final Map<String, String> parameter) {
		this.parameter = parameter;
	}

	/**
	 * Sets the jobStatus.
	 * 
	 * @param status
	 *            the jobStatus to set
	 */
	public void setStatus(final JobState status) {
		state = status;
	}

	/**
	 * Sets the jobTypeId.
	 * 
	 * @param typeId
	 *            the jobTypeId to set
	 */
	public void setTypeId(final String typeId) {
		this.typeId = typeId;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Job [").append(id).append(" (type ").append(typeId).append(")");

		final JobState state = getState();
		if (state != JobState.NONE) {
			builder.append(", ").append(state);
		}
		final long start = lastStart;
		if (start > 0) {
			builder.append(", last started ").append(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(start));
		}

		final IStatus result = lastResult;
		if (result != null) {
			builder.append(", last result ").append(result);
		}

		return builder.append("]").toString();
	}

}
