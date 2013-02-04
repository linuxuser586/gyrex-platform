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
package org.eclipse.gyrex.jobs.internal.manager;

/**
 * A monitor for notifications about job state changes.
 */
public interface IJobStateWatch {

	/**
	 * The job changed it's state.
	 * <p>
	 * Note, the watch might be triggered while the job manager still holds an
	 * internal job modification lock. Thus, any attempts to further modify the
	 * state of a job must be done asynchronously to avoid dead-locks.
	 * </p>
	 * 
	 * @param jobId
	 */
	void jobStateChanged(String jobId);

}
