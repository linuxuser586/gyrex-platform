/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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

	private static final char SEPARATOR_CHAR = ',';

	private static final String PARAMETER = "parameter";

	private static final String JOB_TYPE_ID = "jobTypeId";
	private static final String CRON_EXPRESSION = "cronExpression";
	private static final String ENABLED = "enabled";
	private static final String PRECEDING_ENTRIES = "precedingEntries";
	private static final String QUEUE_ID = "queueId";

	private static void setIfNotNullOrRemoveIfNull(final Preferences node, final String key, final String value) {
		if (value != null) {
			node.put(key, value);
		} else {
			node.remove(key);
		}
	}

	public static void validateCronExpression(final String expression) throws IllegalArgumentException {
		if (CronExpression.isValidExpression(Schedule.asQuartzCronExpression(expression)))
			return;

		try {
			new CronExpression(Schedule.asQuartzCronExpression(expression));

			// no exception but still invalid
			throw new IllegalArgumentException("invalid cron expression: " + expression);
		} catch (final ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private final String id;
	private ScheduleImpl schedule;
	private boolean enabled;
	private String cronExpression;
	private String jobTypeId;
	private Map<String, String> jobParamater;
	private String queueId;

	private Collection<String> precedingEntries;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param schedule
	 */
	public ScheduleEntryImpl(final String id, final ScheduleImpl schedule) {
		this.schedule = schedule;
		if (!IdHelper.isValidId(id))
			throw new IllegalArgumentException("invalid entry id");
		this.id = id;
		enabled = true; // default is enabled (backwards compatibility)
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
		return schedule.getId().concat("__entry__").concat(id);
	}

	@Override
	public Map<String, String> getJobParameter() {
		if (null != jobParamater)
			return Collections.unmodifiableMap(jobParamater);
		return Collections.emptyMap();
	}

	@Override
	public String getJobTypeId() {
		return jobTypeId;
	}

	@Override
	public Collection<String> getPrecedingEntries() {
		final Collection<String> ids = precedingEntries;
		if (ids == null)
			return Collections.emptyList();

		return Collections.unmodifiableCollection(ids);
	}

	@Override
	public String getQueueId() {
		return queueId;
	}

	@Override
	public ScheduleImpl getSchedule() {
		return schedule;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	void load(final Preferences node) throws BackingStoreException {
		try {
			setJobTypeId(node.get(JOB_TYPE_ID, null));
			setEnabled(node.getBoolean(ENABLED, true /* default true (backwards compatibility) */));
			if (node.nodeExists(PARAMETER)) {
				final Preferences paramNode = node.node(PARAMETER);
				final String[] keys = paramNode.keys();
				jobParamater = new HashMap<String, String>(keys.length);
				for (final String key : keys) {
					jobParamater.put(key, paramNode.get(key, null));
				}
			}
			final String expression = node.get(CRON_EXPRESSION, null);
			if (expression != null) {
				setCronExpression(expression);
			}
			setQueueId(node.get(QUEUE_ID, null));
			setPrecedingEntries(StringUtils.split(node.get(PRECEDING_ENTRIES, null), SEPARATOR_CHAR));
		} catch (final IllegalArgumentException e) {
			throw new BackingStoreException(String.format("Unable to load entry '%s'. %s", id, e.getMessage()), e);
		}
	}

	void saveWithoutFlush(final Preferences node) throws BackingStoreException {
		// ensure required values are set
		setJobTypeId(jobTypeId);

		node.put(JOB_TYPE_ID, jobTypeId);
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

		if (cronExpression != null) {
			node.put(CRON_EXPRESSION, cronExpression);
		} else {
			node.remove(CRON_EXPRESSION);
		}

		setIfNotNullOrRemoveIfNull(node, CRON_EXPRESSION, cronExpression);
		setIfNotNullOrRemoveIfNull(node, QUEUE_ID, queueId);
		setIfNotNullOrRemoveIfNull(node, PRECEDING_ENTRIES, StringUtils.join(precedingEntries, SEPARATOR_CHAR));
	}

	@Override
	public void setCronExpression(final String cronExpression) {
		if (StringUtils.isNotBlank(cronExpression)) {
			validateCronExpression(cronExpression);
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
		// wrap map (to create a clone)
		jobParamater = null != jobParameter ? new HashMap<String, String>(jobParameter) : null;
	}

	@Override
	public void setJobTypeId(final String jobTypeId) {
		if (!IdHelper.isValidId(jobTypeId))
			throw new IllegalArgumentException("invalid type id");
		this.jobTypeId = jobTypeId;
	}

	@Override
	public void setPrecedingEntries(final String... scheduleEntryIds) throws IllegalArgumentException {
		if ((scheduleEntryIds == null) || (scheduleEntryIds.length == 0)) {
			precedingEntries = null;
		} else {
			final ArrayList<String> list = new ArrayList<>(scheduleEntryIds.length);

			for (int i = 0; i < scheduleEntryIds.length; i++) {
				if (IdHelper.isValidId(scheduleEntryIds[i]))
					throw new IllegalArgumentException("invalid schedule entry id at index " + i + ": " + scheduleEntryIds[i]);
				list.add(scheduleEntryIds[i]);
			}
			// check for loops
			final LinkedList<String> sequence = new LinkedList<>();
			sequence.add(id);
			ScheduleImpl.checkExecutionSequenceForLoops(this, sequence, list);

			// set
			precedingEntries = list;
		}

	}

	@Override
	public void setQueueId(final String queueId) throws IllegalArgumentException {
		if ((queueId != null) && !IdHelper.isValidId(queueId))
			throw new IllegalArgumentException("invalide queue id: " + queueId);
		this.queueId = queueId;

	}

	public void setSchedule(final ScheduleImpl schedule) {
		this.schedule = schedule;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ScheduleEntryImpl [id=").append(id).append(", cronExpression=").append(cronExpression).append(", jobProviderId=").append(jobTypeId).append(", jobParamater=").append(jobParamater).append("]");
		return builder.toString();
	}
}
