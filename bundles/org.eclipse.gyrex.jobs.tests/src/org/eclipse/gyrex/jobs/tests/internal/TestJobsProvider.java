/**
 * Copyright (c) 2011, 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.jobs.tests.internal;

import java.util.Arrays;

import org.eclipse.gyrex.jobs.IJobContext;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.jobs.Job;

public class TestJobsProvider extends JobProvider {

	public static final String ID_TESTABLE_JOB = "tests.testable.job";

	/**
	 * Creates a new instance.
	 */
	public TestJobsProvider() {
		super(Arrays.asList(ID_TESTABLE_JOB));
	}

	@Override
	public Job createJob(final String typeId, final IJobContext context) throws Exception {
		if (ID_TESTABLE_JOB.equals(typeId)) {
			return new TestableJob(context);
		}
		return null;
	}

}
