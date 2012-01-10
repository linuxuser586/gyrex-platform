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
package org.eclipse.gyrex.persistence.storage;

import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryPreferencesBasedMetadata;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.content.BasicRepositoryContentTypeSupport;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentTypeSupport;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;

import org.eclipse.core.runtime.PlatformObject;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import org.apache.commons.lang.StringUtils;

/**
 * A repository is a concrete instance of a data store where a particular set of
 * data will be persisted.
 * <p>
 * In Gyrex, repositories are an abstraction for an actual data store accessed
 * using a defined persistence technology (eg. a MySQL database accessed via
 * JDBC, a flat CSV file, a distributed in-memory hast table, etc.). A specific
 * repository may only be accessed using the technology defined by the
 * repository implementation. Typically, they are configured whitin Gyrex
 * through an administrative interface. Their implementation is contributed to
 * Gyrex by {@link RepositoryProvider repository providers}.
 * </p>
 * <p>
 * In order to simplify operation of Gyrex repositories will support
 * administrative tasks. For example, it will be possible to transfer a
 * repository into a read-only mode or disabled it completely for maintenance
 * purposes. It will also be possible to dump a repository (or portions of it)
 * into a repository provider independent format. The dump may be loaded later
 * into another repository.
 * </p>
 * <p>
 * This class must be subclassed by clients that contribute a repository
 * implementation to Gyrex. As such it is considered part of a service provider
 * API which may evolve faster than the general API. Please get in touch with
 * the development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public abstract class Repository extends PlatformObject implements IRepositoryContstants {

	/**
	 * Utility method to create a well formated metrics id based on a repository
	 * provider and a specified repository id.
	 * 
	 * @param repositoryProvider
	 *            the repository provider
	 * @param repositoryId
	 *            the repository id
	 * @return a well formatted metrics id
	 */
	protected static String createMetricsId(final RepositoryProvider repositoryProvider, final String repositoryId) {
		return repositoryProvider.getProviderId() + "." + repositoryId + ".metrics";
	}

	/** the repository id */
	private final String repositoryId;

	/** the repository provider */
	private final RepositoryProvider repositoryProvider;

	/** indicates if the repository has been closed */
	private volatile boolean closed;

	/** the repository metrics */
	private final MetricSet metrics;

	/** the repository metrics registration */
	private volatile ServiceRegistration metricsRegistration;

	/** the content type support */
	private volatile RepositoryContentTypeSupport repositoryContentTypeSupport;

	/**
	 * Creates and returns a new repository instance.
	 * <p>
	 * Subclasses must call this constructor to initialize the base repository
	 * instance.
	 * </p>
	 * <p>
	 * The provided metrics will be registered with Gyrex on behalf of the
	 * bundle which loaded this class.
	 * </p>
	 * 
	 * @param repositoryId
	 *            the repository id (may not be <code>null</code>, must conform
	 *            to {@link IdHelper#isValidId(String)})
	 * @param repositoryProvider
	 *            the repository provider (may not be <code>null</code>)
	 * @param metrics
	 *            the metrics used by this repository (may not be
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if an invalid argument was specified
	 */
	protected Repository(final String repositoryId, final RepositoryProvider repositoryProvider, final MetricSet metrics) throws IllegalArgumentException {
		if (null == repositoryId) {
			throw new IllegalArgumentException("repository id must not be null");
		}
		if (null == repositoryProvider) {
			throw new IllegalArgumentException("repository provider must not be null");
		}
		if (null == metrics) {
			throw new IllegalArgumentException("metrics must not be null");
		}

		if (!IdHelper.isValidId(repositoryId)) {
			throw new IllegalArgumentException(MessageFormat.format("repository id \"{0}\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", repositoryId));
		}

		this.repositoryId = repositoryId;
		this.repositoryProvider = repositoryProvider;
		this.metrics = metrics;

		// register the metrics (this is the last call)
		registerMetrics();
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
			try {
				metricsRegistration.unregister();
			} catch (final IllegalStateException ignored) {
				// already unregistered
			}
			metricsRegistration = null;
		}
	}

	/**
	 * Called by the platform when a repository is no longer needed and all held
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
	 * Returns the {@link RepositoryContentTypeSupport} for the repository.
	 * <p>
	 * The default implementation returns a
	 * {@link BasicRepositoryContentTypeSupport basic support strategy}.
	 * Subclasses should override to provide a more sophisticated support
	 * strategy.
	 * </p>
	 * 
	 * @return the {@link RepositoryContentTypeSupport} (may not be
	 *         <code>null</code>)
	 */
	public RepositoryContentTypeSupport getContentTypeSupport() {
		if (null != repositoryContentTypeSupport) {
			return repositoryContentTypeSupport;
		}
		return repositoryContentTypeSupport = new BasicRepositoryContentTypeSupport(this);
	}

	/**
	 * Returns a human readable description of the repository that can be
	 * displayed to administrators, etc. An empty String is returned if no
	 * description is available.
	 * <p>
	 * The default implementation returns an empty String.
	 * </p>
	 * 
	 * @return a human readable description
	 */
	public String getDescription() {
		return "";
	}

	/**
	 * Returns the repository metadata with the specified id.
	 * <p>
	 * This is a handle only method. The returned metadata may not exist.
	 * </p>
	 * <p>
	 * The default implementation returns an instance which stores metadata in
	 * the repository preferences. Subclasses may override and provide a custom
	 * implementation.
	 * </p>
	 * 
	 * @param id
	 *            the metadata identifier (must validate using
	 *            {@link IdHelper#isValidId(String)})
	 * @return the {@link RepositoryMetadata}
	 * @see RepositoryMetadata
	 */
	public RepositoryMetadata getMetadata(final String id) {
		final RepositoryRegistry registry = PersistenceActivator.getInstance().getRepositoriesManager();
		final IRepositoryDefinition definition = registry.getRepositoryDefinition(repositoryId);
		if (definition == null) {
			throw new IllegalStateException(String.format("Repository definition not found for repository %s.", repositoryId));
		}
		return new RepositoryPreferencesBasedMetadata(definition.getRepositoryPreferences(), id, repositoryId);
	}

	/**
	 * Returns the repository metrics.
	 * 
	 * @return the metrics
	 */
	protected final MetricSet getMetrics() {
		return metrics;
	}

	/**
	 * Returns the name of the repository. This is the name of the class without
	 * the package name and used by {@link #toString()}.
	 * 
	 * @return the name of the repository
	 */
	private String getName() {
		String string = getClass().getName();
		final int index = string.lastIndexOf('.');
		if (index != -1) {
			string = string.substring(index + 1, string.length());
		}
		return string;
	}

	/**
	 * Returns the repository identifier.
	 * <p>
	 * The identifier is unique within Gyrex and persistent across session.
	 * </p>
	 * 
	 * @return the repository identifier
	 */
	public final String getRepositoryId() {
		return repositoryId;
	}

	/**
	 * Returns the repository provider.
	 * <p>
	 * The repository provider defines the underlying persistence technology. It
	 * gives advices on what is support by this repository and what not.
	 * </p>
	 * 
	 * @return the repository provider
	 */
	public final RepositoryProvider getRepositoryProvider() {
		return repositoryProvider;
	}

	/**
	 * Indicates if this <code>Repository</code> object has been closed.
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
		properties.put(Constants.SERVICE_VENDOR, getName() + "[" + getRepositoryId() + "]");
		properties.put(Constants.SERVICE_DESCRIPTION, "Metrics for repository '" + getRepositoryId() + "'.");
		properties.put(SERVICE_PROPERTY_REPOSITORY_ID, getRepositoryId());
		try {
			final String description = getDescription();
			if (StringUtils.isNotBlank(description)) {
				properties.put(SERVICE_PROPERTY_REPOSITORY_DESCRIPTION, description);
			}
		} catch (final Exception e) {
			// registerMetrics is called during object construction, therefore #getDescription might not be ready yet.
		}

		// register service
		metricsRegistration = bundleContext.registerService(MetricSet.SERVICE_NAME, metrics, properties);
	}

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * repository including information about the repository implementation and
	 * the repository id.
	 * 
	 * @return a string representation of the repository
	 */
	@Override
	public String toString() {
		return getName() + " {" + repositoryId + "}";
	}
}
