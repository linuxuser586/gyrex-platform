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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import org.eclipse.gyrex.common.internal.services.IServiceProxyChangeListener;
import org.eclipse.gyrex.common.internal.services.IServiceProxyDisposalListener;
import org.eclipse.gyrex.common.internal.services.ServiceProxy;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.common.services.ServiceNotAvailableException;
import org.eclipse.gyrex.common.services.annotations.DynamicService;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.ContextDebug;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;
import org.eclipse.gyrex.context.internal.IContextDisposalListener;

import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServicePermission;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Gyrex object supplier for the e4 injector.
 */
@SuppressWarnings("restriction")
public class GyrexContextObjectSupplier extends PrimaryObjectSupplier {

	/** executes re-injection on service proxy disposal */
	private static final class ReinjectOnDisposal implements IServiceProxyDisposalListener {
		private final IRequestor requestor;

		private ReinjectOnDisposal(final IRequestor requestor) {
			this.requestor = requestor;
		}

		@Override
		public void disposed(final IServiceProxy<?> proxy) {
			if (!requestor.isValid()) {
				return; // ignore
			}
			if (ContextDebug.injection) {
				LOG.debug("Service proxy ({}) disposed, re-injecting ({})", proxy, requestor);
			}
			requestor.resolveArguments(false);
			requestor.execute();
		}
	}

	/** executes re-injection on service changes */
	private static final class ReinjectOnUpdate implements IServiceProxyChangeListener {
		private final IRequestor requestor;

		private ReinjectOnUpdate(final IRequestor requestor) {
			this.requestor = requestor;
		}

		@Override
		public boolean serviceChanged(final IServiceProxy<?> proxy) {
			if (!requestor.isValid()) {
				return false; // remove this listener
			}
			if (ContextDebug.injection) {
				LOG.debug("Service proxy ({}) changed, re-injecting ({})", proxy, requestor);
			}
			requestor.resolveArguments(false);
			requestor.execute();
			return true;
		}
	}

	/**
	 * allows to enable dynamic injection (not recommended at this point, see
	 * http://dev.eclipse.org/mhonarc/lists/e4-dev/msg05749.html)
	 */
	public static boolean dynamicInjectionEnabled = Boolean.getBoolean("gyrex.context.dynamicInjection");

	/** logger */
	private static final Logger LOG = LoggerFactory.getLogger(GyrexContextObjectSupplier.class);

	/** associated context */
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
	public void get(final IObjectDescriptor[] descriptors, final Object[] actualValues, final IRequestor requestor, final boolean initial, final boolean track, final boolean group) {
		// process descriptors
		for (int i = 0; i < descriptors.length; i++) {
			// only fill in missing values
			if (actualValues[i] != IInjector.NOT_A_VALUE) {
				continue;
			}

			final IObjectDescriptor descriptor = descriptors[i];
			final Class<?> key = getKey(descriptor);
			if (IRuntimeContext.class.equals(key)) {
				// inject the handle to the underlying context
				actualValues[i] = context.getHandle();
			} else if (null != key) {
				// find a context object
				Object value = context.get(key);

				// try OSGi service
				if (null == value) {
					value = getService(key, descriptor, requestor, initial, track, group);
				}

				// use what we have (if not null)
				if (null != value) {
					actualValues[i] = value;
				}
			}
		}

		// hook dispose listener (we just support disposals at the moment)
		// TODO: investigate support for individual value changes
		// FIXME: this may also be subjected to memory issues (too many listeners)
		if (track) {
			context.addDisposable(new IContextDisposalListener() {
				@Override
				public void contextDisposed(final IRuntimeContext handle) {
					if (requestor.isValid()) {
						requestor.disposed(GyrexContextObjectSupplier.this);
					}
				}
			});
		}
	}

	private BundleContext getBundleContext(final IRequestor requestor) {
		final Class<?> requestingObjectClass = requestor.getRequestingObjectClass();
		if (null == requestingObjectClass) {
			return null;
		}

		final Bundle bundle = FrameworkUtil.getBundle(requestingObjectClass);
		if (null == bundle) {
			return null;
		}

		return bundle.getBundleContext();
	}

