/*******************************************************************************
 * Copyright (c) 2013 <enter-company-name-here> and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.manager;

import java.util.Collection;

import org.eclipse.gyrex.jobs.history.IJobHistory;
import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;
import org.eclipse.gyrex.jobs.spi.storage.IJobHistoryStorage;

/**
 * {@link IJobHistory} backed by a {@link IJobHistoryStorage}.
 */
public final class StorageBackedJobHistory implements IJobHistory {

	/** jobId */
	private final String jobId;
	/** storage */
	private final IJobHistoryStorage storage;

	/**
	 * Creates a new instance.
	 * 
	 * @param jobId
	 * @param storage
	 */
	public StorageBackedJobHistory(final String jobId, final IJobHistoryStorage storage) {
		this.jobId = jobId;
		this.storage = storage;
	}

	@Override
	public Collection<IJobHistoryEntry> getEntries() {
		try {
			return new PagableHistoryEntryCollection(jobId, storage, 500);
		} catch (final Exception e) {
			throw new IllegalStateException("Error reading history.", e);
		}
	}

	@Override
	public String toString() {
		return String.format("%s[ %s (using %s) ]", getClass().getSimpleName(), jobId, storage);
	}

}