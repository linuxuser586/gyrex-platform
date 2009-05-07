package org.eclipse.gyrex.context.internal;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.services.IDisposable;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ILookupStrategy;
import org.eclipse.gyrex.common.logging.LogAudience;
import org.eclipse.gyrex.common.logging.LogImportance;
import org.eclipse.gyrex.common.logging.LogSource;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.provider.TypeRegistration;
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
		// empty

	}

	/**
	 * Finds a filter if available from the context configuration.
	 * 
	 * @param context
	 *            the context
	 * @param typeName
	 *            the requested type name
	 * @return the filter (maybe <code>null</code> if none is explicitly defined
	 *         for the context
	 */
	private Filter findFilter(final IRuntimeContext context, final String typeName) {
		// the preferences
		final IEclipsePreferences root = new PlatformScope().getNode(ContextActivator.SYMBOLIC_NAME);

		// the context path
		IPath contextPath = context.getContextPath();

		// lookup filter in this context
		Filter filter = readFilterFromPreferences(context, root, contextPath, typeName);
		if (null != filter) {
			return filter;
		}

		// search parent contexts
		while ((null == filter) && !contextPath.isRoot()) {
			filter = readFilterFromPreferences(context, root, contextPath = contextPath.removeLastSegments(1), typeName);
		}

		// return what we have (may be nothing)
		return filter;
	}

	@Override
	public Object lookup(final String name, final IEclipseContext context) {
		// get the runtime context
		final GyrexContextImpl runtimeContext = (GyrexContextImpl) context.get(GyrexContextImpl.class.getName());
		if (null == runtimeContext) {
			return null;
		}

		// check that there is a type registration
		final TypeRegistration typeRegistration = runtimeContext.getContextRegistry().getObjectProviderRegistry().getType(name);
		if (null == typeRegistration) {
			return null;
		}

		// find the filter
		final Filter filter = findFilter(runtimeContext, name);

		// return our helper context function to instantiate the object
		return new GyrexContextObject(runtimeContext, typeRegistration, name, filter);
	}

	/**
	 * Reads a filter from the preferences of the specified context path.
	 * 
	 * @param context
	 *            the context
	 * @param root
	 *            the preferences root node
	 * @param contextPath
	 *            the context path
	 * @param typeName
	 *            the type name
	 * @return
	 */
	private Filter readFilterFromPreferences(final IRuntimeContext context, final IEclipsePreferences root, final IPath contextPath, final String typeName) {
		// get the preferences
		final String preferencesPath = contextPath.toString();
		try {
			if (!root.nodeExists(preferencesPath)) {
				return null;
			}
		} catch (final BackingStoreException e) {
			ContextActivator.getInstance().getLog().log(MessageFormat.format("Error while accessing the preferences backend for context path \"{0}\": {1}", contextPath, e.getMessage()), e, context, LogAudience.ADMIN, LogImportance.WARNING, LogSource.PLATFORM);
			return null;
		}

		// get the filter string
		final String filterString = root.node(preferencesPath).get(typeName, null);
		if (null == filterString) {
			return null;
		}

		// create the filter
		try {
			return FrameworkUtil.createFilter(filterString);
		} catch (final InvalidSyntaxException e) {
			ContextActivator.getInstance().getLog().log(MessageFormat.format("Invalid syntax in context path \"{0}\" key \"{1}\": {2} ", contextPath, typeName, e.getMessage()), e, context, LogAudience.ADMIN, LogImportance.WARNING, LogSource.PLATFORM);
			return null;
		}
	};

}