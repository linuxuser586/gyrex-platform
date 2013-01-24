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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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

	public static <T> void verifyFilterContainsObjectClassConditionForServiceInterface(final Class<T> serviceInterface, final String filter) {
		final String requiredObjectClassCondition = String.format("&(objectClass=%s)", serviceInterface.getName());
		if (!filter.contains(requiredObjectClassCondition))
			throw new IllegalArgumentException(String.format("Filter '%s' does not match the service class condition '%s'!", filter, requiredObjectClassCondition));
	}

	private final BundleContext bundleContext;
	private final Class<T> serviceInterface;
	private final String filter;
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

	/** the generated dynamic proxy */
	private volatile T dynamicProxy;
	private final Object dynamicProxyCreationLock = new Object();

	/** a background thread for notifying listeners */
	private final Job notifyServiceChangeListeners;

	{
		notifyServiceChangeListeners = new Job("Notify service proxy change listeners") {
			@Override
			protected org.eclipse.core.runtime.IStatus run(final IProgressMonitor monitor) {
				// check for disposal
				if (disposed || monitor.isCanceled())
					return Status.CANCEL_STATUS;

				// notify
				for (final IServiceProxyChangeListener changeListener : changeListeners) {
					if (disposed || monitor.isCanceled())
						return Status.CANCEL_STATUS;
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
	 * <p>
	 * The service proxy will start tracking the service immediatly. This is
	 * done in order to provide immediatly validation of the specified filter.
	 * </p>
	 * <p>
	 * If no filter is specified, a default <code>objectClass</code> filter will
	 * be generated an used.
	 * </p>
	 * 
	 * @param bundleContext
	 *            the bundle context against which the tracking is done
	 * @param serviceInterface
	 *            the service interface to track
	 * @param filter
	 *            the filter to use for the service tracker (maybe
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the specified arguments is invalid
	 */
	public ServiceProxy(final BundleContext bundleContext, final Class<T> serviceInterface, final String filter) throws IllegalArgumentException {
		this.bundleContext = bundleContext;
		this.serviceInterface = serviceInterface;
		if (null != filter) {
			// verify filter
			verifyFilterContainsObjectClassConditionForServiceInterface(serviceInterface, filter);

			// set filter
			this.filter = filter;

			// open service
			try {
				open();
			} catch (final InvalidSyntaxException e) {
				// fail with IllegalArgumentException
				throw new IllegalArgumentException(String.format("Invalid filter '%s'. %s", filter, e.getMessage()), e);
			}
		} else {
			// generate a filter
			this.filter = String.format("(objectClass=%s)", serviceInterface.getName());
			try {
				// open the service
				open();
			} catch (final InvalidSyntaxException e) {
				// fail with IllegalStateException (per contract)
				throw new IllegalStateException(String.format("The framework did not accept our generated filter '%s'. %s", this.filter, e.getMessage()), e);
			}
		}
	}

	public void addChangeListener(final IServiceProxyChangeListener listener) {
		checkDisposed();
		changeListeners.add(listener);
	}

	/**
	 * Adds a change lister.
	 * <p>
	 * The listener is notified immediatly if the current service does not match
	 * the expected service.
	 * </p>
	 * 
	 * @param listener
	 * @param expectedService
	 */
	public void addChangeListener(final IServiceProxyChangeListener listener, final T expectedService) {
		checkDisposed();
		changeListeners.add(listener);

		// trigger if service is different
		if ((expectedService != null) && (expectedService != dynamicProxy)) {
			final Iterator<T> iterator = services.iterator();
			final T service = iterator.hasNext() ? iterator.next() : null;
			if (service != expectedService) {
				listener.serviceChanged(this);
			}
		}
	}

	public void addDisposalListener(final IServiceProxyDisposalListener listener) {
		checkDisposed();
		disposalListeners.add(listener);
	}

	private void checkDisposed() {
		if (disposed)
			throw new IllegalStateException(String.format("The service proxy for service '%s' has been disposed.", serviceInterface.getName()));
	}

	@SuppressWarnings("unchecked")
	private T createProxy() {
		checkDisposed();
		return (T) Proxy.newProxyInstance(new BundleDelegatingClassLoader(bundleContext.getBundle()), new Class[] { serviceInterface }, this);
	}

	@Override
	public void dispose() {
		if (disposed)
			return;
		else {
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
		T proxy = dynamicProxy;
		if (null != proxy)
			return proxy;

		// ensure that at most one proxy is created
		synchronized (dynamicProxyCreationLock) {
			proxy = dynamicProxy;
			if (null != proxy)
				return proxy;

			return dynamicProxy = createProxy();
		}
	}

	@Override
	public T getService() throws ServiceNotAvailableException {
		checkDisposed();

		// return the first available service
		final Iterator<T> iterator = services.iterator();
		final T service = iterator.hasNext() ? iterator.next() : null;
		if (null == service)
			throw new ServiceNotAvailableException(bundleContext, serviceInterface.getName());
		return service;
	}

	@Override
	public List<T> getServices() throws IllegalStateException {
		checkDisposed();

		// each invocations returns a fresh read-only view
		return Collections.unmodifiableList(services);
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		checkDisposed();

		// need to handle hashCode and equals within the proxy (for proper usage in collections)
		if (method.getName().equals("hashCode") && ((null == args) || (args.length == 0)))
			return System.identityHashCode(proxy); // rely entirely on identity
		else if (method.getName().equals("equals") && (null != args) && (args.length == 1))
			return proxy == args[0]; // rely entirely on identity
		else if (method.getName().equals("toString") && ((null == args) || (args.length == 0)))
			return toString(); // use ServiceProxy implementation

		try {
			return method.invoke(getService(), args);
		} catch (final InvocationTargetException e) {
			throw e.getTargetException();
		} catch (final IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException(String.format("Error calling method '%s' of service '%s'. %s", method.toString(), serviceInterface.getName(), e.getMessage()), e);
		}
	}

	/**
	 * Called during construction to hook the service listener with the bundle
	 * context
	 * 
	 * @throws InvalidSyntaxException
	 *             in case the filter is invalid
	 */
	private void open() throws InvalidSyntaxException {
		// add listener
		bundleContext.addServiceListener(this, filter);

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
		} else {
			try {
				builder.append(" USING ").append(serviceReferences.firstKey());
			} catch (final NoSuchElementException e) {
				builder.append(" NO-SERVICE");
			}
		}
		return builder.toString();
	};
}
