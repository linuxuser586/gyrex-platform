/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Mike Tschierschke - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.util.ContextHashUtil;
import org.eclipse.gyrex.jobs.spi.storage.IJobHistoryStorage;
import org.eclipse.gyrex.jobs.spi.storage.JobHistoryEntryStorable;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Store which persists job history in cloud preferences.
 */
public class CloudPreferncesJobHistoryStorage implements IJobHistoryStorage {
	private static final String KEY_RESULT_SEVERITY = "resultSeverity";
	private static final String KEY_RESULT_MESSAGE = "resultMessage";
	private static final String KEY_TIMESTAMP = "timestamp";
	private static final String KEY_QUEUED_TRIGGER = "queuedTrigger";
	private static final String KEY_CANCELLED_TRIGGER = "canceledTrigger";
	private static final int MAX_HISTORY_SIZE = 120;
	public static final int MAX_RESULT_MESSAGE_SIZE = Integer.getInteger("gyrex.jobs.history.maxMessageLength", 4096); // ~4K

	private static final Logger LOG = LoggerFactory.getLogger(CloudPreferncesJobHistoryStorage.class);

	private static final String NODE_HISTORY = "history";

	private static IStatus convertStatus(final IStatus status) {
		// FIXME: implement better deserialization of Status (must support MultiStatus and plug-in id, but don't need to support exception)
		// for now we just convert the message to not loose any important information
		return new Status(status.getSeverity(), JobsActivator.SYMBOLIC_NAME, StringUtils.left(getFormattedMessage(status, 0), MAX_RESULT_MESSAGE_SIZE));
	}

	private static IStatus deserializeStatus(final Preferences node) {
		// FIXME: implement better deserialization of Status (must support MultiStatus and plug-in id, but don't need to support exception)
		return new Status(node.getInt(KEY_RESULT_SEVERITY, IStatus.CANCEL), JobsActivator.SYMBOLIC_NAME, node.get(KEY_RESULT_MESSAGE, ""));
	}

	public static String getFormattedMessage(final IStatus status, final int ident) {
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

	public static IEclipsePreferences getHistoryNode(final String jobStorageKey) throws BackingStoreException {
		return (IEclipsePreferences) getJobsHistoryNode().node(jobStorageKey);
	}

	public static IEclipsePreferences getJobsHistoryNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(JobsActivator.SYMBOLIC_NAME).node(NODE_HISTORY);
	}

	public static JobHistoryEntryStorable readItem(final Preferences node) {
		final JobHistoryEntryStorable historyEntry = new JobHistoryEntryStorable();
		historyEntry.setTimestamp(node.getLong(KEY_TIMESTAMP, 0));
		historyEntry.setResult(deserializeStatus(node));
		historyEntry.setQueuedTrigger(node.get(KEY_QUEUED_TRIGGER, StringUtils.EMPTY));
		historyEntry.setCancelledTrigger(node.get(KEY_CANCELLED_TRIGGER, null));
		try {
			historyEntry.setParameter(CloudPreferncesJobStorage.readParameter(node));
		} catch (final BackingStoreException e) {
			LOG.warn("Failed to read parameter for job history {}. {}", new Object[] { node.absolutePath(), ExceptionUtils.getRootCauseMessage(e), e });
		}
		return historyEntry;
	}

	private static void serializeStatus(final IStatus status, final Preferences node) {
		// FIXME: implement better serialization of Status (must support MultiStatus and plug-in id, but don't need to support exception)
		node.put(KEY_RESULT_MESSAGE, convertStatus(status).getMessage());
		node.putInt(KEY_RESULT_SEVERITY, status.getSeverity());
	}

	private final ContextHashUtil contextHash;

	/**
	 * Creates a new instance.
	 */
	@Inject
	public CloudPreferncesJobHistoryStorage(final IRuntimeContext context) {
		contextHash = new ContextHashUtil(context);
	}

	@Override
	public void add(final String jobId, final JobHistoryEntryStorable historyEntry) throws Exception {
		// load existing entries
		final IEclipsePreferences historyNode = getHistoryNode(contextHash.toInternalId(jobId));
		final String[] childrenNames = historyNode.childrenNames();
		final SortedSet<JobHistoryEntryStorable> entries = new TreeSet<JobHistoryEntryStorable>();
		for (final String entryId : childrenNames) {
			entries.add(readItem(historyNode.node(entryId)));
		}

		// add new entry
		entries.add(historyEntry);

		// shrink to size limit
		shrinkToSizeLimit(entries);

		// create new entries
		for (final JobHistoryEntryStorable entry : entries) {
			final String entryId = String.valueOf(entry.getTimestamp());
			if (historyNode.nodeExists(entryId)) {
				continue;
			}

			final Preferences node = historyNode.node(entryId);
			node.putLong(KEY_TIMESTAMP, entry.getTimestamp());
			serializeStatus(entry.getResult(), node);

			if (null != entry.getQueuedTrigger()) {
				node.put(KEY_QUEUED_TRIGGER, entry.getQueuedTrigger());
			}
			if (null != entry.getCancelledTrigger()) {
				node.put(KEY_CANCELLED_TRIGGER, entry.getCancelledTrigger());
			}
		}

		// remove entries over size limit
		for (final String entryId : historyNode.childrenNames()) {
			// check if there is en entry for the node
			for (final JobHistoryEntryStorable entry : entries) {
				if (entryId.equals(String.valueOf(entry.getTimestamp()))) {
					continue;
				}
			}
			historyNode.node(entryId).removeNode();
		}

		// flush
		historyNode.flush();

	}

	@Override
	public int count(final String jobId) throws Exception {
		final IEclipsePreferences historyNode = getHistoryNode(contextHash.toInternalId(jobId));
		final String[] childrenNames = historyNode.childrenNames();
		return childrenNames.length;
	}

	@Override
	public Collection<JobHistoryEntryStorable> find(final String jobId, final int offset, final int fetchSize) throws Exception {
		if (offset > 0)
			return Collections.emptyList();

		final IEclipsePreferences historyNode = getHistoryNode(contextHash.toInternalId(jobId));
		final String[] childrenNames = historyNode.childrenNames();
		final SortedSet<JobHistoryEntryStorable> entries = new TreeSet<JobHistoryEntryStorable>();
		for (final String entryId : childrenNames) {
			entries.add(readItem(historyNode.node(entryId)));
		}

		shrinkToSizeLimit(entries);

		return entries;
	}

	private void shrinkToSizeLimit(final SortedSet<JobHistoryEntryStorable> entries) {
		// remove the last entries if over size
		while (entries.size() > MAX_HISTORY_SIZE) {
			entries.remove(entries.last());
		}
	}

}
