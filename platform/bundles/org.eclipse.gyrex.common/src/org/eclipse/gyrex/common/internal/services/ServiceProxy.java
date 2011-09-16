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
package org.eclipse.gyrex.common.internal.services;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.common.services.ServiceNotAvailableException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IServiceProxy} implementation
 */
public class ServiceProxy<T> implements IServiceProxy<T>, InvocationHandler, ServiceListener {

	private static final Logger LOG = LoggerFactory.getLogger(ServiceProxy.class);

	public static <T> void verifyFilterContainsServiceInterfaceCondition(final Class<T> serviceInterface, final Filter filter) {
		final String requiredObjectClassCondition = String.format("&(objectClass=%s)", serviceInterface.getName());
		if (!filter.toString().contains(requiredObjectClassCondition)) {
			throw new IllegalArgumentException(String.format("Filter '%s' does not match the service class condition '%s'!", filter.toString(), requiredObjectClassCondition));
		}
	}

	private final BundleContext bundleContext;
	private final Class<T> serviceInterface;
	private final Filter filter;
	private final CopyOnWriteArraySet<IServiceProxyDisposalListener> disposalListeners = new CopyOnWriteArraySet<IServiceProxyDisposalListener>();
	private final CopyOnWriteArraySet<IServiceProxyChangeListener> changeListeners = new CopyOnWriteArraySet<IServiceProxyChangeListener>();

	/**
	 * map of service references sorted based on ranking (must be guarded by
	 * <code>synchronized(serviceReferences) {}</code>)
	 */
	private final SortedMap<ServiceReference<T>, T> serviceReferences = new TreeMap<ServiceReference<T>, T>(new Comparator<ServiceReference<T>>() {
		@Override
		public int compare(final ServiceReference<T> r1, final ServiceReference<T> r2) {
			// inverse order
			return r2.compareTo(r1);
		}
	});

	/** sorted snapshot of services */
	private final CopyOnWriteArrayList<T> services = new CopyOnWriteArrayList<T>();

	/** indicated if disposed */
	private volatile boolean disposed;

	private volatile boolean active;
	private volatile T dynamicProxy;
	private final Job notifyServiceChangeListeners;

	{
		notifyServiceChangeListeners = new Job("Notify service proxy change listeners") {
			@Override
			protected org.eclipse.core.runtime.IStatus run(final IProgressMonitor monitor) {
				// check for disposal
				if (disposed) {
					return Status.CANCEL_STATUS;
				}

				// notify
				for (final IServiceProxyChangeListener changeListener : changeListeners) {
					if (disposed || monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					if (!changeListener.serviceChanged(ServiceProxy.this)) {
						changeListeners.remove(changeListener);
					}
				}

				return Status.OK_STATUS;
			}
		};
		notifyServiceChangeListeners.setSystem(true);
		notifyServiceChangeListeners.setPriority(Job.SHORT);
	}

	/**
	 * Creates a new service proxy instance which will track the specified
	 * service.
	 * 
	 * @param bundleContext
	 *            the bundle context against which the tracking is done
	 * @param serviceInterface
	 *            the service interface to track
	 */
	public ServiceProxy(final BundleContext bundleContext, final Class<T> serviceInterface) {
		this.bundleContext = bundleContext;
		this.serviceInterface = serviceInterface;
		final String filterString = String.format("(objectClass=%s)", serviceInterface.getName());
		try {
			filter = bundleContext.createFilter(filterString);
		} catch (final InvalidSyntaxException e) {
			throw new IllegalStateException(String.format("The framework did not accept our generated filter '%s'. %s", filterString, e.getMessage()), e);
		}
	}

	/**
	 * Creates a new service proxy instance which will track the specified
	 * service.
	 * 
	 * @param bundleContext
	 *            the bundle context against which the tracking is done
	 * @param serviceInterface
	 *            the service interface to track
	 * @param filter
	 *            the filter to use for the service tracker
	 */
	public ServiceProxy(final BundleContext bundleContext, final Class<T> serviceInterface, final Filter filter) {
		this.bundleContext = bundleContext;
		this.serviceInterface = serviceInterface;
		this.filter = filter;
		if (null == filter) {
			throw new IllegalArgumentException("Filter must not be null!");
		} else {
			verifyFilterContainsServiceInterfaceCondition(serviceInterface, filter);
		}
	}

	public void addChangeListener(final IServiceProxyChangeListener listener) {
		checkDisposed();
		changeListeners.add(listener);
	}

	public void addDisposalListener(final IServiceProxyDisposalListener listener) {
		checkDisposed();
		disposalListeners.add(listener);
	}

	private void checkDisposed() {
		if (disposed) {
			throw new IllegalStateException(String.format("The service proxy for service '%s' has been disposed.", serviceInterface.getName()));
		}
	}

	@SuppressWarnings("unchecked")
	private T createProxy() {
		checkDisposed();
		return (T) Proxy.newProxyInstance(new BundleDelegatingClassLoader(bundleContext.getBundle()), new Class[] { serviceInterface }, this);
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		} else {
			disposed = true;
		}

		// cancel job (in any case)
		notifyServiceChangeListeners.cancel();

		// unget all services
		ServiceReference<?>[] references;
		synchronized (serviceReferences) {
			references = serviceReferences.keySet().toArray(new ServiceReference<?>[0]);
			serviceReferences.clear();
			services.clear();
		}
		for (final ServiceReference<?> reference : references) {
			bundleContext.ungetService(reference);
		}

		// unset proxy
		dynamicProxy = null;

		// notify
		for (final IServiceProxyDisposalListener listener : disposalListeners) {
			listener.disposed(this);
		}
	}

