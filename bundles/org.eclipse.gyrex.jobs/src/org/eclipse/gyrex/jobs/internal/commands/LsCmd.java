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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.services.queue.IMessage;
import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.cloud.services.queue.IQueueServiceProperties;
import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.JobState;
import org.eclipse.gyrex.jobs.history.IJobHistoryEntry;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.manager.JobHungDetectionHelper;
import org.eclipse.gyrex.jobs.internal.manager.JobImpl;
import org.eclipse.gyrex.jobs.internal.manager.StorableBackedJobHistoryEntry;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleStore;
import org.eclipse.gyrex.jobs.internal.storage.CloudPreferncesJobHistoryStorage;
import org.eclipse.gyrex.jobs.internal.storage.CloudPreferncesJobStorage;
import org.eclipse.gyrex.jobs.internal.util.ContextHashUtil;
import org.eclipse.gyrex.jobs.internal.worker.JobInfo;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleWorkingCopy;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.lang.time.DateFormatUtils;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 *
 */
public class LsCmd extends Command {

	private static String toRelativeTime(final long duration) {
		if (duration < TimeUnit.MINUTES.toMillis(2))
			return "a minute ago";
		else if (duration < TimeUnit.HOURS.toMillis(2))
			return String.format("%d minutes ago", TimeUnit.MILLISECONDS.toMinutes(duration));
		else
			return String.format("%d hours ago", TimeUnit.MILLISECONDS.toMinutes(duration));
	}

	@Argument(index = 0, usage = "specify what to list (schedules, jobs or providers)", required = true, metaVar = "WHAT")
	String what;

	@Argument(index = 1, usage = "an optional filter string", required = false, metaVar = "FILTER")
	String searchString;

	@Option(name = "-q", aliases = { "--queue-id" }, usage = "an optional queue id", required = false, metaVar = "QUEUE-ID")
	String queueId = IJobManager.DEFAULT_QUEUE;

	/**
	 * Creates a new instance.
	 */
	public LsCmd() {
		super("<schedules|providers|running|waiting|all|job|queue> [<filterString>] - lists schedules or jobs");
	}

	@Override
	protected void doExecute() throws Exception {
		if (StringUtils.isBlank(what)) {
			printf("ERROR: please specify what to list");
			return;
		}

		if (StringUtils.startsWithIgnoreCase("providers", what)) {
			printProviders();
		} else if (StringUtils.startsWithIgnoreCase("schedules", what)) {
			printSchedules();
		} else if (StringUtils.startsWithIgnoreCase("running", what)) {
			printJobs(JobState.RUNNING);
		} else if (StringUtils.startsWithIgnoreCase("waiting", what)) {
			printJobs(JobState.WAITING);
		} else if (StringUtils.startsWithIgnoreCase("all", what)) {
			printJobs(null);
		} else if (StringUtils.startsWithIgnoreCase("job", what)) {
			printJobs(null);
		} else if (StringUtils.startsWithIgnoreCase("queues", what)) {
			printJobsQueue();
		}
	}

	private String getActiveNodeId(final String storageId) {
		try {
			return JobHungDetectionHelper.getProcessingNodeId(storageId, null);
		} catch (final IllegalStateException e) {
			return String.format("[%s]", e.getMessage());
		}
	}

	private SortedSet<String> getJobIds(final JobState state) throws BackingStoreException {
		final String[] storageIds = CloudPreferncesJobStorage.getJobsNode().childrenNames();
		if (null == state)
			return new TreeSet<String>(Arrays.asList(storageIds));

		final TreeSet<String> jobIds = new TreeSet<String>();
		for (final String storageId : storageIds) {
			if (StringUtils.equals(CloudPreferncesJobStorage.getJobsNode().node(storageId).get("status", null), state.name())) {
				jobIds.add(storageId);
			}
		}
		return jobIds;
	}

