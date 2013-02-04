/**
 * Copyright (c) 2010, 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.frameworklogadapter.internal;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.log.LogFilter;
import org.eclipse.equinox.log.SynchronousLogListener;

import org.eclipse.core.runtime.adaptor.EclipseStarter;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

/**
 * The Gyrex SLF4J forwarder is responsible for routing all framework log
 * messages to SLF4J.
 * <p>
 * However, the adapter does not define a strict dependency on SLF4J. Instead,
 * it uses a mixture of OSGi services and reflection to integrate with SLF4J if
 * it becomes available. This allows the backend to come and go while the bridge
 * buffers massages in between.
 * </p>
 */
public class GyrexSlf4jForwarder implements SynchronousLogListener, LogFilter {

	final class LogBufferFlush implements Runnable {
		@Override
		public void run() {
			// spin the loop (as long as there is a logger and the buffer is not empty)
			while (!closed.get() && !logBuffer.isEmpty()) {
				final SLF4JLogger logger = activeLogger.get();
				if (null == logger) {
					return;
				}

				final LogEntry entry = logBuffer.poll();
				if (null == entry) {
					return;
				}

				logger.log(entry);
			}
		}
	}

	final static class SLF4JLogger {
		static final int TRACE_INT = 00;
		static final int DEBUG_INT = 10;
		static final int INFO_INT = 20;
		static final int WARN_INT = 30;
		static final int ERROR_INT = 40;

		private static Method findLogMethod(final Object logger) {
			// find log method from org.slf4j.spi.LocationAwareLogger
			final Method[] methods = logger.getClass().getMethods();
			for (final Method method : methods) {
				final Class<?>[] parameterTypes = method.getParameterTypes();
				// public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t);
				if ("log".equals(method.getName()) && (parameterTypes.length == 6) && parameterTypes[1].equals(String.class) && parameterTypes[2].equals(Integer.TYPE) && parameterTypes[3].equals(String.class) && parameterTypes[5].equals(Throwable.class)) {
					return method;
				}
			}
			return null;
		}

		private final Object logger;
		private final Method logMethod;

		public SLF4JLogger(final Object logger) {
			this.logger = logger;
			logMethod = findLogMethod(logger);
		}

		private Object getLevel(final LogEntry entry) {
			switch (entry.getLevel()) {
				case LogService.LOG_ERROR:
					return ERROR_INT;
				case LogService.LOG_WARNING:
					return WARN_INT;
				case LogService.LOG_INFO:
					return INFO_INT;
				case LogService.LOG_DEBUG:
				default:
					return DEBUG_INT;
			}
		}

		public void log(final LogEntry entry) {
			if (null == logMethod) {
				return;
			}
			try {
				// public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t);
				logMethod.invoke(logger, null, "org.eclipse.osgi.framework.log.FrameworkLogEntry", getLevel(entry), getMessage(entry), null, entry.getException());
			} catch (final Throwable e) {
				// give up
				e.printStackTrace();
			}
		}
	}

	private static final int DEFAULT_CAPACITY = 200;
	static final String EQUINOX_LOGGER_NAME = "org.eclipse.equinox.logger";
	static final String NEWLINE = System.getProperty("line.separator");

	private static void addServiceProperty(final ServiceReference sr, final String key, final StringBuilder serviceInfo) {
		final Object value = sr.getProperty(key);
		if (null != value) {
			if (serviceInfo.length() > 0) {
				serviceInfo.append(", ");
			}

			serviceInfo.append(key).append(": ");
			if (value.getClass().isArray()) {
				final Object[] values = (Object[]) value;
				String separator = "";
				for (final Object val : values) {
					serviceInfo.append(separator).append(val);
					separator = ",";
				}
			} else {
				serviceInfo.append(value);
			}
		}
	}

	static String getLevel(final LogEntry entry) {
		switch (entry.getLevel()) {
			case LogService.LOG_ERROR:
				return "ERROR";
			case LogService.LOG_WARNING:
				return "WARNING";
			case LogService.LOG_INFO:
				return "INFO";
			case LogService.LOG_DEBUG:
			default:
				return "DEBUG";
		}
	}

