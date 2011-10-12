package org.eclipse.gyrex.jobs.tests.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.osgi.framework.BundleContext;

public class JobsTestsActivator extends BaseBundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/**
	 * Creates a new instance.
	 */
	public JobsTestsActivator() {
		super("org.eclipse.gyrex.jobs.tests");
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		JobsTestsActivator.context = context;
		getServiceHelper().registerService(JobProvider.class, new TestJobsProvider(), null, null, null, null);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		JobsTestsActivator.context = null;
	}

}
