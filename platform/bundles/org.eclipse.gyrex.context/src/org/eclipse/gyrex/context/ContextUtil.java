/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context;

import org.eclipse.core.runtime.IPath;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Provides access to Gyrex contexts.
 * <p>
 * Access to Gyrex contexts is provided through this central class. Internally,
 * it uses a pluggable strategy for resolving the path to a context.
 * </p>
 * <p>
 * Note, using security it is possible to limit access to certain contexts to a
 * trusted group.
 * </p>
 * 
 * @deprecated will be removed later, please use the
 *             {@link IRuntimeContextRegistry}
 */
@Deprecated
public class ContextUtil {

	/**
	 * Returns the Gyrex context with the specified path.
	 * <p>
	 * Note, security may be used to verify that the caller is allowed to access
	 * the specified context.
	 * </p>
	 * 
	 * @param contextPath
	 *            the context path
	 * @return
	 */
	public static IRuntimeContext get(final IPath contextPath) {
		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		if (stackTrace.length <= 1) {
			return null;
		}
		final Bundle bundle = FrameworkUtil.getBundle(stackTrace[stackTrace.length - 2].getClass());
		if (null == bundle) {
			return null;
		}

		final BundleContext bundleContext = bundle.getBundleContext();
		if (null == bundleContext) {
			return null;
		}

		final ServiceReference serviceReference = bundleContext.getServiceReference(IRuntimeContextRegistry.class.getName());
		if (null == serviceReference) {
			return null;
		}

		final IRuntimeContextRegistry service = (IRuntimeContextRegistry) bundleContext.getService(serviceReference);
		if (null == service) {
			return null;
		}

		try {
			return service.get(contextPath);
		} finally {
			bundleContext.ungetService(serviceReference);
		}
	}

}
