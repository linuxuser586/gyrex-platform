package org.eclipse.gyrex.preferences.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.preferences.IPreferencesService;

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
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		// flush the preferences
		getPreferencesService().getRootNode().node(PlatformScope.NAME).flush();

		instanceRef.set(null);

		preferenceServiceProxy.dispose();
		preferenceServiceProxy = null;
	}

	@Override
	protected Class getDebugOptions() {
		return PreferencesDebug.class;
	}

	public IPreferencesService getPreferencesService() {
		return preferenceServiceProxy.getService();
	}

}