	private Class<?> getClass(final Type type) {
		if (type instanceof Class<?>) {
			return (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			try {
				return (Class<?>) ((ParameterizedType) type).getRawType();
			} catch (final Exception ignored) {
				if (ContextDebug.injection) {
					LOG.debug("Unable to cast raw type of ParameterizedType ({}) to Class.", type, ignored);
				}
			}
		}
		return null;
	}

	private Class<?> getCollectionElementType(final IObjectDescriptor descriptor) {
		// assume that the element is a ParameterizedType
		final Type elementType = descriptor.getDesiredType();
		if (elementType instanceof ParameterizedType) {
			final Type[] typeArguments = ((ParameterizedType) elementType).getActualTypeArguments();
			// Collections can only be parameterized with one argument
			if (typeArguments.length == 1) {
				return getClass(typeArguments[0]);
			}
		}

		if (ContextDebug.injection) {
			LOG.debug("Unable to detect collection element type for object descriptor ({}).", descriptor);
		}
		return null;
	}

	private Class<?> getKey(final IObjectDescriptor descriptor) {
		return getClass(descriptor.getDesiredType());
	}

	private Object getService(final Class<?> key, final IObjectDescriptor descriptor, final IRequestor requestor, final boolean initial, final boolean track, final boolean group) {
		// look for @Service annotation
		final DynamicService serviceAnnotation = descriptor.getQualifier(DynamicService.class);
		if (null == serviceAnnotation) {
			if (ContextDebug.injection) {
				LOG.debug("No @Service annotation present on ({}).", descriptor);
			}
			return null;
		}

		// the context does not contain a context object
		// however, there might still be a service available
		// that the requestor is able to use (in the context)
		// thus, get the bundle context of the requestor
		final BundleContext bundleContext = getBundleContext(requestor);
		if (null == bundleContext) {
			if (ContextDebug.injection) {
				LOG.debug("No bundle context found for requestor ({}).", requestor);
			}
			return null;
		}

		// check if we have a collection
		final boolean collectionOfServices = Collection.class.isAssignableFrom(key) || List.class.isAssignableFrom(key);

		// detect OSGi service interface
		final Class<?> serviceInterface = collectionOfServices ? getCollectionElementType(descriptor) : key;

		// verify that the bundle requesting the service has the necessary permissions to use it
		// (stack-based permission checks will only check the DI engine stack; thus we need
		// to verify manually, similar to section 112.9.3 of DS spec with DCR)

		if (!bundleContext.getBundle().hasPermission(new ServicePermission(serviceInterface.getName(), "get"))) {
			if (ContextDebug.injection) {
				LOG.debug("Bundle ({}) does not have permissions to get service ({}) for requestor ({}).", new Object[] { bundleContext.getBundle(), serviceInterface, requestor });
			}
			return null;
		}

		// track service
		final IServiceProxy<?> proxy;

		// look for additional filter
		final String filter = serviceAnnotation.filter();
		if (StringUtils.isNotBlank(filter)) {
			try {
				proxy = context.getServiceLocator(bundleContext).trackService(serviceInterface, filter);
			} catch (final InvalidSyntaxException e) {
				throw new InjectionException(String.format("Error parsing filter '%s' specified in annotation at %s. %s", filter, descriptor, e.getMessage()), e);
			}
		} else {
			proxy = context.getServiceLocator(bundleContext).trackService(serviceInterface);
		}

		// test for collection / list
		if (collectionOfServices) {
			// log message
			if (ContextDebug.injection) {
				LOG.debug("Injecting service collection ({}).", proxy);
			}

			// inject read-only collection which contains service references
			return proxy.getServices();
		}

		if (track && dynamicInjectionEnabled && !serviceAnnotation.preferProxy()) {
			// try to inject actual service object because we can update dynamically
			try {
				final Object service = proxy.getService();

				// dynamic injection was successful
				if (ContextDebug.injection) {
					LOG.debug("Injected real service instance ({}).", proxy);
				}

				// add a listener that updates the injection whenever the service changes
				((ServiceProxy<?>) proxy).addChangeListener(new ReinjectOnUpdate(requestor));
				// add a listener that updates the injection whenever the proxy goes
				((ServiceProxy<?>) proxy).addDisposalListener(new ReinjectOnDisposal(requestor));

				// done;
				return service;
			} catch (final ServiceNotAvailableException e) {
				if (ContextDebug.injection) {
					LOG.debug("Service not available ({}), falling back to proxy.", proxy);
				}
				// fallback to proxy
				return proxy.getProxy();
			}
		} else {
			// inject proxy because services may come and go at any time
			return proxy.getProxy();
		}
	}

	@Override
	public void pauseRecording() {
		// no-op
	}

	@Override
	public void resumeRecording() {
		// no-op
	}

}
