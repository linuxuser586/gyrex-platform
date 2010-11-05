package org.eclipse.gyrex.preferences.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesActivator extends BaseBundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(PreferencesActivator.class);

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
		ZooKeeperGate.addConnectionMonitor(ZooKeeperBasedPreferences.connectionMonitor);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		// flush the preferences
		try {
			getPreferencesService().getRootNode().node(PlatformScope.NAME).flush();
		} catch (final Exception e) {
			LOG.warn("Failed to flush platform preferences. Changes migt be lost. {}", e.getMessage());
		}

		ZooKeeperGate.removeConnectionMonitor(ZooKeeperBasedPreferences.connectionMonitor);

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
