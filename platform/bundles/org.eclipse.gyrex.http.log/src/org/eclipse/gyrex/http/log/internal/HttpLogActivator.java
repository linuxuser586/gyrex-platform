package org.eclipse.gyrex.log.http.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class HttpLogActivator implements BundleActivator {

	private LogReaderServiceTracker logReaderServiceTracker;

	@Override
	public void start(final BundleContext context) throws Exception {
		logReaderServiceTracker = new LogReaderServiceTracker(context);
		logReaderServiceTracker.open();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		if (null != logReaderServiceTracker) {
			logReaderServiceTracker.close();
			logReaderServiceTracker = null;
		}
	}

}