	static String getMessage(final LogEntry entry) {
		final ServiceReference sr = entry.getServiceReference();
		if (null != sr) {
			final StringBuilder serviceInfo = new StringBuilder(400);

			addServiceProperty(sr, Constants.SERVICE_PID, serviceInfo);
			addServiceProperty(sr, Constants.SERVICE_ID, serviceInfo);
			addServiceProperty(sr, Constants.SERVICE_VENDOR, serviceInfo);
			addServiceProperty(sr, Constants.OBJECTCLASS, serviceInfo);

			return String.format("%s - %s", entry.getMessage(), serviceInfo.toString());
		}

		final Bundle bundle = entry.getBundle();
		if ((null != bundle) && (bundle.getBundleId() != 0L)) {
			return String.format("%s - %s, id: %d", entry.getMessage(), bundle.getSymbolicName(), bundle.getBundleId());
		}

		return entry.getMessage();
	}

	static PrintStream getStream(final LogEntry entry) {
		switch (entry.getLevel()) {
			case LogService.LOG_ERROR:
				return System.err;
			case LogService.LOG_DEBUG:
				return EclipseStarter.debug ? System.out : null;
			default:
				return System.out;
		}
	}

	final AtomicBoolean closed = new AtomicBoolean(false);
	final AtomicReference<SLF4JLogger> activeLogger = new AtomicReference<SLF4JLogger>();
	final BlockingQueue<LogEntry> logBuffer;
	final ExecutorService logDispatcher;
	private final AtomicBoolean capacityWarningPrinted = new AtomicBoolean(false);

	/**
	 * Creates a new instance.
	 * 
	 * @param bufferSize
	 * @param fallbackToSysout
	 */
	public GyrexSlf4jForwarder(final int bufferSize, final boolean fallbackToSysout) {
		if (!fallbackToSysout) {
			logBuffer = new LinkedBlockingQueue<LogEntry>(bufferSize > 0 ? bufferSize : DEFAULT_CAPACITY);
			logDispatcher = Executors.newSingleThreadExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(final Runnable r) {
					final Thread thread = new Thread(r, "Gyrex FrameworkLog Buffer Dispatcher");
					thread.setDaemon(true);
					return thread;
				}
			});
		} else {
			logBuffer = null;
			logDispatcher = null;
		}
	}

	/**
	 * Closes the forwarder
	 */
	public void close() {
		if (closed.compareAndSet(false, true)) {
			shutdown();
		}
	}

	@Override
	public boolean isLoggable(final Bundle bundle, final String loggerName, final int logLevel) {
		// we only forward messaged from the Eclipse logger of the default log service
		if ((loggerName != null) && !EQUINOX_LOGGER_NAME.equals(loggerName)) {
			return false;
		}

		// check the log level
		switch (logLevel) {
			case LogService.LOG_WARNING:
			case LogService.LOG_ERROR:
				return true;

			case LogService.LOG_INFO:
			case LogService.LOG_DEBUG:
			default:
				return EclipseStarter.debug;
		}
	}

	@Override
	public void logged(final LogEntry entry) {
		if ((null != entry) && !closed.get()) {
			// log directly if logger is available, otherwise add to buffer
			final SLF4JLogger logger = activeLogger.get();
			if (null != logger) {
				logger.log(entry);
			} else if (logBuffer == null) {
				final PrintStream stream = getStream(entry);
				if (null != stream) {
					// we try to simulate the pattern used in LogbackConfigurator
					stream.printf("%1$tH:%1$tM:%1$tS.%1$tL [%2$s] %3$-5s %4$s - %5$s%n", new Date(entry.getTime()), Thread.currentThread().getName(), getLevel(entry), EQUINOX_LOGGER_NAME, getMessage(entry));
				}
			} else if (!logBuffer.offer(entry)) {
				if (EclipseStarter.debug && !capacityWarningPrinted.get() && capacityWarningPrinted.compareAndSet(false, true)) {
					System.err.printf("[Eclipse Gyrex] Log buffer capacity limit reached. Some framework log messages will be discarded. Try increasing the buffer size (system property '%s') or enable fallback to STDOUT (system property '%s').%n", FrameworkLogAdapterHook.PROP_BUFFER_SIZE, FrameworkLogAdapterHook.PROP_FALLBACK_TO_SYSOUT);
				}
			}
		}
	}

	public void setSLF4JLogger(final Object logger) {
		// create and set logger
		activeLogger.set(null != logger ? new SLF4JLogger(logger) : null);

		// flush buffer (every time a logger is set)
		if (logDispatcher != null) {
			logDispatcher.execute(new LogBufferFlush());
		}
	}

	private void shutdown() {
		// shutdown executor
		if (logDispatcher != null) {
			logDispatcher.shutdownNow();
		}

		// clear all buffers
		if (logBuffer != null) {
			logBuffer.clear();
		}
	}
}
