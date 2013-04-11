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

import org.eclipse.gyrex.logback.config.model.Appender;
import org.eclipse.gyrex.logback.config.model.LogbackConfig;
import org.eclipse.gyrex.logback.config.model.Logger;
import org.eclipse.gyrex.logback.config.spi.AppenderProvider;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import ch.qos.logback.classic.Level;

public class PreferenceBasedLogbackConfigStore {

	private static final String TYPE = "type";
	private static final String LEVEL = "level";
	private static final String INHERIT_OTHER_APPENDERS = "inheritOtherAppenders";
	private static final String APPENDER_REFS = "appenderRefs";
	private static final String LOGGERS = "loggers";
	private static final String APPENDERS = "appenders";
	private static final String DEFAULT_APPENDER_REFS = "defaultAppenderRefs";
	private static final String DEFAULT_LEVEL = "defaultLevel";

	private Appender loadAppender(final Preferences node) throws Exception {
		final String typeId = node.get(TYPE, null);
		final AppenderProvider provider = LogbackConfigActivator.getInstance().getAppenderProviderRegistry().getProvider(typeId);
		if (provider != null) {
			final Appender appender = provider.loadAppender(typeId, node);
			if (appender != null)
				return appender;
		}
		// TODO can we support a generic appender?
		throw new IllegalArgumentException(String.format("unknown appender type '%s' (appender '%s')", typeId, node.name()));
	}

	public LogbackConfig loadConfig(final Preferences node) throws Exception {
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
			config.addAppender(loadAppender(node.node(APPENDERS).node(appender)));
		}

		final String[] loggers = node.node(LOGGERS).childrenNames();
		for (final String logger : loggers) {
			config.addLogger(loadLogger(logger, node.node(LOGGERS).node(logger)));
		}

		return config;
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

	private void saveAppender(final Appender appender, final Preferences node) throws Exception {
		final AppenderProvider provider = LogbackConfigActivator.getInstance().getAppenderProviderRegistry().getProvider(appender.getTypeId());
		if (provider != null) {
			provider.writeAppender(appender, node);
			node.put(TYPE, appender.getTypeId());
		} else
			throw new IllegalArgumentException(String.format("unknown appender type '%s' (appender '%s')", appender.getClass().getSimpleName(), appender.getName()));
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

	public void saveConfig(final LogbackConfig config, final Preferences node) throws Exception {
		if (config.getDefaultLevel() != Level.INFO) {
			node.put(DEFAULT_LEVEL, config.getDefaultLevel().toString());
		} else {
			node.remove(DEFAULT_LEVEL);
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
