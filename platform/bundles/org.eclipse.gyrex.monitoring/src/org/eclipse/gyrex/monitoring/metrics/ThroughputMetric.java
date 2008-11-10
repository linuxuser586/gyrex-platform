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
	 * the request hit rate per minute (including failed requests) since the
	 * last statistics reset
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
	private volatile long requestsStatsProcessingTime;

	/**
	 * the average number of time consumed processing a request (excluding
	 * failed requests) since the last statistics reset
	 */
	private volatile long requestsStatsProcessingTimeAverage;

	/**
	 * Creates a new throughput metric instance.
	 * 
	 * @param id
	 *            the metric id
	 */
	public ThroughputMetric(final String id) {
		super(id);
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
	 * @see org.eclipse.cloudfree.monitoring.metrics.BaseMetric#doResetStats()
	 */
	@Override
	protected void doResetStats() {
		requestsStatsHigh = 0;
		requestsStatsProcessed = 0;
		requestsStatsHitRatePerMinute = 0;
		requestsStatsFailed = 0;
		requestsStatsHigh = 0;
		requestsStatsSize = 0;
		requestsStatsSizeAverage = 0;
		requestsStatsProcessingTime = 0;
		requestsStatsProcessingTimeAverage = 0;
	}

	@Override
	protected Object[] dumpMetrics() {
		return new Object[] { "active|high|processed|rate|size|size average|time|time average", getRequestsActive(), getRequestsStatsHigh(), getRequestsStatsProcessed(), getRequestsStatsHitRatePerMinute(), getRequestsStatsSize(), getRequestsStatsSizeAverage(), getRequestsStatsProcessingTime(), getRequestsStatsProcessingTimeAverage() };
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
		return requestsStatsProcessingTime;
	}

	/**
	 * Returns the average number of time consumed processing a request since
	 * the last statistics reset.
	 * 
	 * @return the average number of time consumed processing a request since
	 *         the last statistics reset
	 */
	public long getRequestsStatsProcessingTimeAverage() {
		return requestsStatsProcessingTimeAverage;
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
			requestsStatsProcessingTime += processingTime;
			requestsStatsProcessingTimeAverage = requestsStatsProcessingTime / requestsStatsProcessed;
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
	 * @return <code>System.currentTimeMillis()</code> for convenience
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
		return System.currentTimeMillis();
	}

	private void updateFailureRate() {
		// the failure rate is percentage of failed vs. total requests
		final long total = requestsStatsFailed + requestsStatsProcessed;
		requestsStatsFailureRate = total == 0 ? 0 : ((requestsStatsFailed / total) * 100);
	}

	private void updateHitRate() {
		final long totalRequestProcessed = requestsStatsProcessed + requestsStatsFailed;
		final long durationSeconds = getStatsSinceTS() / 1000;
		final long durationMinutes = getStatsSinceTS() / 60000;
		final long durationHours = getStatsSinceTS() / 3600000;
		requestsStatsHitRatePerSecond = ((durationSeconds == 0) || (totalRequestProcessed == 0)) ? 0 : (totalRequestProcessed / durationSeconds);
		requestsStatsHitRatePerMinute = ((durationMinutes == 0) || (totalRequestProcessed == 0)) ? 0 : (totalRequestProcessed / durationMinutes);
		requestsStatsHitRatePerHour = ((durationHours == 0) || (totalRequestProcessed == 0)) ? 0 : (totalRequestProcessed / durationHours);
	}
}
