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
	 * Returns a dynamic proxy implementing the service interface for
	 * transparent access to the service object.
	 * 
	 * @return a dynamic proxy implementing the service interface
	 */
	T getProxy();

	/**
	 * Returns a service object for one of the services being tracked by this
	 * <code>IServiceProxy</code> object.
	 * <p>
	 * <p>
	 * Note, the returned service object must not be hold on for a longer
	 * duration. It is only intended for short durations.
	 * </p>
	 * 
	 * @return a service object
	 * @throws ServiceNotAvailableException
	 *             is no service is available
	 */
	T getService() throws ServiceNotAvailableException;

	/**
	 * Configures a timeout for method invocations on the service interface.
	 * <p>
	 * Note, this method becomes useless <em>after</em> the dynamic proxy was
	 * created by calling {@link #get()}.
	 * </p>
	 * <p>
	 * If the <code>timeout</code> argument is <code>0</code> any proxy method
	 * invocation will <strong>block</strong> till a service becomes available.
	 * This option should be used with caution.
	 * </p>
	 * <p>
	 * If the <code>timeout</code> argument is negative (i.e.,
	 * <code>&lt; 0</code>) any wait will be disabled.
	 * </p>
	 * 
	 * @param timeout
	 *            the time to wait for the service, <code>0</code> to block or
	 *            <code>negative</code> to disable any wait
	 * @param unit
	 *            the time unit of the timeout argument
	 * @return the service proxy object to allow a convenient programming style
	 */
	//IServiceProxy<T> setTimeout(final long timeout, final TimeUnit unit);
}