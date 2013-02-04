/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.util;

import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A job which waits for a job to finish.
 */
public final class WaitForJobToFinishJob extends Job {
	private final Job job;
	private final CountDownLatch signal;

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 * @param job
	 * @param signal
	 */
	public WaitForJobToFinishJob(final String name, final Job job, final CountDownLatch signal) {
		super(name);
		this.job = job;
		this.signal = signal;
		setSystem(true);
		setPriority(Job.SHORT);
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			if (job.getState() == Job.RUNNING) {
				job.join();
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			signal.countDown();
		}
		return Status.OK_STATUS;
	}
}