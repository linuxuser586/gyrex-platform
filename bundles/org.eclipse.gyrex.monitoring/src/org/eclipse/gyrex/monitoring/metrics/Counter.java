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

/**
 * A counter that may be used to track events that increment a value (eg.,
 * durations, invocations).
 * <p>
 * Note, this class is not thread safe. Concurrent access to any of the methods
 * in this class must be coordinated by the caller.
 * </p>
 */
public final class Counter {

	/** total value of all submitted amounts */
	private volatile long value;

	/** number of submitted amounts */
	private volatile long numberOfSamples;

	/** smallest submitted amount */
	private volatile long low;

	/** highest submitted amount */
	private volatile long high;

	/** base value for calculating variance and standard deviation of amounts */
	private volatile long variance100;

	private double calculatePercentile(final double z) {
		return getAverage() + (z * getStandardDeviation());
	}

	/**
	 * Returns the average of all submitted amounts.
	 * 
	 * @return
	 */
	public long getAverage() {
		return value / numberOfSamples;
	}

	/**
	 * Returns the highest submitted amount since the last reset.
	 * 
	 * @return the highest submitted amount
	 */
	public long getHigh() {
		return high;
	}

	/**
	 * Returns the lowest submitted amount since the last reset.
	 * 
	 * @return the lowest submitted amount
	 */
	public long getLow() {
		return low;
	}

	/**
	 * Returns the number of submitted samples, i.e. how often
	 * {@link #increment(long)} has been invoked since the last reset.
	 * 
	 * @return the number of submitted samples
	 */
	public long getNumberOfSamples() {
		return numberOfSamples;
	}

	public double getPercentile95() {
		return calculatePercentile(1.65D);
	}

	public double getPercentile99() {
		return calculatePercentile(2.33D);
	}

	/**
	 * Returns the standard deviation for the total value of all submitted
	 * amounts.
	 * 
	 * @return the standard deviation for the total value of all submitted
	 *         amounts
	 */
	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	/**
	 * Returns the total value of all submitted amounts since the last reset.
	 * 
	 * @return the total value of all submitted amounts
	 */
	public long getValue() {
		return value;
	}

	/**
	 * Returns the variance for the total value of all submitted amounts.
	 * 
	 * @return the variance for the total value of all submitted amounts
	 */
	public double getVariance() {
		if (numberOfSamples > 1)
			return (variance100) / 100.0 / (numberOfSamples - 1);
		return 0.0D;
	}

	/**
	 * Increments the counter by the specified amount.
	 * <p>
	 * This increment the number of processed requests, add the processing time
	 * and update the average processing time.
	 * </p>
	 * 
	 * @param amount
	 *            the increment amount
	 */
	public void increment(final long amount) {
		numberOfSamples++;
		value += amount;
		high = Math.max(amount, high);
		low = Math.min(amount, low);
		if (value > 1) {
			high = Math.max(amount, high);
			low = Math.min(amount, low);
			final long delta10 = (amount * 10) - ((value * 10) / numberOfSamples);
			variance100 += (delta10 * delta10);
		} else {
			high = amount;
			low = amount;
			variance100 = 0;
		}
	}

	/**
	 * Resets the counter
	 */
	public void reset() {
		value = 0;
		numberOfSamples = 0;
		low = 0;
		high = 0;
		variance100 = 0;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(value);
		builder.append(" (");
		builder.append(low);
		builder.append("/");
		builder.append(high);
		builder.append(")");
		return builder.toString();
	}
}
