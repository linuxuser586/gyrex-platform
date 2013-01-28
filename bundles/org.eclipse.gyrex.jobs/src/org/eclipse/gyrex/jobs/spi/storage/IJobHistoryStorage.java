/*******************************************************************************
 * Copyright (c) 2013 AGETO and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.spi.storage;

import java.util.Collection;

import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.history.IJobHistory;
import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;

/**
 * A store for persisting job execution data ({@link IJobHistory} & Co.).
 * <p>
 * Implementations of this class must be made available as
 * {@link RuntimeContextObjectProvider context objects}. The system may be
 * configured with a standard implementation at the root context level. They can
 * be overridden at any context level node using the standard Gyrex context
 * configuration capabilities. Different storage implementations within one
 * context are not supported.
 * </p>
 * <p>
 * If no standard history storage implementation is available in a context, no
 * job history will be persisted and no job history will be available.
 * </p>
 * <p>
 * This interface must be implemented by clients that contribute a history store
 * implementation to Gyrex. As such it is considered part of a service provider
 * API which may evolve faster than the general API. Please get in touch with
 * the development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public interface IJobHistoryStorage {

	/**
	 * Adds an entry to the underlying data store for a specific
	 * {@link IJob#getId() job id}.
	 * 
	 * @param jobId
	 *            the {@link IJob#getId() job id} (may not be <code>null</code>)
	 * @param historyEntry
	 *            the history entry to add (may not be <code>null</code>)
	 * @throws Exception
	 *             in case of errors accessing the underlying data store and/or
	 *             loading the job history
	 */
	void add(String jobId, JobHistoryEntryStorable historyEntry) throws Exception;

	/**
	 * Returns the number of available history entries from the underlying data
	 * store for a specific {@link IJob#getId() job id}.
	 * 
	 * @param jobId
	 *            the {@link IJob#getId() job id} (may not be <code>null</code>)
	 * @return the number of available {@link JobHistoryEntryStorable storables}
	 *         for the specified job
	 * @throws Exception
	 *             in case of errors accessing the underlying data store and/or
	 *             loading the job history
	 */
	int count(String jobId) throws Exception;

	/**
	 * Looks up a job history from the underlying data store for a specific
	 * {@link IJob#getId() job id}.
	 * <p>
	 * The result must be ordered by the natural order of
	 * {@link IJobHistoryEntry} (i.e.
	 * {@link JobHistoryEntryStorable#getTimestamp()} with reverse order; most
	 * recent entry is first, oldest is last) in a way that is consistent across
	 * invocations.
	 * </p>
	 * 
	 * @param jobId
	 *            the {@link IJob#getId() job id} (may not be <code>null</code>)
	 * @return an unmodifiable, ordered collection of history entries (may not
	 *         be <code>null</code>)
	 * @throws Exception
	 *             in case of errors accessing the underlying data store and/or
	 *             loading the job history
	 */
	Collection<JobHistoryEntryStorable> find(String jobId, int offset, int fetchSize) throws Exception;
}
