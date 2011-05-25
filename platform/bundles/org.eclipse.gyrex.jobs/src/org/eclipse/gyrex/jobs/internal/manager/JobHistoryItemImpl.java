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

/**
 * Simple implementation of of a {@link IJobHistoryEntry}
 */
final class JobHistoryItemImpl implements IJobHistoryEntry {

	private final String result;
	private final long timestamp;
	private final int severity;

	JobHistoryItemImpl(final long timestamp, final String result, final int severity) {
		this.timestamp = timestamp;
		this.result = result;
		this.severity = severity;
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
}
