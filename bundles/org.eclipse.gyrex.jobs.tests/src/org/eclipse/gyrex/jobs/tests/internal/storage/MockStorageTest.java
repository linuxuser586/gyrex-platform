/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.tests.internal.storage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.gyrex.context.IModifiableRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.history.IJobHistory;
import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.spi.storage.IJobHistoryStorage;
import org.eclipse.gyrex.jobs.spi.storage.JobHistoryEntryStorable;
import org.eclipse.gyrex.jobs.tests.internal.JobsTestsActivator;
import org.eclipse.gyrex.jobs.tests.internal.TestJobsProvider;

import org.eclipse.core.runtime.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class MockStorageTest {

	private IRuntimeContextRegistry contextRegistry;
	private IModifiableRuntimeContext testContext;

	@Before
	public void setUp() throws Exception {
		contextRegistry = JobsTestsActivator.getInstance().getService(IRuntimeContextRegistry.class);
		testContext = contextRegistry.get(Path.ROOT).createWorkingCopy();
	}

	@After
	public void tearDown() throws Exception {
		testContext.dispose();
		testContext = null;
		contextRegistry = null;
	}

	@Test
	public void test() {
		final SortedSet<JobHistoryEntryStorable> history = new TreeSet<>();

		testContext.setLocal(IJobHistoryStorage.class, new IJobHistoryStorage() {

			@Override
			public void add(final String jobId, final JobHistoryEntryStorable historyEntry) throws Exception {
				history.add(historyEntry);
			}

			@Override
			public int count(final String jobId) throws Exception {
				return history.size();
			}

			@Override
			public Collection<JobHistoryEntryStorable> find(final String jobId, final int offset, final int fetchSize) throws Exception {
				return history;
			}
		});

		final String jobId = "test" + System.nanoTime();
		final IJobManager jobManager = testContext.get(IJobManager.class);
		final IJob job = jobManager.createJob(TestJobsProvider.ID_TESTABLE_JOB, jobId, null);
		assertNotNull(job);
		IJobHistory jobHistory = jobManager.getHistory(jobId);
		assertNotNull(jobHistory);
		assertNotNull(jobHistory.getEntries());
		assertTrue(jobHistory.getEntries().isEmpty());

		history.add(new JobHistoryEntryStorable());
		jobHistory = jobManager.getHistory(jobId);
		assertNotNull(jobHistory);
		assertNotNull(jobHistory.getEntries());
		assertFalse(jobHistory.getEntries().isEmpty());

		// now test we get all objects
		final Iterator<IJobHistoryEntry> stream = jobHistory.getEntries().iterator();
		assertTrue(stream.hasNext());
		assertNotNull(stream.next());
		assertFalse(stream.hasNext());
		try {
			stream.next();
			fail("missing NoSuchElementException");
		} catch (final NoSuchElementException e) {
			// good
		}

		assertEquals(jobHistory.getEntries().size(), jobHistory.getEntries().toArray().length);
	}

}
