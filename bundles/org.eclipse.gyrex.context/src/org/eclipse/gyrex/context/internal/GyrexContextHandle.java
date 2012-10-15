/**
 * Copyright (c) 2009, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.internal;

import org.eclipse.gyrex.context.IModifiableRuntimeContext;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.di.IRuntimeContextInjector;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.services.IRuntimeContextServiceLocator;

import org.eclipse.core.runtime.IPath;

import org.osgi.framework.BundleContext;

/**
 * Handle to a Gyrex context.
 * <p>
 * We use handles so that clients can hold onto a "context" for a longer time
 * but we can dispose the internal context at any time.
 * </p>
 */
public class GyrexContextHandle extends BaseContext implements IRuntimeContext {

	public GyrexContextHandle(final IPath contextPath, final ContextRegistryImpl contextRegistry) {
		super(contextPath, contextRegistry);
	}

	@Override
	public IModifiableRuntimeContext createWorkingCopy() {
		return new LocalContext(this, getContextRegistry());
	}

	@Override
	public <T> T get(final Class<T> type) throws IllegalArgumentException {
		return get().get(type);
	}

	@Override
	public IRuntimeContextInjector getInjector() {
		return get().getInjector();
	}

	@Override
	public IRuntimeContextPreferences getPreferences() {
		return get().getPreferences();
	}

	@Override
	public IRuntimeContextServiceLocator getServiceLocator(final BundleContext bundleContext) {
		return get().getServiceLocator(bundleContext);
	}
}
