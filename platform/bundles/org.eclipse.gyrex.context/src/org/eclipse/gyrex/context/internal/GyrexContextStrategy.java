package org.eclipse.gyrex.context.internal;

import java.text.MessageFormat;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.services.IDisposable;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ILookupStrategy;
import org.eclipse.gyrex.common.logging.LogAudience;
import org.eclipse.gyrex.common.logging.LogImportance;
import org.eclipse.gyrex.common.logging.LogSource;
import org.eclipse.gyrex.preferences.PlatformScope;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
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
		final GyrexContextImpl runtimeContext = (GyrexContextImpl) context.get(GyrexContextImpl.GYREX_CONTEXT);
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
		// TODO Auto-generated method stub

	}

	@Override
	public Object lookup(final String name, final IEclipseContext context) {
		// get the runtime context
		final GyrexContextImpl runtimeContext = (GyrexContextImpl) context.get(GyrexContextImpl.class.getName());
		if (null == runtimeContext) {
			return null;
		}

		final IEclipsePreferences root = new PlatformScope().getNode(ContextActivator.SYMBOLIC_NAME);
		try {
			// get the preferences
			final String preferencesPath = runtimeContext.getContextPath().toString();
			if (!root.nodeExists(preferencesPath)) {
				return null;
			}

			// get the filter string
			final String filterString = root.node(preferencesPath).get(name, preferencesPath);
			if (null == filterString) {
				return null;
			}

			// create the filter
			Filter filter;
			try {
				filter = FrameworkUtil.createFilter(filterString);
			} catch (final InvalidSyntaxException e) {
				ContextActivator.getInstance().getLog().log(MessageFormat.format("Invalid syntax in context \"{0}\" key \"{1}\": {2} ", runtimeContext, name, e.getMessage()), e, runtimeContext, LogAudience.ADMIN, LogImportance.WARNING, LogSource.PLATFORM);
				return null;
			}

			// create context function
			return new GyrexContextObject(name, filter);
		} catch (final BackingStoreException e) {
			// ignore
		}

		// give up
		return null;
	}
}