	private void printJob(final JobImpl job) throws Exception {
		final StrBuilder info = new StrBuilder();
		info.appendln(job.getId());
		info.appendFixedWidthPadLeft("type: ", 26, ' ').appendln(job.getTypeId());
		info.appendFixedWidthPadLeft("state: ", 26, ' ').appendln(job.getState());
		info.appendFixedWidthPadLeft("last start time: ", 26, ' ').appendln(job.getLastStart() > -1 ? DateFormatUtils.formatUTC(job.getLastStart(), "yyyy-MM-dd 'at' HH:mm:ss z") : "never");
		info.appendFixedWidthPadLeft("last successfull finish: ", 26, ' ').appendln(job.getLastSuccessfulFinish() > -1 ? DateFormatUtils.formatUTC(job.getLastSuccessfulFinish(), "yyyy-MM-dd 'at' HH:mm:ss z") : "never");
		info.appendFixedWidthPadLeft("last result: ", 26, ' ').appendln(job.getLastResult() != null ? job.getLastResult().getMessage() : "(not available)");

		final String activeNodeId = getActiveNodeId(job.getStorageKey());
		info.appendFixedWidthPadLeft("active on: ", 26, ' ').appendln(null != activeNodeId ? activeNodeId : "(not active)");

		final IEclipsePreferences historyNode = CloudPreferncesJobHistoryStorage.getJobsHistoryNode();
		if (historyNode.nodeExists(job.getStorageKey())) {
			final IEclipsePreferences jobHistory = CloudPreferncesJobHistoryStorage.getHistoryNode(job.getStorageKey());
			final String[] childrenNames = jobHistory.childrenNames();
			final SortedSet<IJobHistoryEntry> entries = new TreeSet<IJobHistoryEntry>();
			for (final String entryId : childrenNames) {
				entries.add(new StorableBackedJobHistoryEntry(CloudPreferncesJobHistoryStorage.readItem(jobHistory.node(entryId))));
			}

			info.appendNewLine();
			for (final IJobHistoryEntry entry : entries) {
				info.appendln(entry.toString());
			}
		}

		printf("%s", info.toString());
	}

	private void printJobs(final JobState state) throws Exception {
		// get all matching jobs and sort
		final SortedSet<String> storageIds = getJobIds(state);

		// show single job if id matches
		if (StringUtils.isNotBlank(searchString) && IdHelper.isValidId(searchString)) {
			for (final String storageId : storageIds) {
				final String externalId = ContextHashUtil.getExternalId(storageId);
				if (StringUtils.equals(searchString, externalId)) {
					final JobImpl job = CloudPreferncesJobStorage.readJob(externalId, CloudPreferncesJobStorage.getJobsNode().node(storageId));
					if (null != job) {
						printJob(job);
						return;
					}
				}
			}
		}

		// list all
		boolean found = false;
		for (final String storageId : storageIds) {
			final String externalId = ContextHashUtil.getExternalId(storageId);
			if (StringUtils.isBlank(searchString) || StringUtils.contains(storageId, searchString)) {
				final String activeNodeId = getActiveNodeId(storageId);
				if (activeNodeId != null) {
					printf("%s (active on %s, %s)", externalId, activeNodeId, storageId);
				} else {
					printf("%s (%s)", externalId, storageId);
				}
				found = true;
			}
		}

		if (!found) {
			if (null != state) {
				printf("no %s jobs found", state.name());
			} else {
				printf("no jobs found");
			}
			return;
		}
	}

	private void printJobsQueue() {
		if (!IdHelper.isValidId(queueId)) {
			printf("ERROR: invalid queueId: %s", queueId);
			return;
		}

		// get the queue
		final IQueue queue = JobsActivator.getInstance().getQueueService().getQueue(queueId, null);
		if (queue == null) {
			printf("ERROR: queue not found: %s", queueId);
			return;
		}

		// get message
		final HashMap<String, Object> properties = new HashMap<>(2);
		properties.put(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT, new Long(0));
		final List<IMessage> message = queue.receiveMessages(500, properties);
		if (message.isEmpty()) {
			printf("Queue '%s' is empty!", queueId);
			return;
		}

		printf("Found %d messages:", message.size());
		for (final IMessage m : message) {
			try {
				final JobInfo jobInfo = JobInfo.parse(m);
				if ((searchString == null) || StringUtils.containsIgnoreCase(jobInfo.getJobId(), searchString) || StringUtils.containsIgnoreCase(jobInfo.getContextPath().toString(), searchString)) {
					printf("  %s (%s, %s, %s)", jobInfo.getJobId(), jobInfo.getContextPath(), toRelativeTime(System.currentTimeMillis() - jobInfo.getQueueTimestamp()), jobInfo.getQueueTrigger());
				}
			} catch (final IOException e) {
				printf("  %s", e.getMessage());
			}
		}
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
		boolean found = false;
		for (final String storageId : storageIds) {
			final String externalId = ScheduleManagerImpl.getExternalId(storageId);
			if (StringUtils.isBlank(searchString) || StringUtils.contains(storageId, searchString)) {
				printf("%s (storage key %s)", externalId, storageId);
				found = true;
			}
		}

		if (!found) {
			printf("No schedules found!");
		}
	}
}
