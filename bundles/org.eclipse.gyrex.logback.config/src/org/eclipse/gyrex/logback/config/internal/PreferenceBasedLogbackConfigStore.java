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

import java.util.List;
import java.util.Map;

import org.eclipse.gyrex.logback.config.internal.model.Appender;
import org.eclipse.gyrex.logback.config.internal.model.ConsoleAppender;
import org.eclipse.gyrex.logback.config.internal.model.FileAppender;
import org.eclipse.gyrex.logback.config.internal.model.FileAppender.RotationPolicy;
import org.eclipse.gyrex.logback.config.internal.model.LogbackConfig;
import org.eclipse.gyrex.logback.config.internal.model.Logger;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

import ch.qos.logback.classic.Level;

public class PreferenceBasedLogbackConfigStore {

	private static final String ROTATION_POLICY = "rotationPolicy";
	private static final String SIFTING_MDC_PROPERTYDEFAULT_VALUE = "siftingMdcPropertydefaultValue";
	private static final String SIFTING_MDC_PROPERTY_NAME = "siftingMdcPropertyName";
	private static final String CONSOLE = "console";
	private static final String FILE = "file";
	private static final String TYPE = "type";
	private static final String LEVEL = "level";
	private static final String INHERIT_OTHER_APPENDERS = "inheritOtherAppenders";
	private static final String APPENDER_REFS = "appenderRefs";
	private static final String COMPRESS_ROTATED_LOGS = "compressRotatedLogs";
	private static final String MAX_FILE_SIZE = "maxFileSize";
	private static final String MAX_HISTORY = "maxHistory";
	private static final String FILE_NAME = "fileName";
	private static final String PATTERN = "pattern";
	private static final String LOGGERS = "loggers";
	private static final String APPENDERS = "appenders";
	private static final String DEFAULT_APPENDER_REFS = "defaultAppenderRefs";
	private static final String DEFAULT_LEVEL = "defaultLevel";

