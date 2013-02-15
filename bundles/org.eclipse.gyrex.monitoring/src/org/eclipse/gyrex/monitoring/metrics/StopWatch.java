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

import java.util.concurrent.TimeUnit;

/**
 * A stop watch may be used to track measured times (eg., durations).
 * <p>
 * Note, this class is not thread safe. Concurrent access to any of the methods
 * in this class must be coordinated by the caller.
 * </p>
 * <p>
 * The results of this stop watch are undefined if {@link #start()} and
 * {@link #stop()} are called out-of-order or multiple times.
 * </p>
 */
public class StopWatch {

	public static interface StopCallback {
		void stopped(StopWatch stopWatch);
	}

	private final StopCallback stopCallback;
	private volatile long startTimeNanos;
	private volatile long endTimeNanos;

	/**
	 * Creates a new stop watch.
	 */
	public StopWatch() {
		this(null);
	}

	/**
	 * Creates a new stop watch.
	 * 
	 * @param callback
	 *            an optional stop callback that may be triggered when
	 *            {@link #stop()} is called (maybe <code>null</code>)
	 */
	public StopWatch(final StopCallback stopCallback) {
		this.stopCallback = stopCallback;
	}

	/**
	 * Returns the measured duration in nanoseconds.
	 * 
	 * @return the measured duration in nanoseconds
	 */
	private long getDuration() {
		return endTimeNanos - startTimeNanos;
	}

	/**
	 * @param timeUnit
	 * @return
	 */
	public long getDuration(final TimeUnit timeUnit) {
		return timeUnit.convert(getDuration(), TimeUnit.NANOSECONDS);
	}

	/**
	 * Sets the start time to the current value of {@link System#nanoTime()}.
	 */
	public void start() {
		startTimeNanos = System.nanoTime();
	}

	/**
	 * Sets the end time to the current value of {@link System#nanoTime()}.
	 * <p>
	 * If this stop watch is associated with a {@link TimerMetric} the
	 * {@link #getDuration()} will be automatically recorded with the metric. No
	 * further action is required on the metric.
	 * </p>
	 */
	public void stop() {
		endTimeNanos = System.nanoTime();
		if (stopCallback != null) {
			stopCallback.stopped(this);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		if (endTimeNanos > startTimeNanos) {
			builder.append(getDuration());
			builder.append("ns");
		} else if (startTimeNanos > 0) {
			builder.append("started at ");
			builder.append(startTimeNanos);
		} else {
			builder.append("0");
		}
		return builder.toString();
	}

}
