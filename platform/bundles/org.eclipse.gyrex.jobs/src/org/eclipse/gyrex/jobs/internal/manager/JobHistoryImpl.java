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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.jobs.history.IJobHistory;
import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Simple implementation of of a {@link IJobHistoryEntry}
 */
final class JobHistoryImpl implements IJobHistory {

	/** RESULT_SEVERITY */
	private static final String KEY_RESULT_SEVERITY = "resultSeverity";
	/** RESULT_MESSAGE */
	private static final String KEY_RESULT_MESSAGE = "resultMessage";
	/** TIMESTAMP */
	private static final String KEY_TIMESTAMP = "timestamp";
	private final String jobId;
	private final IPath contextPath;
	private List<IJobHistoryEntry> entries;

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

	/**
	 * @param resultTimestamp
	 * @param message
	 * @param severity
	 */
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
	public List<IJobHistoryEntry> getEntries() {
		if (null == entries) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableList(entries);
	}

	/**
	 * Returns the jobId.
	 * 
	 * @return the jobId
	 */
	public String getJobId() {
		return jobId;
	}

	/**
	 * @param historyNode
	 * @throws BackingStoreException
	 */
	public void load(final IEclipsePreferences historyNode) throws BackingStoreException {
		final String[] childrenNames = historyNode.childrenNames();
		entries = new ArrayList<IJobHistoryEntry>(childrenNames.length);
		for (final String entryId : childrenNames) {
			final Preferences node = historyNode.node(entryId);
			final long ts = node.getLong(KEY_TIMESTAMP, 0);
			final String message = node.get(KEY_RESULT_MESSAGE, "");
			final int severity = node.getInt(KEY_RESULT_SEVERITY, IStatus.CANCEL);
			entries.add(new JobHistoryItemImpl(ts, message, severity));
		}
	}

	/**
	 * @param historyNode
	 * @throws BackingStoreException
	 */
	public void save(final IEclipsePreferences historyNode) throws BackingStoreException {
		if (null == entries) {
//			final String[] childrenNames = historyNode.childrenNames();
//			for (final String child : childrenNames) {
//				historyNode.node(child).removeNode();
//			}
			return;
		}

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

		historyNode.flush();
	}
}
