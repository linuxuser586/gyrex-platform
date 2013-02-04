/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.commands;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.jobs.internal.storage.CloudPreferencesCleanupJob;

import org.kohsuke.args4j.Option;

/**
 * Performs cleanup of jobs
 */
public class CleanupCmd extends Command {

	@Option(name = "-days", aliases = "--max-days-since-last-run", usage = "the number of days worth of job history to keep")
	Integer maxDays;

	/**
	 * Creates a new instance.
	 */
	public CleanupCmd() {
		super("triggers a clean up of old and hung jobs");
	}

	@Override
	protected void doExecute() throws Exception {
		final CloudPreferencesCleanupJob job = new CloudPreferencesCleanupJob();
		if ((maxDays != null) && (maxDays.intValue() > 0)) {
			job.setMaxDaysSinceLastRun(maxDays.intValue());
		}
		job.schedule();
		printf("Cleanup started and expected to finish asynchronously!");
	}

}
