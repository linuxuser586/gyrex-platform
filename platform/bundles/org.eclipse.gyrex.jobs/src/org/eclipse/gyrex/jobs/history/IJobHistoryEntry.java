/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.history;

import org.eclipse.gyrex.jobs.IJob;

import org.eclipse.core.runtime.IStatus;

/**
 * History log entry for a {@link IJob job} provided via a
 * {@link IJobHistoryManager history manager}.
 * <p>
 * A history item provides information about the time and state of a particular
 * execution of a job.
 * </p>
 * <p>
 * Job history entries implement the {@link Comparable} interface based on
 * {@link #getTimeStamp()} in reverse order, i.e. a more recent (aka. "later")
 * entry is less then an earlier (aka. "older") entry.
 * </p>
 * 
 * @see IStatus
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IJobHistoryEntry extends Comparable<IJobHistoryEntry> {

	/**
	 * Returns the trigger specified when the the job producing the result was
	 * cancelled.
	 * 
	 * @return the trigger specified when the the job producing the result was
	 *         cancelled (may only be not <code>null</code> if the job execution
	 *         was cancelled)
	 */
	String getCancelledTrigger();

	/**
	 * Returns the trigger specified when the the job producing the result was
	 * queued.
	 * 
	 * @return the trigger specified when the the job producing the result was
	 *         queued
	 */
	String getQueuedTrigger();

	/**
	 * Returns the status of the execution result.
	 * 
	 * @return a status (never <code>null</code>)
	 */
	IStatus getResult();

	/**
	 * Returns the milliseconds from the Java epoch of
	 * <code>1970-01-01T00:00:00Z</code> when the result was created.
	 * 
	 * @return the milliseconds from the Java epoch of
	 *         <code>1970-01-01T00:00:00Z</code> when the result was created
	 */
	long getTimeStamp();
}
