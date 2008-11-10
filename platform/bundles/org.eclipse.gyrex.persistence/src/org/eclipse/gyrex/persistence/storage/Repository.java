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
package org.eclipse.cloudfree.persistence.storage;

import java.text.MessageFormat;


import org.eclipse.cloudfree.monitoring.metrics.MetricSet;
import org.eclipse.cloudfree.persistence.internal.PersistenceActivator;
import org.eclipse.cloudfree.persistence.storage.type.RepositoryType;
import org.eclipse.core.runtime.PlatformObject;
import org.osgi.framework.ServiceRegistration;

/**
 * A repository is a concrete instance of a repository type. You may think of it
 * as a database. It defines the data store where a particular set of data will
 * be persisted.
 * <p>
 * A specific repository may only be accessed using the technology defined by
 * the repository type.
 * </p>
 * <p>
 * In order to simplify operation of the CloudFree platform repositories will
 * support administrative tasks. For example, it will be possible to transfer a
 * repository into a read-only mode or disabled it completely for maintenance
 * purposes. It will also be possible to dump a repository (or portions of it)
 * into a repository type independent format. The dump may be loaded later into
 * another repository.
 * </p>
 * <p>
 * This class must be subclassed by clients that contribute a repository type to
 * the CloudFree Platform.
 * </p>
 */
public abstract class Repository extends PlatformObject {

	/**
	 * Indicates if the specified id is a valid repository id.
	 * <p>
	 * By definition, a repository id must not be <code>null</code> or the empty
	 * string and may only contain lowercase latin characters <code>a..z</code>,
	 * numbers <code>0..9</code>, <code>'.'</code>, <code>'-'</code> and
	 * <code>'_'</code>. This method must be used to validate the repository id.
	 * </p>
	 * 
	 * @param repositoryId
	 *            the repository id
	 * @return <code>true</code> if the repository id is valid,
	 *         <code>false</code> otherwise
	 */
	public static boolean isValidRepositoryId(final String repositoryId) {
		if (null == repositoryId) {
			return false;
		}

		if (repositoryId.equals("")) {
			return false;
		}

		// verify chars
		for (int i = 0; i < repositoryId.length(); i++) {
			final char c = repositoryId.charAt(i);
			if (((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || (c == '.') || (c == '_') || (c == '-')) {
				continue;
			} else {
				return false;
			}
		}

		return true;
	}

	/** the repository id */
	private final String repositoryId;

	/** the repository type */
	private final RepositoryType repositoryType;

	/** indicates if the repository has been closed */
	private volatile boolean closed;

	/** the repository metrics */
	private final MetricSet metrics;

	/** the repository metrics registration */
	private volatile ServiceRegistration metricsRegistration;

	/**
	 * Creates and returns a new repository instance.
	 * <p>
	 * Subclasses must call this constructor to initialize the base repository
	 * instance.
	 * </p>
	 * <p>
	 * The provided metrics will be registered with the CloudFree platform.
	 * </p>
	 * 
	 * @param repositoryId
	 *            the repository id (may not be <code>null</code>, must conform
	 *            to {@link #isValidRepositoryId(String)})
	 * @param repositoryType
	 *            the repository type (may not be <code>null</code>)
	 * @param metrics
	 *            the metrics used by this repository (may not be
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if an invalid argument was specified
	 */
	protected Repository(final String repositoryId, final RepositoryType repositoryType, final MetricSet metrics) throws IllegalArgumentException {
		if (null == repositoryId) {
			throw new IllegalArgumentException("repository id must not be null");
		}
		if (null == repositoryType) {
			throw new IllegalArgumentException("repository type must not be null");
		}
		if (null == metrics) {
			throw new IllegalArgumentException("metrics must not be null");
		}

		if (!Repository.isValidRepositoryId(repositoryId)) {
			throw new IllegalArgumentException(MessageFormat.format("repository id \"{0}\" is invalid", repositoryId));
		}

		this.repositoryId = repositoryId;
		this.repositoryType = repositoryType;
		this.metrics = metrics;

		// register the metrics
		metricsRegistration = PersistenceActivator.getInstance().getServiceHelper().registerService(MetricSet.class.getName(), metrics, getName() + "(" + repositoryId + ")", "Metrics for repository " + repositoryId, null, null);
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
	 * @return
	 */
	public RepositoryContentTypeSupport getContentTypeSupport() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns a human readable description of the repository that can be
	 * displayed to administrators, etc. An empty String is returned of no
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
	 * The identifier is unique within the CloudFree Platform and persistent
	 * across session.
	 * </p>
	 * 
	 * @return the repository identifier
	 */
	public final String getRepositoryId() {
		return repositoryId;
	}

	/**
	 * Returns the repository type.
	 * <p>
	 * The repository type defines the underlying persistence technology. It
	 * gives advices on what is support by this repository and what not.
	 * </p>
	 * 
	 * @return the repository type
	 */
	public final RepositoryType getRepositoryType() {
		return repositoryType;
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
	 * Returns a string containing a concise, human-readable description of the
	 * repository.
	 * 
	 * @return a string representation of the repository
	 */
	@Override
	public String toString() {
		return getName() + " {" + repositoryId + "}";
	}
}
