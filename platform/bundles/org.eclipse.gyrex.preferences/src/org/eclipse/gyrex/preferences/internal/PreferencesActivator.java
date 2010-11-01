package org.eclipse.gyrex.preferences.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.common.services.ServiceNotAvailableException;
import org.eclipse.gyrex.preferences.internal.spi.IPlatformPreferencesStorage;

import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;

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

	private IServiceProxy<IPreferencesService> preferenceServiceProxy;
	private IServiceProxy<IPlatformPreferencesStorage> storageProxy;

	/**
	 * Called by the framework to create a new instance.
	 */
	public PreferencesActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);
		preferenceServiceProxy = getServiceHelper().trackService(IPreferencesService.class);
		storageProxy = getServiceHelper().trackService(IPlatformPreferencesStorage.class);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);

		preferenceServiceProxy.dispose();
		preferenceServiceProxy = null;

		storageProxy.dispose();
		storageProxy = null;

		// flush the preferences
		new InstanceScope().getNode(SYMBOLIC_NAME).flush();
	}

	public IPreferencesService getPreferencesService() {
		return preferenceServiceProxy.getService();
	}

	public IPlatformPreferencesStorage getStorage() {
		try {
			return storageProxy.getService();
		} catch (final ServiceNotAvailableException e) {
			return new InstanceLocalStorage();
		}
	}
}
