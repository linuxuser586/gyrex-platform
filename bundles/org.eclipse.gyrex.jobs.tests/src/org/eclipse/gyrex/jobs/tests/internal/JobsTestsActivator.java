/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.tests.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.osgi.framework.BundleContext;

public class JobsTestsActivator extends BaseBundleActivator {

	private static JobsTestsActivator instance;

	public static JobsTestsActivator getInstance() {
		final JobsTestsActivator activator = instance;
		if (activator == null)
			throw new IllegalStateException("inactive");
		return activator;
	}

	public JobsTestsActivator() {
		super("org.eclipse.gyrex.jobs.tests");
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
		getServiceHelper().registerService(JobProvider.class, new TestJobsProvider(), null, null, null, null);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
	}

}
