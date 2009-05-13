package org.eclipse.gyrex.context.internal;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.services.IDisposable;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ILookupStrategy;
import org.eclipse.gyrex.context.internal.configuration.ContextConfiguration;
import org.eclipse.gyrex.context.internal.provider.TypeRegistration;
import org.eclipse.gyrex.preferences.PlatformScope;
import org.osgi.framework.Filter;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This is the e4 context strategy implementation for the Gyrex contextual
 * runtime.
 * <p>
 * The context strategy is responsible for the lookup of contextual objects. It
 * resolves objects to the one configured for the context. It also keeps track
 * for instantiated context objects and ensures that everything is properly
 * updated and/or released/disposed when necessary.
 * </p>
 */
final class GyrexContextStrategy implements ILookupStrategy, IDisposable {

	public static final GyrexContextStrategy SINGLETON = new GyrexContextStrategy();

	/**
	 * Hidden constructor.
	 */
	private GyrexContextStrategy() {
		// empty
	}

	@Override
	public boolean containsKey(final String name, final IEclipseContext context) {
		// get the runtime context
		final GyrexContextImpl runtimeContext = (GyrexContextImpl) context.get(GyrexContextImpl.ECLIPSE_CONTEXT_KEY);
		if (null == runtimeContext) {
			return false;
		}

		// we simply check if there is an entry in the preferences
		final IEclipsePreferences root = new PlatformScope().getNode(ContextActivator.SYMBOLIC_NAME);
		try {
			final String preferencesPath = runtimeContext.getContextPath().toString();
			return root.nodeExists(preferencesPath) && (root.node(preferencesPath).get(name, null) != null);
		} catch (final BackingStoreException e) {
			// ignore
		}

		// not available
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.services.IDisposable#dispose()
	 */
	@Override
	public void dispose() {
		// empty

	}

	@Override
	public Object lookup(final String name, final IEclipseContext context) {
		// get the runtime context
		final GyrexContextImpl runtimeContext = (GyrexContextImpl) context.get(GyrexContextImpl.ECLIPSE_CONTEXT_KEY);
		if (null == runtimeContext) {
			return null;
		}

		// check that there is a type registration
		final TypeRegistration typeRegistration = runtimeContext.getContextRegistry().getObjectProviderRegistry().getType(name);
		if (null == typeRegistration) {
			return null;
		}

		// find the filter
		final Filter filter = ContextConfiguration.findFilter(runtimeContext, name);

		// return our helper context function to instantiate the object
		return new GyrexContextObject(runtimeContext, typeRegistration, name, filter);
	}

}