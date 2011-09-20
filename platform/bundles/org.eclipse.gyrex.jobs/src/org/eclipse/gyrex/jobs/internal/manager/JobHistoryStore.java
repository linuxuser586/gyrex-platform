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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;

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
		historyNode.flush();
	}

	static IEclipsePreferences getHistoryNode(final String jobStorageKey) throws BackingStoreException {
		final IEclipsePreferences jobsNode = getJobsNode();
		if (!jobsNode.nodeExists(jobStorageKey)) {
			throw new BackingStoreException(String.format("Job node '%s' not found!", jobStorageKey));
		}
		return (IEclipsePreferences) jobsNode.node(jobStorageKey).node(NODE_HISTORY);
	}

	public static IEclipsePreferences getJobsNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(JobsActivator.SYMBOLIC_NAME).node(NODE_JOBS);
	}

}
