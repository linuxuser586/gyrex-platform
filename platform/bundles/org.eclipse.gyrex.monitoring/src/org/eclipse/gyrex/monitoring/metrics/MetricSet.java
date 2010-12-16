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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * An immutable set of {@link BaseMetric metrics}.
 * <p>
 * Typically, individual metrics are thread safe in terms of concurrent updates.
 * However, retrieving all metric values in one atomic operation is not
 * supported.
 * </p>
 * <p>
 * Metrics must be registered with Gyrex by registering them as OSGi services
 * using {@link #SERVICE_NAME this class name}.
 * </p>
 * <p>
 * This class may be extended by clients to provide a convenient set of metrics.
 * </p>
 */
public abstract class MetricSet extends BaseMetric {

	/** the OSGi service name */
	public static final String SERVICE_NAME = MetricSet.class.getName();

	/** the metrics */
	private final List<BaseMetric> metrics;

	/** the description */
	private final String description;

	/**
	 * Creates a new metric set using the specified id and metrics.
	 * <p>
	 * The metrics are stored in an immutable way and can be retrieved using
	 * {@link #getMetric(int, Class)}.
	 * </p>
	 * <p>
	 * The ids of the metrics may but don't need to be prefixed with the metric
	 * set id. In any case the will be interpreted within the scope of the
	 * metric set id.
	 * </p>
	 * 
	 * @param id
	 *            the metric id
	 * @param description
	 *            the metric description
	 * @param metrics
	 *            the metrics which form this set
	 */
	protected MetricSet(final String id, final String description, final BaseMetric... metrics) {
		super(id);
		if (null == metrics) {
			throw new IllegalArgumentException("metrics may not be null");
		}

		// save a copy to prevent external modifications
		this.metrics = new ArrayList<BaseMetric>(metrics.length);
		for (final BaseMetric metric : metrics) {
			this.metrics.add(metric);
		}

		// save description
		this.description = StringUtils.trimToEmpty(description);
	}

	/**
	 * Resets all metrics in this set.
	 * <p>
	 * At the time this method is invoked, the current thread has acquired the
	 * {@link #getWriteLock() write lock} already. Subclasses must
	 * <strong>not</strong> modify the write lock.
	 * </p>
	 * <p>
	 * Subclasses my extend and may call <code>super</code> where appropriate.
	 * </p>
	 * <p>
	 * Note, this method is called by {@link #resetStats()} and should not be
	 * invoked directly.
	 * </p>
	 */
	@Override
	protected void doResetStats() {
		// reset all metric
		for (final BaseMetric metric : metrics) {
			// we call reset stats to properly lock the metric
			metric.resetStats();
		}
	}

	/**
	 * Not used.
	 */
	@Override
	final Object[] dumpMetrics() {
		return NO_METRICS;
	}

	/**
	 * Returns a human readable description of the metric that can be displayed
	 * to administrators, etc. An empty String is returned if no description is
	 * available.
	 * 
	 * @return a human readable description
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Returns a metric stored at the specified position in this set.
	 * 
	 * @param <T>
	 *            the metric type
	 * @param position
	 *            the position
	 * @param metricType
	 *            the metric type
	 * @return the metric
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range (
	 *             <code>index &lt; 0 || index &gt;= size()</code>)
	 * @throws IllegalArgumentException
	 *             if the metric at the specified position cannot be cased to
	 *             the given type
	 */
	@SuppressWarnings("unchecked")
	protected final <T extends BaseMetric> T getMetric(final int position, final Class<T> metricType) throws IndexOutOfBoundsException, IllegalArgumentException {
		final BaseMetric metric = metrics.get(position);
		if (null == metric) {
			return null;
		}
		if (!metricType.isAssignableFrom(metric.getClass())) {
			throw new IllegalArgumentException(MessageFormat.format("metric at position {0} is not of type {1} but of type {2}", position, metricType.getName(), metric.getClass().getName()));
		}
		return (T) metric;
	}

	/**
	 * Returns the metrics contained in the set.
	 * <p>
	 * Although public this method must not be called by clients. It exposes the
	 * raw metrics which isn't of any generally use for clients. Typically,
	 * sub-classes provide a more suitable API for working with metrics. The
	 * framework uses this method to obtain the raw metrics for processing
	 * purposes.
	 * </p>
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * @return an unmodifiable list of metrics contained in the set
	 */
	public final List<BaseMetric> getMetrics() {
		return Collections.unmodifiableList(metrics);
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
		toString.append(getClass().getName()).append('(').append(getId()).append(')');
		toString.append(" [");
		final Object[] metrics = this.metrics.toArray();
		for (int i = 0; i < metrics.length; i++) {
			if (i > 0) {
				toString.append(", ");
			} else {
				toString.append(' ');
			}
			final Object metric = metrics[i];
			if (null != metric) {
				toString.append(metric);
			}
			if (i == metrics.length - 1) {
				toString.append(' ');
			}
		}
		toString.append("]");
		return toString.toString();
	}
}
