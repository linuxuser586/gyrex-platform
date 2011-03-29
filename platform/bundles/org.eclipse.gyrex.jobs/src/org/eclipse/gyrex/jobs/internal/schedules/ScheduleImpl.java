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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntryWorkingCopy;
import org.eclipse.gyrex.jobs.schedules.IScheduleWorkingCopy;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;

/**
 *
 */
public class ScheduleImpl implements ISchedule, IScheduleWorkingCopy {

	private static final String JOBS = "jobs";
	public static final String TIME_ZONE = "timeZone";
	public static final String ENABLED = "enabled";
	public static final String QUEUE_ID = "queueId";

	private final IEclipsePreferences node;
	private final String id;

	private String queueId;
	private boolean enabled;
	private TimeZone timeZone;
	private Map<String, ScheduleEntryImpl> entriesById;

	/**
	 * Creates a new instance.
	 * 
	 * @param node
	 * @throws BackingStoreException
	 */
	public ScheduleImpl(final Preferences node) throws BackingStoreException {
		this.node = (IEclipsePreferences) node;
		id = node.name();
		load();
	}

	@Override
	public IScheduleEntryWorkingCopy createEntry(final String entryId) {
		if (!IdHelper.isValidId(entryId)) {
			throw new IllegalArgumentException("invalid id: " + id);
		}

		final Preferences entriesNode = getEntriesNode();

		try {
			if (entriesById.containsKey(entryId)) {
				throw new IllegalStateException(String.format("entry '%s' already exists", entryId));
			}

			final ScheduleEntryImpl entry = new ScheduleEntryImpl(entriesNode.node(entryId));
			entriesById.put(entryId, entry);
			return entry;
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public List<IScheduleEntry> getEntries() {
		if (null != entriesById) {
			final Collection<ScheduleEntryImpl> values = entriesById.values();
			final List<IScheduleEntry> entries = new ArrayList<IScheduleEntry>(values.size());
			entries.addAll(values);
			return Collections.unmodifiableList(entries);
		} else {
			return Collections.emptyList();
		}
	}

	private Preferences getEntriesNode() {
		return node.node(JOBS);
	}

	@Override
	public IScheduleEntryWorkingCopy getEntry(final String entryId) {
		if (!IdHelper.isValidId(entryId)) {
			throw new IllegalArgumentException("invalid id: " + id);
		}

		final ScheduleEntryImpl entry = entriesById.get(entryId);
		if (null == entry) {
			throw new IllegalStateException(String.format("entry '%s' does not exist", entryId));
		}

		return entry;
	}

	@Override
	public String getId() {
		return id;
	}

	/**
	 * Returns the node.
	 * 
	 * @return the node
	 */
	public IEclipsePreferences getPreferenceNode() {
		return node;
	}

	@Override
	public String getQueueId() {
		return queueId;
	}

	@Override
	public TimeZone getTimeZone() {
		return timeZone != null ? timeZone : DateUtils.UTC_TIME_ZONE;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void load() throws BackingStoreException {
		queueId = node.get(QUEUE_ID, null);
		enabled = node.getBoolean(ENABLED, false);
		timeZone = TimeZone.getTimeZone(node.get(TIME_ZONE, "GMT"));

		final Preferences jobs = getEntriesNode();
		final String[] childrenNames = jobs.childrenNames();
		entriesById = new HashMap<String, ScheduleEntryImpl>(childrenNames.length);
		for (final String jobName : childrenNames) {
			entriesById.put(jobName, new ScheduleEntryImpl(node.node(jobName)));
		}
	}

	public void save() throws BackingStoreException {
		if (StringUtils.isNotBlank(queueId)) {
			node.put(QUEUE_ID, queueId);
		} else {
			node.remove(QUEUE_ID);
		}
		node.putBoolean(ENABLED, enabled);
		final String tz = null != timeZone ? timeZone.getID() : null;
		if ((null != tz) && !DateUtils.UTC_TIME_ZONE.getID().equals(tz)) {
			node.put(TIME_ZONE, tz);
		} else {
			node.remove(TIME_ZONE);
		}

		// update entries
		for (final ScheduleEntryImpl entry : entriesById.values()) {
			entry.saveWithoutFlush();
		}
		// remove obsolete entries
		final Preferences jobs = getEntriesNode();
		for (final String jobName : jobs.childrenNames()) {
			if (!entriesById.containsKey(jobName) && node.nodeExists(jobName)) {
				node.node(jobName).removeNode();
			}
		}

		node.flush();
	}

	@Override
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void setTimeZone(final TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ScheduleImpl [id=").append(id).append(", enabled=").append(enabled).append(", queueId=").append(queueId).append(", timeZone=").append(timeZone).append("]");
		return builder.toString();
	}

}
