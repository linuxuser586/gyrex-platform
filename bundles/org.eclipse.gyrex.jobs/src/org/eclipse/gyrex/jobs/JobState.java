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
	ABORTING

}
