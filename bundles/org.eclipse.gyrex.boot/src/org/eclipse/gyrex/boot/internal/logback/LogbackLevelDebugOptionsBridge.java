/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.logback;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.boot.internal.BootActivator;
import org.eclipse.gyrex.common.services.ServiceNotAvailableException;

import org.eclipse.osgi.service.debug.DebugOptions;

import org.apache.commons.lang.StringUtils;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;

/**
 * A simple util for bridging debug options into logback levels.
 * <p>
 * This class is tightly integrated with {@link LogbackConfigurator}. Please do
 * not instantiate it directly.
 * </p>
 */
public class LogbackLevelDebugOptionsBridge {

	private static final String MAIN_BUNDLE_DEBUG_OPTION = "/debug";

	public static String getLoggerNameForDebugOption(final String debugOption) {
		if (null == debugOption)
			return null;

		// only calculate a logger name for the main bundle debug option
		if (!debugOption.endsWith(MAIN_BUNDLE_DEBUG_OPTION))
			return null;

		return StringUtils.removeEnd(debugOption, MAIN_BUNDLE_DEBUG_OPTION);
	}

	final ConcurrentMap<String, String[]> overriddenLogLevels = new ConcurrentHashMap<String, String[]>();

	/**
	 * Creates a new instance.
	 */
	LogbackLevelDebugOptionsBridge() {
		// empty
	}

	/**
	 * Initialize log level overrides from debug options.
	 * <p>
	 * This may only be called during bootstrapping before any custom overrides
	 * are set. Your milage may vary if called while the application is running.
	 * </p>
	 * 
	 * @throws Exception
	 */
	void initializeLogLevelOverrides() throws Exception {
		// reset current overrides
		overriddenLogLevels.clear();

		// add a note to the status manager
		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		final StatusManager sm = lc.getStatusManager();
		if (sm != null) {
			sm.add(new InfoStatus("Initializing log level overrides.", this));
		}

		// apply new overrides
		try {
			final Map<String, String> options = BootActivator.getInstance().getService(DebugOptions.class).getOptions();
			for (final Entry<String, String> e : options.entrySet()) {
				final String loggerName = getLoggerNameForDebugOption(e.getKey());
				if (loggerName != null) {
					if ((null != e.getValue()) && !"false".equalsIgnoreCase(e.getValue())) {
						setLogLevelOverride(loggerName, "DEBUG");
					}
				}
			}
		} catch (final ServiceNotAvailableException e) {
			// no debug options available (ignore)
		}
	}

	void setLogLevelOverride(final String loggerName, final String level) throws Exception {
		// get logger
		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		final StatusManager sm = lc.getStatusManager();
		final Logger logger = lc.getLogger(loggerName);

		// get levels
		final Level currentLevel = logger.getLevel();
		final Level newLevel = Level.toLevel(level, null);

		if (newLevel == null) {
			// reset any current override
			final String[] removed = overriddenLogLevels.remove(loggerName);
			if (removed != null) {
				final Level toRestore = Level.toLevel(removed[1], null);
				if (sm != null) {
					sm.add(new InfoStatus(String.format("Resetting level for logger '%s'.", loggerName, logger.getEffectiveLevel()), this));
				}
				logger.setLevel(toRestore);
			}
		} else if (newLevel != currentLevel) {
			// apply new override
			overriddenLogLevels.put(loggerName, new String[] { level, null != currentLevel ? currentLevel.levelStr : null });
			if (sm != null) {
				sm.add(new InfoStatus(String.format("Overriding level for logger '%s' to '%s'.", loggerName, newLevel), this));
			}
			logger.setLevel(newLevel);
		}
	}
}
