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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.gyrex.context.IModifiableRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.history.IJobHistory;
import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;
import org.eclipse.gyrex.jobs.internal.storage.CloudPreferncesJobHistoryStorage;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.spi.storage.IJobHistoryStorage;
import org.eclipse.gyrex.jobs.spi.storage.JobHistoryEntryStorable;
import org.eclipse.gyrex.jobs.tests.internal.JobsTestsActivator;
import org.eclipse.gyrex.jobs.tests.internal.TestJobsProvider;
import org.eclipse.gyrex.junit.GyrexServerResource;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class CloudHistoryStorageTest {

	@ClassRule
	public static final GyrexServerResource server = new GyrexServerResource();

	@BeforeClass
	public static void clearJobHistory() throws Exception {
		final IEclipsePreferences jobsHistoryNode = CloudPreferncesJobHistoryStorage.getJobsHistoryNode();
		final String[] childrenNames = jobsHistoryNode.childrenNames();
		for (final String string : childrenNames) {
			jobsHistoryNode.node(string).removeNode();
		}
		jobsHistoryNode.flush();
		Thread.sleep(5000);
		jobsHistoryNode.sync();
		Thread.sleep(2000);
	}

	static JobHistoryEntryStorable createHistoryEntry(final int severity, final String message) {
		final JobHistoryEntryStorable e = new JobHistoryEntryStorable();
		e.setTimestamp(System.currentTimeMillis());
		e.setQueuedTrigger("test");
		e.setCancelledTrigger("test-cancel");
		e.setResult(new Status(severity, "test", message));
		final HashMap<String, String> p = new HashMap<String, String>();
		p.put("param", "arg");
		e.setParameter(p);
		return e;
	}

	private IRuntimeContextRegistry contextRegistry;

	private IModifiableRuntimeContext testContext;
	private CloudPreferncesJobHistoryStorage storage;

	private IJobManager jobManager;

	private IJobHistory assertHistorySize(final String jobId, final int expected) {
		final IJobHistory jobHistory = jobManager.getHistory(jobId);
		assertNotNull(jobHistory);
		assertNotNull(jobHistory.getEntries());
		assertEquals(expected, jobHistory.getEntries().size());
		return jobHistory;
	}

	@Before
	public void setUp() throws Exception {
		contextRegistry = JobsTestsActivator.getInstance().getService(IRuntimeContextRegistry.class);
		testContext = contextRegistry.get(Path.ROOT).createWorkingCopy();
		storage = new CloudPreferncesJobHistoryStorage(testContext);
		testContext.setLocal(IJobHistoryStorage.class, storage);
		jobManager = testContext.get(IJobManager.class);
	}

	@After
	public void tearDown() throws Exception {
		testContext.dispose();
		testContext = null;
		contextRegistry = null;
		storage = null;
		jobManager = null;
	}

	@Test
	public void testAddHistory() throws Exception {
		final String jobId = "test" + System.nanoTime();

		// get job
		final IJob job = jobManager.createJob(TestJobsProvider.ID_TESTABLE_JOB, jobId, null);
		assertNotNull(job);

		// check history is empty
		assertHistorySize(jobId, 0);

		// add a dummy entry
		storage.add(jobId, createHistoryEntry(IStatus.ERROR, "Error"));

		// assert entry is there
		assertHistorySize(jobId, 1);

		// add 2nd entry
		storage.add(jobId, createHistoryEntry(IStatus.INFO, "Info"));

		// assert entry is there
		final IJobHistory jobHistory = assertHistorySize(jobId, 2);

		// now test we get all objects
		final Iterator<IJobHistoryEntry> stream = jobHistory.getEntries().iterator();
		assertTrue(stream.hasNext());
		final IJobHistoryEntry latest = stream.next();
		assertNotNull(latest);
		assertNotNull(latest.getResult());
		assertEquals(IStatus.INFO, latest.getResult().getSeverity());

		assertTrue(stream.hasNext());
		final IJobHistoryEntry oldest = stream.next();
		assertNotNull(oldest);
		assertNotNull(oldest.getResult());
		assertEquals(IStatus.ERROR, oldest.getResult().getSeverity());

		assertFalse(stream.hasNext());
		try {
			stream.next();
			fail("missing NoSuchElementException");
		} catch (final NoSuchElementException e) {
			// good
		}

		assertEquals(jobHistory.getEntries().size(), jobHistory.getEntries().toArray().length);
	}

	@Test
	public void testMaxHistorySize() throws Exception {
		final String jobId = "test2" + System.nanoTime();

		// get job
		final IJob job = jobManager.createJob(TestJobsProvider.ID_TESTABLE_JOB, jobId, null);
		assertNotNull(job);

		// check history is empty
		assertHistorySize(jobId, 0);

		// add a bunch of entries
		JobHistoryEntryStorable lastEntry = null;
		for (int i = 0; i < (CloudPreferncesJobHistoryStorage.MAX_HISTORY_SIZE + 7); i++) {
			storage.add(jobId, lastEntry = createHistoryEntry(IStatus.INFO, "Status " + i));
		}

		// assert max size not exceeded
		final IJobHistory history = assertHistorySize(jobId, CloudPreferncesJobHistoryStorage.MAX_HISTORY_SIZE);

		// now verify that the first entry is the last one added
		final IJobHistoryEntry first = history.getEntries().iterator().next();
		assertEquals("INFO: " + lastEntry.getResult().getMessage(), first.getResult().getMessage());
	}
}
