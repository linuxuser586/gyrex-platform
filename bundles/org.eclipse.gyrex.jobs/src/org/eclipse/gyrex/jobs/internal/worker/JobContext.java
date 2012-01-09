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
package org.eclipse.gyrex.jobs.internal.worker;

import java.util.Collections;
import java.util.Map;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.IJobContext;

/**
 *
 */
public class JobContext implements IJobContext {

	private final Map<String, String> jobProperties;
	private final IRuntimeContext context;
	private final String jobId;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param info
	 */
	public JobContext(final IRuntimeContext context, final JobInfo info) {
		this.context = context;
		jobId = info.getJobId();
		jobProperties = Collections.unmodifiableMap(info.getJobProperties());
	}

	@Override
	public IRuntimeContext getContext() {
		return context;
	}

	@Override
	public String getJobId() {
		return jobId;
	}

	@Override
	public Map<String, String> getParameter() {
		return jobProperties;
	}

}
