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
package org.eclipse.gyrex.jobs.internal.manager;

import java.util.Collections;
import java.util.Map;

import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;
import org.eclipse.gyrex.jobs.spi.storage.JobHistoryEntryStorable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.commons.lang.CharSetUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * {@link IJobHistoryEntry} wrapping a {@link JobHistoryEntryStorable}
 */
public class StorableBackedJobHistoryEntry implements IJobHistoryEntry {

	private static int calculateHashCode(final JobHistoryEntryStorable storable) {
		IStatus status = storable.getResult();
		if (status == null) {
			status = Status.CANCEL_STATUS;
		}
		final long timestamp = storable.getTimestamp();
		final int prime = 31;
		int result = 1;
		result = (prime * result) + status.getMessage().hashCode(); // result is never null
		result = (prime * result) + status.getSeverity();
		result = (prime * result) + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	private final JobHistoryEntryStorable storable;
	private final int hashCode;

	/**
	 * Creates a new instance.
	 */
	public StorableBackedJobHistoryEntry(final JobHistoryEntryStorable storable) {
		this.storable = storable;
		hashCode = calculateHashCode(storable);
	}

	@Override
	public int compareTo(final IJobHistoryEntry o) {
		final long otherTimeStamp = o.getTimeStamp();
		if (otherTimeStamp > getTimeStamp())
			// other timestamp is greater --> we are are older and greater
			return 1;
		if (otherTimeStamp < getTimeStamp())
			// other timestamp is less --> we are newer and less
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
		final StorableBackedJobHistoryEntry other = (StorableBackedJobHistoryEntry) obj;
		if (getTimeStamp() != other.getTimeStamp())
			return false;
		if (!getResult().getMessage().equals(other.getResult().getMessage()))
			// result is never null
			return false;
		if (getResult().getSeverity() != other.getResult().getSeverity())
			return false;
		return true;
	}

	@Override
	public String getCancelledTrigger() {
		return storable.getCancelledTrigger();
	}

	@Override
	public Map<String, String> getParameter() {
		final Map<String, String> map = storable.getParameter();
		if (null == map)
			return Collections.emptyMap();
		return Collections.unmodifiableMap(map);
	}

	@Override
	public String getQueuedTrigger() {
		return StringUtils.trimToEmpty(storable.getQueuedTrigger());
	}

	@Override
	public IStatus getResult() {
		return null != storable.getResult() ? storable.getResult() : Status.CANCEL_STATUS;
	}

	@Override
	public long getTimeStamp() {
		return storable.getTimestamp();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(DateFormatUtils.SMTP_DATETIME_FORMAT.format(getTimeStamp())).append(" ");
		switch (getResult().getSeverity()) {
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
				builder.append(getResult().getSeverity());
				break;
		}
		if (StringUtils.isNotBlank(getResult().getMessage())) {
			builder.append(" ").append(StringUtils.replaceChars(CharSetUtils.delete(getResult().getMessage(), "\t\r\b"), '\n', '|'));
		}
		return builder.toString();
	}

}
