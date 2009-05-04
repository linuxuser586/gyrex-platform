package org.eclipse.gyrex.preferences.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.osgi.framework.BundleContext;

public class PreferencesActivator extends BaseBundleActivator {

	/** BSN */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.preferences";

	private static final AtomicReference<PreferencesActivator> instanceRef = new AtomicReference<PreferencesActivator>();

	public static PreferencesActivator getInstance() {
		final PreferencesActivator preferencesActivator = instanceRef.get();
		if (null == preferencesActivator) {
			throw new IllegalStateException("inactive");
		}
		return preferencesActivator;
	}

	/** the service proxy */
	private IServiceProxy<IPreferencesService> preferenceServiceProxy;

	/**
	 * Called by the framework to create a new instance.
	 */
	public PreferencesActivator() {
		super(SYMBOLIC_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);
		preferenceServiceProxy = getServiceHelper().trackService(IPreferencesService.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);
		preferenceServiceProxy.dispose();
		preferenceServiceProxy = null;
	}

	public IPreferencesService getPreferencesService() {
		return preferenceServiceProxy.getService();
	}
}
