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
package org.eclipse.gyrex.jobs.internal.schedules;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.internal.scheduler.Schedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntryWorkingCopy;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronExpression;

/**
 *
 */
public class ScheduleEntryImpl implements IScheduleEntry, IScheduleEntryWorkingCopy {

	private static final String PARAMETER = "parameter";

	private static final String JOB_PROVIDER_ID = "jobProviderId";
	private static final String CRON_EXPRESSION = "cronExpression";
	private final Preferences node;

	private final String id;
	private String cronExpression;

	private String jobProviderId;
	private Map<String, String> jobParamater;

	/**
	 * Creates a new instance.
	 * 
	 * @param node
	 * @throws BackingStoreException
	 */
	public ScheduleEntryImpl(final Preferences node) throws BackingStoreException {
		this.node = node;
		id = node.name();
		load();
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
	public Map<String, String> getJobParameter() {
		if (null != jobParamater) {
			return Collections.unmodifiableMap(jobParamater);
		}
		return Collections.emptyMap();
	}

	@Override
	public String getJobProviderId() {
		return jobProviderId;
	}

	void load() throws BackingStoreException {
		cronExpression = node.get(CRON_EXPRESSION, null);
		jobProviderId = node.get(JOB_PROVIDER_ID, null);
		if (node.nodeExists(PARAMETER)) {
			final Preferences paramNode = node.node(PARAMETER);
			final String[] keys = paramNode.childrenNames();
			jobParamater = new HashMap<String, String>(keys.length);
			for (final String key : keys) {
				jobParamater.put(key, paramNode.get(key, null));
			}
		}
	}

	void saveWithoutFlush() throws BackingStoreException {
		node.put(CRON_EXPRESSION, cronExpression);
		node.put(JOB_PROVIDER_ID, jobProviderId);

		if ((null != jobParamater) && !jobParamater.isEmpty()) {
			final Preferences paramNode = node.node(PARAMETER);
			// remove obsolete keys
			for (final String key : paramNode.childrenNames()) {
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
			throw new IllegalArgumentException("invalid cron expression; see http://en.wikipedia.org/wiki/Cron#CRON_expression");
		}

		this.cronExpression = cronExpression;
	}

	@Override
	public void setJobParameter(final Map<String, String> jobParameter) {
		jobParamater = new HashMap<String, String>(jobParameter);
	}

	@Override
	public void setJobProviderId(final String jobProviderId) {
		if (!IdHelper.isValidId(jobProviderId)) {
			throw new IllegalArgumentException("invalid provider id");
		}
		this.jobProviderId = jobProviderId;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ScheduleEntryImpl [id=").append(id).append(", cronExpression=").append(cronExpression).append(", jobProviderId=").append(jobProviderId).append(", jobParamater=").append(jobParamater).append("]");
		return builder.toString();
	}
}
