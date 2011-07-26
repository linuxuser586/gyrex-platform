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

import org.eclipse.core.runtime.IStatus;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * Simple implementation of of a {@link IJobHistoryEntry}
 */
final class JobHistoryItemImpl implements IJobHistoryEntry {

	private final String result;
	private final long timestamp;
	private final int severity;

	JobHistoryItemImpl(final long timestamp, final String result, final int severity) {
		this.timestamp = timestamp;
		this.result = StringUtils.trimToEmpty(result);
		this.severity = severity;
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
		if (o.getSeverity() == getSeverity()) {
			return result.compareTo(o.getResult());
		} else {
			// severity is different
			// a higher severity also makes this item less so that it appears earlier then others
			return getSeverity() > o.getSeverity() ? -1 : 1;
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
		if (severity != other.severity) {
			return false;
		}
		if (!result.equals(other.result)) {
			// result is never null
			return false;
		}
		return true;
	}

	@Override
	public String getResult() {
		return result;
	}

	@Override
	public int getSeverity() {
		return severity;
	}

	@Override
	public long getTimeStamp() {
		return timestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.result.hashCode(); // result is never null
		result = (prime * result) + severity;
		result = (prime * result) + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("JobHistoryItemImpl [");
		builder.append(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(timestamp)).append(", ");
		switch (severity) {
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
				builder.append(severity);
				break;
		}
		if (StringUtils.isNotBlank(result)) {
			builder.append(", ").append(result);
		}
		builder.append("]");
		return builder.toString();
	}
}
