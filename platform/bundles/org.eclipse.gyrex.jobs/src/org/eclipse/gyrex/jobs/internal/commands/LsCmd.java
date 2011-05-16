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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleWorkingCopy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.kohsuke.args4j.Argument;

/**
 *
 */
public class LsCmd extends Command {

	@Argument(index = 0, usage = "specify what to list (schedules, jobs or providers)", required = true, metaVar = "WHAT")
	String what;

	@Argument(index = 1, usage = "an optional filter string", required = false, metaVar = "FILTER")
	String searchString;

	/**
	 * Creates a new instance.
	 */
	public LsCmd() {
		super("<schedules|jobs|providers> <contextPath> [<filterString>] - lists schedules or jobs");
	}

	@Override
	protected void doExecute() throws Exception {
		if (StringUtils.isBlank(what)) {
			printf("ERROR: please specify what to list");
			return;
		}

		if (StringUtils.startsWith("providers", what)) {
			printProviders();
		} else

		if (StringUtils.startsWith("schedules", what)) {
			printSchedules();
		} else if (StringUtils.startsWith("jobs", what)) {
			printJobs();
		}
	}

	private void printJobs() {
//		final IJobManager jobManager = context.get(IJobManager.class);
//
//		// show single schedule if id matches
//		if (StringUtils.isNotBlank(searchString) && IdHelper.isValidId(searchString)) {
//			final IJob job = jobManager.getJob(searchString);
//			if (null != job) {
//
//				final StrBuilder info = new StrBuilder();
//				info.append(job.getId());
//				info.append("                    type: ").appendln(job.getTypeId());
//				info.append("                   state: ").appendln(job.getState());
//				info.append("         last start time: ").appendln(job.getLastStart() > -1 ? DateFormatUtils.formatUTC(job.getLastStart(), "yyyy-MM-dd 'at' HH:mm:ss z") : "never");
//				info.append(" last successfull finish: ").appendln(job.getLastSuccessfulFinish() > -1 ? DateFormatUtils.formatUTC(job.getLastSuccessfulFinish(), "yyyy-MM-dd 'at' HH:mm:ss z") : "never");
//
//				printf("%s", info.toString());
//				return;
//			}
//		}
//
//		// list all
//		final SortedSet<String> schedules = new TreeSet<String>(jobManager.getJobs());
//		for (final String id : schedules) {
//			if (StringUtils.isBlank(searchString) || StringUtils.contains(id, searchString)) {
//				final IJob job = jobManager.getJob(searchString);
//				if (null == job) {
//					continue;
//				}
//
//				printf("%s (%s) [%s]", job.getId(), job.getTypeId(), job.getState().toString());
//			}
//		}
	}

	private void printProviders() {
		final SortedSet<String> providers = new TreeSet<String>(JobsActivator.getInstance().getJobProviderRegistry().getProviders());
		for (final String id : providers) {
			if (StringUtils.isBlank(searchString) || StringUtils.contains(id, searchString)) {
				printf(id);
			}
		}
		return;
	}

	private void printSchedule(final ISchedule schedule) {
		final StrBuilder info = new StrBuilder();
		info.append(schedule.getId());
		if (!schedule.isEnabled()) {
			info.appendln(" DISABLED");
		}

		final TimeZone timeZone = schedule.getTimeZone();
		info.append("  time zone: ").append(timeZone.getDisplayName(false, TimeZone.LONG, Locale.US)).appendln(timeZone.useDaylightTime() ? " (will adjust to daylight changes)" : " (independent of daylight changes)");
		info.append("      queue: ").appendln(null != schedule.getQueueId() ? schedule.getQueueId() : "(default)");
		String prefix = "    entries: ";

		final List<IScheduleEntry> entries = schedule.getEntries();
		if (!entries.isEmpty()) {
			for (final IScheduleEntry entry : entries) {
				info.append(prefix).append(entry.getId()).append(' ').append(entry.getCronExpression()).append(' ').append(entry.getJobTypeId()).appendNewLine();
				prefix = "             ";
				final Map<String, String> parameter = entry.getJobParameter();
				if (!parameter.isEmpty()) {
					final Set<Entry<String, String>> entrySet = parameter.entrySet();
					for (final Entry<String, String> param : entrySet) {
						info.append(prefix).append("    ").append(param.getKey()).append('=').appendln(param.getValue());
					}
				}
			}
		} else {
			info.append(prefix).appendln("(none)");
		}
		printf("%s", info.toString());
		return;
	}

	private void printSchedules() throws Exception {

		// get all known schedules and sort
		final SortedSet<String> storageIds = new TreeSet<String>(Arrays.asList(ScheduleStore.getSchedules()));

		// first search for a direct id match
		if (StringUtils.isNotBlank(searchString)) {
			for (final String storageId : storageIds) {
				final String externalId = ScheduleManagerImpl.getExternalId(storageId);
				if (StringUtils.equals(searchString, externalId)) {
					final IScheduleWorkingCopy schedule = ScheduleStore.load(storageId, externalId, false);
					if (null != schedule) {
						printSchedule(schedule);
						return;
					}
				}
			}
		}

		// list schedules
		for (final String storageId : storageIds) {
			final String externalId = ScheduleManagerImpl.getExternalId(storageId);
			if (StringUtils.isBlank(searchString) || StringUtils.contains(storageId, searchString)) {
				printf("%s (storage key %s)", externalId, storageId);
			}
		}
	}

}
