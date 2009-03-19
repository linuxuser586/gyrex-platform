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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class for a metric.
 * <p>
 * Every metric can be identified by an {@link #getId() id}. It's recommended to
 * follow Java package naming for ids.
 * </p>
 * <p>
 * Typically, metrics are thread safe in terms of concurrent updates. However,
 * retrieving all metric values in one atomic operation is not supported.
 * </p>
 * <p>
 * Note, although this class is marked <strong>abstract</strong> it is not
 * allowed to be subclassed outside the monitoring framework.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class BaseMetric {

	static final String[] NO_METRICS = new String[0];

	protected static final DateFormat ISO_8601_UTC = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

	/** the id */
	private final String id;

	/** a lock for protecting against concurrent set and reset */
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	/** the last reset timestamp */
	private volatile long statsSince;

	/**
	 * Creates a new metric using the specified id.
	 * 
	 * @param id
	 *            the metric id
	 */
	protected BaseMetric(final String id) {
		if (null == id) {
			throw new IllegalArgumentException("id may not be null");
		}
		this.id = id;

		// note, we do not invoke resetStats here because calling non-private
		// methods during object initialization is problematic at best
		statsSince = System.currentTimeMillis();
	}

	/**
	 * Hook for subclasses to overwrite when statistics need to be resetted.
	 * <p>
	 * At the time this method is invoked, the current thread has acquired the
	 * {@link #getWriteLock() write lock} already. Subclasses must
	 * <strong>not</strong> modify the write lock.
	 * </p>
	 * <p>
	 * The default implementation does nothing. Subclasses my extend.
	 * </p>
	 * <p>
	 * Note, this method is called by {@link #resetStats()} and should not be
	 * invoked directly.
	 * </p>
	 */
	protected void doResetStats() {
		// empty
	}

	/**
	 * Returns a text info of the metric for use by {@link #toString()}.
	 * <p>
	 * Note, this operation is not atomic.
	 * </p>
	 * 
	 * @return a text info of the metric
	 */
	protected Object[] dumpMetrics() {
		return NO_METRICS;
	}

	/**
	 * Returns the metric id.
	 * 
	 * @return the metric id
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Returns the name of the metric. This is the name of the class without the
	 * package name and used by {@link #toString()}.
	 * 
	 * @return the name of the metric
	 */
	String getName() {
		String string = getClass().getName();
		final int index = string.lastIndexOf('.');
		if (index != -1) {
			string = string.substring(index + 1, string.length());
		}
		return string;
	}

	/**
	 * Returns a lock which can be used in combination with the
	 * {@link #getWriteLock() write lock} to protect against unsafe reads.
	 * 
	 * @return a lock for write operations
	 */
	protected final Lock getReadLock() {
		return readWriteLock.readLock();
	}

	/**
	 * Returns the time since the last {@link #resetStats() statistics reset}.
	 * <p>
	 * Note the date string is an <a
	 * href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601 UTC</a> string of
	 * the form <code>[YYYY][MM][DD]T[hh][mm][ss]Z</code>.
	 * </p>
	 * 
	 * @return the the time since the last statistics reset
	 */
	public final String getStatsSince() {
		return ISO_8601_UTC.format(new Date(statsSince));
	}

	/**
	 * Returns the time stamp since the last {@link #resetStats() statistics
	 * reset}.
	 * <p>
	 * Note this is the raw time stamp in milliseconds analog to
	 * {@link System#currentTimeMillis()}.
	 * </p>
	 * 
	 * @return the the time since the last statistics reset
	 */
	protected final long getStatsSinceTS() {
		return statsSince;
	}

	/**
	 * Returns a lock which should be used to protect against concurrent set and
	 * reset operations.
	 * 
	 * @return a lock for write operations
	 */
	protected final Lock getWriteLock() {
		return readWriteLock.writeLock();
	}

	/**
	 * Resets the metric statistics but not the metric values.
	 */
	public final void resetStats() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			statsSince = System.currentTimeMillis();
			doResetStats();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * metric.
	 * 
	 * @return a string representation of the metric
	 */
	@Override
	public String toString() {
		final StringBuilder toString = new StringBuilder();
		toString.append(getName()).append('(').append(getId()).append(')');
		toString.append(" {");
		final Object[] metrics = dumpMetrics();
		for (int i = 0; i < metrics.length; i++) {
			if (i > 0) {
				toString.append(',');
			}
			final Object metric = metrics[i];
			if (null != metric) {
				toString.append(metric);
			}
		}
		toString.append('}');
		return toString.toString();
	}

}
