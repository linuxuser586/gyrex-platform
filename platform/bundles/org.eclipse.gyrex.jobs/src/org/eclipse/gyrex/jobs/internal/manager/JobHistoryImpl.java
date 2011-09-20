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

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.gyrex.jobs.history.IJobHistory;
import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Simple implementation of of a {@link IJobHistoryEntry}.
 */
final class JobHistoryImpl implements IJobHistory {

	private static final String KEY_RESULT_SEVERITY = "resultSeverity";
	private static final String KEY_RESULT_MESSAGE = "resultMessage";
	private static final String KEY_TIMESTAMP = "timestamp";
	private static final int MAX_HISTORY_SIZE = 120;

	private final String jobId;

	private final IPath contextPath;
	private SortedSet<IJobHistoryEntry> entries;

	/**
	 * Creates a new instance.
	 * 
	 * @param jobId
	 * @param contextPath
	 */
	public JobHistoryImpl(final String jobId, final IPath contextPath) {
		this.jobId = jobId;
		this.contextPath = contextPath;
	}

	public void createEntry(final long resultTimestamp, final String message, final int severity) {
		entries.add(new JobHistoryItemImpl(resultTimestamp, message, severity));
	}

	/**
	 * Returns the contextPath.
	 * 
	 * @return the contextPath
	 */
	public IPath getContextPath() {
		return contextPath;
	}

	@Override
	public Collection<IJobHistoryEntry> getEntries() {
		if (null == entries) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableCollection(entries);
	}

	/**
	 * Returns the jobId.
	 * 
	 * @return the jobId
	 */
	public String getJobId() {
		return jobId;
	}

	public void load(final IEclipsePreferences historyNode) throws BackingStoreException {
		final String[] childrenNames = historyNode.childrenNames();
		entries = new TreeSet<IJobHistoryEntry>();
		for (final String entryId : childrenNames) {
			final Preferences node = historyNode.node(entryId);
			final long ts = node.getLong(KEY_TIMESTAMP, 0);
			final String message = node.get(KEY_RESULT_MESSAGE, "");
			final int severity = node.getInt(KEY_RESULT_SEVERITY, IStatus.CANCEL);
			entries.add(new JobHistoryItemImpl(ts, message, severity));
		}

		shrinkToSizeLimit();
	}

	public void save(final IEclipsePreferences historyNode) throws BackingStoreException {
		if (null == entries) {
			// never loaded
			return;
		}

		// shrink to size limit
		shrinkToSizeLimit();

		// create new entries
		for (final IJobHistoryEntry entry : entries) {
			final String entryId = String.valueOf(entry.getTimeStamp());
			if (historyNode.nodeExists(entryId)) {
				continue;
			}
			final Preferences node = historyNode.node(entryId);
			node.putLong(KEY_TIMESTAMP, entry.getTimeStamp());
			node.put(KEY_RESULT_MESSAGE, entry.getResult());
			node.putInt(KEY_RESULT_SEVERITY, entry.getSeverity());
		}

		// remove entries over size limit
		final String[] childrenNames = historyNode.childrenNames();
		for (final String entryId : childrenNames) {
			final Preferences node = historyNode.node(entryId);
			final long ts = node.getLong(KEY_TIMESTAMP, 0);
			final String message = node.get(KEY_RESULT_MESSAGE, "");
			final int severity = node.getInt(KEY_RESULT_SEVERITY, IStatus.CANCEL);
			final JobHistoryItemImpl entry = new JobHistoryItemImpl(ts, message, severity);
			if (!entries.contains(entry)) {
				node.removeNode();
			}
		}

		// flush
		historyNode.flush();
	}

	private void shrinkToSizeLimit() {
		// remove the last entries if over size
		while (entries.size() > MAX_HISTORY_SIZE) {
			entries.remove(entries.last());
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("JobHistoryImpl [jobId=").append(jobId).append(", contextPath=").append(contextPath).append(", entries=").append(entries).append("]");
		return builder.toString();
	}
}
