/*******************************************************************************
 * Copyright (c) 2013 <enter-company-name-here> and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.profiling;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.monitoring.metrics.BaseMetric;
import org.eclipse.gyrex.monitoring.metrics.BaseMetric.MetricFactory;

/**
 * A transaction is the central class for profiling an operation such as a
 * request or a background process.
 * <p>
 * A transaction allows to profile operation timings and invocation counts. It
 * maintains a stack of active operations in order to build a profiling
 * hierarchy. Metrics may be added to a transaction in order to collect
 * additional details.
 * </p>
 * <p>
 * A transaction is typically used to record data within the current thread.
 * Therefore, this class is not thread safe. The transaction of the current
 * thread can be obtained from {@link Profiler}.
 * </p>
 * <p>
 * Note, although this class is not marked <strong>final</strong> it is not
 * allowed to be subclassed outside the monitoring framework.
 * </p>
 * <p>
 * Warning: This is part of a new API that has not been finalized yet. Please
 * get in touch with the Gyrex developments if you intend to use it and this
 * warning is still present.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class Transaction {

	/**
	 * Configuration hint constant to enable collection of CPU time of the
	 * thread the transaction is used in (value is 1&lt;&lt;1).
	 */
	public static final int COLLECT_THREAD_CPU_TIME = 1 << 1;

	/** common date format */
	static final DateFormat ISO_8601_UTC = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

	private final String id;
	private final long created;
	private final long threadCpuTimeStart;
	private final TimeUnit timeUnit;
	private final Map<String, BaseMetric> metricsById;

	private volatile long duration;
	private volatile long consumedThreadCpuTime;

	/**
	 * Creates a new transaction.
	 * <p>
	 * Note, when a new transaction is created it will be initialized
	 * immediatly, i.e. any time sensitive metrics (such as thread CPU time,
	 * etc.) will be initialized using the time when a transaction object is
	 * created.
	 * </p>
	 * 
	 * @param id
	 *            the transaction id (must be valid according to
	 *            {@link BaseMetric#isValidId(String)})
	 * @param hints
	 *            configuration hints (multiple joined bit wise using OR) to
	 *            enabled or disable collection of additional data
	 */
	public Transaction(final String id, final int hints, final TimeUnit timeUnit) {
		if (!BaseMetric.isValidId(id))
			throw new IllegalArgumentException("id is invalid (see BaseMetric#isValidId): " + id);
		this.id = id;
		if (timeUnit == null)
			throw new IllegalArgumentException("no time unit specified");
		this.timeUnit = timeUnit;
		metricsById = new HashMap<String, BaseMetric>(5);

		if (((hints & COLLECT_THREAD_CPU_TIME) != 0) && ManagementFactory.getThreadMXBean().isCurrentThreadCpuTimeSupported()) {
			threadCpuTimeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		} else {
			threadCpuTimeStart = 0L;
		}

		// mark creation time
		created = System.currentTimeMillis();
	}

	/**
	 * Indicates if the transaction contains a metric with the specified id.
	 * 
	 * @param id
	 *            the id of the metric
	 * @return <code>true</code> if a metric with the specified id is available;
	 *         <code>false</code> otherwise
	 */
	public boolean containsMetric(final String id) {
		return metricsById.containsKey(id);
	}

	/**
	 * Marks a transaction finished.
	 */
	public void finished() {
		duration = System.currentTimeMillis() - created;
		if (threadCpuTimeStart > 0L) {
			consumedThreadCpuTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - threadCpuTimeStart;
		}
	}

	/**
	 * Returns the time when this transaction was created.
	 * <p>
	 * Note the date string is an <a
	 * href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601 UTC</a> string of
	 * the form <code>[YYYY][MM][DD]T[hh][mm][ss]Z</code>.
	 * </p>
	 * 
	 * @return the the time when this transaction was created
	 */
	public final String getCreationTime() {
		return ISO_8601_UTC.format(new Date(created));
	}

	/**
	 * Returns the time stamp when this transaction was created.
	 * <p>
	 * Note this is the raw time stamp in milliseconds analog to
	 * {@link System#currentTimeMillis()}.
	 * </p>
	 * 
	 * @return the the time when this transaction was created
	 */
	protected final long getCreationTimeTS() {
		return created;
	}

	/**
	 * Returns the measured duration of the transaction.
	 * 
	 * @return the transaction duration (maybe 0 if {@link #finished()} hasn't
	 *         been called yet)
	 */
	public long getDurationTime() {
		return timeUnit.convert(duration, TimeUnit.MILLISECONDS);
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
	 * Returns a metric with the specified id from the underlying map.
	 * <p>
	 * If no metric with the specified id is available, <code>null</code> will
	 * be returned.
	 * </p>
	 * 
	 * @param id
	 *            the id of the metric to retrieve
	 * @param <T>
	 *            the expected type of the metric; no type verification will be
	 *            done but an unchecked cast
	 * @return the metric (casted to <code>T</code>; may be <code>null</code>)
	 */
	@SuppressWarnings("unchecked")
	public <T extends BaseMetric> T getMetric(final String id) {
		return (T) metricsById.get(id);
	}

	/**
	 * Convenience method which create a new metric if no metric is available in
	 * the underlying map.
	 * 
	 * @param id
	 *            the id of the metric to retrieve
	 * @param factory
	 *            the metric factory
	 * @param <T>
	 *            the expected type of the metric; no type verification will be
	 *            done but an unchecked cast
	 * @return the metric (casted to <code>T</code>)
	 */
	@SuppressWarnings("unchecked")
	public <T extends BaseMetric> T getOrCreateMetric(final String id, final MetricFactory<T> factory) {
		BaseMetric metric = metricsById.get(id);
		if (metric == null) {
			metricsById.put(id, metric = factory.create(id));
		}
		return (T) metric;
	}

	/**
	 * Returns the total CPU time consumed by the transaction thread.
	 * <p>
	 * The time is collected from {@link ThreadMXBean#getCurrentThreadCpuTime()}
	 * .
	 * </p>
	 * 
	 * @return the total CPU time consumed by the transaction thread or 0L if
	 *         tracking of CPU is not enabled or is not possible
	 * @see ThreadMXBean#getCurrentThreadCpuTime()
	 */
	public long getThreadCpuTimeConsumed() {
		if (threadCpuTimeStart <= 0L)
			return 0L;
		return timeUnit.convert(consumedThreadCpuTime, TimeUnit.NANOSECONDS);
	}

	/**
	 * Computes and returns the average CPU utilization of the transaction
	 * thread.
	 * <p>
	 * A return value of 1.0 means 100%; 0 means 0%.
	 * </p>
	 * 
	 * @return the average CPU utilization of the transaction thread
	 */
	public double getThreadCpuUtilizationAverage() {
		if ((threadCpuTimeStart <= 0L) || (duration <= 0L))
			return 0;
		return (double) TimeUnit.NANOSECONDS.toMillis(consumedThreadCpuTime) / (double) duration;
	}

	/**
	 * Returns the transaction time unit.
	 * 
	 * @return the transaction time unit
	 */
	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	/**
	 * Adds the specified metric to the transaction.
	 * <p>
	 * The metric can be retrieved from the transaction using its
	 * {@link BaseMetric#getId() id} and {@link #getMetric(String)}. If a metric
	 * with the same id is already registered it will be replaced with the
	 * specified metric.
	 * </p>
	 * 
	 * @param metric
	 *            the metric to add (must not be <code>null</code>)
	 */
	public <T extends BaseMetric> void putMetric(final T metric) {
		metricsById.put(metric.getId(), metric);
	}

	/**
	 * Removes and returns a metric with the specified id from the underlying
	 * map.
	 * <p>
	 * If no metric with the specified id is available, <code>null</code> will
	 * be returned.
	 * </p>
	 * 
	 * @param id
	 *            the id of the metric to remove
	 * @param <T>
	 *            the expected type of the metric; no type verification will be
	 *            done but an unchecked cast
	 * @return the metric (casted to <code>T</code>; may be <code>null</code>)
	 */
	@SuppressWarnings("unchecked")
	public <T extends BaseMetric> T removeMetric(final String id) {
		return (T) metricsById.remove(id);
	}

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * transaction.
	 * 
	 * @return a string representation of the transaction
	 */
	@Override
	public String toString() {
		final StringBuilder toString = new StringBuilder();
		toString.append(getClass().getSimpleName()).append('(').append(getId()).append(')');
		return toString.toString();
	}
}
