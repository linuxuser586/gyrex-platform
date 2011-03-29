/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.jobs;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.jobs.Job;

/**
 *
 */
public class JobsJobProvider extends JobProvider {

	/** STRING */
	private static final String PING_JOB = JobsActivator.SYMBOLIC_NAME + ".pingJob";

	/**
	 * Creates a new instance.
	 * 
	 * @param ids
	 */
	public JobsJobProvider() {
		super(Arrays.asList(PING_JOB));
	}

	@Override
	public Job newJob(final String id, final Map<String, String> jobParameter) {
		if (PING_JOB.equals(id)) {
			return new PingJob(jobParameter);
		}

		return null;
	}

}
