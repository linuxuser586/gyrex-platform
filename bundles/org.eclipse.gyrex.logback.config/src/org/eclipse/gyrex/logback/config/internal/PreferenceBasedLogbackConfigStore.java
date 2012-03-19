/**
 * Copyright (c) 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.logback.config.internal;

import org.eclipse.gyrex.logback.config.internal.model.Appender;
import org.eclipse.gyrex.logback.config.internal.model.ConsoleAppender;
import org.eclipse.gyrex.logback.config.internal.model.FileAppender;
import org.eclipse.gyrex.logback.config.internal.model.LogbackConfig;
import org.eclipse.gyrex.logback.config.internal.model.Logger;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

import ch.qos.logback.classic.Level;

public class PreferenceBasedLogbackConfigStore {

	private static final String LEVEL = "level";
	private static final String INHERIT_OTHER_APPENDERS = "inheritOtherAppenders";
	private static final String APPENDER_REFS = "appenderRefs";
	private static final String COMPRESS_ROTATED_LOGS = "compressRotatedLogs";
	private static final String MAX_FILE_SIZE = "maxFileSize";
	private static final String MAX_HISTORY = "maxHistory";
	private static final String FILE_NAME = "fileName";
	private static final String KEY_PATTERN = "pattern";
	private static final String LOGGERS = "loggers";
	private static final String APPENDERS = "appenders";
	private static final String DEFAULT_APPENDER_REFS = "defaultAppenderRefs";
	private static final String DEFAULT_LEVEL = "defaultLevel";

	private Appender loadAppender(final String name, final Preferences node) throws BackingStoreException {
		final String type = node.get("type", null);
		if (StringUtils.equals(type, "file")) {
			return loadFileAppender(name, node);
		} else if (StringUtils.equals(type, "console")) {
			return loadConsoleAppender(name, node);
		}
		throw new IllegalArgumentException(String.format("unknown appender type '%s' (appender '%s')", type, name));
	}

	public LogbackConfig loadConfig(final Preferences node) throws BackingStoreException {
		final LogbackConfig config = new LogbackConfig();

		final String defaultLevel = node.get(DEFAULT_LEVEL, null);
		if (null != defaultLevel) {
			config.setDefaultLevel(Level.toLevel(defaultLevel, Level.INFO));
		}
		for (final String appender : node.node(DEFAULT_APPENDER_REFS).keys()) {
			config.getDefaultAppenders().add(appender);
		}

		final String[] appenders = node.node(APPENDERS).childrenNames();
		for (final String appender : appenders) {
			config.addAppender(loadAppender(appender, node.node(APPENDERS).node(appender)));
		}

		final String[] loggers = node.node(LOGGERS).childrenNames();
		for (final String logger : loggers) {
			config.addLogger(loadLogger(logger, node.node(LOGGERS).node(logger)));
		}

		return config;
	}

	private Appender loadConsoleAppender(final String name, final Preferences node) throws BackingStoreException {
		final ConsoleAppender appender = new ConsoleAppender();
		appender.setName(name);
		appender.setPattern(node.get(KEY_PATTERN, null));
		return appender;
	}

	private Appender loadFileAppender(final String name, final Preferences node) throws BackingStoreException {
		final FileAppender fileAppender = new FileAppender();
		fileAppender.setName(name);
		fileAppender.setPattern(node.get(KEY_PATTERN, null));
		fileAppender.setFileName(node.get(FILE_NAME, null));
		fileAppender.setMaxHistory(node.get(MAX_HISTORY, null));
		fileAppender.setMaxFileSize(node.get(MAX_FILE_SIZE, null));
		if (null != node.get(COMPRESS_ROTATED_LOGS, null)) {
			fileAppender.setCompressRotatedLogs(node.getBoolean(COMPRESS_ROTATED_LOGS, true));
		}
		return fileAppender;
	}

	private Logger loadLogger(final String name, final Preferences node) throws BackingStoreException {
		final Logger logger = new Logger();
		logger.setName(name);
		if (null != node.get(LEVEL, null)) {
			logger.setLevel(Level.toLevel(node.get(LEVEL, null), Level.INFO));
		}
		if (null != node.get(INHERIT_OTHER_APPENDERS, null)) {
			logger.setInheritOtherAppenders(node.getBoolean(INHERIT_OTHER_APPENDERS, true));
		}
		for (final String appender : node.node(APPENDER_REFS).keys()) {
			logger.getAppenderReferences().add(appender);
		}
		return logger;
	}
}
