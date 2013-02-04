/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO Service GmbH and others.
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
import java.util.Collection;

import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;
import org.eclipse.gyrex.context.internal.IContextDisposalListener;
import org.eclipse.gyrex.context.provider.di.ExtendedObjectResolver;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import org.apache.commons.lang.StringUtils;

public class GyrexContextObjectSupplier extends BaseContextObjectSupplier {

	private final GyrexContextImpl contextImpl;

	public GyrexContextObjectSupplier(final GyrexContextImpl contextImpl) {
		this.contextImpl = contextImpl;
	}

	@Override
	protected void addDisposable(final IContextDisposalListener listener) {
		contextImpl.addDisposable(listener);
	}

	@Override
	protected Object getContextObject(final Class<?> key) {
		if (key == null)
			return null;

		if (IRuntimeContext.class.equals(key))
			// inject handle to the context
			return contextImpl.getHandle();

		// find a context object
		return contextImpl.get(key);
	}

	@Override
	protected Object getQualifiedObjected(final Class<?> type, final Annotation annotation) {
		if (!(annotation.annotationType() instanceof Class<?>))
			// ignore unknown annotation types
			return null;

		final BundleContext bundleContext = ContextActivator.getInstance().getBundle().getBundleContext();
		final String filter = '(' + ExtendedObjectResolver.ANNOTATION_PROPERTY + '=' + annotation.annotationType().getName() + ')';
		try {
			final Collection<ServiceReference<ExtendedObjectResolver>> refs = bundleContext.getServiceReferences(ExtendedObjectResolver.class, filter);
			if (!refs.isEmpty()) {
				for (final ServiceReference<ExtendedObjectResolver> ref : refs) {
					final ExtendedObjectResolver resolver = bundleContext.getService(ref);
					if (resolver != null) {
						try {
							// FIXME: we should look at supporting dynamic behavior
							final Object result = resolver.get(type, contextImpl.getHandle(), annotation);
							if (result != null)
								return result;
						} finally {
							bundleContext.ungetService(ref);
						}
					}
				}
			}
		} catch (final InvalidSyntaxException e) {
			throw new IllegalStateException(String.format("Error computing filter expression (%s) for annotation (%s). Please report Gyrex bug! (%s)", filter, annotation, e.getMessage()));
		}
		return null;
	}

	@Override
	protected IServiceProxy<?> trackService(final BundleContext bundleContext, final Class<?> serviceInterface, final String filter) throws InvalidSyntaxException {
		if (StringUtils.isNotBlank(filter))
			return contextImpl.getServiceLocator(bundleContext).trackService(serviceInterface, filter);

		return contextImpl.getServiceLocator(bundleContext).trackService(serviceInterface);
	}
}
