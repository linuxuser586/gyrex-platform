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

import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.manager.JobManagerImpl;
import org.eclipse.gyrex.jobs.manager.IJobManager;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Command to cancel a running/waiting job.
 */
public class CancelJobCmd extends Command {

	@Argument(index = 0, usage = "the path of the context the schedule will operate in", required = true, metaVar = "CONTEXTPATH")
	String contextPath;

	@Argument(index = 1, usage = "job id filter string", required = true, metaVar = "JOB-ID-FILTER")
	String searchString;

	@Option(name = "-reset", usage = "flag to force a reset of a job state (default is no)", required = false)
	boolean resetJobState;

	/**
	 * Creates a new instance.
	 */
	public CancelJobCmd() {
		super("<filterString> - cancels all matching jobs");
	}

	@Override
	protected void doExecute() throws Exception {
		if (StringUtils.isBlank(searchString)) {
			printf("ERROR: please specify a job filter (use '*' to cancel all)");
			return;
		}

		final IPath parsedContextPath = new Path(contextPath);
		final IRuntimeContext context = JobsActivator.getInstance().getService(IRuntimeContextRegistry.class).get(parsedContextPath);
		if (null == context) {
			printf("Context '%s' not defined!", parsedContextPath);
			return;
		}

		final IJobManager jobManager = context.get(IJobManager.class);

		// get all matching jobs and sort
		final SortedSet<String> jobIds = new TreeSet<String>(jobManager.getJobs());

		// cancel single job if id matches
		if (!StringUtils.equals(searchString, "*") && IdHelper.isValidId(searchString)) {
			if (jobIds.contains(searchString)) {
				jobManager.cancelJob(searchString, "console");
				if (resetJobState) {
					((JobManagerImpl) jobManager).setJobState(searchString, JobState.ABORTING, JobState.NONE, null, System.currentTimeMillis());
				}
				printf("Job %s canceled.", searchString);
				return;
			}
		}

		// cancel all matching jobs
		for (final String jobId : jobIds) {
			if (StringUtils.equals(searchString, "*") || StringUtils.contains(jobId, searchString)) {
				try {
					jobManager.cancelJob(jobId, "console");
					if (resetJobState) {
						((JobManagerImpl) jobManager).setJobState(jobId, JobState.ABORTING, JobState.NONE, null, System.currentTimeMillis());
					}
					printf("Job %s canceled.", jobId);
				} catch (final IllegalArgumentException e) {
					printf("Job %s not WAITING or RUNNING.", jobId);
				} catch (final IllegalStateException e) {
					printf("Error cancelling job %s. %s", jobId, ExceptionUtils.getRootCauseMessage(e));
				}
			}
		}
	}

}
