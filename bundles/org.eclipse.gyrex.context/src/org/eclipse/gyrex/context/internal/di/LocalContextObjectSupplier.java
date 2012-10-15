/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal.di;

import java.lang.annotation.Annotation;

import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.IContextDisposalListener;
import org.eclipse.gyrex.context.internal.LocalContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

public class LocalContextObjectSupplier extends BaseContextObjectSupplier {

	private final LocalContext context;

	public LocalContextObjectSupplier(final LocalContext context) {
		this.context = context;
	}

	@Override
	protected void addDisposable(final IContextDisposalListener listener) {
		// not supported for local contexts
	}

	@Override
	protected Object getContextObject(final Class<?> key) {
		if (key == null)
			return null;

		if (IRuntimeContext.class.equals(key))
			// inject the local context to the underlying context
			return context;

		// find a local context object
		return context.getLocal(key);
	}

	@Override
	protected Object getQualifiedObjected(final Class<?> key, final Annotation annotation) {
		// not support for local contexts
		return null;
	}

	@Override
	protected IServiceProxy<?> trackService(final BundleContext bundleContext, final Class<?> serviceInterface, final String filter) throws InvalidSyntaxException {
		// not support for local contexts
		return null;
	}

}
