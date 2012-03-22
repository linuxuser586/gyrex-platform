/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.common.services;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.internal.services.ServiceProxyPool;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A helper for working with OSGi services.
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
 * Services may be imported using one of the <code>track..</code> methods. The
 * imported services will be tracked internally. The handed out service objects
 * are proxies which delegate to an available service. If no service is
 * available, the proxies may fail, or discard a call depending on the desired
 * behavior.
 * </p>
 * <p>
 * This class may be instantiated directly by clients. In such a case they must
 * ensure that {@link #dispose()} is called when the instantiated service helper
 * is no longer used. However, the use through {@link BaseBundleActivator} is
 * encouraged.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class BundleServiceHelper {

	private final AtomicReference<BundleContext> contextRef = new AtomicReference<BundleContext>();
	private final String symbolicName;
	private final ServiceProxyPool serviceProxyPool;

	/**
	 * Creates a new instance.
	 * <p>
	 * Note, this method is typically called by the framework during bundle
	 * start. Clients which call it directly because that want to instantiate a
	 * service helper directly must properly {@link #dispose()} it when no
	 * longer needed.
	 * </p>
	 * 
	 * @param context
	 *            the bundle context
	 */
	public BundleServiceHelper(final BundleContext context) {
		contextRef.set(context);
		symbolicName = context.getBundle().getSymbolicName();
		serviceProxyPool = new ServiceProxyPool(context);
	}

	/**
	 * Disposes the service helper and release all tracked services.
	 * <p>
	 * This is typically called by a bundle activator when the bundle is
	 * stopped.
	 * </p>
	 * <p>
	 * When this method is called the context reference will be cleared so that
	 * no new service can be registered or consumed. Additionally, all resources
	 * will be released. Thus, consumed services will become unavailable and
	 * monitored service registrations will be released but not unregistered
	 * because this will be done by the framework anyway.
	 * </p>
	 * <p>
	 * Note, this method is typically called by the framework during bundle
	 * shutdown. Clients which instantiated their own service helper directly
	 * must also call it to properly dispose the instance.
	 * </p>
	 */
	public void dispose() {
		// unset the context
		contextRef.set(null);
		// dispose the proxy pool
		serviceProxyPool.dispose();
	}

	private IllegalStateException newInactiveException() {
		return new IllegalStateException(String.format("Bundle '%s' is inactive.", symbolicName));
	}

	/**
	 * Registers an OSGi service.
	 * <p>
	 * This is a convenience method which registers an OSGi service. The OSGi
	 * framework automatically unregisters all services a bundle may have
	 * registered when the bundle is stopped.
	 * </p>
	 * 
	 * @param clazz
	 *            The class name under which the service can be located.
	 * @param service
	 *            The service object or a <code>ServiceFactory</code> object.
	 * @param vendor
	 *            an optional {@link Constants#SERVICE_VENDOR service vendor} to
	 *            use (may be <code>null</code>)
	 * @param description
	 *            an optional {@link Constants#SERVICE_DESCRIPTION service
	 *            description} to use (may be <code>null</code>)
	 * @param pid
	 *            an optional {@link Constants#SERVICE_PID service pid} to use
	 *            (may be <code>null</code>)
	 * @param ranking
	 *            a {@link Constants#SERVICE_RANKING service ranking} to use
	 *            (may be <code>null</code>)
	 * @param <S>
	 *            the service type
	 * @return a <code>ServiceRegistration</code> object for use by the bundle
	 *         registering the service to update the service's properties or to
	 *         unregister the service
	 * @see BundleContext#registerService(String, Object, Dictionary)
	 */
	public <S> ServiceRegistration<S> registerService(final Class<S> clazz, final S service, final String vendor, final String description, final String pid, final Integer ranking) {
		final BundleContext bundleContext = contextRef.get();
		if (null == bundleContext) {
			throw newInactiveException();
		}

		final Dictionary<String, Object> properties = new Hashtable<String, Object>(4);
		if (null != pid) {
			properties.put(Constants.SERVICE_PID, pid);
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

		return bundleContext.registerService(clazz, service, properties);
	}

	/**
	 * Registers an OSGi service.
	 * <p>
	 * This is a convenience method which registers an OSGi service. The OSGi
	 * framework automatically unregisters all services a bundle may have
	 * registered when the bundle is stopped.
	 * </p>
	 * 
	 * @param clazz
	 *            The class name under which the service can be located.
	 * @param service
	 *            The service object or a <code>ServiceFactory</code> object.
	 * @param vendor
	 *            an optional {@link Constants#SERVICE_VENDOR service vendor} to
	 *            use (may be <code>null</code>)
	 * @param description
	 *            an optional {@link Constants#SERVICE_DESCRIPTION service
	 *            description} to use (may be <code>null</code>)
	 * @param pid
	 *            an optional {@link Constants#SERVICE_PID service pid} to use
	 *            (may be <code>null</code>)
	 * @param ranking
	 *            a {@link Constants#SERVICE_RANKING service ranking} to use
	 *            (may be <code>null</code>)
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
			properties.put(Constants.SERVICE_PID, pid);
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

		return bundleContext.registerService(clazz, service, properties);
	}

	/**
	 * Registers an OSGi service.
	 * <p>
	 * This is a convenience method which registers an OSGi service. The OSGi
	 * framework automatically unregisters all services a bundle may have
	 * registered when the bundle is stopped.
	 * </p>
	 * 
	 * @param classes
	 *            The class names under which the service can be located.
	 * @param service
	 *            The service object or a <code>ServiceFactory</code> object.
	 * @param vendor
	 *            an optional {@link Constants#SERVICE_VENDOR service vendor} to
	 *            use (may be <code>null</code>)
	 * @param description
	 *            an optional {@link Constants#SERVICE_DESCRIPTION service
	 *            description} to use (may be <code>null</code>)
	 * @param pid
	 *            an optional {@link Constants#SERVICE_PID service pid} to use
	 *            (may be <code>null</code>)
	 * @param ranking
	 *            a {@link Constants#SERVICE_RANKING service ranking} to use
	 *            (may be <code>null</code>)
	 * @return a <code>ServiceRegistration</code> object for use by the bundle
	 *         registering the service to update the service's properties or to
	 *         unregister the service
	 * @see BundleContext#registerService(String, Object, Dictionary)
	 */
	public ServiceRegistration registerService(final String[] classes, final Object service, final String vendor, final String description, final String pid, final Integer ranking) {
		final BundleContext bundleContext = contextRef.get();
		if (null == bundleContext) {
			throw newInactiveException();
		}

		final Dictionary<String, Object> properties = new Hashtable<String, Object>(4);
		if (null != pid) {
			properties.put(Constants.SERVICE_PID, pid);
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

		return bundleContext.registerService(classes, service, properties);
	}

	/**
	 * Tracks an OSGi service.
	 * <p>
	 * This is a convenience method which works similar to
	 * {@link ServiceTracker} for consuming an OSGi service. The tracker is kept
	 * open as long as the bundle is not stopped.
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
	 * @throws IllegalArgumentException
	 *             if the service interface is <code>null</code>
	 */
	public <T> IServiceProxy<T> trackService(final Class<T> serviceInterface) throws IllegalArgumentException {
		return trackService(serviceInterface, null);
	}

	/**
	 * Tracks an OSGi service matching the specified filter.
	 * <p>
	 * This is a convenience method which installs a {@link ServiceTracker} for
	 * consuming an OSGi service. The tracker is kept open as long as the bundle
	 * is not stopped.
	 * </p>
	 * <p>
	 * A filter can be specified to further limit the available services.
	 * However, the filter is required to contain an objectClass condition for
	 * the service interface name in the form '
	 * <code>&amp;(objectClass=&lt;serviceInterfaceName&gt;)</code>'. An
	 * {@link IllegalArgumentException} wrapping a
	 * {@link InvalidSyntaxException} will be thrown in case the filter is
	 * invalid.
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
	 * @param filter
	 *            the filter for tracking the service (must contain an
	 *            <code>objectClass</code> condition for the service interface
	 *            if not <code>null</code>)
	 * @return the service proxy object
	 * @throws IllegalArgumentException
	 *             if the service interface is <code>null</code> or the
	 *             specified filter is invalid
	 */
	public <T> IServiceProxy<T> trackService(final Class<T> serviceInterface, final String filter) throws IllegalArgumentException {
		if (null == serviceInterface) {
			throw new IllegalArgumentException("serviceInterface must not be null");
		}

		final BundleContext bundleContext = contextRef.get();
		if (null == bundleContext) {
			throw newInactiveException();
		}

		return serviceProxyPool.getOrCreate(serviceInterface, filter);
	}

}
