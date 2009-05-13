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
package org.eclipse.gyrex.context.internal;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;

/**
 * Handle to a Gyrex context.
 * <p>
 * we use handles so that clients can hold onto a "context" for a longer time
 * but we can dispose the internal context at any time.
 * </p>
 */
public class GyrexContextHandle extends PlatformObject implements IRuntimeContext {

	private final IPath contextPath;
	private final ContextRegistryImpl contextRegistry;
	private final AtomicReference<WeakReference<GyrexContextImpl>> realContext = new AtomicReference<WeakReference<GyrexContextImpl>>();

	/**
	 * Creates a new instance.
	 * 
	 * @param contextPath
	 */
	public GyrexContextHandle(final IPath contextPath, final ContextRegistryImpl contextRegistry) {
		this.contextPath = contextPath;
		this.contextRegistry = contextRegistry;
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public GyrexContextImpl get() {
		GyrexContextImpl gyrexContextImpl;
		final WeakReference<GyrexContextImpl> weakReference = realContext.get();
		if (null != weakReference) {
			gyrexContextImpl = weakReference.get();
			if ((null == gyrexContextImpl) || gyrexContextImpl.isDisposed()) {
				gyrexContextImpl = contextRegistry.getRealContext(getContextPath());
				realContext.set(new WeakReference<GyrexContextImpl>(gyrexContextImpl));
			}
		} else {
			gyrexContextImpl = contextRegistry.getRealContext(getContextPath());
			realContext.set(new WeakReference<GyrexContextImpl>(gyrexContextImpl));
		}
		return gyrexContextImpl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.context.IRuntimeContext#get(java.lang.Class)
	 */
	@Override
	public <T> T get(final Class<T> type) throws IllegalArgumentException {
		return get().get(type);
	}

	@Override
	public IPath getContextPath() {
		return contextPath;
	}

	@Override
	public String toString() {
		return "Gyrex Context Handle [" + contextPath.toString() + "]";
	}
}
