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

import java.lang.reflect.Type;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;

import org.eclipse.e4.core.di.IDisposable;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;

/**
 * The Gyrex object supplier for the e4 injector.
 */
@SuppressWarnings("restriction")
public class GyrexContextObjectSupplier extends PrimaryObjectSupplier {

	private final GyrexContextImpl context;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public GyrexContextObjectSupplier(final GyrexContextImpl context) {
		this.context = context;
	}

	@Override
	public void get(final IObjectDescriptor[] descriptors, final Object[] actualValues, final IRequestor requestor, final boolean track, final boolean group) {
		// process descriptors
		for (int i = 0; i < descriptors.length; i++) {
			// only fill in missing values
			if (actualValues[i] != IInjector.NOT_A_VALUE) {
				continue;
			}

			final Class<?> key = getKey(descriptors[i]);
			if (IRuntimeContext.class.equals(key)) {
				actualValues[i] = context.getHandle();
			} else if (null != key) {
				final Object value = context.get(key);
				if (null != value) {
					actualValues[i] = value;
				}
			}
		}

		// hook dispose listener (we just support disposals at the moment)
		// TODO: investigate support for individual value changes
		if (track) {
			context.addDisposable(new IDisposable() {
				@Override
				public void dispose() {
					requestor.disposed(GyrexContextObjectSupplier.this);
				}
			});
		}
	}

	private Class<?> getKey(final IObjectDescriptor descriptor) {
		final Type elementType = descriptor.getDesiredType();
		if (elementType instanceof Class<?>) {
			return (Class<?>) elementType;
		}
		return null;
	}

}
