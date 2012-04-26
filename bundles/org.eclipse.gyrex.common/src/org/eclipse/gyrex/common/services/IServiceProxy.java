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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A proxy for tracking and accessing a single OSGi service instance.
 * <p>
 * The proxy tracks a specific OSGi service interface. It provides a dynamic
 * proxy which implements the service interface for transparent access to the
 * service.
 * </p>
 * 
 * @param <T>
 *            the service interface of the tracked service
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IServiceProxy<T> {

	/**
	 * Disposes the service proxy and releases any resources.
	 * <p>
	 * After the service proxy has been disposed it can no longer be used.
	 * </p>
	 */
	void dispose();

	/**
	 * Returns a dynamic proxy implementing the service interface for
	 * transparent access to the service object.
	 * <p>
	 * The returned proxy may be used in collections. It's
	 * {@link Object#equals(Object) equals} and {@link Object#hashCode()
	 * hashCode} will implement exact identity comparison using the returned
	 * proxy object instance. Implementations will also ensure that at most one
	 * proxy instance is created and subsequent invocations will return the same
	 * proxy instance as long as the service proxy is not disposed.
	 * </p>
	 * 
	 * @return a dynamic proxy implementing the service interface
	 */
	T getProxy();

//	/**
//	 * Configures a timeout for method invocations on the service interface.
//	 * <p>
//	 * Note, this method becomes useless <em>after</em> the dynamic proxy was
//	 * created by calling {@link #get()}.
//	 * </p>
//	 * <p>
//	 * If the <code>timeout</code> argument is <code>0</code> any proxy method
//	 * invocation will <strong>block</strong> till a service becomes available.
//	 * This option should be used with caution.
//	 * </p>
//	 * <p>
//	 * If the <code>timeout</code> argument is negative (i.e.,
//	 * <code>&lt; 0</code>) any wait will be disabled.
//	 * </p>
//	 *
//	 * @param timeout
//	 *            the time to wait for the service, <code>0</code> to block or
//	 *            <code>negative</code> to disable any wait
//	 * @param unit
//	 *            the time unit of the timeout argument
//	 * @return the service proxy object to allow a convenient programming style
//	 */
//	IServiceProxy<T> setTimeout(final long timeout, final TimeUnit unit);

	/**
	 * Returns a service object for one of the services being tracked by this
	 * <code>IServiceProxy</code> object.
	 * <p>
	 * Note, the returned service object must not be hold on for a longer
	 * duration. It is only intended for short durations. The bundle which
	 * provides the service may be stopped at any time, which makes the service
	 * invalid. If you do not wish this behavior you need to
	 * {@link BundleContext#getService(org.osgi.framework.ServiceReference) get
	 * the service} directly from the {@link BundleContext} and
	 * {@link BundleContext#ungetService(org.osgi.framework.ServiceReference)
	 * unget} it when you are done. This procedure requires more code but
	 * ensures that the service is available during your usage.
	 * </p>
	 * 
	 * @return a service object
	 * @throws ServiceNotAvailableException
	 *             is no service is available
	 */
	T getService() throws ServiceNotAvailableException;

	/**
	 * Returns a collection of service object for all services currently being
	 * tracked by this <code>IServiceProxy</code> object.
	 * <p>
	 * Note, the returned collection can be hold on for a longer duration. It
	 * represents a view and will be updated when new services become available
	 * or existing services change or go away.
	 * </p>
	 * <p>
	 * In contrast, the service objects within the collection must not be hold
	 * on for a longer period of time. The bundle which provides a service may
	 * be stopped at any time, which makes the service invalid. If you do not
	 * wish this behavior you need to
	 * {@link BundleContext#getService(org.osgi.framework.ServiceReference) get
	 * the service} directly from the {@link BundleContext} and
	 * {@link BundleContext#ungetService(org.osgi.framework.ServiceReference)
	 * unget} it when you are done. This procedure requires more code but
	 * ensures that the service is available during your usage.
	 * </p>
	 * <p>
	 * Note, the thread-safety semantics of the underlying collection are
	 * similar to the ones of {@link CopyOnWriteArrayList}. Therefore, traversal
	 * operations don't need to synchronize on the returned collection object.
	 * </p>
	 * <p>
	 * The collection of services will be ordered based on the inverse natural
	 * order of service objects (which is spec'ed by
	 * {@link ServiceReference#compareTo(Object)}), i.e. a service with a higher
	 * ranking comes before a service with a lower ranking.
	 * </p>
	 * 
	 * @return an unmodifiable collection of service object
	 */
	List<T> getServices() throws IllegalStateException;
}