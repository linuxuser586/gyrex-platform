/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.common.services;

import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.cloudfree.common.internal.services.ServiceProxyPool;
import org.eclipse.cloudfree.common.runtime.BaseBundleActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A bundle helper for working with OSGi services.
 * <p>
 * This class provides methods for registering and consuming OSGi services in a
 * way that makes development easier.
 * </p>
 * <p>
 * Services may be registered using one of the <code>register..</code> methods.
 * They can also be unregistered or updated while the bundle is active via the
 * <code>unregister..</code> and <code>update..</code> methods provided in this
 * class. When the associated bundle (provided at initialization time) is
 * stopped the registered services will be unregistered by the framework
 * according to {@link Bundle#stop()}. {@link ServiceRegistration}s are
 * maintained internally and destroyed when {@link #stop()} is called.
 * </p>
 * <p>
 * Services may be imported using one of the <code>consume..</code> methods. The
 * imported services will be tracked internally. The handed out service objects
 * are proxies which delegate to an available service. If no service is
 * available, the proxies may fail, or discard a call depending on the desired
 * behavior.
 * </p>
 * <p>
 * This class may be instantiated directly by clients. However, the use through
 * {@link BaseBundleActivator} is encouraged.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class BundleServiceHelper {

	/**
	 * Wraps a service registration so that its registration object is removed
	 * on {@link #unregister()}.
	 */
	private final class HelperServiceRegistration implements ServiceRegistration {
		private final ServiceRegistration serviceRegistration;

		private HelperServiceRegistration(final ServiceRegistration serviceRegistration) {
			this.serviceRegistration = serviceRegistration;
		}

		@Override
		public ServiceReference getReference() {
			return serviceRegistration.getReference();
		}

		@Override
		public void setProperties(final Dictionary properties) {
			serviceRegistration.setProperties(properties);
		}

		@Override
		public void unregister() {
			try {
				serviceRegistration.unregister();
			} finally {
				registeredServices.remove(serviceRegistration);
			}
		}
	}

	private final AtomicReference<BundleContext> contextRef = new AtomicReference<BundleContext>();
	private final String symbolicName;
	private final List<ServiceRegistration> registeredServices = new CopyOnWriteArrayList<ServiceRegistration>();
	private final ServiceProxyPool serviceProxyPool;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public BundleServiceHelper(final BundleContext context) {
		contextRef.set(context);
		symbolicName = context.getBundle().getSymbolicName();
		serviceProxyPool = new ServiceProxyPool(context);
	}

	/**
	 * Called by {@link BaseBundleActivator#stop(BundleContext)} when the bundle
	 * is stopped.
	 * <p>
	 * When this method is called the context reference will be cleared so that
	 * no new service can be registered or consumed. Additionally, all resources
	 * will be released. Thus, consumed services will become unavailable and
	 * monitored service registrations will be released but not unregistered
	 * because this will be done by the framework anyway.
	 * </p>
	 * <p>
	 * Note, this method is typically called by the framework during bundle
	 * shutdown. Clients must not call it directly.
	 * </p>
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void dispose() {
		contextRef.set(null);
		registeredServices.clear();
	}

	private IllegalStateException newInactiveException() {
		return new IllegalStateException(MessageFormat.format("Bundle ''{0}'' is inactive.", symbolicName));
	}

	/**
	 * Registers an OSGi service.
	 * <p>
	 * This is a convenience method which registers an OSGi service and
	 * remembers the registration as long as the bundle is not stopped. The OSGi
	 * framework automatically unregisters all services a bundle may have
	 * registered when the bundle is stopped.
	 * </p>
	 * 
	 * @param clazz
	 *            The class name under which the service can be located.
	 * @param service
	 *            The service object or a <code>ServiceFactory</code> object.
	 * @return a <code>ServiceRegistration</code> object for use by the bundle
	 *         registering the service to update the service's properties or to
	 *         unregister the service
	 * @see BundleContext#registerService(String, Object, Dictionary)
	 */
	public ServiceRegistration registerService(final String clazz, final Object service, final String vendor, final String description, final String pid, final Integer ranking) {
		final BundleContext bundleContext = contextRef.get();
		if (null == bundleContext) {
			throw newInactiveException();
		}

		final Dictionary<String, Object> properties = new Hashtable<String, Object>(4);
		if (null != pid) {
			properties.put(Constants.SERVICE_VENDOR, pid);
		}
		if (null != vendor) {
			properties.put(Constants.SERVICE_VENDOR, vendor);
		}
		if (null != description) {
			properties.put(Constants.SERVICE_DESCRIPTION, description);
		}
		if (null != ranking) {
			properties.put(Constants.SERVICE_RANKING, ranking);
		}

		final ServiceRegistration serviceRegistration = bundleContext.registerService(clazz, service, properties);
		registeredServices.add(serviceRegistration);
		return new HelperServiceRegistration(serviceRegistration);
	}

	/**
	 * Tracks an OSGi service.
	 * <p>
	 * This is a convenience method which installs a {@link ServiceTracker} for
	 * consuming an OSGi service. The tracker is kept open as long as the bundle
	 * is not stopped.
	 * </p>
	 * <p>
	 * The service tracker is made available to the caller through a service
	 * proxy. The service proxy ensures access to the service object in a
	 * convenient way.
	 * </p>
	 * 
	 * @param <T>
	 *            the service interface
	 * @param serviceInterface
	 *            the service interface class
	 * @return the service proxy object
	 */
	public <T> IServiceProxy<T> trackService(final Class<T> serviceInterface) {
		final BundleContext bundleContext = contextRef.get();
		if (null == bundleContext) {
			throw newInactiveException();
		}

		return serviceProxyPool.getOrCreate(serviceInterface);
	}

	/**
	 * Tracks an OSGi service matching the specified filter.
	 * <p>
	 * This is a convenience method which installs a {@link ServiceTracker} for
	 * consuming an OSGi service. The tracker is kept open as long as the bundle
	 * is not stopped.
	 * </p>
	 * <p>
	 * The service tracker is made available to the caller through a service
	 * proxy. The service proxy ensures access to the service object in a
	 * convenient way.
	 * </p>
	 * 
	 * @param <T>
	 *            the service interface
	 * @param serviceInterface
	 *            the service interface class
	 * @return the service proxy object
	 */
	public <T> IServiceProxy<T> trackService(final Class<T> serviceInterface, final Filter filter) {
		if (!filter.toString().contains(serviceInterface.getName())) {
			throw new IllegalArgumentException("Filter '" + filter.toString() + "' does not match the service class' " + serviceInterface.getName() + "'!");
		}

		final BundleContext bundleContext = contextRef.get();
		if (null == bundleContext) {
			throw newInactiveException();
		}

		return serviceProxyPool.getOrCreate(serviceInterface, filter);
	}

}
