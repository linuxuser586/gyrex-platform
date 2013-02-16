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
package org.eclipse.gyrex.monitoring.metrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.eclipse.gyrex.monitoring.metrics.StopWatch.StopCallback;

/**
 * A metric for monitoring durations (eg. request execution times).
 * <p>
 * The timer metric allows to measure the duration of "requests" processed by a
 * system. A request can be anything from a single method invocation, a single
 * web request till a large process involving many subsequent operations.
 * </p>
 * <p>
 * Note, although this class is not marked <strong>final</strong> it is not
 * allowed to be subclassed outside the monitoring framework.
 * </p>
 * 
 * @since 1.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public class TimerMetric extends BaseMetric {

	private final TimeUnit timeUnit;
	private final Counter duration = new Counter();
	private final StopCallback stopCallback = new StopCallback() {

		@Override
		public void stopped(final StopWatch stopWatch) {
			processFinished(stopWatch.getDuration(getTimeUnit()));
		}
	};

	/**
	 * Creates a new timer metric instance using time unit
	 * {@link TimeUnit#MILLISECONDS}.
	 * 
	 * @param id
	 *            the metric id
	 */
	public TimerMetric(final String id) {
		this(id, TimeUnit.MILLISECONDS);
	}

	/**
	 * Creates a new timer metric instance.
	 * 
	 * @param id
	 *            the metric id
	 * @param timeUnit
	 *            the time unit used in the metric
	 */
	public TimerMetric(final String id, final TimeUnit timeUnit) {
		super(id);
		if (timeUnit == null)
			throw new IllegalArgumentException("no time unit specified");
		this.timeUnit = timeUnit;
	}

	@Override
	void doResetStats() {
		duration.reset();
	}

	@Override
	Object[] dumpMetrics() {
		return new Object[] { "count|time|time average|time high|time low|time stddev", getProcessingCount(), getProcessingTime(), getProcessingTimeAverage(), getProcessingTimeHigh(), getProcessingTimeLow(), getProcessingTimeStandardDeviation() };
	}

	/**
	 * Returns the number of finished processes, i.e. how often
	 * {@link #processFinished(long)} has been called.
	 * 
	 * @return the number of finished processes
	 */
	public long getProcessingCount() {
		return duration.getNumberOfSamples();
	}

	/**
	 * Returns the total number of time consumed processing requests since the
	 * last statistics reset.
	 * 
	 * @return the total number of time consumed processing requests since the
	 *         last statistics reset
	 */
	public long getProcessingTime() {
		return duration.getValue();
	}

	/**
	 * Returns the average number of time consumed processing a request since
	 * the last statistics reset.
	 * 
	 * @return the average number of time consumed processing a request since
	 *         the last statistics reset
	 */
	public long getProcessingTimeAverage() {
		return duration.getAverage();
	}

	/**
	 * Returns the highest number of time consumed processing a request since
	 * the last statistics reset.
	 * 
	 * @return the highest number of time consumed processing a request since
	 *         the last statistics reset
	 */
	public long getProcessingTimeHigh() {
		return duration.getHigh();
	}

	/**
	 * Returns the lowest number of time consumed processing a request since the
	 * last statistics reset.
	 * 
	 * @return the lowest number of time consumed processing a request since the
	 *         last statistics reset
	 */
	public long getProcessingTimeLow() {
		return duration.getLow();
	}

	/**
	 * Returns the standard deviation for the total number of time consumed
	 * processing requests since the last statistics reset.
	 * 
	 * @return the standard deviation for the total number of time consumed
	 *         processing requests since the last statistics reset
	 */
	public double getProcessingTimeStandardDeviation() {
		return Math.sqrt(getProcessingTimeVariance());
	}

	/**
	 * Returns the variance for the total number of time consumed processing
	 * requests since the last statistics reset.
	 * 
	 * @return the variance for the total number of time consumed processing
	 *         requests since the last statistics reset
	 */
	public double getProcessingTimeVariance() {
		final Lock lock = getReadLock();
		lock.lock();
		try {
			return duration.getVariance();
		} finally {
			lock.unlock();
		}
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
		attributes.add(new MetricAttribute("processingCount", "the number of finished processed since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("processingTime", "the total number of time consumed processing requests (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("processingTime", "the total number of time consumed processing requests (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("processingTimeAverage", "the average number of time consumed processing a request (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("processingTimeHigh", "the highest number of time consumed processing a request (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("processingTimeLow", "the lowest number of time consumed processing a request (excluding failed requests) since the last statistics reset", Long.class));
		attributes.add(new MetricAttribute("processingTimeStandardDeviation", "the standard deviation for the total number of time consumed processing requests (excluding failed requests) since the last statistics reset", Double.class));
		attributes.add(new MetricAttribute("timeUnit", "the time unit used in the metric", String.class));
	}

	@Override
	void populateAttributeValues(final Map<String, Object> values) {
		super.populateAttributeValues(values);
		values.put("processingCount", getProcessingCount());
		values.put("processingTime", getProcessingTime());
		values.put("processingTimeAverage", getProcessingTimeAverage());
		values.put("processingTimeHigh", getProcessingTimeHigh());
		values.put("processingTimeLow", getProcessingTimeLow());
		values.put("processingTimeStandardDeviation", getProcessingTimeStandardDeviation());
		values.put("timeUnit", getTimeUnit().toString());
	}

	/**
	 * Records a duration time.
	 * <p>
	 * This will increment the number of processed requests, add the duration
	 * time and update the average processing time.
	 * </p>
	 * 
	 * @param processingTime
	 *            the time it took to process the request
	 */
	public void processFinished(final long processingTime) {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			duration.increment(processingTime);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Marks a process start.
	 * <p>
	 * This will instantiate and start a new {@link StopWatch}. Callers must
	 * invoke {@link StopWatch#stop()} when the process finished. The returned
	 * {@link StopWatch} is initialized with a callback that will call
	 * {@link #processFinished(long)} when {@link StopWatch#stop()} is invoked.
	 * </p>
	 * 
	 * @return a {@link StopWatch} for marking the process end
	 */
	public StopWatch processStarted() {
		final StopWatch watch = new StopWatch(stopCallback);
		watch.start();
		return watch;
	}
}
