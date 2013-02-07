/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.spi.storage;

import java.util.Map;

import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;

import org.eclipse.core.runtime.IStatus;

/**
 * Storable counterpart to {@link IJobHistoryEntry}.
 * <p>
 * This class may be subclassed or instantiated by history storage providers. As
 * such it is considered part of a service provider API which may evolve faster
 * than the general API. Please get in touch with the development team through
 * the prefered channels listed on <a href="http://www.eclipse.org/gyrex">the
 * Gyrex website</a> to stay up-to-date of possible changes.
 * </p>
 */
public class JobHistoryEntryStorable implements Comparable<JobHistoryEntryStorable> {

	private IStatus result;
	private long timestamp;
	private String queuedTrigger;
	private String cancelledTrigger;
	private Map<String, String> parameter;

	@Override
	public int compareTo(final JobHistoryEntryStorable o) {

		final long t1 = getTimestamp();
		final long t2 = o.getTimestamp();
		if (t2 > t1)
			// other timestamp is greater --> t1 is older and greater
			return 1;
		if (t2 < t1)
			// other timestamp is less --> t1 is newer and less
			return -1;

		// compare result message if severity is the same
		if (o.getResult().getSeverity() == getResult().getSeverity())
			return getResult().getMessage().compareTo(o.getResult().getMessage());
		else
			// severity is different
			// a higher severity also makes this item less so that it appears earlier then others
			return getResult().getSeverity() > o.getResult().getSeverity() ? -1 : 1;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final JobHistoryEntryStorable other = (JobHistoryEntryStorable) obj;
		if (timestamp != other.timestamp)
			return false;
		if (result == null)
			return other.result == null;
		if (other.result == null)
			return result == null;
		if (!result.getMessage().equals(other.result.getMessage()))
			return false;
		if (result.getSeverity() != other.result.getSeverity())
			return false;
		return true;
	}

	/**
	 * Returns the cancelledTrigger.
	 * 
	 * @return the cancelledTrigger
	 * @see IJobHistoryEntry#getCancelledTrigger()
	 */
	public String getCancelledTrigger() {
		return cancelledTrigger;
	}

	/**
	 * Returns the parameter.
	 * 
	 * @return the parameter
	 * @see IJobHistoryEntry#getParameter()
	 */
	public Map<String, String> getParameter() {
		return parameter;
	}

	/**
	 * Returns the queuedTrigger.
	 * 
	 * @return the queuedTrigger
	 * @see IJobHistoryEntry#getQueuedTrigger()
	 */
	public String getQueuedTrigger() {
		return queuedTrigger;
	}

	/**
	 * Returns the result.
	 * 
	 * @return the result
	 * @see IJobHistoryEntry#getResult()
	 */
	public IStatus getResult() {
		return result;
	}

	/**
	 * Returns the timestamp.
	 * 
	 * @return the timestamp
	 * @see IJobHistoryEntry#getTimeStamp()
	 */
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if (this.result != null) {
			result = (prime * result) + this.result.getMessage().hashCode();
			result = (prime * result) + this.result.getSeverity();
		}
		result = (prime * result) + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	/**
	 * Sets the cancelledTrigger.
	 * 
	 * @param cancelledTrigger
	 *            the cancelledTrigger to set
	 */
	public void setCancelledTrigger(final String cancelledTrigger) {
		this.cancelledTrigger = cancelledTrigger;
	}

	/**
	 * Sets the parameter.
	 * 
	 * @param parameter
	 *            the parameter to set
	 */
	public void setParameter(final Map<String, String> parameter) {
		this.parameter = parameter;
	}

	/**
	 * Sets the queuedTrigger.
	 * 
	 * @param queuedTrigger
	 *            the queuedTrigger to set
	 */
	public void setQueuedTrigger(final String queuedTrigger) {
		this.queuedTrigger = queuedTrigger;
	}

	/**
	 * Sets the result.
	 * 
	 * @param result
	 *            the result to set
	 */
	public void setResult(final IStatus result) {
		this.result = result;
	}

	/**
	 * Sets the timestamp.
	 * 
	 * @param timestamp
	 *            the timestamp to set
	 */
	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

}
