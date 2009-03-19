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
package org.eclipse.gyrex.services.common.provider;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.gyrex.common.context.IContext;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.services.common.IService;
import org.eclipse.gyrex.services.common.status.IStatusMonitor;
import org.osgi.framework.ServiceRegistration;

/**
 * Base class for {@link IService services}.
 * <p>
 * In addition to the {@link IService} interface it enforces service
 * implementors to include central Gyrex concepts (eg., monitoring and
 * metrics).
 * </p>
 * <p>
 * Clients that want to contribute a service implementation must subclass this
 * base class and provide a {@link ServiceProvider}.
 * </p>
 * 
 * @see ServiceProvider
 */
public abstract class BaseService extends PlatformObject implements IService {

	/**
	 * Utility method to create a well formated metrics id based on a service id
	 * (eg. a <code>"com.company.service.xyz.impl"</code>) and a specified
	 * context.
	 * <p>
	 * Note, the service id should not identify the generic service interface
	 * but the concrete service implementation.
	 * </p>
	 * 
	 * @param serviceImplementationId
	 *            an identifier for the service implementation
	 * @param context
	 *            the context
	 * @return a well formatted metrics id
	 */
	protected static String createMetricsId(final String serviceImplementationId, final IContext context) {
		return serviceImplementationId + "[" + context.getContextPath().toString() + "].metrics";
	}

	/** the context */
	private final IContext context;

	/** the status monitor */
	private final IStatusMonitor statusMonitor;

	/** the repository metrics */
	private final MetricSet metrics;

	/** indicates if the service has been closed */
	private volatile boolean closed;

	/** the service metrics registration */
	private volatile ServiceRegistration metricsRegistration;

	/**
	 * Protected constructor called by sub classes to provide the context and
	 * metrics.
	 * <p>
	 * Note, each service is required to provide metrics so that it can be
	 * monitored by Gyrex. The metrics will be registered
	 * automatically with the platform.
	 * </p>
	 * 
	 * @param context
	 *            the context the service operates in
	 * @param metrics
	 *            the service metrics
	 */
	protected BaseService(final IContext context, final IStatusMonitor statusMonitor, final MetricSet metrics) {
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}
		if (null == statusMonitor) {
			throw new IllegalArgumentException("statusMonitor must not be null");
		}
		if (null == metrics) {
			throw new IllegalArgumentException("metrics must not be null");
		}
		this.context = context;
		this.statusMonitor = statusMonitor;
		this.metrics = metrics;

		// register metrics
		// TODO: implement caching for service instances before we register metrics
		//metricsRegistration = ServicesActivator.getInstance().getServiceHelper().registerService(MetricSet.class.getName(), metrics, this.getClass().getSimpleName(), "Metrics for service " + this.getClass().getSimpleName(), null, null);
	}

	/**
	 * Called by the platform when a repository is no longer needed and all held
	 * resources must be released.
	 * <p>
	 * The implementation sets the {@link #isClosed() closed} state, calls
	 * {@link #doClose()} and unregisters the repository metrics. Subclasses
	 * should extend {@link #doClose()} and permanently release any resources.
	 * </p>
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final void close() {
		closed = true;
		try {
			doClose();
		} finally {
			final ServiceRegistration metricsRegistration = this.metricsRegistration;
			if (null != metricsRegistration) {
				metricsRegistration.unregister();
				this.metricsRegistration = null;
			}
		}
	}

	/**
	 * Called by the platform when a service is no longer needed and all held
	 * resources should be released.
	 * <p>
	 * The default implementation does nothing. Subclasses may overwrite and
	 * extend.
	 * </p>
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void doClose() {
		// empty
	}

	/**
	 * Returns an object which is an instance of the given class associated with
	 * this object. Returns <code>null</code> if no such object can be found.
	 * <p>
	 * This implementation of the method declared by <code>IAdaptable</code>
	 * passes the request along to the context and then to the platform's
	 * adapter service; roughly <code>getContext().getAdapter(adapter)</code>
	 * and if the first call returned <code>null</code>
	 * <code>IAdapterManager.getAdapter(this, adapter)</code>. Subclasses may
	 * override this method (however, if they do so, they must invoke the method
	 * on their superclass to ensure that the context and the Platform's adapter
	 * manager is consulted).
	 * </p>
	 * 
	 * @param adapter
	 *            the class to adapt to
	 * @return the adapted object or <code>null</code>
	 * @see IAdaptable#getAdapter(Class)
	 */
	@Override
	public Object getAdapter(final Class adapter) {
		// ask the context first
		final Object contextAdapter = getContext().getAdapter(adapter);
		if (null != contextAdapter) {
			return contextAdapter;
		}

		// fallback to adapter manager
		return super.getAdapter(adapter);
	}

	@Override
	public final IContext getContext() {
		return context;
	}

	/**
	 * Returns the metrics of the service.
	 * 
	 * @return the metrics
	 */
	protected final MetricSet getMetrics() {
		return metrics;
	}

	/**
	 * Returns the service {@link IStatusMonitor status monitor}.
	 * 
	 * @return the status monitor
	 */
	protected final IStatusMonitor getStatusMonitor() {
		return statusMonitor;
	}

	/**
	 * Indicates if this service object has been closed.
	 * <p>
	 * This method is guaranteed to return <code>true</code> only when it is
	 * called after the method {@link #close()} has been called.
	 * </p>
	 * 
	 * @return <code>true</code> if the repository is closed; <code>false</code>
	 *         otherwise
	 */
	public final boolean isClosed() {
		return closed;
	}

}
