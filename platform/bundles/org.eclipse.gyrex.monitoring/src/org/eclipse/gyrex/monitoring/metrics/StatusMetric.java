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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * A metric for monitoring a status (eg. "up" or "down").
 * <p>
 * A status metric is typically used to monitor the status of a component. In
 * addition to the plain status it provides attributes which help administrator
 * to understand the metric better.
 * </p>
 * <p>
 * Note, although this class is not marked <strong>final</strong> it is not
 * allowed to be subclassed outside the monitoring framework.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class StatusMetric extends BaseMetric {

	/** the status */
	private volatile String status;

	/** the reason for a status change */
	private volatile String statusChangeReason;

	/** the last status change time */
	private volatile long statusChangeTime;

	/** a counter for status changes since the last reset */
	private volatile long statusStatsChangeCount;

	/**
	 * Creates a new status metric instance.
	 * 
	 * @param id
	 *            the metric id
	 * @param initialStatus
	 *            the initial status to set (may not be <code>null</code>)
	 * @param initialStatusReason
	 *            the initial status change reason to set (may not be
	 *            <code>null</code>)
	 */
	public StatusMetric(final String id, final String initialStatus, final String initialStatusReason) {
		super(id);
		if (null == initialStatus) {
			throw new IllegalArgumentException("initialStatus may not be null");
		}
		if (null == initialStatusReason) {
			throw new IllegalArgumentException("initialStatusReason change reason may not be null");
		}

		// note, we do not invoke setStatus here because calling non-private
		// methods during object initialization is problematic at best
		status = initialStatus;
		statusChangeReason = initialStatusReason;
	}

	/**
	 * Resets the status metric.
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
		statusStatsChangeCount = 0;
	}

	@Override
	Object[] dumpMetrics() {
		return new Object[] { "status|reason|since|total changes", getStatus(), getStatusChangeReason(), getStatusChangeTime(), getStatusStatsChangeCount() };
	}

	/**
	 * Returns the status.
	 * 
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Returns the reason for a status change.
	 * 
	 * @return the reason for a status change
	 */
	public String getStatusChangeReason() {
		return statusChangeReason;
	}

	/**
	 * Returns the time of the last status change.
	 * <p>
	 * Note the date string is an <a
	 * href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601 UTC</a> string of
	 * the form <code>[YYYY][MM][DD]T[hh][mm][ss]Z</code>.
	 * </p>
	 * 
	 * @return the time of the last status change
	 */
	public String getStatusChangeTime() {
		return ISO_8601_UTC.format(new Date(statusChangeTime));
	}

	/**
	 * Returns the number of status changes since the last {@link #resetStats()
	 * reset}.
	 * <p>
	 * Note, the status change count is not overflow safe.
	 * </p>
	 * 
	 * @return the the number of status changes since the last statistics reset
	 */
	public long getStatusStatsChangeCount() {
		return statusStatsChangeCount;
	}

	@Override
	void populateAttributes(final List<MetricAttribute> attributes) {
		super.populateAttributes(attributes);
		attributes.add(new MetricAttribute("status", "the status", String.class));
		attributes.add(new MetricAttribute("statusChangeReason", "the reason for the status change", String.class));
		attributes.add(new MetricAttribute("statusChangeTime", "the last status change time", String.class));
		attributes.add(new MetricAttribute("statusStatsChangeCount", "a counter for status changes since the last reset", Long.class));
	}

	@Override
	void populateAttributeValues(final Map<String, Object> values) {
		super.populateAttributeValues(values);
		values.put("status", getStatus());
		values.put("statusChangeReason", getStatusChangeReason());
		values.put("statusChangeTime", getStatusChangeTime());
		values.put("statusStatsChangeCount", getStatusStatsChangeCount());
	}

	/**
	 * Sets a new status.
	 * <p>
	 * Note, it is considered best practice to provide a reason for the status
	 * change. System operators will appreciate it.
	 * </p>
	 * 
	 * @param status
	 *            the status (may not be <code>null</code>)
	 * @param statusChangeReason
	 *            the status change reason (may not be <code>null</code>)
	 */
	public void setStatus(final String status, final String statusChangeReason) {
		if (null == status) {
			throw new IllegalArgumentException("status may not be null");
		}
		if (null == statusChangeReason) {
			throw new IllegalArgumentException("status change reason may not be null");
		}

		final Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			this.status = status;
			this.statusChangeReason = statusChangeReason;
			statusChangeTime = System.currentTimeMillis();
			statusStatsChangeCount++;
		} finally {
			writeLock.unlock();
		}
	}
}
