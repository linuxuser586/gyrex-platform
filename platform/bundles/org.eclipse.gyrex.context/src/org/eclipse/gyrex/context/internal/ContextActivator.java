package org.eclipse.gyrex.context.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.internal.manager.ContextManagerImpl;
import org.eclipse.gyrex.context.internal.preferences.GyrexContextPreferencesProvider;
import org.eclipse.gyrex.context.internal.provider.ObjectProviderRegistry;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.manager.IRuntimeContextManager;
import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

public class ContextActivator extends BaseBundleActivator {

	/** SYMBOLIC_NAME */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.context";

	private static final AtomicReference<ContextActivator> instanceRef = new AtomicReference<ContextActivator>();

	public static ContextActivator getInstance() {
		final ContextActivator contextActivator = instanceRef.get();
		if (null == contextActivator) {
			throw new IllegalStateException(NLS.bind("The Gyrex contextual runtime bundle {0} is inactive.", SYMBOLIC_NAME));
		}
		return contextActivator;
	}

	private final AtomicReference<IServiceProxy<IPreferencesService>> preferencesServiceProxyRef = new AtomicReference<IServiceProxy<IPreferencesService>>();

	/**
	 * Creates a new instance.
	 */
	public ContextActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);

		// track the preferences service
		preferencesServiceProxyRef.set(getServiceHelper().trackService(IPreferencesService.class));

		// register the preferences provider first so that it can be found by ObjectProviderRegistry immediately
		final GyrexContextPreferencesProvider preferencesProvider = new GyrexContextPreferencesProvider();
		getServiceHelper().registerService(RuntimeContextObjectProvider.class.getName(), preferencesProvider, "Eclipse Gyrex Project", "Contextual preferences object provider for contexts.", null, null);

		// start the object provider registry
		final ObjectProviderRegistry objectProviderRegistry = new ObjectProviderRegistry();
		objectProviderRegistry.start(context);
		addShutdownParticipant(objectProviderRegistry);

		// register the context registry
		final ContextRegistryImpl contextRegistry = new ContextRegistryImpl(objectProviderRegistry);
		getServiceHelper().registerService(IRuntimeContextRegistry.class.getName(), contextRegistry, "Eclipse.org Gyrex", "Eclipse Gyrex Contextual Runtime Registry", null, null);
		addShutdownParticipant(contextRegistry);

		// start the context manager
		final ContextManagerImpl contextManager = new ContextManagerImpl(contextRegistry);
		getServiceHelper().registerService(IRuntimeContextManager.class.getName(), contextManager, "Eclipse.org Gyrex", "Eclipse Gyrex Contextual Runtime Manager", null, null);
		addShutdownParticipant(contextManager);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);
		preferencesServiceProxyRef.set(null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#getDebugOptions()
	 */
	@Override
	protected Class getDebugOptions() {
		return ContextDebug.class;
	}

	public IPreferencesService getPreferencesService() {
		final IServiceProxy<IPreferencesService> serviceProxy = preferencesServiceProxyRef.get();
		if (null == serviceProxy) {
			throw createBundleInactiveException();
		}
		return serviceProxy.getService();
	}
}
