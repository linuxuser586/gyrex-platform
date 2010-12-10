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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.CharSet;
import org.apache.commons.lang.StringUtils;

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

	/** char set of allowed id chars */
	static final CharSet ALLOWED_ID_CHARS = CharSet.getInstance(new String[] { "a-z", "A-Z", "0-9", ".", "-", "_" });

	/** helper constant */
	static final String[] NO_METRICS = new String[0];

	/** common date format */
	protected static final DateFormat ISO_8601_UTC = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

	/**
	 * Indicates if the specified id is a valid metric id.
	 * <p>
	 * By definition, all identifiers used within Metrics APIs must not be
	 * <code>null</code> or the empty string and may only contain the following
	 * printable ASCII characters.
	 * <ul>
	 * <li>lower- and uppercase letters <code>a..z</code> and <code>A..Z</code></li>
	 * <li>numbers <code>0..9</code></li>
	 * <li><code>'.'</code></li>
	 * <li><code>'-'</code></li>
	 * <li><code>'_'</code></li>
	 * </ul>
	 * </p>
	 * <p>
	 * This method is used to validate identifiers within the Gyrex API. Clients
	 * may call it to verify user entered ids.
	 * </p>
	 * 
	 * @param id
	 *            the id
	 * @return <code>true</code> if the id is valid, <code>false</code>
	 *         otherwise
	 */
	public static boolean isValidId(final String id) {
		// not null or blank
		if (StringUtils.isBlank(id)) {
			return false;
		}

		// scan for invalid chars
		for (final char c : id.toCharArray()) {
			if (!ALLOWED_ID_CHARS.contains(c)) {
				return false;
			}
		}

		return true;
	}

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
	 *            the metric id (must be valid according to
	 *            {@link #isValidId(String)})
	 */
	protected BaseMetric(final String id) {
		if (!isValidId(id)) {
			throw new IllegalArgumentException("id is invalid (see BaseMetric#isValidId): " + id);
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
	 * 
	 * @noreference This method is not intended to be referenced by clients.
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
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected Object[] dumpMetrics() {
		return NO_METRICS;
	}

	/**
	 * Returns a list of attributes contained in the metric.
	 * <p>
	 * Although public this method must not be called by clients. The framework
	 * uses this method to obtain further information about a metric for
	 * processing purposes.
	 * </p>
	 * 
	 * @return an unmodifiable collection of metric attributes
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final List<MetricAttribute> getAttributes() {
		final List<MetricAttribute> attributes = new ArrayList<MetricAttribute>();
		populateAttributes(attributes);
		return Collections.unmodifiableList(attributes);
	}

	/**
	 * Returns a map of attribute values contained in the metric.
	 * <p>
	 * Although public this method must not be called by clients. The framework
	 * uses this method to obtain further information about a metric for
	 * processing purposes.
	 * </p>
	 * 
	 * @return an unmodifiable map of metric attribute values
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Map<String, ?> getAttributeValues() {
		final Map<String, Object> attributeValues = new HashMap<String, Object>();
		final Lock lock = getWriteLock();
		lock.lock();
		try {
			populateAttributeValues(attributeValues);
		} finally {
			lock.unlock();
		}
		return Collections.unmodifiableMap(attributeValues);
	}

	/**
	 * Returns the metric id.
	 * <p>
	 * The metric id is used to uniquely identify a metric with the system. It's
	 * generally a good practise to start the id with a scope identifier (eg., a
	 * bundle symbolic name).
	 * </p>
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
	 * @noreference This method is not intended to be referenced by clients.
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
	 * Populates the specified map with metric attribute information for use by
	 * {@link #getAttributes()}.
	 * <p>
	 * Subclasses should override and add the attributes they defined. They must
	 * call <code>super</code> in order to also populate the attributes defined
	 * in the super class.
	 * </p>
	 * <p>
	 * Note, this method is called by {@link #getAttributes()} and should not be
	 * invoked directly.
	 * </p>
	 * 
	 * @param attributes
	 *            the list to populate with the attributes
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void populateAttributes(final List<MetricAttribute> attributes) {
		attributes.add(new MetricAttribute("statsSince", "the last reset time", String.class));
	}

	/**
	 * Populates the specified map with metric attribute values for use by
	 * {@link #getAttributeValues()}.
	 * <p>
	 * Subclasses should override and add values of attributes they defined.
	 * They must call <code>super</code> in order to also populate the
	 * attributes defined in the super class.
	 * </p>
	 * <p>
	 * At the time this method is invoked, the current thread has acquired the
	 * {@link #getWriteLock() write lock} already. Subclasses must
	 * <strong>not</strong> modify the write lock.
	 * </p>
	 * <p>
	 * Note, this method is called by {@link #getAttributeValues()} and should
	 * not be invoked directly.
	 * </p>
	 * 
	 * @param values
	 *            the map to populate with the attribute values
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void populateAttributeValues(final Map<String, Object> values) {
		values.put("statsSince", getStatsSince());
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
