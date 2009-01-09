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
package org.eclipse.cloudfree.persistence.jdbc.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.eclipse.cloudfree.monitoring.metrics.MetricSet;
import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.cloudfree.persistence.storage.provider.RepositoryProvider;

/**
 * Common base class for a repository which can be accessed via JDBC.
 * <p>
 * Typically, a JDBC repository provides common functionality for working with
 * databases. It is like a richer {@link DataSource} because it provides
 * additional features such as connection pooling and schema management.
 * </p>
 * <p>
 * This class may be subclassed by clients that contribute a JDBC repository
 * type to the CloudFree Platform.
 * </p>
 */
public abstract class JdbcRepository extends Repository {

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
	 *            the repository id (may not be <code>null</code>)
	 * @param repositoryType
	 *            the repository type (may not be <code>null</code>)
	 * @param metrics
	 *            the metrics used by this repository (may not be
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if an invalid argument was specified
	 */
	protected JdbcRepository(final String repositoryId, final RepositoryProvider repositoryType, final MetricSet metrics) {
		super(repositoryId, repositoryType, metrics);
	}

	/**
	 * Attempts to establish a connection with the data source that this
	 * repository instance represents.
	 * <p>
	 * Depending on the configuration of the repository this method may block
	 * and wait till a connection is available.
	 * </p>
	 * 
	 * @return a connection to the data source
	 * @exception SQLException
	 *                if a database access error occurs
	 */
	public abstract Connection getConnection() throws SQLException;

	/**
	 * Attempts to establish a connection with the data source that this
	 * repository instance represents within the given waiting time and while
	 * the current thread has not been {@linkplain Thread#interrupt()
	 * interrupted}.
	 * 
	 * @param timeout
	 *            the time to wait for a connection
	 * @param unit
	 *            the time unit of the timeout argument
	 * @return a connection to the data source or <code>null</code> if no
	 *         connection could be established within the given waiting time.
	 * @exception SQLException
	 *                if a database access error occurs
	 */
	public abstract Connection getConnection(final long timeout, final TimeUnit timeUnit) throws SQLException;
}
