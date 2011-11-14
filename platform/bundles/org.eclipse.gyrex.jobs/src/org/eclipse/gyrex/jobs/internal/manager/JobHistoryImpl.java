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
import org.eclipse.gyrex.jobs.internal.JobsActivator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * Simple implementation of of a {@link IJobHistoryEntry}.
 */
public class JobHistoryImpl implements IJobHistory {

	private static final String KEY_RESULT_SEVERITY = "resultSeverity";
	private static final String KEY_RESULT_MESSAGE = "resultMessage";
	private static final String KEY_TIMESTAMP = "timestamp";
	private static final String KEY_QUEUED_TRIGGER = "queuedTrigger";
	private static final String KEY_CANCELLED_TRIGGER = "canceledTrigger";
	private static final int MAX_HISTORY_SIZE = 120;

	private static IStatus convertStatus(final IStatus status) {
		// FIXME: implement better deserialization of Status (must support MultiStatus and plug-in id, but don't need to support exception)
		// for now we just convert the message to not loose any important information
		return new Status(status.getSeverity(), JobsActivator.SYMBOLIC_NAME, JobHistoryImpl.getFormattedMessage(status, 0));
	}

	private static IStatus deserializeStatus(final Preferences node) {
		// FIXME: implement better deserialization of Status (must support MultiStatus and plug-in id, but don't need to support exception)
		return new Status(node.getInt(KEY_RESULT_SEVERITY, IStatus.CANCEL), JobsActivator.SYMBOLIC_NAME, node.get(KEY_RESULT_MESSAGE, ""));
	}

	static String getFormattedMessage(final IStatus status, final int ident) {
		final StrBuilder builder = new StrBuilder();
		builder.appendPadding(ident, ' ');
		switch (status.getSeverity()) {
			case IStatus.CANCEL:
				builder.append("ABORT: ");
				break;
			case IStatus.ERROR:
				builder.append("ERROR: ");
				break;
			case IStatus.WARNING:
				builder.append("WARNING: ");
				break;
			case IStatus.INFO:
				builder.append("INFO: ");
				break;
		}
		builder.append(status.getMessage());
		if (status.getCode() != 0) {
			builder.append(" [code ").append(status.getCode()).append("]");
		}
		if (status.isMultiStatus()) {
			final IStatus[] children = status.getChildren();
			for (final IStatus child : children) {
				builder.appendNewLine();
				builder.append(getFormattedMessage(child, ident + 2));
			}
		}
		return builder.toString();
	}

	public static JobHistoryItemImpl readItem(final Preferences node) {
		final long ts = node.getLong(KEY_TIMESTAMP, 0);
		final String queuedTrigger = node.get(KEY_QUEUED_TRIGGER, StringUtils.EMPTY);
		final String cancelledTrigger = node.get(KEY_CANCELLED_TRIGGER, null);
		return new JobHistoryItemImpl(ts, deserializeStatus(node), queuedTrigger, cancelledTrigger);
	}

	private static void serializeStatus(final IStatus status, final Preferences node) {
		// FIXME: implement better serialization of Status (must support MultiStatus and plug-in id, but don't need to support exception)
		node.put(KEY_RESULT_MESSAGE, status.getMessage());
		node.putInt(KEY_RESULT_SEVERITY, status.getSeverity());
	}

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

	public void createEntry(final long resultTimestamp, final IStatus result, final long lastQueuedTimestamp, String lastQueueTrigger, final long lastCanceledTimestamp, String lastCanceledTrigger) {
		// only pass queue trigger to history if it makes sense
		if (lastQueuedTimestamp > resultTimestamp) {
			lastQueueTrigger = String.format("Specified trigger is incorrect. Job already queued after receiving result. (%s)", lastQueueTrigger);
		}

		// only pass cancellation trigger to history if it makes sense
		// TODO: is logic is brittle, we may need a better way to record cancellations in history
		if ((lastCanceledTimestamp < lastQueuedTimestamp) || (lastCanceledTimestamp > resultTimestamp)) {
			lastCanceledTrigger = null;
		}

		entries.add(new JobHistoryItemImpl(resultTimestamp, convertStatus(result), lastQueueTrigger, lastCanceledTrigger));
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
			entries.add(readItem(historyNode.node(entryId)));
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
			serializeStatus(entry.getResult(), node);

			if (null != entry.getQueuedTrigger()) {
				node.put(KEY_QUEUED_TRIGGER, entry.getQueuedTrigger());
			}
			if (null != entry.getCancelledTrigger()) {
				node.put(KEY_CANCELLED_TRIGGER, entry.getCancelledTrigger());
			}
		}

		// remove entries over size limit
		final String[] childrenNames = historyNode.childrenNames();
		for (final String entryId : childrenNames) {
			final Preferences node = historyNode.node(entryId);
			// FIXME: we should not relay on item for comparison (maybe just the timestamp is enough?)
			if (!entries.contains(readItem(node))) {
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
