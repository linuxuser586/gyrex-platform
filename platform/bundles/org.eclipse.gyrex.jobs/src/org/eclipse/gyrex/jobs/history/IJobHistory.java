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
package org.eclipse.gyrex.jobs.history;

import java.util.List;

import org.eclipse.gyrex.jobs.IJob;

/**
 * The history of all tracked executions of a {@link IJob job}.
 */
public interface IJobHistory {

	/**
	 * Returns the history entries.
	 * 
	 * @return an unmodifiable list of history entries
	 */
	List<IJobHistoryEntry> getEntries();

}
