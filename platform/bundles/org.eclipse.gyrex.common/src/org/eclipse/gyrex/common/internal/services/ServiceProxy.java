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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

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
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@link IServiceProxy} implementation
 */
public class ServiceProxy<T> implements IServiceProxy<T>, InvocationHandler, ServiceListener {

	private final Class<T> serviceInterface;
	private final AtomicReference<ServiceTracker<T, T>> serviceTrackerRef = new AtomicReference<ServiceTracker<T, T>>();
	private final AtomicReference<BundleContext> bundleContextRef = new AtomicReference<BundleContext>();
	private final CopyOnWriteArraySet<IServiceProxyDisposalListener> disposalListeners = new CopyOnWriteArraySet<IServiceProxyDisposalListener>();
	private final CopyOnWriteArraySet<IServiceProxyChangeListener> changeListeners = new CopyOnWriteArraySet<IServiceProxyChangeListener>();
	private volatile T dynamicProxy;

	private final Job notifyChangeListenersJob;
	{
		notifyChangeListenersJob = new Job("Notify service proxy change listeners") {
			@Override
			protected org.eclipse.core.runtime.IStatus run(final IProgressMonitor monitor) {
				// check for disposal
				if (bundleContextRef.get() == null) {
					return Status.CANCEL_STATUS;
				}

				// notify
				for (final IServiceProxyChangeListener changeListener : changeListeners) {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					changeListener.serviceChanged(ServiceProxy.this);
				}

				return Status.OK_STATUS;
			};
		};
		notifyChangeListenersJob.setSystem(true);
		notifyChangeListenersJob.setPriority(Job.SHORT);
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
		this.serviceInterface = serviceInterface;
		this.bundleContextRef.set(bundleContext);
		serviceTrackerRef.set(new ServiceTracker<T, T>(bundleContext, serviceInterface, null));
		final String filterString = String.format("(objectClass=%s)", serviceInterface.getName());
		try {
			bundleContext.addServiceListener(this, filterString);
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
		this.serviceInterface = serviceInterface;
		this.bundleContextRef.set(bundleContext);
		serviceTrackerRef.set(new ServiceTracker<T, T>(bundleContext, filter, null));
		try {
			bundleContext.addServiceListener(this, filter.toString());
		} catch (final InvalidSyntaxException e) {
			throw new IllegalArgumentException(String.format("Invalid filter '%s'. %s", filter.toString(), e.getMessage()), e);
		}
	}

	public void addChangeListener(final IServiceProxyChangeListener listener) {
		changeListeners.add(listener);
	}

	public void addDisposalListener(final IServiceProxyDisposalListener listener) {
		disposalListeners.add(listener);
	}

	@SuppressWarnings("unchecked")
	private T createProxy() {
		final BundleContext bundleContext = bundleContextRef.get();
		if (null == bundleContext) {
			throw new IllegalStateException("inactive");
		}
		return (T) Proxy.newProxyInstance(new BundleDelegatingClassLoader(bundleContext.getBundle()), new Class[] { serviceInterface }, this);
	}

	@Override
	public void dispose() {
		// cancel job (in any case)
		notifyChangeListenersJob.cancel();

		// clear bundle context reference (indicates disposal)
		bundleContextRef.set(null);

		// close tracker
		final ServiceTracker tracker = serviceTrackerRef.getAndSet(null);
		if (null != tracker) {
			tracker.close();
		}
		// unset proxy
		dynamicProxy = null;
		// notify
		for (final IServiceProxyDisposalListener listener : disposalListeners) {
			listener.disposed(this);
		}
	}

	@Override
	public T getProxy() {
		if (null != dynamicProxy) {
			return dynamicProxy;
		}
		return dynamicProxy = createProxy();
	}

	@Override
	public T getService() throws ServiceNotAvailableException {
		final ServiceTracker<T, T> serviceTracker = serviceTrackerRef.get();
		if (null == serviceTracker) {
			throw new ServiceNotAvailableException(bundleContextRef.get(), serviceInterface.getName());
		}
		// ensure the tracker is open
		serviceTracker.open();

		final T service = serviceTracker.getService();
		if (null == service) {
			throw new ServiceNotAvailableException(bundleContextRef.get(), serviceInterface.getName());
		}
		return service;
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		return method.invoke(getService(), args);
	}

	public void removeChangeListener(final IServiceProxyChangeListener listener) {
		changeListeners.remove(listener);
	}

	public void removeDisposalListener(final IServiceProxyDisposalListener listener) {
		disposalListeners.remove(listener);
	}

	@Override
	public void serviceChanged(final ServiceEvent event) {
		// just schedule the notify job
		// note, we add a delay in order to protect from an event storm
		// as well as to process the events asynchronously
		notifyChangeListenersJob.schedule(500L);
	}
}
