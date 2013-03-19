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
package org.eclipse.gyrex.jobs;

import java.util.Map;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.core.runtime.IStatus;

/**
 * The context of a job execution.
 * <p>
 * The context provides access to the configuration of a specific job execution.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJobContext {

	/**
	 * Returns the {@link IRuntimeContext runtime context} for the current
	 * execution.
	 * 
	 * @return the {@link IRuntimeContext runtime context}
	 */
	IRuntimeContext getContext();

	/**
	 * Returns the identifier of the job.
	 * 
	 * @return the job id
	 */
	String getJobId();

	/**
	 * Returns the milliseconds from the Java epoch of
	 * <code>1970-01-01T00:00:00Z</code> when the job last started and finished
	 * successfully.
	 * <p>
	 * A job execution is considered successful if the result severity is
	 * neither {@link IStatus#ERROR} nor {@link IStatus#CANCEL}.
	 * </p>
	 * 
	 * @return the milliseconds from the Java epoch of
	 *         <code>1970-01-01T00:00:00Z</code> of the last start resulting in
	 *         a successful finish of the job, or <code>-1</code> if the job
	 *         never finished it's execution until now
	 * @since 1.2
	 */
	long getLastSuccessfulStart();

	/**
	 * Returns {@link IJob#getParameter() the job parameter} for the current
	 * execution.
	 * 
	 * @return an unmodifiable map of job parameter (an empty map if none were
	 *         defined).
	 */
	Map<String, String> getParameter();

}
