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
package org.eclipse.cloudfree.monitoring.metrics;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

/**
 * An immutable set of {@link BaseMetric metrics}.
 * <p>
 * Typically, individual metrics are thread safe in terms of concurrent updates.
 * However, retrieving all metric values in one atomic operation is not
 * supported.
 * </p>
 * <p>
 * Metrics must be registered with the CloudFree platform by registering them as
 * OSGi services using this class.
 * </p>
 * <p>
 * This class may be extended by clients to provide a convenient set of metrics.
 * </p>
 */
public abstract class MetricSet extends BaseMetric {

	/** the metrics */
	private final List<BaseMetric> metrics;

	/**
	 * Creates a new metric set using the specified id and metrics.
	 * <p>
	 * The metrics are stored in an immutable list and can be retrieved using
	 * {@link #getMetric(int, Class)}.
	 * </p>
	 * 
	 * @param id
	 *            the metric id
	 * @param metrics
	 *            the metrics which form this set
	 */
	protected MetricSet(final String id, final BaseMetric... metrics) {
		super(id);

		if (null == metrics) {
			throw new IllegalArgumentException("metrics may not be null");
		}
		this.metrics = Arrays.asList(Arrays.copyOf(metrics, metrics.length));
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
	protected final Object[] dumpMetrics() {
		return NO_METRICS;
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
	protected <T extends BaseMetric> T getMetric(final int position, final Class<T> metricType) throws IndexOutOfBoundsException, IllegalArgumentException {
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
	 * Returns a string containing a concise, human-readable description of the
	 * metric.
	 * 
	 * @return a string representation of the metric
	 */
	@Override
	public String toString() {
		final StringBuilder toString = new StringBuilder();
		toString.append(getName()).append('(').append(getId()).append(')');
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
