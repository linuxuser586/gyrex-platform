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

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * The context of a job execution.
 * <p>
 * A job context provides access to the configuration of a specific job
 * execution. It also provides conventions for delta processing (such as
 * {@link #getLastSuccessfulStart()}) and logging ({@link #getLogger()}).
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
	 * <p>
	 * Job implementors might use this value for implementing delta processing.
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
	 * Returns a job specific {@link Logger SLF4J logger}.
	 * <p>
	 * Job implementors are encourage to use this logger for logging job
	 * activity details. Jobs return a {@link IStatus result} when done. However
	 * such a result is typically less detailed, i.e. it usually contains a
	 * summary but no detailed processing log.
	 * </p>
	 * <p>
	 * Per convention, the {@link Logger#getName() logger name} will be computed
	 * based on the prefix <code>job.</code> plus the job type id followed by a
	 * dot followed by the job id. The worker engine might attach special
	 * collectors to the returned logger for the duration of the job execution.
	 * They might collect logs into a persistent, centralized data store.
	 * </p>
	 * <p>
	 * During the job execution the {@link MDC} will be prepared with the
	 * context path (key <code>gyrex.contextPath</code>) and the job id (key
	 * <code>gyrex.jobId</code>).
	 * </p>
	 * 
	 * @return a job specific logger
	 * @since 1.2
	 */
	Logger getLogger();

	/**
	 * Returns {@link IJob#getParameter() the job parameter} for the current
	 * execution.
	 * 
	 * @return an unmodifiable map of job parameter (an empty map if none were
	 *         defined).
	 */
	Map<String, String> getParameter();

}
