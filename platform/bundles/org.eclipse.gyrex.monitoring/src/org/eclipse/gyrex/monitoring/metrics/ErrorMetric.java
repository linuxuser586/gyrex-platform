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
package org.eclipse.gyrex.monitoring.metrics;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

/**
 * A metric for monitoring errors.
 * <p>
 * An error metric is typically used to monitor the errors of a component. In
 * addition to the plain errors it provides attributes which help administrator
 * to understand the metric better.
 * </p>
 * <p>
 * Note, although this class is not marked <strong>final</strong> it is not
 * allowed to be subclassed outside the monitoring framework.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ErrorMetric extends BaseMetric {

	/**
	 * Captures statistical information about an error.
	 */
	public static final class ErrorStats {

		private final String error;
		private final String errorDetails;
		private volatile long errorOccuredCount;

		/**
		 * Creates a new instance.
		 * 
		 * @param error
		 *            the error
		 * @param errorDetails
		 *            the error details
		 */
		/*package*/ErrorStats(final String error, final String errorDetails) {
			this.error = error;
			this.errorDetails = errorDetails;
		}

		/**
		 * Increments the error counter.
		 * <p>
		 * Note, this is not thread-safe. It is expected that this method
		 * participates in the protection of {@link ErrorMetric}.
		 * </p>
		 */
		/*package*/void errorOccured() {
			errorOccuredCount++;
		}

		/**
		 * Returns the error.
		 * 
		 * @return the error
		 */
		public String getError() {
			return error;
		}

		/**
		 * Returns the error details.
		 * 
		 * @return the error details
		 */
		public String getErrorDetails() {
			return errorDetails;
		}

		/**
		 * Returns how often this error occurred.
		 * 
		 * @return how often this error occurred.
		 */
		public long getErrorOccuredCount() {
			return errorOccuredCount;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("Error [").append(error).append("] with details [").append(errorDetails).append("] occured ").append(errorOccuredCount).append(" time(s).");
			return builder.toString();
		}
	}

	private static final ErrorStats[] NO_STATS = new ErrorStats[0];

	private static final String EMPTY = "";

	/** the last error */
	private volatile String lastError;

	/** the last error details (eg. stack trace) */
	private volatile String lastErrorDetails;

	/** the last error time */
	private volatile long lastErrorChangeTime;

	/** a total number of errors since the last reset */
	private volatile long totalNumberOfErrors;

	/** a map recording error stats */
	private final ConcurrentMap<String, ErrorStats> errorStats;

	/**
	 * Creates a new error metric instance.
	 * 
	 * @param id
	 *            the metric id
	 * @param trackErrorStats
	 *            <code>true</code> if errors should be tracked to generate
	 *            {@link #getErrorStats() statistics}, <code>false</code>
	 *            otherwise
	 */
	public ErrorMetric(final String id, final boolean trackErrorStats) {
		super(id);

		// note, we do not invoke setStatus here because calling non-private
		// methods during object initialization is problematic at best
		lastError = EMPTY;
		lastErrorDetails = EMPTY;

		// initialize error stats
		if (trackErrorStats) {
			errorStats = new ConcurrentHashMap<String, ErrorStats>(5);
		} else {
			errorStats = null;
		}
	}

	/**
	 * Resets the status metric.
	 * <p>
	 * Subclasses may extend but are required to call <code>super</code>.
	 * </p>
	 * <p>
	 * At the time this method is invoked, the current thread has acquired the
	 * {@link #getWriteLock() write lock} already. Subclasses must
	 * <strong>not</strong> modify the write lock.
	 * </p>
	 * <p>
	 * Note, this method is called by {@link #resetStats()} and should not be
	 * invoked directly.
	 * </p>
	 * 
	 * @see org.eclipse.gyrex.monitoring.metrics.BaseMetric#doResetStats()
	 */
	@Override
	protected void doResetStats() {
		totalNumberOfErrors = 0;
		if (null != errorStats) {
			errorStats.clear();
		}
	}

	@Override
	protected Object[] dumpMetrics() {
		return new Object[] { "error|detail|since|total errors", getLastError(), getLastErrorDetails(), getLastErrorChangeTime(), getTotalNumberOfErrors() };
	}

	/**
	 * Returns the collected error statistics.
	 * <p>
	 * Note, the returned statistics array represents the set of statistics at
	 * the time of invoking this method. But the individual {@link ErrorStats}
	 * objects are live objects which will continue to be updated when that
	 * particular error occurred again.
	 * </p>
	 * 
	 * @return the error statistics
	 */
	public ErrorStats[] getErrorStats() {
		if (null != errorStats) {
			int tries = 0;
			do {
				try {
					return errorStats.values().toArray(NO_STATS);
				} catch (final ConcurrentModificationException e) {
					tries++;
				}
			} while (tries < 3);
		}
		return NO_STATS;
	}

	/**
	 * Returns the last error.
	 * 
	 * @return the last error
	 */
	public String getLastError() {
		return lastError;
	}

	/**
	 * Returns the time of the last error.
	 * <p>
	 * Note the date string is an <a
	 * href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601 UTC</a> string of
	 * the form <code>[YYYY][MM][DD]T[hh][mm][ss]Z</code>.
	 * </p>
	 * 
	 * @return the time of the last error change
	 */
	public String getLastErrorChangeTime() {
		return ISO_8601_UTC.format(new Date(lastErrorChangeTime));
	}

	/**
	 * Returns the details for the last error.
	 * 
	 * @return the details for the last error
	 */
	public String getLastErrorDetails() {
		return lastErrorDetails;
	}

	/**
	 * Returns the total number of errors since the last {@link #resetStats()
	 * reset}.
	 * <p>
	 * Note, the number of errors counter is not overflow safe.
	 * </p>
	 * 
	 * @return the total number of errors since the last statistics reset
	 */
	public long getTotalNumberOfErrors() {
		return totalNumberOfErrors;
	}

	@Override
	protected void populateAttributes(final List<MetricAttribute> attributes) {
		super.populateAttributes(attributes);
		attributes.add(new MetricAttribute("lastError", "the last error", String.class));
		attributes.add(new MetricAttribute("lastErrorDetails", "the last error details (eg. stack trace)", String.class));
		attributes.add(new MetricAttribute("lastErrorChangeTime", "the last error time", String.class));
		attributes.add(new MetricAttribute("totalNumberOfErrors", "a total number of errors since the last reset", Long.class));
	}

	@Override
	protected void populateAttributeValues(final Map<String, Object> values) {
		super.populateAttributeValues(values);
		values.put("lastError", getLastError());
		values.put("lastErrorDetails", getLastErrorDetails());
		values.put("lastErrorChangeTime", getLastErrorChangeTime());
		values.put("totalNumberOfErrors", getTotalNumberOfErrors());
	}

	/**
	 * Sets a new error.
	 * <p>
	 * Note, it is considered best practice to provide a detail for the error.
	 * System operators will appreciate it.
	 * </p>
	 * 
	 * @param error
	 *            the error (may not be <code>null</code>)
	 * @param errorDetails
	 *            the error details (may not be <code>null</code>)
	 */
	public void setLastError(final String error, final String errorDetails) {
		if (null == error) {
			throw new IllegalArgumentException("error may not be null");
		}
		if (null == errorDetails) {
			throw new IllegalArgumentException("error details may not be null");
		}

		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			// set error
			lastError = error;
			lastErrorDetails = errorDetails;
			lastErrorChangeTime = System.currentTimeMillis();
			totalNumberOfErrors++;

			// update stats
			if (null != errorStats) {
				final String key = error.concat(errorDetails);
				if (!errorStats.containsKey(key)) {
					errorStats.put(key, new ErrorStats(error, errorDetails));
				}
				errorStats.get(key).errorOccured();
			}
		} finally {
			writeLock.unlock();
		}
	}
}
