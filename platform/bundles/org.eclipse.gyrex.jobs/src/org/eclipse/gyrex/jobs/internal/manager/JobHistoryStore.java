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

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Store which persists job history in cloud preferences.
 */
public class JobHistoryStore {

	private static final String NODE_HISTORY = "history";
	private static final String NODE_JOBS = "jobs";

	public static JobHistoryImpl create(final String jobStorageKey, final String jobId, final IRuntimeContext context) throws BackingStoreException {
		final IEclipsePreferences historyNode = getHistoryNode(jobStorageKey);
		final JobHistoryImpl jobHistoryImpl = new JobHistoryImpl(jobId, context.getContextPath());
		jobHistoryImpl.load(historyNode);
		return jobHistoryImpl;
	}

	public static void flush(final String jobStorageKey, final JobHistoryImpl history) throws BackingStoreException {
		final IEclipsePreferences historyNode = getHistoryNode(jobStorageKey);
		history.save(historyNode);
	}

	public static IEclipsePreferences getHistoryNode(final String jobStorageKey) throws BackingStoreException {
		// migrate old history (if necessary)
		if (getJobsNode().nodeExists(jobStorageKey + IPath.SEPARATOR + NODE_HISTORY)) {
			migrateOldJobHistory(jobStorageKey);
		}

		return (IEclipsePreferences) getJobsHistoryNode().node(jobStorageKey);
	}

	public static IEclipsePreferences getJobsHistoryNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(JobsActivator.SYMBOLIC_NAME).node(NODE_HISTORY);
	}

	public static IEclipsePreferences getJobsNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(JobsActivator.SYMBOLIC_NAME).node(NODE_JOBS);
	}

	/**
	 * Migrates the old job history (stored under jobs/jobid/history) to the new
	 * location (history/jobid).
	 * 
	 * @param jobStorageKey
	 * @throws BackingStoreException
	 */
	private static void migrateOldJobHistory(final String jobStorageKey) throws BackingStoreException {
		// try up to a minute migrating the history
		final long abortTime = System.currentTimeMillis() + 60000L;
		while (getJobsNode().nodeExists(jobStorageKey + IPath.SEPARATOR + NODE_HISTORY) && (abortTime > System.currentTimeMillis())) {
			// ensure that the latest info is used
			getJobsNode().node(jobStorageKey).sync();

			// move from old to new
			try {
				final Preferences newHistory = getJobsHistoryNode().node(jobStorageKey);
				final Preferences oldHistory = getJobsNode().node(jobStorageKey + IPath.SEPARATOR + NODE_HISTORY);
				for (final String name : oldHistory.childrenNames()) {
					final Preferences oldItem = oldHistory.node(name);
					final Preferences newItem = newHistory.node(name);
					for (final String key : oldItem.keys()) {
						final String value = oldItem.get(key, null);
						if (null != value) {
							newItem.put(key, value);
						}
					}
				}
				newHistory.flush();

				final Preferences oldHistoryParent = oldHistory.parent();
				oldHistory.removeNode();
				oldHistoryParent.flush();
			} catch (final Exception e) {
				// ignore exception and retry
			}
		}
	}
}
