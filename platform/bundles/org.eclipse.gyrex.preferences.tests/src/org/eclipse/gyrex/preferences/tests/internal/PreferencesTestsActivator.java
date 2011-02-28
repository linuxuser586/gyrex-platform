package org.eclipse.gyrex.preferences.tests.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class PreferencesTestsActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		PreferencesTestsActivator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		PreferencesTestsActivator.context = null;
	}

}
