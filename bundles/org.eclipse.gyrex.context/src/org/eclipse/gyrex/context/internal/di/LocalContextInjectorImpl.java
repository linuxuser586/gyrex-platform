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

import org.eclipse.gyrex.context.di.IRuntimeContextInjector;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;
import org.eclipse.gyrex.context.internal.LocalContext;

import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.InjectorFactory;

/**
 * The injector implementation.
 */
@SuppressWarnings("restriction")
public class LocalContextInjectorImpl implements IRuntimeContextInjector {

	// the injector
	private final IInjector injector = InjectorFactory.makeInjector();
	private final GyrexContextObjectSupplier objectSupplier;
	private final BaseContextObjectSupplier localObjectSupplier;

	public LocalContextInjectorImpl(final GyrexContextImpl context, final LocalContext localContext) {
		objectSupplier = context.getInjector().getObjectSupplier();
		localObjectSupplier = new LocalContextObjectSupplier(localContext);
	}

	@Override
	public <T> T make(final Class<T> clazz) throws InjectionException {
		return injector.make(clazz, objectSupplier, localObjectSupplier);
	}

	@Override
	public void uninject(final Object object) throws InjectionException {
		injector.uninject(object, objectSupplier);
	}

}
