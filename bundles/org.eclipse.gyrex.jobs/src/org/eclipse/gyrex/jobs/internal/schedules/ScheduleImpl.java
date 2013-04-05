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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleWorkingCopy;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ScheduleImpl implements ISchedule, IScheduleWorkingCopy {

	private static final String JOBS = "jobs";
	public static final String TIME_ZONE = "timeZone";
	public static final String ENABLED = "enabled";
	public static final String QUEUE_ID = "queueId";
	public static final String CONTEXT_PATH = "contextPath";

	public static void checkExecutionSequenceForLoops(final IScheduleEntry entry, final LinkedList<String> executionSequence, final Collection<String> precedingEntries) throws IllegalArgumentException {
		for (final String precedingEntryId : precedingEntries) {
			if (executionSequence.contains(precedingEntryId))
				throw new IllegalArgumentException(String.format("Found loop in schedule %s for preceding entry %s of entry %s in execution sequence %s.", entry.getSchedule().getId(), precedingEntryId, entry.getId(), StringUtils.join(executionSequence.descendingIterator(), "->")));
			else {
				// no loop, add to sequence and continue check with the entry
				executionSequence.add(precedingEntryId);
				final ScheduleEntryImpl precedingEntry = ((ScheduleImpl) entry.getSchedule()).getEntry(precedingEntryId);
				checkExecutionSequenceForLoops(precedingEntry, executionSequence, precedingEntry.getPrecedingEntries());
			}
		}
	}

	private final IEclipsePreferences node;
	private final String id;

	private IPath contextPath;
	private String queueId;
	private boolean enabled;
	private TimeZone timeZone;
	private Map<String, ScheduleEntryImpl> entriesById;

	private Map<String, Collection<String>> entriesToTriggerByPrecedingEntryId;

	private static final Logger LOG = LoggerFactory.getLogger(ScheduleImpl.class);

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param node
	 * @throws BackingStoreException
	 */
	public ScheduleImpl(final String id, final Preferences node) throws BackingStoreException {
		this.id = id;
		this.node = (IEclipsePreferences) node;
	}

	void checkModifiable() throws IllegalStateException {
		// note: we cannot rely on #isEnabled()
		// check the real node
		boolean notModifiable;
		try {
			notModifiable = (node != null) && node.nodeExists("") && node.getBoolean(ENABLED, false);
		} catch (final BackingStoreException e) {
			// treat as not modifiable
			notModifiable = true;
		}
		if (notModifiable)
			throw new IllegalStateException("A schedule must not be modified while it is enabled!");
	}

	@Override
	public ScheduleEntryImpl createEntry(final String entryId) throws IllegalArgumentException, IllegalStateException {
		checkModifiable();

		if (!IdHelper.isValidId(entryId))
			throw new IllegalArgumentException("invalid id: " + id);

		if (null == entriesById) {
			entriesById = new HashMap<String, ScheduleEntryImpl>(4);
		} else if (entriesById.containsKey(entryId))
			throw new IllegalStateException(String.format("entry '%s' already exists", entryId));

		final ScheduleEntryImpl entry = new ScheduleEntryImpl(entryId, this);
		entriesById.put(entryId, entry);
		return entry;
	}

	/**
	 * Returns the contextPath.
	 * 
	 * @return the contextPath
	 */
	public IPath getContextPath() {
		final IPath path = contextPath;
		if (null == path)
			throw new IllegalStateException(String.format("Schedule %s is invalid. Its context path is missing! Please delete and re-create schedule.", id));

		return path;
	}

	@Override
	public List<IScheduleEntry> getEntries() {
		if (null != entriesById) {
			final Collection<ScheduleEntryImpl> values = entriesById.values();
			final List<IScheduleEntry> entries = new ArrayList<IScheduleEntry>(values.size());
			entries.addAll(values);
			return Collections.unmodifiableList(entries);
		} else
			return Collections.emptyList();
	}

	private Preferences getEntriesNode() {
		return node.node(JOBS);
	}

	/**
	 * Returns the entries to trigger after the specified entry.
	 * 
	 * @param precedingEntryId
	 *            the preceding entry id
	 * @return an unmodifable list of entries to trigger
	 */
	public Collection<String> getEntriesToTriggerAfter(final String precedingEntryId) {
		final Map<String, Collection<String>> map = entriesToTriggerByPrecedingEntryId;
		if ((map == null) || (null == map.get(precedingEntryId)))
			return Collections.emptyList();

		return Collections.unmodifiableCollection(map.get(precedingEntryId));
	}

	@Override
	public ScheduleEntryImpl getEntry(final String entryId) throws IllegalArgumentException, IllegalStateException {
		if (!IdHelper.isValidId(entryId))
			throw new IllegalArgumentException("invalid id: " + id);

		final ScheduleEntryImpl entry = null != entriesById ? entriesById.get(entryId) : null;
		if (null == entry)
			throw new IllegalStateException(String.format("entry '%s' does not exist", entryId));

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

	public String getStorageKey() {
		return node.name();
	}

	@Override
	public TimeZone getTimeZone() {
		return timeZone != null ? timeZone : DateUtils.UTC_TIME_ZONE;
	}

	public boolean hasEntry(final String id) {
		return entriesById.containsKey(id);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public ScheduleImpl load() throws BackingStoreException {
		// ensure the schedule node is current
		node.sync();

		// load data
		queueId = node.get(QUEUE_ID, null);
		enabled = node.getBoolean(ENABLED, false);
		if (null != node.get(CONTEXT_PATH, null)) {
			contextPath = new Path(node.get(CONTEXT_PATH, null));
		} else
			throw new IllegalStateException(String.format("Schedule %s is invalid. Its context path is missing! Please delete and re-create.", id));
		timeZone = TimeZone.getTimeZone(node.get(TIME_ZONE, "GMT"));

		final Preferences entries = getEntriesNode();
		final String[] childrenNames = entries.childrenNames();
		entriesById = new HashMap<String, ScheduleEntryImpl>(childrenNames.length);
		for (final String entryId : childrenNames) {
			final ScheduleEntryImpl entryImpl = new ScheduleEntryImpl(entryId, this);
			entryImpl.load(entries.node(entryId));
			entriesById.put(entryId, entryImpl);
		}

		refreshDependenciesMap();

		return this;
	}

	private void refreshDependenciesMap() {
		// build new dependency map
		final Map<String, Collection<String>> entriesToTriggerByPrecedingEntryId = new HashMap<String, Collection<String>>();
		for (final IScheduleEntry entry : entriesById.values()) {
			final Collection<String> precedingEntries = entry.getPrecedingEntries();
			if (precedingEntries.isEmpty()) {
				continue;
			}

			// build execution sequence and detect loops
			final LinkedList<String> executionSequence = new LinkedList<>();
			executionSequence.add(entry.getId());
			try {
				checkExecutionSequenceForLoops(entry, executionSequence, precedingEntries);
			} catch (final IllegalArgumentException e) {
				// log warning and skip whole execution chain
				LOG.warn(e.getMessage());
				continue;
			}

			// no loop found; add to map and continue
			for (final String precedingEntryId : precedingEntries) {
				if (!entriesToTriggerByPrecedingEntryId.containsKey(precedingEntryId)) {
					entriesToTriggerByPrecedingEntryId.put(precedingEntryId, new HashSet<String>(2));
				}
				entriesToTriggerByPrecedingEntryId.get(precedingEntryId).add(entry.getId());
			}
		}

		// set map
		this.entriesToTriggerByPrecedingEntryId = entriesToTriggerByPrecedingEntryId;
	}

	@Override
	public void removeEntry(final String entryId) throws IllegalArgumentException, IllegalStateException {
		checkModifiable();

		if (null == entriesById)
			throw new IllegalStateException("schedule isn't initialized properly");

		if (!entriesById.containsKey(entryId))
			throw new IllegalArgumentException(String.format("schedule dosn't contain entry '%s'", entryId));

		final ScheduleEntryImpl entry = getEntry(entryId);
		final IRuntimeContext context = JobsActivator.getInstance().getService(IRuntimeContextRegistry.class).get(getContextPath());
		final IJobManager jobManager = context.get(IJobManager.class);
		jobManager.removeJob(entry.getJobId());

		entriesById.remove(entryId);
		entry.setSchedule(null);
	}

	public void save() throws BackingStoreException {
		if (enabled) {
			// note: only check if the new state is also enabled
			// (must always be possible to disable an enable schedule)
			checkModifiable();
		}

		if (StringUtils.isNotBlank(queueId)) {
			node.put(QUEUE_ID, queueId);
		} else {
			node.remove(QUEUE_ID);
		}
		node.put(CONTEXT_PATH, getContextPath().toString());
		node.putBoolean(ENABLED, enabled);
		final String tz = null != timeZone ? timeZone.getID() : null;
		if ((null != tz) && !DateUtils.UTC_TIME_ZONE.getID().equals(tz)) {
			node.put(TIME_ZONE, tz);
		} else {
			node.remove(TIME_ZONE);
		}

		final Preferences jobs = getEntriesNode();
		if (null != entriesById) {
			// update entries
			for (final ScheduleEntryImpl entry : entriesById.values()) {
				entry.saveWithoutFlush(jobs.node(entry.getId()));
			}
			// remove obsolete entries
			for (final String jobName : jobs.childrenNames()) {
				if (!entriesById.containsKey(jobName) && jobs.nodeExists(jobName)) {
					jobs.node(jobName).removeNode();
				}
			}
		} else {
			jobs.removeNode();
		}

		node.flush();
	}

	/**
	 * Sets the contextPath.
	 * 
	 * @param contextPath
	 *            the contextPath to set
	 */
	public void setContextPath(final IPath contextPath) {
		if (null == contextPath)
			throw new IllegalArgumentException("Context path must not be null!");
		this.contextPath = contextPath;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void setTimeZone(final TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ScheduleImpl [id=").append(id).append(", enabled=").append(enabled).append(", queueId=").append(queueId).append(", timeZone=").append(timeZone).append("]");
		return builder.toString();
	}

}
