package org.eclipse.gyrex.cloud.internal;

import org.eclipse.osgi.util.NLS;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service tracker that launches a singleton instance of an Eclipse
 * application.
 */
final class EquinoxApplicationLauncher extends ServiceTracker<ApplicationDescriptor, ApplicationDescriptor> {

	private static final Logger LOG = LoggerFactory.getLogger(EquinoxApplicationLauncher.class);
	private final String applicationExtensionId;
	private ApplicationHandle runingApp;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 *            the bundle context
	 * @param applicationExtensionId
	 *            the application extension id
	 * @throws InvalidSyntaxException
	 *             if the applicationExtensionId is invalid
	 */
	public EquinoxApplicationLauncher(final BundleContext context, final String applicationExtensionId) throws InvalidSyntaxException {
		super(context, context.createFilter(NLS.bind("(&(objectClass={0})(service.pid={1}))", ApplicationDescriptor.class.getName(), applicationExtensionId)), null);
		this.applicationExtensionId = applicationExtensionId;
	}

	@Override
	public ApplicationDescriptor addingService(final ServiceReference<ApplicationDescriptor> reference) {
		final ApplicationDescriptor appDescriptor = super.addingService(reference);
		try {
			synchronized (this) {
				if (runingApp == null) {
					runingApp = appDescriptor.launch(null);
				}
			}
		} catch (final ApplicationException e) {
			LOG.error("Error starting application {}! {}", applicationExtensionId, e.getMessage());
		}
		return appDescriptor;
	}

	@Override
	public void removedService(final ServiceReference<ApplicationDescriptor> reference, final ApplicationDescriptor service) {
		synchronized (this) {
			if ((runingApp != null) && (runingApp.getApplicationDescriptor() == service)) {
				runingApp.destroy();
				runingApp = null;
			}
		}
		super.removedService(reference, service);
	}
}