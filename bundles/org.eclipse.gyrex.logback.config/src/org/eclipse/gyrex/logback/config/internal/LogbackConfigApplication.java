package org.eclipse.gyrex.logback.config.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.eclipse.equinox.app.IApplication;

import org.eclipse.gyrex.boot.internal.app.LogbackConfigurator;
import org.eclipse.gyrex.common.internal.applications.BaseApplication;
import org.eclipse.gyrex.logback.config.internal.xml.AppenderRef;
import org.eclipse.gyrex.logback.config.internal.xml.ConsoleAppender;
import org.eclipse.gyrex.logback.config.internal.xml.Level;
import org.eclipse.gyrex.logback.config.internal.xml.LevelChangePropagator;
import org.eclipse.gyrex.logback.config.internal.xml.LogbackConfig;
import org.eclipse.gyrex.logback.config.internal.xml.RootLogger;
import org.eclipse.gyrex.preferences.CloudScope;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackConfigApplication extends BaseApplication implements IApplication, IPreferenceChangeListener {

	private static final String PREF_LAST_MODIFIED = "lastModified";
	private static final Logger LOG = LoggerFactory.getLogger(LogbackConfigApplication.class);

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

	private File generateConfig() {
		// build config
		final LogbackConfig config = new LogbackConfig();
		config.elements.add(new LevelChangePropagator());
		final ConsoleAppender consoleAppender = new ConsoleAppender();
		config.elements.add(consoleAppender);
		final RootLogger rootLogger = new RootLogger();
		rootLogger.level = Level.WARN;
		rootLogger.appenders.add(new AppenderRef(consoleAppender));
		config.elements.add(rootLogger);

		// get state location
		final File parentFolder = Platform.getStateLocation(LogbackConfigActivator.getInstance().getBundle()).append("logback.xml").toFile();
		if (!parentFolder.isDirectory() && !parentFolder.mkdirs()) {
			throw new IllegalStateException(String.format("Unable to create configs directory (%s).", parentFolder));
		}

		// save file
		final File configFile = new File(parentFolder, String.format("%s.xml", DateFormatUtils.format(getLastModified(), "yyyyMMdd-HHmmssSSS")));
		try {
			final JAXBContext context = JAXBContext.newInstance(RootLogger.class);
			context.createMarshaller().marshal(context, configFile);
		} catch (final JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// cleanup directory
		removeOldFiles(parentFolder);

		// TODO Auto-generated method stub
		return configFile;
	}

	private long getLastModified() {
		return CloudScope.INSTANCE.getNode(LogbackConfigActivator.SYMBOLIC_NAME).getLong(PREF_LAST_MODIFIED, System.currentTimeMillis());
	}

	@Override
	protected Logger getLogger() {
		return LOG;
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
			configFile = generateConfig();
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

	private void removeOldFiles(final File parentFolder) {
		// only keep last 5 files
		File[] files = parentFolder.listFiles();
		if (null != files) {
			// remove any directories
			for (final File file : files) {
				if (file.isDirectory()) {
					FileUtils.deleteQuietly(file);
				}
			}

			// refresh
			files = parentFolder.listFiles();

			// remove old files
			if ((null != files) && (files.length > 5)) {
				// sort based on file name (reverse order)
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(final File o1, final File o2) {
						return o2.getName().compareTo(o1.getName());
					}
				});

				for (int i = 5; i < files.length; i++) {
					FileUtils.deleteQuietly(files[i]);
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
