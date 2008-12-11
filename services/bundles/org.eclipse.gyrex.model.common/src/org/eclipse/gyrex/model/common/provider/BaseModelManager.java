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
package org.eclipse.cloudfree.model.common.provider;

import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.cloudfree.model.common.IModelManager;
import org.eclipse.cloudfree.model.common.internal.ModelActivator;
import org.eclipse.cloudfree.monitoring.metrics.MetricSet;
import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.PlatformObject;
import org.osgi.framework.ServiceRegistration;

/**
 * Base class for {@link IModelManager model managers}.
 * <p>
 * In addition to the {@link IModelManager} interface it enforces model manager
 * implementors to include central CloudFree concepts (eg., monitoring and
 * metrics).
 * </p>
 * <p>
 * Clients that want to contribute a model manager implementation must subclass
 * this base class to be backwards compatible.
 * </p>
 * 
 * @see ModelProvider
 * @param <T>
 *            the repository type the manager expects
 */
public abstract class BaseModelManager<T extends Repository> extends PlatformObject implements IModelManager {

	/**
	 * Utility method to create a well formated metrics id based on a model id
	 * (eg. a <code>"com.company.xyz.model.impl"</code>), a specified context
	 * and a repository.
	 * <p>
	 * Note, the model id should not identify the generic model interface but
	 * the concrete model implementation.
	 * </p>
	 * 
	 * @param modelImplementationId
	 *            an identifier for the model implementation
	 * @param context
	 *            the context
	 * @param repository
	 *            the repository
	 * @return a well formatted metrics id
	 */
	protected static String createMetricsId(final String modelImplementationId, final IContext context, final Repository repository) {
		return modelImplementationId + "[" + context.getContextPath().toString() + "," + repository.getRepositoryId() + "].metrics";
	}

	private final IContext context;
	private final T repository;

	/** indicates if the service has been closed */
	private volatile boolean closed;

	/** the repository metrics */
	private final MetricSet metrics;

	/** the repository metrics registration */
	private volatile ServiceRegistration metricsRegistration;

	/**
	 * Protected constructor typically called by sub classes (through
	 * {@link ModelProvider providers}) to provide the context and repository
	 * passed through {@link ModelProvider}.
	 * <p>
	 * Note, each manager is required to provide metrics so that it can be
	 * monitored by the CloudFree platform. The metrics will be registered
	 * automatically with the platform.
	 * </p>
	 * 
	 * @param context
	 *            the context the manager operates in
	 * @param repository
	 *            the repository the manager must use
	 * @param metrics
	 *            the manager metrics
	 */
	protected BaseModelManager(final IContext context, final T repository, final MetricSet metrics) {
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}
		if (null == repository) {
			throw new IllegalArgumentException("repository must not be null");
		}
		if (null == metrics) {
			throw new IllegalArgumentException("metrics must not be null");
		}
		this.context = context;
		this.repository = repository;
		this.metrics = metrics;

		// register metrics
		metricsRegistration = ModelActivator.getInstance().getServiceHelper().registerService(MetricSet.class.getName(), metrics, this.getClass().getSimpleName(), "Metrics for model manager " + this.getClass().getSimpleName(), null, null);
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
			metricsRegistration.unregister();
			metricsRegistration = null;
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
	 * passes the request along to the context and then to theplatform's adapter
	 * manager; roughly <code>getContext().getAdapter(adapter)</code> and if the
	 * first call returned <code>null</code>
	 * <code>Platform.getAdapterManager().getAdapter(this, adapter)</code>.
	 * Subclasses may override this method (however, if they do so, they must
	 * invoke the method on their superclass to ensure that the context and the
	 * Platform's adapter manager are consulted).
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

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.model.common.IModelManager#getContext()
	 */
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
	 * Returns the repository the manager must use.
	 * 
	 * @return the repository
	 */
	protected final T getRepository() {
		return repository;
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
