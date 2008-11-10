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
 * A metric to monitor limited resources (channels, parallel connections,
 * threads).
 * <p>
 * The capacity metric allows to track "channels" in use and collects statistics
 * about the channel usage.
 * </p>
 * <p>
 * Note, although this class is not marked <strong>final</strong> it is not
 * allowed to be subclassed outside the monitoring framework.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CapacityMetric extends BaseMetric {

	/** the number of channels in use */
	private volatile long channelsUsed;

	/** the total number of channels available */
	private volatile long channelsCapacity;

	/** the high water mark since the last statistics reset */
	private volatile long channelsStatsHigh;

	/** the total number of processed requests since the last statistics reset */
	private volatile long channelsStatsRequests;

	/** the total number of requests denied since the last statistics reset */
	private volatile long channelsStatsDenied;

	/** the total time in milliseconds requests had to wait for a channel */
	private volatile long channelsStatsWaitTime;

	/** the average time in milliseconds a request had to wait for a channel */
	private volatile long channelsStatsWaitTimeAverage;

	/**
	 * Creates a new capacity metric instance.
	 * 
	 * @param id
	 *            the metric id
	 */
	public CapacityMetric(final String id, final long initialChannelsCapacity) {
		super(id);
		channelsCapacity = initialChannelsCapacity;
	}

	/**
	 * Increments the number of channels denied.
	 */
	public void channelDenied() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			channelsStatsDenied++;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Marks a channel usage end.
	 * <p>
	 * This will decrement the number of channels in use.
	 * </p>
	 * 
	 * @param waitTime
	 *            the time the request had to wait
	 */
	public void channelFinished() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			channelsUsed--;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Marks a channel usage start.
	 * <p>
	 * This will increment the number of channels in use as well as the number
	 * of requests, the wait time and update the average wait time.
	 * </p>
	 * 
	 * @param waitTime
	 *            the time the request had to wait (use <code>0</code> if not
	 *            tracked)
	 */
	public void channelStarted(final long waitTime) {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			channelsUsed++;
			if (channelsUsed > channelsStatsHigh) {
				channelsStatsHigh = channelsUsed;
			}
			channelsStatsRequests++;
			channelsStatsWaitTime += waitTime;
			channelsStatsWaitTimeAverage = channelsStatsWaitTime / channelsStatsRequests;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Resets the capacity metrics.
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
		channelsStatsHigh = 0;
		channelsStatsRequests = 0;
		channelsStatsDenied = 0;
		channelsStatsWaitTime = 0;
		channelsStatsWaitTimeAverage = 0;
	}

	@Override
	protected Object[] dumpMetrics() {
		return new Object[] { "used|capacity|high|requests|denied|wait|average wait", getChannelsUsed(), getChannelsCapacity(), getChannelsStatsHigh(), getChannelsStatsRequests(), getChannelsStatsDenied(), getChannelsStatsWaitTime(), getChannelsStatsWaitTimeAverage() };
	}

	/**
	 * Returns the total number of channels available.
	 * 
	 * @return the total number of channels available
	 */
	public long getChannelsCapacity() {
		return channelsCapacity;
	}

	/**
	 * Returns the total number of requests denied since the last statistics
	 * reset.
	 * 
	 * @return the total number of requests denied since the last statistics
	 *         reset
	 */
	public long getChannelsStatsDenied() {
		return channelsStatsDenied;
	}

	/**
	 * Returns the high water mark since the last statistics reset.
	 * 
	 * @return the high water mark since the last statistics reset
	 */
	public long getChannelsStatsHigh() {
		return channelsStatsHigh;
	}

	/**
	 * Returns the total number of processed requests since the last statistics
	 * reset.
	 * 
	 * @return the total number of processed requests since the last statistics
	 *         reset
	 */
	public long getChannelsStatsRequests() {
		return channelsStatsRequests;
	}

	/**
	 * Returns the total time in milliseconds requests had to wait for a
	 * channel.
	 * 
	 * @return the total time in milliseconds requests had to wait for a channel
	 */
	public long getChannelsStatsWaitTime() {
		return channelsStatsWaitTime;
	}

	/**
	 * Returns the average time in milliseconds a request had to wait for a
	 * channel.
	 * 
	 * @return the average time in milliseconds a request had to wait for a
	 *         channel
	 */
	public long getChannelsStatsWaitTimeAverage() {
		return channelsStatsWaitTimeAverage;
	}

	/**
	 * Returns the number of channels in use.
	 * 
	 * @return the number of channels in use
	 */
	public long getChannelsUsed() {
		return channelsUsed;
	}

	/**
	 * Sets the total number of channels available.
	 * 
	 * @param channelsCapacity
	 *            the total number of channels available
	 */
	public void setChannelsCapacity(final long channelsCapacity) {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			this.channelsCapacity = channelsCapacity;
		} finally {
			writeLock.unlock();
		}
	}
}
