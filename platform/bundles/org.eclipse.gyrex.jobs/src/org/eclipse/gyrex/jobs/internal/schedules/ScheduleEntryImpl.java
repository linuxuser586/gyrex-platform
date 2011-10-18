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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.schedules;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.internal.scheduler.Schedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleEntryWorkingCopy;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronExpression;

/**
 *
 */
public class ScheduleEntryImpl implements IScheduleEntry, IScheduleEntryWorkingCopy {

	private static final String PARAMETER = "parameter";

	private static final String JOB_TYPE_ID = "jobTypeId";
	private static final String JOB_ID = "jobId";
	private static final String CRON_EXPRESSION = "cronExpression";
	private static final String ENABLED = "enabled";

	private final String id;
	private final String scheduleId;

	private boolean enabled;

	private String cronExpression;
	private String jobTypeId;
	private Map<String, String> jobParamater;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param scheduleId
	 */
	public ScheduleEntryImpl(final String id, final String scheduleId) {
		this.scheduleId = scheduleId;
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid entry id");
		}
		this.id = id;
		enabled = Boolean.TRUE;
	}

	@Override
	public String getCronExpression() {
		return cronExpression;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getJobId() {
		return scheduleId.concat("__entry__").concat(id);
	}

	@Override
	public Map<String, String> getJobParameter() {
		if (null != jobParamater) {
			return Collections.unmodifiableMap(jobParamater);
		}
		return Collections.emptyMap();
	}

	@Override
	public String getJobTypeId() {
		return jobTypeId;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	void load(final Preferences node) throws BackingStoreException {
		try {
			setCronExpression(node.get(CRON_EXPRESSION, null));
			setJobTypeId(node.get(JOB_TYPE_ID, null));
			setEnabled(node.getBoolean(ENABLED, Boolean.TRUE));
			if (node.nodeExists(PARAMETER)) {
				final Preferences paramNode = node.node(PARAMETER);
				final String[] keys = paramNode.keys();
				jobParamater = new HashMap<String, String>(keys.length);
				for (final String key : keys) {
					jobParamater.put(key, paramNode.get(key, null));
				}
			}
		} catch (final IllegalArgumentException e) {
			throw new BackingStoreException(String.format("Unable to load entry '%s'. %s", id, e.getMessage()), e);
		}
	}

	void saveWithoutFlush(final Preferences node) throws BackingStoreException {
		// ensure required values are set
		setJobTypeId(jobTypeId);
		setCronExpression(cronExpression);

		node.put(CRON_EXPRESSION, cronExpression);
		node.put(JOB_TYPE_ID, jobTypeId);
		node.put(JOB_ID, getJobId());
		node.putBoolean(ENABLED, enabled);

		if ((null != jobParamater) && !jobParamater.isEmpty()) {
			final Preferences paramNode = node.node(PARAMETER);
			// remove obsolete keys
			for (final String key : paramNode.keys()) {
				if (StringUtils.isBlank(jobParamater.get(key))) {
					paramNode.remove(key);
				}
			}
			// add updated/new paramas
			for (final String key : jobParamater.keySet()) {
				final String value = jobParamater.get(key);
				if (StringUtils.isNotBlank(value)) {
					paramNode.put(key, value);
				}
			}
		} else if (node.nodeExists(PARAMETER)) {
			node.node(PARAMETER).removeNode();
		}
	}

	@Override
	public void setCronExpression(final String cronExpression) {
		if (!CronExpression.isValidExpression(Schedule.asQuartzCronExpression(cronExpression))) {
			try {
				new CronExpression(Schedule.asQuartzCronExpression(cronExpression));
				// no exception but still invalid
				throw new IllegalArgumentException("invalid cron expression");
			} catch (final ParseException e) {
				throw new IllegalArgumentException("error parsing cron expression: " + e.getMessage(), e);
			}
		}

		this.cronExpression = cronExpression;
	}

	/**
	 * Sets the enabled flag.
	 * 
	 * @param enabled
	 *            the enabled flag to set
	 */
	@Override
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void setJobParameter(final Map<String, String> jobParameter) {
		jobParamater = new HashMap<String, String>(jobParameter);
	}

	@Override
	public void setJobTypeId(final String jobTypeId) {
		if (!IdHelper.isValidId(jobTypeId)) {
			throw new IllegalArgumentException("invalid type id");
		}
		this.jobTypeId = jobTypeId;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ScheduleEntryImpl [id=").append(id).append(", cronExpression=").append(cronExpression).append(", jobProviderId=").append(jobTypeId).append(", jobParamater=").append(jobParamater).append("]");
		return builder.toString();
	}
}
