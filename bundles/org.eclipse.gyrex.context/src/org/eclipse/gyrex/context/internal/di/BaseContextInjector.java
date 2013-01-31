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

import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.InjectorFactory;

@SuppressWarnings("restriction")
public abstract class BaseContextInjector implements IRuntimeContextInjector {

	// the injector
	private final IInjector injector = InjectorFactory.makeInjector();
	private final BaseContextObjectSupplier objectSupplier;

	public BaseContextInjector(final BaseContextObjectSupplier objectSupplier) {
		this.objectSupplier = objectSupplier;
	}

	public BaseContextObjectSupplier getObjectSupplier() {
		return objectSupplier;
	}

	@Override
	public <T> T make(final Class<T> clazz) throws InjectionException {
		return injector.make(clazz, getObjectSupplier());
	}

	@Override
	public void uninject(final Object object) throws InjectionException {
		injector.uninject(object, getObjectSupplier());
	}

}
