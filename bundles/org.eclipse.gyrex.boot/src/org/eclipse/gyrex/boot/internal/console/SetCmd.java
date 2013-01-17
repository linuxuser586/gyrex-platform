/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.console;

import org.eclipse.gyrex.boot.internal.logback.LogbackConfigurator;
import org.eclipse.gyrex.boot.internal.logback.LogbackLevelDebugOptionsBridge;
import org.eclipse.gyrex.common.console.Command;

import org.eclipse.osgi.service.debug.DebugOptions;

import org.apache.commons.lang.exception.ExceptionUtils;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class SetCmd extends Command {

	@Argument(index = 0, usage = "specify an option name", metaVar = "OPTION", required = true)
	String name;
	@Argument(index = 1, usage = "specify an option value to set (or none to unset)", metaVar = "VALUE", required = false)
	String value;

	@Option(name = "-trace", aliases = "--enable-full-trace", usage = "an optional flag to enable all possible log levels of relevant loggers")
	boolean fullTrace;

	/**
	 * Creates a new instance.
	 */
	public SetCmd() {
		super("<NAME> <VALUE> - sets a debug option");
	}

	@Override
	protected void doExecute() throws Exception {
		// set debug option
		final DebugOptions debugOptions = DebugConsoleCommands.getDebugOptions();
		if (null == value) {
			debugOptions.removeOption(name);
			printf("Debug option (%s) has been unset.", name);
		} else {
			debugOptions.setOption(name, value);
			printf("Debug option (%s) set to (%s).", name, value);
		}

		// update logger
		String loggerName = null;
		try {
			// get logger name
			// note, we call this inside try/catch in order to be save here in case some dependencies aren't there
			loggerName = LogbackLevelDebugOptionsBridge.getLoggerNameForDebugOption(name);
			if (loggerName != null) {
				if ((value != null) && !value.equalsIgnoreCase("false")) {
					final String level = fullTrace ? "ALL" : "DEBUG";
					LogbackConfigurator.setLogLevelOverride(loggerName, level);
					printf("Log level of logger (%s) updated to (%s).", loggerName, level);
				} else {
					LogbackConfigurator.setLogLevelOverride(loggerName, null);
					printf("Log level of logger (%s) has been resetted to its default.", loggerName);
				}
			}
		} catch (final Exception e) {
			// workaround for LOGBACK-758
			if ((e instanceof NullPointerException) && (e.getStackTrace().length > 0) && e.getStackTrace()[0].getMethodName().equals("asJULLevel")) {
				printf("Logback logger (%s) was resetted to its default propererly.%nWARNING: Due to LOGBACK-758 the change did not propagate properly to JUL loggers.", loggerName);
			} else {
				printf("WARNING: Unable to update logger (%s). %s", loggerName, ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}
}