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
package org.eclipse.cloudfree.persistence.jdbc.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.eclipse.cloudfree.monitoring.metrics.BaseMetric;
import org.eclipse.cloudfree.monitoring.metrics.ErrorMetric;
import org.eclipse.cloudfree.monitoring.metrics.MetricSet;
import org.eclipse.cloudfree.monitoring.metrics.PoolMetric;
import org.eclipse.cloudfree.monitoring.metrics.StatusMetric;

public class JdbcRepositoryMetrics extends MetricSet {

	private static String getError(final SQLException sqlException) {
		final String error = MessageFormat.format("[SQLException] {0}; SQLState: {1}, Vendor ErrorCode: {2}", sqlException.getMessage(), sqlException.getSQLState(), sqlException.getErrorCode());
		return error;
	}

	private static String getErrorDetails(final String sql, SQLException sqlException) {
		final StringWriter errorDetailsStringWriter = new StringWriter(1024);
		final PrintWriter errorDetailsWriter = new PrintWriter(errorDetailsStringWriter);
		errorDetailsWriter.append("[SQL] ").append(sql);
		errorDetailsWriter.println();
		while (sqlException != null) {
			errorDetailsWriter.append("[SQLException]  ").append(sqlException.getMessage()).append("; ");
			errorDetailsWriter.append("SQLState: ").append(sqlException.getSQLState()).append(", ");
			errorDetailsWriter.append("Vendor ErrorCode: ").append(String.valueOf(sqlException.getErrorCode()));
			errorDetailsWriter.println();
			sqlException.printStackTrace(errorDetailsWriter);
			sqlException = sqlException.getNextException();
		}
		errorDetailsWriter.flush();
		return errorDetailsStringWriter.toString();
	}

	private final PoolMetric poolMetric;

	private final StatusMetric poolStatusMetric;

	private final ErrorMetric errorMetric;

	protected JdbcRepositoryMetrics(final String id, final String initialStatus, final String initialStatusReason, final long initialChannelsCapacity, final long initialChannelsMinimum) {
		super(id, new BaseMetric[] { new StatusMetric(id + ".status", initialStatus, initialStatusReason), new PoolMetric(id + ".pool", initialChannelsCapacity, initialChannelsMinimum), new ErrorMetric(id + ".errors", true) });
		poolStatusMetric = getMetric(0, StatusMetric.class);
		poolMetric = getMetric(1, PoolMetric.class);
		errorMetric = getMetric(2, ErrorMetric.class);
	}

	/**
	 * Returns the last error metric.
	 * 
	 * @return the last error metric
	 */
	public ErrorMetric getErrorMetric() {
		return errorMetric;
	}

	/**
	 * Returns the pool metric.
	 * 
	 * @return the pool metric
	 */
	public PoolMetric getPoolMetric() {
		return poolMetric;
	}

	/**
	 * Returns the pool status metric.
	 * 
	 * @return the pool status metric
	 */
	public StatusMetric getPoolStatusMetric() {
		return poolStatusMetric;
	}

	public void setClosed(final String reason) {
		getPoolStatusMetric().setStatus("closed", reason);
	}

	public void setSQLError(final String sql, final SQLException sqlException) {
		final String error = getError(sqlException);
		final String errorDetails = getErrorDetails(sql, sqlException);
		getErrorMetric().setLastError(error, errorDetails);
	}
}
