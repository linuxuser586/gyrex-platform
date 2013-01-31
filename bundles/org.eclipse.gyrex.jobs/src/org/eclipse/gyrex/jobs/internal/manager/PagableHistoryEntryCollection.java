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

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;
import org.eclipse.gyrex.jobs.spi.storage.IJobHistoryStorage;
import org.eclipse.gyrex.jobs.spi.storage.JobHistoryEntryStorable;

/**
 * Collections paging through {@link IJobHistoryStorage#find(String, int, int)}
 */
public class PagableHistoryEntryCollection extends AbstractCollection<IJobHistoryEntry> {

	private final class PagableHistoryEntryIterator implements Iterator<IJobHistoryEntry> {
		int offset = 0;
		Iterator<JobHistoryEntryStorable> storablesIterator;

		@Override
		public boolean hasNext() {
			return (offset < totalEntryCount) || ((null != storablesIterator) && storablesIterator.hasNext());
		}

		@Override
		public IJobHistoryEntry next() {
			if ((storablesIterator != null) && storablesIterator.hasNext())
				return new StorableBackedJobHistoryEntry(storablesIterator.next());

			if (offset >= totalEntryCount)
				throw new NoSuchElementException();
			else {
				offset += pageSize;
			}

			try {
				storablesIterator = storage.find(jobId, offset, pageSize).iterator();
				return new StorableBackedJobHistoryEntry(storablesIterator.next());
			} catch (final Exception e) {
				throw new IllegalStateException("Error accessing job history store.", e);
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return String.format("%s[ %s (total %d, offset %d, page size %d) (using %s) ]", getClass().getSimpleName(), jobId, totalEntryCount, offset, pageSize, storage);
		}
	}

	final int totalEntryCount;
	final int pageSize;
	final int pages;
	final String jobId;
	final IJobHistoryStorage storage;

	public PagableHistoryEntryCollection(final String jobId, final IJobHistoryStorage storage, final int pageSize) throws Exception {
		this.jobId = jobId;
		this.storage = storage;
		this.pageSize = Math.max(pageSize, 1);
		totalEntryCount = storage.count(jobId);
		pages = (int) Math.floor((double) totalEntryCount / (double) this.pageSize);
	}

	@Override
	public Iterator<IJobHistoryEntry> iterator() {
		return new PagableHistoryEntryIterator();
	}

	@Override
	public int size() {
		return totalEntryCount;
	}

	@Override
	public String toString() {
		return String.format("%s[ %s (%d) (using %s) ]", getClass().getSimpleName(), jobId, totalEntryCount, storage);
	}
}