	private void doUpdateServices() {
		synchronized (serviceReferences) {
			// we simulate a replace with "clear & addAll"
			services.clear();
			services.addAll(serviceReferences.values());
		}
	}

	@Override
	public T getProxy() {
		checkDisposed();
		if (null != dynamicProxy) {
			return dynamicProxy;
		}
		return dynamicProxy = createProxy();
	}

	@Override
	public T getService() throws ServiceNotAvailableException {
		checkDisposed();

		// ensure the listener is registered and initial services populated
		open();

		// return the first available service
		final Iterator<T> iterator = services.iterator();
		final T service = iterator.hasNext() ? iterator.next() : null;
		if (null == service) {
			throw new ServiceNotAvailableException(bundleContext, serviceInterface.getName());
		}
		return service;
	}

	@Override
	public Collection<T> getServices() throws IllegalStateException {
		checkDisposed();

		// ensure the listener is registered and initial services populated
		open();

		// each invocations returns a fresh read-only view
		return Collections.unmodifiableCollection(services);
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		checkDisposed();
		return method.invoke(getService(), args);
	}

	private void open() {
		if (active) {
			return;
		}

		// protect for concurrent modifications during initial population
		synchronized (serviceReferences) {
			checkDisposed();

			// check/set active
			if (active) {
				return;
			} else {
				active = true;
			}

			try {
				// add listener
				bundleContext.addServiceListener(this, filter.toString());

				// populate initial list of services
				final Collection<ServiceReference<T>> references = bundleContext.getServiceReferences(serviceInterface, filter.toString());
				for (final ServiceReference<T> reference : references) {
					final T service = bundleContext.getService(reference);
					if (null != service) {
						serviceReferences.put(reference, service);
					}
				}

				// update services snapshot
				doUpdateServices();
			} catch (final InvalidSyntaxException e) {
				throw new IllegalStateException(String.format("Invalid filter '%s'. %s", filter.toString(), e.getMessage()), e);
			}
		}
	}

	public void removeChangeListener(final IServiceProxyChangeListener listener) {
		changeListeners.remove(listener);
	}

	public void removeDisposalListener(final IServiceProxyDisposalListener listener) {
		disposalListeners.remove(listener);
	}

	@Override
	public void serviceChanged(final ServiceEvent event) {
		@SuppressWarnings("unchecked")
		final ServiceReference<T> serviceReference = (ServiceReference<T>) event.getServiceReference();

		// handle event
		switch (event.getType()) {
			case ServiceEvent.REGISTERED:
			case ServiceEvent.MODIFIED:
				synchronized (serviceReferences) {
					if (!serviceReferences.containsKey(serviceReference)) {
						// get service
						final T service = bundleContext.getService(serviceReference);
						if (null != service) {
							// remember service
							serviceReferences.put(serviceReference, service);

							// update list of services
							// (synchronously within event notification)
							doUpdateServices();
						}
					}
				}
				break;

			case ServiceEvent.MODIFIED_ENDMATCH:
			case ServiceEvent.UNREGISTERING:
				synchronized (serviceReferences) {
					if (serviceReferences.containsKey(serviceReference)) {
						// remove registration
						final T service = serviceReferences.remove(serviceReference);
						if (null != service) {
							// unget service (if we had one)
							bundleContext.ungetService(serviceReference);

							// update list of services
							// (synchronously within event notification)
							doUpdateServices();
						}
					}
				}
				break;

			default:
				LOG.warn("Unhandled service event ({}, type {}).", event, event.getType());
				break;
		}

		// notify change listeners asynchronously
		// note, we add a delay in order to protect from an event storm
		notifyServiceChangeListeners.schedule(500L);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ServiceProxy [").append(serviceInterface.getName()).append("]");
		if (disposed) {
			builder.append(" DISPOSED");
		}
		return builder.toString();
	};
}
