/*******************************************************************************
 * Copyright (c) 2008, 2012 Gunnar Wagenknecht and others.
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * A metric for monitoring throughput (eg. requests, invocations).
 * <p>
 * The throughput metric allows to measure the number of "requests" processed by
 * a system. A request can be anything from a single method invocation, a single
 * web request till a large process involving many subsequent operations.
 * </p>
 * <p>
 * Note, although this class is not marked <strong>final</strong> it is not
 * allowed to be subclassed outside the monitoring framework.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ThroughputMetric extends BaseMetric {

	/** the number of active requests */
	private volatile long requestsActive;

	/** the high water mark since the last statistics reset */
	private volatile long requestsStatsHigh;

	/**
	 * the total number of requests processed (excluding failed requests) since
	 * the last statistics reset
	 */
	private volatile long requestsStatsProcessed;

	/**
	 * the request hit rate per hour (including failed requests) since the last
	 * statistics reset
	 */
	private volatile long requestsStatsHitRatePerHour;

	/**
	 * the request hit rate per minute (including failed requests) since the
	 * last statistics reset
	 */
	private volatile long requestsStatsHitRatePerMinute;

	/**
	 * the request hit rate per second (including failed requests) since the
	 * last statistics reset
	 */
	private volatile long requestsStatsHitRatePerSecond;

	/** the total number of failed requests since the last statistics reset */
	private volatile long requestsStatsFailed;

	/**
	 * the request failure rate since the last statistics reset (percentage of
	 * failed requests vs. processed + failed requests)
	 */
	private volatile float requestsStatsFailureRate;

	/**
	 * the total number of size units processed by requests (excluding failed
	 * requests) since the last statistics reset
	 */
	private volatile long requestsStatsSize;

	/**
	 * the average number of size units processed by a request (excluding failed
	 * requests) since the last statistics reset
	 */
	private volatile long requestsStatsSizeAverage;

	/**
	 * the total number of time consumed processing requests (excluding failed
	 * requests) since the last statistics reset
	 */
	private final Counter requestsStatsProcessingTime = new Counter();

	private final TimeUnit timeUnit;

	/**
	 * Creates a new throughput metric instance using time unit
	 * {@link TimeUnit#MILLISECONDS}.
	 * 
	 * @param id
	 *            the metric id
	 */
	public ThroughputMetric(final String id) {
		this(id, TimeUnit.MILLISECONDS);
	}

	/**
	 * Creates a new throughput metric instance.
	 * 
	 * @param id
	 *            the metric id
	 * @param timeUnit
	 *            the time unit used in the metric
	 */
	public ThroughputMetric(final String id, final TimeUnit timeUnit) {
		super(id);
		if (timeUnit == null)
			throw new IllegalArgumentException("no time unit specified");
		this.timeUnit = timeUnit;
	}

	/**
	 * Resets the throughput metric.
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
	void doResetStats() {
		requestsStatsHigh = 0;
		requestsStatsProcessed = 0;
		requestsStatsHitRatePerMinute = 0;
		requestsStatsFailed = 0;
		requestsStatsHigh = 0;
		requestsStatsSize = 0;
		requestsStatsSizeAverage = 0;
		requestsStatsProcessingTime.reset();
	}

	@Override
	Object[] dumpMetrics() {
		return new Object[] { "active|high|processed|rate|size|size average|time|time average|time high|time low|time stddev", getRequestsActive(), getRequestsStatsHigh(), getRequestsStatsProcessed(), getRequestsStatsHitRatePerMinute(), getRequestsStatsSize(), getRequestsStatsSizeAverage(), getRequestsStatsProcessingTime(), getRequestsStatsProcessingTimeAverage(), getRequestsStatsProcessingTimeHigh(), getRequestsStatsProcessingTimeLow(), getRequestsStatsProcessingTimeStandardDeviation() };
	}

	/**
	 * Returns the number of active requests.
	 * 
	 * @return the number of active requests
	 */
	public long getRequestsActive() {
		return requestsActive;
	}

	/**
	 * Returns the total number of failed requests since the last statistics
	 * reset.
	 * 
	 * @return the total number of failed requests
	 */
	public long getRequestsStatsFailed() {
		return requestsStatsFailed;
	}

	/**
	 * Returns the percentage of failed requests vs. processed + failed requests
	 * since the last statistics reset.
	 * 
	 * @return the request failure rate
	 */
	public float getRequestsStatsFailureRate() {
		return requestsStatsFailureRate;
	}

	/**
	 * Returns the high water mark since the last statistics reset.
	 * 
	 * @return the high water mark since the last statistics reset
	 */
	public long getRequestsStatsHigh() {
		return requestsStatsHigh;
	}

	/**
	 * Returns the request hit rate per hour since the last statistics reset.
	 * <p>
	 * Note, this operation makes an attempt to update the hit rate before it is
	 * returned.
	 * </p>
	 * 
	 * @return the request hit rate per hour since the last statistics reset
	 */
	public long getRequestsStatsHitRatePerHour() {
		// try to update the hit rate
		final Lock writeLock = getWriteLock();
		if (writeLock.tryLock()) {
			try {
				updateHitRate();
			} finally {
				writeLock.unlock();
			}
		}

		// return what we have
		return requestsStatsHitRatePerHour;
	}

	/**
	 * Returns the request hit rate per minute since the last statistics reset.
	 * <p>
	 * Note, this operation makes an attempt to update the hit rate before it is
	 * returned.
	 * </p>
	 * 
	 * @return the request hit rate per minute since the last statistics reset
	 */
	public long getRequestsStatsHitRatePerMinute() {
		// try to update the hit rate
		final Lock writeLock = getWriteLock();
		if (writeLock.tryLock()) {
			try {
				updateHitRate();
			} finally {
				writeLock.unlock();
			}
		}

		// return what we have
		return requestsStatsHitRatePerMinute;
	}

	/**
	 * Returns the request hit rate per second since the last statistics reset.
	 * <p>
	 * Note, this operation makes an attempt to update the hit rate before it is
	 * returned.
	 * </p>
	 * 
	 * @return the request hit rate per second since the last statistics reset
	 */
	public long getRequestsStatsHitRatePerSecond() {
		// try to update the hit rate
		final Lock writeLock = getWriteLock();
		if (writeLock.tryLock()) {
			try {
				updateHitRate();
			} finally {
				writeLock.unlock();
			}
		}

		// return what we have
		return requestsStatsHitRatePerSecond;
	}

	/**
	 * Returns the total number of requests processed (excluding failed
	 * requests) since the last statistics reset.
	 * 
	 * @return the total number of requests processed since the last statistics
	 *         reset
	 */
	public long getRequestsStatsProcessed() {
		return requestsStatsProcessed;
	}

	/**
	 * Returns the total number of time consumed processing requests since the
	 * last statistics reset.
	 * 
	 * @return the total number of time consumed processing requests since the
	 *         last statistics reset
	 */
	public long getRequestsStatsProcessingTime() {
		return requestsStatsProcessingTime.getValue();
	}

	/**
	 * Returns the average number of time consumed processing a request since
	 * the last statistics reset.
	 * 
	 * @return the average number of time consumed processing a request since
	 *         the last statistics reset
	 */
	public long getRequestsStatsProcessingTimeAverage() {
		return requestsStatsProcessingTime.getAverage();
	}

	/**
	 * Returns the highest number of time consumed processing a request since
	 * the last statistics reset.
	 * 
	 * @return the highest number of time consumed processing a request since
	 *         the last statistics reset
	 */
	public long getRequestsStatsProcessingTimeHigh() {
		return requestsStatsProcessingTime.getHigh();
	}

	/**
	 * Returns the lowest number of time consumed processing a request since the
	 * last statistics reset.
	 * 
	 * @return the lowest number of time consumed processing a request since the
	 *         last statistics reset
	 */
	public long getRequestsStatsProcessingTimeLow() {
		return requestsStatsProcessingTime.getLow();
	}

	/**
	 * Returns the standard deviation for the total number of time consumed
	 * processing requests since the last statistics reset.
	 * 
	 * @return the standard deviation for the total number of time consumed
	 *         processing requests since the last statistics reset
	 */
	public double getRequestsStatsProcessingTimeStandardDeviation() {
		return Math.sqrt(getRequestsStatsProcessingTimeVariance());
	}

	/**
	 * Returns the variance for the total number of time consumed processing
	 * requests since the last statistics reset.
	 * 
	 * @return the variance for the total number of time consumed processing
	 *         requests since the last statistics reset
	 */
	public double getRequestsStatsProcessingTimeVariance() {
		final Lock lock = getReadLock();
		lock.lock();
		try {
			return requestsStatsProcessingTime.getVariance();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns the total number of size units processed by requests since the
	 * last statistics reset.
	 * 
	 * @return the total number of size units processed by requests since the
	 *         last statistics reset
	 */
	public long getRequestsStatsSize() {
		return requestsStatsSize;
	}

	/**
	 * Returns the average number of size units processed by a request since the
	 * last statistics reset.
	 * 
	 * @return the average number of size units processed by a request since the
	 *         last statistics reset
	 */
	public long getRequestsStatsSizeAverage() {
		return requestsStatsSizeAverage;
	}

	/**
	 * Returns the metric time unit.
	 * 
	 * @return the metric time unit
	 */
	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	@Override
	void populateAttributes(final List<MetricAttribute> attributes) {
		super.populateAttributes(attributes);
		attributes.add(new MetricAttribute("requestsActive", "the number of active requests", Long.class));
		attributes.add(new MetricAttribute("requestsStatsHigh", "the high water mark since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsProcessed", " the total number of requests processed (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsHitRatePerHour", "the request hit rate per hour (including failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsHitRatePerMinute", "the request hit rate per minute (including failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsHitRatePerSecond", "the request hit rate per second (including failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsFailed", "the total number of failed requests since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsFailureRate", "the request failure rate since the last statistics reset (percentage of failed requests vs. processed + failed requests)", Float.class));
		attributes.add(new MetricAttribute("requestsStatsSize", "the total number of size units processed by requests (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsSizeAverage", "the average number of size units processed by a request (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsProcessingTime", "the total number of time consumed processing requests (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsProcessingTimeAverage", "the average number of time consumed processing a request (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsProcessingTimeHigh", "the highest number of time consumed processing a request (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsProcessingTimeLow", "the lowest number of time consumed processing a request (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("requestsStatsProcessingTimeStandardDeviation", "the standard deviation for the total number of time consumed processing requests (excluding failed requests) since the last statistics reset", Double.class));
		attributes.add(new MetricAttribute("timeUnit", "the time unit used in the metric", String.class));
	}

	@Override
	void populateAttributeValues(final Map<String, Object> values) {
		super.populateAttributeValues(values);
		values.put("requestsActive", getRequestsActive());
		values.put("requestsStatsHigh", getRequestsStatsHigh());
		values.put("requestsStatsProcessed", getRequestsStatsProcessed());
		values.put("requestsStatsHitRatePerHour", getRequestsStatsHitRatePerHour());
		values.put("requestsStatsHitRatePerMinute", getRequestsStatsHitRatePerMinute());
		values.put("requestsStatsHitRatePerSecond", getRequestsStatsHitRatePerSecond());
		values.put("requestsStatsFailed", getRequestsStatsFailed());
		values.put("requestsStatsFailureRate", getRequestsStatsFailureRate());
		values.put("requestsStatsSize", getRequestsStatsSize());
		values.put("requestsStatsSizeAverage", getRequestsStatsSizeAverage());
		values.put("requestsStatsProcessingTime", getRequestsStatsProcessingTime());
		values.put("requestsStatsProcessingTimeAverage", getRequestsStatsProcessingTimeAverage());
		values.put("requestsStatsProcessingTimeHigh", getRequestsStatsProcessingTimeHigh());
		values.put("requestsStatsProcessingTimeLow", getRequestsStatsProcessingTimeLow());
		values.put("requestsStatsProcessingTimeStandardDeviation", getRequestsStatsProcessingTimeStandardDeviation());
		values.put("timeUnit", getTimeUnit().toString());
	}

	/**
	 * Marks a request failed.
	 * <p>
	 * This will decrement the number of active requests and increment the
	 * number of failed requests. The processing time will not be affected due
	 * to failed requests.
	 * </p>
	 */
	public void requestFailed() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			requestsActive--;
			requestsStatsFailed++;
			updateHitRate();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Marks a request finished.
	 * <p>
	 * This will decrement the number of active requests, increment the number
	 * of processed requests, add the processing time and update the average
	 * processing time.
	 * </p>
	 * 
	 * @param sizeUnits
	 *            the size units processed by the request (eg. bytes, rows,
	 *            etc., use <code>0</code> if not tracked)
	 * @param processingTime
	 *            the time it took to process the request (use <code>0</code> if
	 *            not tracked)
	 */
	public void requestFinished(final long sizeUnits, final long processingTime) {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			requestsActive--;
			requestsStatsProcessed++;
			requestsStatsSize += sizeUnits;
			requestsStatsSizeAverage = requestsStatsSize / requestsStatsProcessed;
			requestsStatsProcessingTime.increment(processingTime);
			updateHitRate();
			updateFailureRate();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Marks a request start.
	 * <p>
	 * This will increment the number of active requests. Calles must invoke
	 * {@link #requestFinished(long, long)} or {@link #requestFailed()} when the
	 * request finished.
	 * </p>
	 * 
	 * @return request start time converted to {@link #getTimeUnit() the metric
	 *         time unit} for convenience (based on {@link System#nanoTime()} if
	 *         the precision is microseconds or nanoseconds, otherwise
	 *         {@link System#currentTimeMillis()})
	 */
	public long requestStarted() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			requestsActive++;
			if (requestsActive > requestsStatsHigh) {
				requestsStatsHigh = requestsActive;
			}
			updateHitRate();
			updateFailureRate();
		} finally {
			writeLock.unlock();
		}

		switch (timeUnit) {
			case NANOSECONDS:
				return System.nanoTime();
			case MICROSECONDS:
				return TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
			case MILLISECONDS:
				return System.currentTimeMillis();
			default:
				return getTimeUnit().convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}
	}

	private void updateFailureRate() {
		// the failure rate is percentage of failed vs. total requests
		final long total = requestsStatsFailed + requestsStatsProcessed;
		requestsStatsFailureRate = total == 0 ? 0 : ((requestsStatsFailed / total) * 100);
	}

	private void updateHitRate() {
		final long totalRequestProcessed = requestsStatsProcessed + requestsStatsFailed;
		final long duration = System.currentTimeMillis() - getStatsSinceTS();
		final long durationSeconds = duration / 1000;
		final long durationMinutes = duration / 60000;
		final long durationHours = duration / 3600000;
		requestsStatsHitRatePerSecond = ((durationSeconds == 0) || (totalRequestProcessed == 0)) ? 0 : (totalRequestProcessed / durationSeconds);
		requestsStatsHitRatePerMinute = ((durationMinutes == 0) || (totalRequestProcessed == 0)) ? 0 : (totalRequestProcessed / durationMinutes);
		requestsStatsHitRatePerHour = ((durationHours == 0) || (totalRequestProcessed == 0)) ? 0 : (totalRequestProcessed / durationHours);
	}
}
