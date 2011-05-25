/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.history;

import org.eclipse.gyrex.jobs.IJob;

import org.eclipse.core.runtime.IStatus;

/**
 * History log entry for a {@link IJob job} provided via a
 * {@link IJobHistoryManager history manager}
 * <p>
 * An history item provides information about the time and state of a particular
 * execution of a job.
 * 
 * @see IStatus
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IJobHistoryEntry {

	/**
	 * @return a string representation of the {@link IStatus returned status} of
	 *         the job - never <code>null</code>
	 */
	String getResult();

	/**
	 * @return a severity of the {@link IStatus returned status} of the job
	 * @see IStatus#getSeverity()
	 */
	int getSeverity();

	/**
	 * @return the unix timestamp of the creation of the history item - should
	 *         be maximum close to the time, the job finished his execution or
	 *         an errar was thrown
	 */
	long getTimeStamp();
}
