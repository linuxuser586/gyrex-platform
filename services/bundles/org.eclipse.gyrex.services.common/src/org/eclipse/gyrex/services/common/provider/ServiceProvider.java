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
package org.eclipse.cloudfree.services.common.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.cloudfree.services.common.IService;
import org.eclipse.cloudfree.services.common.ServiceUtil;
import org.eclipse.cloudfree.services.common.status.IStatusMonitor;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;

/**
 * A service provider base class which provides {@link IService service}
 * instances to the CloudFree Platform.
 * <p>
 * A {@link ServiceProvider} provides {@link IService} objects. These service
 * service objects may be obtained from a {@link IContext context} using the the
 * standard {@link IAdaptable#getAdapter(Class) Eclipse adapter mechanise}. In
 * the background, a service provider registry maintains registrations with the
 * the Eclipse {@link IAdapterManager} because it will not always be possible to
 * register adapters directly (eg., because of security constraints enforced by
 * the platform). Thus, a registration of service providers using this class is
 * enforced.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute
 * {@link IService} implementations. It is part of a service provider API and
 * should never be used directly by clients. Service providers must be made
 * available as OSGi services using type {@link ServiceProvider}.
 * </p>
 * 
 * @see ServiceUtil#getService(Class, IContext)
 */
public abstract class ServiceProvider {

	/** the list of services provided by the factory */
	private final List<Class<?>> providedServices;

	/**
	 * Creates a new service provider.
	 * <p>
	 * This constructor is typical called by subclasses with the content type
	 * and a list of services the factory provides.
	 * </p>
	 * <p>
	 * Although not enforced in this constructor, the list should specify the
	 * public interface a service implements not the actual service
	 * implementation. Each interface for one wishes to return a service in
	 * {@link ServiceUtil#getService(Class, IContext)} should be specified.
	 * </p>
	 * <p>
	 * If a class in the list does not extend the {@link IService} interface an
	 * {@link IllegalArgumentException} will be thrown.
	 * </p>
	 * <p>
	 * Note, at least one service must be provided. It is possible to register
	 * additional services through a single provider.
	 * </p>
	 * 
	 * @param contentType
	 *            the required content type for the services
	 * @param providedService
	 *            s service interfaces the factory will provide (must extend
	 *            {@link IService})
	 * @param providedServices
	 *            list of additional service interfaces the factory will provide
	 * @throws IllegalArgumentException
	 *             is any of the provided arguments are invalid
	 */
	protected ServiceProvider(final Class<? extends IService> providedService, final Class... providedServices) throws IllegalArgumentException {
		// note, we need the trick with (Class, Class...) here because 
		// just (Class...) would allow subclass to skip invocation of this constructor
		if (null == providedService) {
			throw new IllegalArgumentException("providedService must not be null");
		}
		if (!IService.class.isAssignableFrom(providedService)) {
			throw new IllegalArgumentException("service '" + providedService.getName() + "' is not assignable to '" + IService.class.getName() + "'");
		}
		final List<Class<?>> services = new ArrayList<Class<?>>(providedServices.length);
		services.add(providedService);
		if (null != providedServices) {
			for (final Class<?> service : providedServices) {
				if (null == service) {
					throw new IllegalArgumentException("providedServices list contains NULL entries which is not supported");
				}
				if (!IService.class.isAssignableFrom(service)) {
					throw new IllegalArgumentException("service '" + service.getName() + "' is not assignable to '" + IService.class.getName() + "'");
				}
				services.add(service);
			}
		}
		this.providedServices = Collections.unmodifiableList(services);
	}

	/**
	 * Called by the CloudFree Platform to create a new service instance of the
	 * specified service type.
	 * <p>
	 * Subclasses must implement this method and return a service instance which
	 * is initialized completely for using the specified repository and context.
	 * </p>
	 * 
	 * @param serviceType
	 *            the service type
	 * @param context
	 *            the context
	 * @param statusMonitor
	 *            the status monitor
	 * @return the service instance or <code>null</code>
	 * @noreference This method is not intended to be referenced by clients
	 *              directly.
	 */
	public abstract BaseService createServiceInstance(Class serviceType, IContext context, IStatusMonitor statusMonitor);

	/**
	 * Returns the collection of service types contributed by this provider.
	 * <p>
	 * This method is generally used by the platform to discover which service
	 * types are supported, in advance of dispatching any actual
	 * {@link #createServiceInstance(Class, IContext, IStatusMonitor)} requests.
	 * </p>
	 * 
	 * @return the collection of adapter types
	 */
	public final Iterable<Class<?>> getProvidedServices() {
		return providedServices;
	}
}