	private Appender loadAppender(final String name, final Preferences node) throws BackingStoreException {
		final String type = node.get(TYPE, null);
		if (StringUtils.equals(type, FILE)) {
			return loadFileAppender(name, node);
		} else if (StringUtils.equals(type, CONSOLE)) {
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
		appender.setPattern(node.get(PATTERN, null));
		return appender;
	}

	private Appender loadFileAppender(final String name, final Preferences node) throws BackingStoreException {
		final FileAppender fileAppender = new FileAppender();
		fileAppender.setName(name);
		fileAppender.setPattern(node.get(PATTERN, null));
		fileAppender.setFileName(node.get(FILE_NAME, null));
		try {
			fileAppender.setRotationPolicy(RotationPolicy.valueOf(node.get(ROTATION_POLICY, null)));
			fileAppender.setMaxHistory(node.get(MAX_HISTORY, null));
			fileAppender.setMaxFileSize(node.get(MAX_FILE_SIZE, null));
			if (null != node.get(COMPRESS_ROTATED_LOGS, null)) {
				fileAppender.setCompressRotatedLogs(node.getBoolean(COMPRESS_ROTATED_LOGS, true));
			}
		} catch (final IllegalArgumentException e) {
			fileAppender.setRotationPolicy(null);
		}
		if (null != node.get(SIFTING_MDC_PROPERTY_NAME, null)) {
			fileAppender.setSiftingMdcPropertyName(node.get(SIFTING_MDC_PROPERTY_NAME, null));
			if (null != node.get(SIFTING_MDC_PROPERTYDEFAULT_VALUE, null)) {
				fileAppender.setSiftingMdcPropertyDefaultValue(node.get(SIFTING_MDC_PROPERTYDEFAULT_VALUE, null));
			}
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

	private void saveAppender(final Appender appender, final Preferences node) throws BackingStoreException {
		if (appender instanceof ConsoleAppender) {
			node.put(TYPE, CONSOLE);
			saveConsoleAppender((ConsoleAppender) appender, node);
		} else if (appender instanceof FileAppender) {
			node.put(TYPE, FILE);
			saveFileAppender((FileAppender) appender, node);
		} else {
			throw new IllegalArgumentException(String.format("unknown appender type '%s' (appender '%s')", appender.getClass().getSimpleName(), appender.getName()));
		}
	}

	private void saveAppenderRefs(final List<String> appenderRefs, final Preferences appenderRefsNode) throws BackingStoreException {
		if (appenderRefs.isEmpty()) {
			appenderRefsNode.removeNode();
		} else {
			for (final String appender : appenderRefsNode.keys()) {
				if (!appenderRefs.contains(appender)) {
					appenderRefsNode.remove(appender);
				}
			}
			for (final String appender : appenderRefs) {
				appenderRefsNode.put(appender, "inUse");
			}
		}
	}

	public void saveConfig(final LogbackConfig config, final Preferences node) throws BackingStoreException {
		if (config.getDefaultLevel() != Level.INFO) {
			node.put(DEFAULT_LEVEL, config.getDefaultLevel().toString());
		}

		saveAppenderRefs(config.getDefaultAppenders(), node.node(DEFAULT_APPENDER_REFS));

		final Preferences appendersNode = node.node(APPENDERS);
		final Map<String, Appender> appenders = config.getAppenders();
		for (final String appender : appendersNode.childrenNames()) {
			if (!appenders.containsKey(appender)) {
				appendersNode.node(appender).removeNode();
			}
		}
		for (final Appender appender : appenders.values()) {
			saveAppender(appender, appendersNode.node(appender.getName()));
		}

		final Preferences loggersNode = node.node(LOGGERS);
		final Map<String, Logger> loggers = config.getLoggers();
		for (final String logger : loggersNode.childrenNames()) {
			if (!loggers.containsKey(logger)) {
				loggersNode.node(logger).removeNode();
			}
		}
		for (final Logger logger : loggers.values()) {
			saveLogger(logger, loggersNode.node(logger.getName()));
		}

		node.flush();
	}

	private void saveConsoleAppender(final ConsoleAppender appender, final Preferences node) {
		if (null != appender.getPattern()) {
			node.put(PATTERN, appender.getPattern());
		} else {
			node.remove(PATTERN);
		}
	}

	private void saveFileAppender(final FileAppender appender, final Preferences node) {
		if (null != appender.getPattern()) {
			node.put(PATTERN, appender.getPattern());
		} else {
			node.remove(PATTERN);
		}
		if (null != appender.getFileName()) {
			node.put(FILE_NAME, appender.getFileName());
		} else {
			node.remove(FILE_NAME);
		}
		if (null != appender.getRotationPolicy()) {
			node.put(ROTATION_POLICY, appender.getRotationPolicy().name());
		} else {
			node.remove(ROTATION_POLICY);
		}
		if (null != appender.getMaxHistory()) {
			node.put(MAX_HISTORY, appender.getMaxHistory());
		} else {
			node.remove(MAX_HISTORY);
		}
		if (null != appender.getMaxFileSize()) {
			node.put(MAX_FILE_SIZE, appender.getMaxFileSize());
		} else {
			node.remove(MAX_FILE_SIZE);
		}
		if (appender.isSeparateLogOutputsPerMdcProperty()) {
			node.put(SIFTING_MDC_PROPERTY_NAME, appender.getSiftingMdcPropertyName());
			node.put(SIFTING_MDC_PROPERTYDEFAULT_VALUE, StringUtils.trimToEmpty(appender.getSiftingMdcPropertyDefaultValue()));
		} else {
			node.remove(SIFTING_MDC_PROPERTY_NAME);
			node.remove(SIFTING_MDC_PROPERTYDEFAULT_VALUE);
		}
		if (!appender.isCompressRotatedLogs()) {
			node.putBoolean(COMPRESS_ROTATED_LOGS, false);
		} else {
			node.remove(COMPRESS_ROTATED_LOGS);
		}
	}

	private void saveLogger(final Logger logger, final Preferences node) throws BackingStoreException {
		if (null != logger.getLevel()) {
			node.put(LEVEL, logger.getLevel().toString());
		} else {
			node.remove(LEVEL);
		}
		if (!logger.isInheritOtherAppenders()) {
			node.putBoolean(INHERIT_OTHER_APPENDERS, false);
		} else {
			node.remove(INHERIT_OTHER_APPENDERS);
		}

		saveAppenderRefs(logger.getAppenderReferences(), node.node(APPENDER_REFS));
	}
}
