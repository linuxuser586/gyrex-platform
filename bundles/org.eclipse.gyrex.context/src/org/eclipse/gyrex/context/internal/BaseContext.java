/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.di.IRuntimeContextInjector;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.services.IRuntimeContextServiceLocator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;

import org.osgi.framework.BundleContext;

public abstract class BaseContext extends PlatformObject implements IRuntimeContext {

	private final ContextRegistryImpl contextRegistry;
	private final IPath contextPath;

	public BaseContext(final IPath contextPath, final ContextRegistryImpl contextRegistry) {
		this.contextPath = contextPath;
		this.contextRegistry = contextRegistry;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IRuntimeContext))
			return false;
		final IRuntimeContext other = (IRuntimeContext) obj;
		if (contextPath == null) {
			if (other.getContextPath() != null)
				return false;
		} else if (!contextPath.toString().equals(other.getContextPath().toString()))
			return false;
		return true;
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public GyrexContextImpl get() {
		// returns the real context
		// we use this indirection in order to allow client code to hold
		// reference to runtime contexts for a longer period of time;
		// under the cover, a context may be disposed and updated an any point
		// in between without the client code noticing; the next time they
		// call any API they will just get a fresh _real_ context
		return contextRegistry.getRealContext(contextPath);
	}

	@Override
	public <T> T get(final Class<T> type) throws IllegalArgumentException {
		return get().get(type);
	}

	@Override
	public final IPath getContextPath() {
		return contextPath;
	}

	protected final ContextRegistryImpl getContextRegistry() {
		return contextRegistry;
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

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((contextPath == null) ? 0 : contextPath.toString().hashCode());
		return result;
	}

	@Override
	public String toString() {
		// TODO: should not leak context path here, we may need a story for this
		return contextPath.toString();
	}

}