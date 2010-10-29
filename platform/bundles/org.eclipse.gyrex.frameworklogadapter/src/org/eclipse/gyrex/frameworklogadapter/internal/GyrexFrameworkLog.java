/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.log.frameworklogadapter.internal;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;

/**
 * The Gyrex framework log is responsible for routing all framework log messages
 * to the Gyrex log system.
 * <p>
 * The Gyrex log system is based on SLF4J. However, the adapter does not define
 * a strict dependency on SLF4J. Instead, it uses a mixture of OSGi services and
 * reflection to integrate with the Gyrex logging backend if it becomes
 * available. This allows the backend to come and go while the FrameworkLog
 * buffers massages in between.
 * </p>
 */
public class GyrexFrameworkLog implements FrameworkLog {

	final class Dispatcher implements Runnable {
		@Override
		public void run() {
			// spin the loop
			while (!Thread.currentThread().isInterrupted()) {
				// poll for entry
				FrameworkLogEntry entry = null;
				while ((null == entry) && !closed.get()) {
					try {
						entry = logBuffer.take();
					} catch (final InterruptedException e) {
						// reset interrupted state but keep polling
						Thread.currentThread().interrupt();
					}
				}

				// log entry
				if ((null != entry) && !closed.get()) {
					final SLF4JLogger logger = activeLogger.get();
					if (null != logger) {
						logger.log(entry);
					}
				}
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

		private Object getLevel(final FrameworkLogEntry entry) {
			switch (entry.getSeverity()) {
				case FrameworkLogEntry.CANCEL:
				case FrameworkLogEntry.ERROR:
					return Integer.toString(ERROR_INT);
				case FrameworkLogEntry.WARNING:
					return Integer.toString(WARN_INT);
				case FrameworkLogEntry.INFO:
					return Integer.toString(INFO_INT);

				case FrameworkLogEntry.OK:
				default:
					return Integer.toString(DEBUG_INT);
			}
		}

		public void log(final FrameworkLogEntry entry) {
			if (null != logMethod) {
				try {
					// public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t);
					logMethod.invoke(logger, null, "org.eclipse.osgi.framework.log.FrameworkLogEntry", getLevel(entry), entry.getMessage(), null, entry.getThrowable());
				} catch (final Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private static FrameworkLogEntry createFrameworkLogEntry(final FrameworkEvent frameworkEvent) {
		final Bundle b = frameworkEvent.getBundle();
		final Throwable t = frameworkEvent.getThrowable();
		final String entry = b.getSymbolicName() == null ? b.getLocation() : b.getSymbolicName();
		int severity;
		switch (frameworkEvent.getType()) {
			case FrameworkEvent.INFO:
				severity = FrameworkLogEntry.INFO;
				break;
			case FrameworkEvent.ERROR:
				severity = FrameworkLogEntry.ERROR;
				break;
			case FrameworkEvent.WARNING:
				severity = FrameworkLogEntry.WARNING;
				break;
			default:
				severity = FrameworkLogEntry.OK;
		}
		return new FrameworkLogEntry(entry, severity, 0, "", 0, t, null); //$NON-NLS-1$
	}

	static void logConsole(final FrameworkLogEntry logEntry, final int level) {
		final PrintStream out;
		final String severity;
		switch (logEntry.getSeverity()) {
			case FrameworkLogEntry.CANCEL:
				severity = "CANCEL";
				out = System.err;
				break;
			case FrameworkLogEntry.ERROR:
				severity = "ERROR";
				out = System.err;
				break;
			case FrameworkLogEntry.WARNING:
				severity = "WARNING";
				out = System.out;
				break;
			case FrameworkLogEntry.INFO:
				severity = "INFO";
				out = System.out;
				break;
			case FrameworkLogEntry.OK:
				severity = "OK";
				out = System.out;
				break;
			default:
				severity = "?";
				out = System.out;
				break;
		}

		final StringBuilder line = new StringBuilder(200);
		for (int i = 0; i < level; i++) {
			line.append("\t");
		}
		line.append('[').append(severity).append(']').append(' ');
		line.append('[').append(logEntry.getEntry()).append(']').append(' ');
		line.append('(').append(Thread.currentThread().getName()).append(')').append(' ');
		line.append(logEntry.getMessage());

		out.println(line.toString());

		final Throwable t = logEntry.getThrowable();
		if (null != t) {
			t.printStackTrace(out);
		}

		final FrameworkLogEntry[] children = logEntry.getChildren();
		if (null != children) {
			for (final FrameworkLogEntry subEntry : children) {
				logConsole(subEntry, level + 1);
			}
		}

	}

	final AtomicBoolean consoleLog = new AtomicBoolean(false);
	final AtomicBoolean closed = new AtomicBoolean(false);
	final BlockingQueue<FrameworkLogEntry> logBuffer = new LinkedBlockingQueue<FrameworkLogEntry>();

	final AtomicReference<SLF4JLogger> activeLogger = new AtomicReference<SLF4JLogger>();

	ExecutorService logDispatcher = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(final Runnable r) {
			final Thread thread = new Thread(r, "Gyrex FrameworkLog Dispatcher");
			thread.setDaemon(true);
			return thread;
		}
	});

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			shutdown();
		}
	}

	@Override
	public File getFile() {
		// not supported
		return null;
	}

	@Override
	public void log(final FrameworkEvent frameworkEvent) {
		if (!closed.get()) {
			log(createFrameworkLogEntry(frameworkEvent));
		}
	}

	@Override
	public void log(final FrameworkLogEntry logEntry) {
		if ((null != logEntry) && !closed.get()) {
			logBuffer.add(logEntry);
			if (consoleLog.get()) {
				logConsole(logEntry, 0);
			}
		}
	}

	@Override
	public void setConsoleLog(final boolean consoleLog) {
		this.consoleLog.set(consoleLog);
	}

	@Override
	public void setFile(final File newFile, final boolean append) throws IOException {
		// ignored
	}

	public void setSLF4JLogger(final Object logger) {
		activeLogger.set(null != logger ? new SLF4JLogger(logger) : null);
		if (null != logger) {
			startDispatcher();
		}
	}

	@Override
	public void setWriter(final Writer newWriter, final boolean append) {
		// ignored
	}

	private void shutdown() {
		// shutdown executor
		logDispatcher.shutdownNow();

		// clear all buffers
		logBuffer.clear();
	}

	private void startDispatcher() {
		if (!closed.get()) {
			try {
				logDispatcher.execute(new Dispatcher());
			} catch (final RejectedExecutionException e) {
				throw new IllegalStateException("inactive", e);
			}
		}
	}
}
