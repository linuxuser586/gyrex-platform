/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Mike Tschierschke - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.manager;

import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;
import org.eclipse.gyrex.jobs.internal.JobsActivator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * Simple implementation of of a {@link IJobHistoryEntry}
 */
final class JobHistoryItemImpl implements IJobHistoryEntry {

	private final IStatus result;
	private final String trigger;
	private final long timestamp;

	JobHistoryItemImpl(final long timestamp, final String message, final String trigger, final int severity) {
		this.timestamp = timestamp;
		// TODO: we may need to support full serialization (eg., MultiStatus and original plug-in id, exception may not work)
		result = new Status(severity, JobsActivator.SYMBOLIC_NAME, StringUtils.trimToEmpty(message));
		this.trigger = trigger;
	}

	@Override
	public int compareTo(final IJobHistoryEntry o) {
		final long otherTimeStamp = o.getTimeStamp();
		if (otherTimeStamp > timestamp) {
			// other timestamp is greater --> we are are older and greater
			return 1;
		}
		if (otherTimeStamp < timestamp) {
			// other timestamp is less --> we are newer and less
			return -1;
		}
		// timestamp are equals

		// compare result message if severity is the same
		if (o.getResult().getSeverity() == result.getSeverity()) {
			return result.getMessage().compareTo(o.getResult().getMessage());
		} else {
			// severity is different
			// a higher severity also makes this item less so that it appears earlier then others
			return result.getSeverity() > o.getResult().getSeverity() ? -1 : 1;
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final JobHistoryItemImpl other = (JobHistoryItemImpl) obj;
		if (timestamp != other.timestamp) {
			return false;
		}
		if (!result.getMessage().equals(other.result.getMessage())) {
			// result is never null
			return false;
		}
		if (result.getSeverity() != other.result.getSeverity()) {
			return false;
		}
		return true;
	}

	@Override
	public IStatus getResult() {
		return result;
	}

	@Override
	public long getTimeStamp() {
		return timestamp;
	}

	@Override
	public String getTrigger() {
		return trigger;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.result.hashCode(); // result is never null
		result = (prime * result) + this.result.getSeverity();
		result = (prime * result) + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("JobHistoryItemImpl [");
		builder.append(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(timestamp)).append(", ");
		switch (result.getSeverity()) {
			case IStatus.OK:
				builder.append("OK");
				break;
			case IStatus.ERROR:
				builder.append("ERROR");
				break;
			case IStatus.WARNING:
				builder.append("WARNING");
				break;
			case IStatus.INFO:
				builder.append("INFO");
				break;
			case IStatus.CANCEL:
				builder.append("CANCEL");
				break;

			default:
				builder.append("severity=");
				builder.append(result.getSeverity());
				break;
		}
		if (StringUtils.isNotBlank(result.getMessage())) {
			builder.append(", ").append(result.getMessage());
		}
		builder.append("]");
		return builder.toString();
	}
}
