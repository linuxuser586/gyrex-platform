package org.eclipse.gyrex.boot.internal.app;

import java.nio.charset.Charset;

import org.eclipse.gyrex.configuration.PlatformConfiguration;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.datalocation.Location;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;

public class LogbackConfigurator {

	public static void configureDefaultContext(final String[] arguments) throws Exception {
		// determine flags
		boolean debug = PlatformConfiguration.isOperatingInDevelopmentMode();
		for (final String arg : arguments) {
			if ("-debug".equalsIgnoreCase(arg)) {
				debug = true;
			}
		}

		// reset LoggerContext
		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.reset();

		// configure
		final StatusManager sm = lc.getStatusManager();
		if (sm != null) {
			sm.add(new InfoStatus("Setting up Gyrex log configuration.", lc));
		}

		// get root logger
		final Logger rootLogger = lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

		// add console logger in debug mode
		if (debug) {
			final ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<ILoggingEvent>();
			ca.setContext(lc);
			ca.setName("console");
			final PatternLayoutEncoder pl = new PatternLayoutEncoder();
			pl.setContext(lc);
			pl.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
			pl.start();

			ca.setEncoder(pl);
			ca.start();

			rootLogger.addAppender(ca);
		} else {
			// increase level
			rootLogger.setLevel(Level.INFO);
		}

		final IPath instanceLogfileDirectory = getLogfileDir();

		// add error logger
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

		rootLogger.addAppender(rfa);
	}

	private static IPath getLogfileDir() {
		final Location instanceLocation = AppActivator.getInstance().getInstanceLocation();
		return new Path(instanceLocation.getURL().getPath()).append("logs");
	}

	public static void reset() throws Exception {
		// reset LoggerContext
		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.reset();
	}

}