/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO and others.
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

import java.io.File;
import java.nio.charset.Charset;
import java.util.logging.LogManager;

import org.eclipse.gyrex.boot.internal.BootActivator;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.datalocation.Location;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.StatusPrinter;

public class LogbackConfigurator {

	private static File logConfigurationFile;

	public static void configureDefaultContext() throws Exception {
		// reset JUL (this should disable the default JUL console output)
		LogManager.getLogManager().reset();

		// don't perform any further configuration if a config file is specified
		if (StringUtils.isNotBlank(System.getProperty("logback.configurationFile"))) {
			return;
		}

		// reset LoggerContext
		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.reset();

		// configure status manager
		if (lc.getStatusManager() == null) {
			lc.setStatusManager(new BasicStatusManager());
		}
		final StatusManager sm = lc.getStatusManager();

		// always good to have a console status listener
		if (Platform.inDebugMode() || Platform.inDevelopmentMode()) {
			final OnConsoleStatusListener onConsoleStatusListener = new OnConsoleStatusListener();
			onConsoleStatusListener.setContext(lc);
			sm.add(onConsoleStatusListener);
			onConsoleStatusListener.start();
		}

		// signal Gyrex configuration
		sm.add(new InfoStatus("Setting up Gyrex log configuration.", lc));

		// ensure log directory exists
		final IPath instanceLogfileDirectory = getLogfileDir();
		if (null != instanceLogfileDirectory) {
			instanceLogfileDirectory.toFile().mkdirs();
		}

		// prefer configuration file from workspace
		final File configurationFile = getLogConfigurationFile();
		if ((null != configurationFile) && configurationFile.isFile() && configurationFile.canRead()) {

			sm.add(new InfoStatus(String.format("Loading configuration from '%s'.", configurationFile.getAbsolutePath()), lc));

			// create our customized configurator
			final JoranConfigurator configurator = new JoranConfigurator() {
				@Override
				protected void addImplicitRules(final Interpreter interpreter) {
					super.addImplicitRules(interpreter);
					// set some properties for log file substitution
					if (null != instanceLogfileDirectory) {
						interpreter.getInterpretationContext().addSubstitutionProperty("gyrex.instance.area.logs", instanceLogfileDirectory.addTrailingSeparator().toOSString());
					}
				}
			};
			configurator.setContext(lc);

			// configuration
			configurator.doConfigure(configurationFile);

			// print logback's internal status
			StatusPrinter.printIfErrorsOccured(lc);

			// done'
			return;
		}

		// get root logger
		final Logger rootLogger = lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

		// propagate level changes to java.util.logging
		final LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
		levelChangePropagator.setResetJUL(true);
		levelChangePropagator.setContext(lc);
		lc.addListener(levelChangePropagator);
		levelChangePropagator.start();

		// add console logger
		rootLogger.addAppender(createConsoleAppender(lc));

		// add error logger
		if (null != instanceLogfileDirectory) {
			rootLogger.addAppender(createErrorLogAppender(lc, instanceLogfileDirectory));
		}

		// set log level
		if (Platform.inDebugMode() || Platform.inDevelopmentMode()) {
			rootLogger.setLevel(Level.DEBUG);
		} else {
			rootLogger.setLevel(Level.INFO);
		}

		// some of our components are very communicative
		// we apply some "smart" defaults for those known 3rdParty libs
		lc.getLogger("org.apache.commons.httpclient").setLevel(Level.WARN);
		lc.getLogger("httpclient.wire").setLevel(Level.WARN);
		lc.getLogger("org.apache.http").setLevel(Level.WARN);
		lc.getLogger("org.apache.zookeeper").setLevel(Level.WARN);
		lc.getLogger("org.apache.solr").setLevel(Level.WARN);
		lc.getLogger("org.apache.sshd").setLevel(Level.INFO);
		lc.getLogger("org.apache.mina").setLevel(Level.WARN);
		lc.getLogger("org.mortbay.log").setLevel(Level.INFO);
		lc.getLogger("org.eclipse.jetty").setLevel(Level.INFO);
		lc.getLogger("org.quartz").setLevel(Level.INFO);

		// print logback's internal status
		StatusPrinter.printIfErrorsOccured(lc);
	}

	private static ConsoleAppender<ILoggingEvent> createConsoleAppender(final LoggerContext lc) {
		final ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<ILoggingEvent>();
		ca.setContext(lc);
		ca.setName("console");
		final PatternLayoutEncoder pl = new PatternLayoutEncoder();
		pl.setContext(lc);
		pl.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
		pl.start();
		ca.setEncoder(pl);
		ca.start();
		return ca;
	}

	private static RollingFileAppender<ILoggingEvent> createErrorLogAppender(final LoggerContext lc, final IPath instanceLogfileDirectory) {
		final RollingFileAppender<ILoggingEvent> rfa = new RollingFileAppender<ILoggingEvent>();
		rfa.setContext(lc);
		rfa.setName("error-log");
		rfa.setFile(instanceLogfileDirectory.append("error.log").toOSString());

		final FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
		rollingPolicy.setMinIndex(1);
		rollingPolicy.setMaxIndex(3);
		rollingPolicy.setFileNamePattern("error.%i.log.zip");
		rollingPolicy.setParent(rfa);
		rfa.setRollingPolicy(rollingPolicy);

		rfa.setTriggeringPolicy(new SizeBasedTriggeringPolicy<ILoggingEvent>("5MB"));

		final PatternLayoutEncoder pl = new PatternLayoutEncoder();
		pl.setContext(lc);
		pl.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
		pl.setCharset(Charset.forName("UTF-8"));
		pl.start();
		rfa.setEncoder(pl);

		final ThresholdFilter tf = new ThresholdFilter();
		tf.setContext(lc);
		tf.setLevel(Level.ERROR.toString());
		tf.start();
		rfa.addFilter(tf);

		rfa.start();
		return rfa;
	}

	private static File getLogConfigurationFile() {
		// use configured file
		final File file = logConfigurationFile;
		if (null != file) {
			return file;
		}

		// use default file
		final Location instanceLocation = BootActivator.getInstance().getInstanceLocation();
		return new Path(instanceLocation.getURL().getPath()).append("etc/logback.xml").toFile();
	}

	private static IPath getLogfileDir() {
		try {
			final Location instanceLocation = BootActivator.getInstance().getInstanceLocation();
			return new Path(instanceLocation.getURL().getPath()).append("logs");
		} catch (final IllegalStateException e) {
			// not available, fallback null (which means no log files will be written)
			return null;
		}
	}

	public static void reset() throws Exception {
		// reset LoggerContext
		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.reset();

		// print logback's internal status
		StatusPrinter.printIfErrorsOccured(lc);
	}

	/**
	 * Allows to change the log configuration file at runtime.
	 * 
	 * @param file
	 *            the file to set
	 * @return the previously used file
	 */
	public static File setLogConfigurationFile(final File file) {
		final File oldFile = logConfigurationFile;
		logConfigurationFile = file;
		return oldFile;
	}
}
