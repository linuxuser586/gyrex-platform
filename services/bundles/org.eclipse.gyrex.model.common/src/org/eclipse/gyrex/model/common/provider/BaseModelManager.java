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
package org.eclipse.gyrex.model.common.provider;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.IRuntimeContextConstants;
import org.eclipse.gyrex.model.common.IModelManager;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.storage.IRepositoryContstants;
import org.eclipse.gyrex.persistence.storage.Repository;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.PlatformObject;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import org.apache.commons.lang.StringUtils;

/**
 * Base class for {@link IModelManager model managers}.
 * <p>
 * In addition to the {@link IModelManager} interface it enforces model manager
 * implementors to include central Gyrex concepts (eg., monitoring and metrics).
 * </p>
 * <p>
 * A {@link BaseModelManager} indicates if modifications are supported by the
 * back-end.
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
	 * Convenience method to create a human-readable metrics description based
	 * on a manager implementation name (eg. a <code>"MyManager"</code>), a
	 * specified context and a repository.
	 * 
	 * @param managerImplementationName
	 *            the manager implementation name
	 * @param context
	 *            the context
	 * @param repository
	 *            the repository
	 * @return a human-readable metrics description
	 */
	protected static String createMetricsDescription(final String managerImplementationName, final IRuntimeContext context, final Repository repository) {
		return String.format("Metrics for %s in context %s backed by repository %s.", managerImplementationName, context.getContextPath(), repository.getRepositoryId());
	}

	/**
	 * Convenience method to create a well formated metrics id based on a
	 * manager implementation id (eg. a
	 * <code>"com.company.xyz.model.impl"</code>), a specified context and a
	 * repository.
	 * 
	 * @param managerImplementationId
	 *            the manager implementation id
	 * @param context
	 *            the context
	 * @param repository
	 *            the repository
	 * @return a well formatted metrics id
	 */
	protected static String createMetricsId(final String managerImplementationId, final IRuntimeContext context, final Repository repository) {
		// handle context paths
		final String contextPath = StringUtils.replaceChars(context.getContextPath().removeTrailingSeparator().makeRelative().toString(), '/', '.');
		// create id
		return managerImplementationId + "." + contextPath + ".metrics";
	}

	private final IRuntimeContext context;
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
	 * monitored by Gyrex. The metrics will be registered automatically with the
	 * platform using the bundle which provides the actual {@link #getClass()
	 * class}.
	 * </p>
	 * 
	 * @param context
	 *            the context the manager operates in
	 * @param repository
	 *            the repository the manager must use
	 * @param metrics
	 *            the manager metrics
	 */
	protected BaseModelManager(final IRuntimeContext context, final T repository, final MetricSet metrics) {
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
		registerMetrics();
	}

	/**
	 * Called by the platform when a model manager is no longer needed and all
	 * held resources must be released.
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
	 * Called by the platform when a model manager is no longer needed and all
	 * held resources should be released.
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
	 * @see org.eclipse.gyrex.model.common.IModelManager#getContext()
	 */
	@Override
	public final IRuntimeContext getContext() {
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

	/**
	 * Registers the metrics on behalf of the bundle which loaded this class.
	 * 
	 * @throws IllegalArgumentException
	 *             if the class was not loaded by a bundle class loader
	 * @throws IllegalStateException
	 *             if the bundle which loaded the class has no valid bundle
	 *             context
	 */
	private void registerMetrics() throws IllegalArgumentException, IllegalStateException {
		// get bundle context
		// TODO: we might need to wrap this into a privileged call
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final BundleContext bundleContext = null != bundle ? bundle.getBundleContext() : null;
		if (null == bundleContext) {
			throw new IllegalStateException("Unable to determin bundle context for class '" + getClass().getName() + "'. Please ensure that this class was loaded by a bundle which is either STARTING, ACTIVE or STOPPING.");
		}

		// create properties
		final Dictionary<String, Object> properties = new Hashtable<String, Object>(2);
		properties.put(Constants.SERVICE_VENDOR, this.getClass().getName());
		properties.put(Constants.SERVICE_DESCRIPTION, "Metrics for model manager implementation '" + this.getClass().getName() + "'.");
		properties.put(IRepositoryContstants.SERVICE_PROPERTY_REPOSITORY_ID, getRepository().getRepositoryId());
		if (null != getRepository().getDescription()) {
			properties.put(IRepositoryContstants.SERVICE_PROPERTY_REPOSITORY_DESCRIPTION, getRepository().getDescription());
		}
		properties.put(IRuntimeContextConstants.SERVICE_PROPERTY_CONTEXT_PATH, getContext().getContextPath().toString());

		// register service
		metricsRegistration = bundleContext.registerService(MetricSet.class.getName(), metrics, properties);
	}

}
