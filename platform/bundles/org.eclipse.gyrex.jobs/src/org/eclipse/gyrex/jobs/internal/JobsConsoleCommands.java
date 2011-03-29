/**
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.jobs.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.internal.registry.JobProviderRegistry;
import org.eclipse.gyrex.jobs.internal.scheduler.Schedule;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntryWorkingCopy;
import org.eclipse.gyrex.jobs.schedules.IScheduleWorkingCopy;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.quartz.CronExpression;

/**
 * Commands for software installations
 */
public class JobsConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(ScheduleManagerImpl manager, JobProviderRegistry registry, CommandInterpreter ci) throws Exception;

		public String getHelp() {
			return help;
		}

		protected void printInvalidArgs(final String errorMessage, final CommandInterpreter ci) {
			ci.println("ERROR: invalid arguments: " + errorMessage);
			ci.println("\t" + getHelp());
		}
	}

	static final Map<String, Command> commands = new TreeMap<String, Command>();
	static {
		commands.put("ls", new Command("<schedules|jobs> [<searchString>] - lists schedules or running jobs") {
			@Override
			public void execute(final ScheduleManagerImpl manager, final JobProviderRegistry registry, final CommandInterpreter ci) throws Exception {
				final String what = ci.nextArgument();
				if (StringUtils.isBlank(what)) {
					ci.println("ERROR: please specify what to list");
					ci.println(getHelp());
					return;
				}

				final String searchString = ci.nextArgument();

				if (StringUtils.startsWith("schedules", what)) {
					// show single schedule if id matches
					if (StringUtils.isNotBlank(searchString) && IdHelper.isValidId(searchString)) {
						final ISchedule schedule = manager.getSchedule(searchString);
						if (null != schedule) {
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
									info.append(prefix).append(entry.getId()).append(' ').append(entry.getCronExpression()).append(' ').append(entry.getJobProviderId()).appendNewLine();
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
							ci.print(info.toString());
							return;
						}
					}
					// list all
					final SortedSet<String> schedules = new TreeSet<String>(manager.getSchedules());
					for (final String id : schedules) {
						if (StringUtils.isBlank(searchString) || StringUtils.contains(id, searchString)) {
							ci.println(id);
						}
					}
				} else if (StringUtils.startsWith("jobs", what)) {
					final SortedSet<String> providers = new TreeSet<String>(registry.getProviders());
					for (final String id : providers) {
						if (StringUtils.isBlank(searchString) || StringUtils.contains(id, searchString)) {
							ci.println(id);
						}
					}
				}
			}
		});

		commands.put("createSchedule", new Command("<scheduleId> [<timeZone>] - creates a schedule") {
			@Override
			public void execute(final ScheduleManagerImpl manager, final JobProviderRegistry registry, final CommandInterpreter ci) throws Exception {
				final String scheduleId = ci.nextArgument();
				if (!IdHelper.isValidId(scheduleId)) {
					ci.println("ERROR: invalid schedule id");
					ci.println(getHelp());
					return;
				}

				final IScheduleWorkingCopy schedule = manager.createSchedule(scheduleId);

				final String tz = ci.nextArgument();
				if (StringUtils.isNotBlank(tz)) {
					schedule.setTimeZone(TimeZone.getTimeZone(tz));
				}

				manager.updateSchedule(schedule);
			}
		});

		commands.put("removeSchedule", new Command("<scheduleId> - removes a schedule") {
			@Override
			public void execute(final ScheduleManagerImpl manager, final JobProviderRegistry registry, final CommandInterpreter ci) throws Exception {
				final String scheduleId = ci.nextArgument();
				if (!IdHelper.isValidId(scheduleId)) {
					ci.println("ERROR: invalid schedule id");
					ci.println(getHelp());
					return;
				}

				manager.removeSchedule(scheduleId);
			}
		});
		commands.put("addEntryToSchedule", new Command("<scheduleId> <entryId> <cronExpression> <jobProviderId> - adds an entry to a schedule") {
			@Override
			public void execute(final ScheduleManagerImpl manager, final JobProviderRegistry registry, final CommandInterpreter ci) throws Exception {
				final String scheduleId = ci.nextArgument();
				if (!IdHelper.isValidId(scheduleId)) {
					ci.println("ERROR: invalid schedule id");
					ci.println(getHelp());
					return;
				}

				final String entryId = ci.nextArgument();
				if (!IdHelper.isValidId(entryId)) {
					ci.println("ERROR: invalid entry id");
					ci.println(getHelp());
					return;
				}

				final String cronExpression = ci.nextArgument();
				if (StringUtils.isBlank(cronExpression)) {
					ci.println("ERROR: missing cron expression");
					ci.println(getHelp());
					return;
				} else {
					try {
						new CronExpression(Schedule.asQuartzCronExpression(cronExpression));
					} catch (final Exception e) {
						ci.println("ERROR: invalid cron expression, please see http://en.wikipedia.org/wiki/Cron#CRON_expression");
						ci.println("       " + ExceptionUtils.getRootCauseMessage(e));
						return;
					}
				}

				final String jobProviderId = ci.nextArgument();
				if (!IdHelper.isValidId(jobProviderId)) {
					ci.println("ERROR: invalid job provider id");
					ci.println(getHelp());
					return;
				} else if (null == JobsActivator.getInstance().getJobProviderRegistry().getProvider(jobProviderId)) {
					ci.println("ERROR: job provider not found");
					return;
				}

				final IScheduleWorkingCopy schedule = manager.createWorkingCopy(scheduleId);
				final IScheduleEntryWorkingCopy entry = schedule.createEntry(entryId);

				entry.setJobProviderId(jobProviderId);
				entry.setCronExpression(cronExpression);

				manager.updateSchedule(schedule);
			}
		});

		commands.put("updateEntryJobParameter", new Command("<scheduleId> <entryId> <jobParamKey> [<jobParamValue>] - sets (or removes) an entry job paramater") {
			@Override
			public void execute(final ScheduleManagerImpl manager, final JobProviderRegistry registry, final CommandInterpreter ci) throws Exception {
				final String scheduleId = ci.nextArgument();
				if (!IdHelper.isValidId(scheduleId)) {
					ci.println("ERROR: invalid schedule id");
					ci.println(getHelp());
					return;
				}

				final String entryId = ci.nextArgument();
				if (!IdHelper.isValidId(entryId)) {
					ci.println("ERROR: invalid entry id");
					ci.println(getHelp());
					return;
				}

				final String jobParamKey = ci.nextArgument();
				if (StringUtils.isBlank(jobParamKey)) {
					ci.println("ERROR: missing job paramater key");
					ci.println(getHelp());
					return;
				}

				final String jobParamValue = ci.nextArgument();

				final IScheduleWorkingCopy schedule = manager.createWorkingCopy(scheduleId);
				final IScheduleEntryWorkingCopy entry = schedule.getEntry(entryId);

				final Map<String, String> parameter = new HashMap<String, String>(entry.getJobParameter());
				if (StringUtils.isBlank(jobParamValue)) {
					parameter.remove(jobParamKey);
				} else {
					parameter.put(jobParamKey, jobParamValue);
				}

				entry.setJobParameter(parameter);

				manager.updateSchedule(schedule);
			}
		});

		commands.put("enableSchedule", new Command("<scheduleId> - enables a schedule") {
			@Override
			public void execute(final ScheduleManagerImpl manager, final JobProviderRegistry registry, final CommandInterpreter ci) throws Exception {
				final String scheduleId = ci.nextArgument();
				if (!IdHelper.isValidId(scheduleId)) {
					ci.println("ERROR: invalid schedule id");
					ci.println(getHelp());
					return;
				}

				final IScheduleWorkingCopy schedule = manager.createWorkingCopy(scheduleId);

				schedule.setEnabled(true);
				manager.updateSchedule(schedule);
			}
		});

		commands.put("disableSchedule", new Command("<scheduleId> - disables a schedule") {
			@Override
			public void execute(final ScheduleManagerImpl manager, final JobProviderRegistry registry, final CommandInterpreter ci) throws Exception {
				final String scheduleId = ci.nextArgument();
				if (!IdHelper.isValidId(scheduleId)) {
					ci.println("ERROR: invalid schedule id");
					ci.println(getHelp());
					return;
				}

				final IScheduleWorkingCopy schedule = manager.createWorkingCopy(scheduleId);

				schedule.setEnabled(false);
				manager.updateSchedule(schedule);
			}
		});
	}

	static void printHelp(final CommandInterpreter ci) {
		ci.println("jobs <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			ci.println("\t" + cmd + " " + commands.get(cmd).getHelp());
		}
	}

	public void _jobs(final CommandInterpreter ci) throws Exception {
		final String command = ci.nextArgument();
		if (command == null) {
			printHelp(ci);
			return;
		}

		final Command cmd = commands.get(command);
		if (cmd == null) {
			ci.println("ERROR: unknown command " + command);
			printHelp(ci);
			return;
		}

		final ScheduleManagerImpl manager;
		final JobProviderRegistry registry;
		try {
			manager = JobsActivator.getInstance().getScheduleManager();
			registry = JobsActivator.getInstance().getJobProviderRegistry();
		} catch (final IllegalStateException e) {
			ci.println("ERROR: Required services not available! " + e.getMessage());
			return;
		}

		try {
			cmd.execute(manager, registry, ci);
		} catch (final Exception e) {
			if (JobsDebug.debug) {
				ci.printStackTrace(e);
			} else {
				ci.println(ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---Jobs Commands---");
		help.appendln("\tjobs <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			help.appendln("\t\t" + cmd + " " + commands.get(cmd).getHelp());
		}
		return help.toString();
	}

}
