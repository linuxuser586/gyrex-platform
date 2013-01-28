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
package org.eclipse.gyrex.jobs;

/**
 * The state of a {@link IJob}.
 */
public enum JobState {

	/**
	 * Job state indicating that a job is not currently doing anything (i.e.,
	 * it's neither waiting or running).
	 * 
	 * @see IJob#getState()
	 */
	NONE,

	/**
	 * Job state indicating that a job is waiting to run.
	 * 
	 * @see IJob#getState()
	 */
	WAITING,

	/**
	 * Job state indicating that a job is running.
	 * 
	 * @see IJob#getState()
	 */
	RUNNING,

	/**
	 * Job state indicating that a job was requested to be canceled.
	 * 
	 * @see IJob#getState()
	 */
	ABORTING;

	/**
	 * Converts the specified value to a valid {@link JobState}.
	 * <p>
	 * If the value is a {@link JobState} it will be returned as is.
	 * </p>
	 * <p>
	 * If the value is a {@link String} it will be evaluated using
	 * {@link JobState#valueOf(String)}. If the evaluation fails,
	 * {@link JobState#NONE} will be returned.
	 * </p>
	 * <p>
	 * If the value can't be converted, {@link JobState#NONE} will be returned.
	 * </p>
	 * 
	 * @param value
	 *            the value to convert
	 * @return the job state (never <code>null</code>)
	 */
	public static JobState toState(final Object value) {
		if (value instanceof JobState)
			return (JobState) value;
		if (value instanceof String) {
			try {
				return Enum.valueOf(JobState.class, (String) value);
			} catch (final IllegalArgumentException e) {
				return JobState.NONE;
			}
		}

		return JobState.NONE;
	}

}
