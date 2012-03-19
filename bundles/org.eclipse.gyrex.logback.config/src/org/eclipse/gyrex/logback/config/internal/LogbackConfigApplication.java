/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.logback.config.internal;

import java.io.File;
import java.util.Map;

import org.eclipse.equinox.app.IApplication;

import org.eclipse.gyrex.boot.internal.logback.LogbackConfigurator;
import org.eclipse.gyrex.common.internal.applications.BaseApplication;
import org.eclipse.gyrex.logback.config.internal.model.LogbackConfig;
import org.eclipse.gyrex.preferences.CloudScope;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application which (when started) hooks a listener with the preferences and
 * updates the instance logback configuration based on a generated logback
 * configuration file.
 */
public class LogbackConfigApplication extends BaseApplication implements IApplication, IPreferenceChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(LogbackConfigApplication.class);

	private static final String PREF_LAST_MODIFIED = "lastModified";
	private static final String PREF_NODE_CONFIG = "config";

	@Override
	protected void doStart(final Map arguments) throws Exception {
		// register preference listener
		CloudScope.INSTANCE.getNode(LogbackConfigActivator.SYMBOLIC_NAME).addPreferenceChangeListener(this);

		// generate initial config
		reloadConfig();
	}

	@Override
	protected Object doStop() {
		// remove listener
		CloudScope.INSTANCE.getNode(LogbackConfigActivator.SYMBOLIC_NAME).removePreferenceChangeListener(this);

		resetConfig();

		return EXIT_OK;
	}

	private File generateConfig(final LogbackConfig config) {
		return new LogbackConfigGenerator(getLastModified(), getParentFolder(), config).generateConfig();
	}

	private LogbackConfig getConfig() throws BackingStoreException {
		final IEclipsePreferences node = CloudScope.INSTANCE.getNode(LogbackConfigActivator.SYMBOLIC_NAME);
		if (node.nodeExists(PREF_NODE_CONFIG)) {
			return new PreferenceBasedLogbackConfigStore().loadConfig(node.node(PREF_NODE_CONFIG));
		}
		return null;
	}

	private long getLastModified() {
		return CloudScope.INSTANCE.getNode(LogbackConfigActivator.SYMBOLIC_NAME).getLong(PREF_LAST_MODIFIED, 0);
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}

	private File getParentFolder() {
		return Platform.getStateLocation(LogbackConfigActivator.getInstance().getBundle()).append("logback.xml").toFile();
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		if (StringUtils.equals(event.getKey(), PREF_LAST_MODIFIED)) {
			reloadConfig();
		}
	}

	private void reloadConfig() {
		// generate new configuration file
		File configFile;
		try {
			final LogbackConfig config = getConfig();
			if (null == config) {
				LOG.debug("No Logback configuration available. Nothing to load.");
				return;
			}
			configFile = generateConfig(config);
		} catch (final Exception e) {
			LOG.error("Exception while generating new Logback configuration. Aborting re-configuration. {}", ExceptionUtils.getRootCause(e), e);
			return;
		}

		// set file
		final File oldFile = LogbackConfigurator.setLogConfigurationFile(configFile);

		// configure
		try {
			LogbackConfigurator.configureDefaultContext();
		} catch (final Exception e) {
			System.err.printf("Error applying new Logback configuration (%s). Reverting to previous one (%s).%n", configFile, oldFile);
			e.printStackTrace(System.err);

			// try revert
			LogbackConfigurator.setLogConfigurationFile(oldFile);
			try {
				LogbackConfigurator.configureDefaultContext();
			} catch (final Exception revertException) {
				System.err.printf("Error reverting Logback configuration (to %s). Trying reset.%n", oldFile);
				revertException.printStackTrace(System.err);

				// try reset
				try {
					LogbackConfigurator.reset();
				} catch (final Exception resetException) {
					System.err.println("Error resetting Logback configuration. Logging won't work as expected. ");
					resetException.printStackTrace(System.err);
				}
			}
		}
	}

	private void resetConfig() {
		// reset config
		LogbackConfigurator.setLogConfigurationFile(null);

		// configure default
		try {
			LogbackConfigurator.configureDefaultContext();
		} catch (final Exception e) {
			System.err.println("Error restoring default Logback configuration. Trying reset.");
			e.printStackTrace(System.err);

			// try reset
			try {
				LogbackConfigurator.reset();
			} catch (final Exception resetException) {
				System.err.println("Error resetting Logback configuration. Logging won't work as expected. ");
				resetException.printStackTrace(System.err);
			}
		}
	}

}
