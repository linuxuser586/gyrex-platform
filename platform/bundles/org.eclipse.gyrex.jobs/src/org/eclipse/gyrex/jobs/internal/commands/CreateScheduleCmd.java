/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.commands;

import java.util.TimeZone;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleManager;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleWorkingCopy;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class CreateScheduleCmd extends Command {

	@Option(name = "-tz", aliases = "--time-zone", usage = "an optional timezone the schedule will operate in")
	String tz;

	@Argument(index = 0, usage = "the path of the context the schedule will operate in", required = true, metaVar = "CONTEXTPATH")
	String contextPath;

	@Argument(index = 1, usage = "the schedule id", required = true, metaVar = "SCHEDULEID")
	String scheduleId;

	/**
	 * Creates a new instance.
	 */
	public CreateScheduleCmd() {
		super("<contextPath> <scheduleId> - creates a schedule");
	}

	@Override
	protected void doExecute() throws Exception {
		final IPath parsedContextPath = new Path(contextPath);
		final IRuntimeContext context = JobsActivator.getInstance().getService(IRuntimeContextRegistry.class).get(parsedContextPath);
		if (null == context) {
			printf("Context '%s' not defined!", parsedContextPath);
		}

		final IScheduleManager manager = context.get(IScheduleManager.class);
		final IScheduleWorkingCopy schedule = manager.createSchedule(scheduleId);

		if (StringUtils.isNotBlank(tz)) {
			schedule.setTimeZone(TimeZone.getTimeZone(tz));
		}

		manager.updateSchedule(schedule);
		printf("Created schedule %s!", schedule.getId());
	}

}
