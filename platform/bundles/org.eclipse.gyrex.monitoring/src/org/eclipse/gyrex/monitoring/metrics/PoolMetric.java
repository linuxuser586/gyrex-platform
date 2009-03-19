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

import java.util.concurrent.locks.Lock;

/**
 * An special capacity metric for monitoring pools.
 * <p>
 * In addition to the capacity metric, the pool metric allows to track idle
 * "channels" as well as statistics about the resources created, released or
 * destroyed.
 * </p>
 * <p>
 * Note, although this class is not marked <strong>final</strong> it is not
 * allowed to be subclassed outside the monitoring framework.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class PoolMetric extends CapacityMetric {

	/** the number of idle channels */
	private volatile long channelsIdle;

	/** the minimum number of channels available */
	private volatile long channelsMinimum;

	/** the total number of resources released since the last statistics reset */
	private volatile long resourcesStatsReleased;

	/**
	 * the total number of resources destroyed due to failures since the last
	 * statistics reset
	 */
	private volatile long resourcesStatsDestroyed;

	/** the total number of resources created since the last statistics reset */
	private volatile long resourcesStatsCreated;

	/**
	 * Creates a new pool metric instance.
	 * 
	 * @param id
	 *            the metric id
	 * @param initialChannelsCapacity
	 *            the initial capacity
	 * @param initialChannelsMinimum
	 *            the initial minimum
	 */
	public PoolMetric(final String id, final long initialChannelsCapacity, final long initialChannelsMinimum) {
		super(id, initialChannelsCapacity);
		channelsMinimum = initialChannelsMinimum;
	}

	/**
	 * Marks a channel busy.
	 * <p>
	 * This will decrement the number of idle channels.
	 * </p>
	 */
	public void channelBusy() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			channelsIdle--;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Marks a channel idle.
	 * <p>
	 * This will increment the number of idle channels.
	 * </p>
	 */
	public void channelIdle() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			channelsIdle++;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Resets the pool metrics.
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
	 * @see org.eclipse.gyrex.monitoring.metrics.CapacityMetric#doResetStats()
	 */
	@Override
	protected void doResetStats() {
		resourcesStatsCreated = 0;
		resourcesStatsReleased = 0;
		resourcesStatsDestroyed = 0;
		// call super
		super.doResetStats();
	}

	@Override
	protected Object[] dumpMetrics() {
		return new Object[] { "used|idle|capacity|min|high|requests|denied|wait|average wait|resources created|resources released|resources destroyed", getChannelsUsed(), getChannelsIdle(), getChannelsCapacity(), getChannelsMinimum(), getChannelsStatsHigh(), getChannelsStatsRequests(), getChannelsStatsDenied(), getChannelsStatsWaitTime(), getChannelsStatsWaitTimeAverage(),
				getResourcesStatsCreated(), getResourcesStatsReleased(), getResourcesStatsDestroyed() };
	}

	/**
	 * Returns the number of idle channels.
	 * 
	 * @return the number of idle channels
	 */
	public long getChannelsIdle() {
		return channelsIdle;
	}

	/**
	 * Returns the minimum number of channels available.
	 * 
	 * @return the minimum number of channels available
	 */
	public long getChannelsMinimum() {
		return channelsMinimum;
	}

	/**
	 * Returns the total number of resources created since the last statistics
	 * reset.
	 * 
	 * @return the total number of resources created since the last statistics
	 *         reset
	 */
	public long getResourcesStatsCreated() {
		return resourcesStatsCreated;
	}

	/**
	 * Returns the total number of resources destroyed due to failures since the
	 * last statistics reset.
	 * 
	 * @return the total number of resources destroyed due to failures since the
	 *         last statistics reset
	 */
	public long getResourcesStatsDestroyed() {
		return resourcesStatsDestroyed;
	}

	/**
	 * Returns the total number of (idle) resources released since the last
	 * statistics reset.
	 * 
	 * @return the total number of (idle) resources released since the last
	 *         statistics reset
	 */
	public long getResourcesStatsReleased() {
		return resourcesStatsReleased;
	}

	/**
	 * Records a created resource.
	 * <p>
	 * This will increment the total number of resources created since the last
	 * statistics reset.
	 * </p>
	 */
	public void resourceCreated() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			resourcesStatsCreated++;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Records a problematic resource destroyed.
	 * <p>
	 * This will increment the total number of resources destroyed due to
	 * failures.
	 * </p>
	 */
	public void resourceDestroyed() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			resourcesStatsDestroyed++;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Records a(n) (idle) resource released.
	 * <p>
	 * This will increment the total number of (idle) resources released.
	 * </p>
	 */
	public void resourceReleased() {
		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			resourcesStatsReleased++;
		} finally {
			writeLock.unlock();
		}
	}
}
