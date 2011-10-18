/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.common.internal.applications;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.slf4j.Logger;

/**
 * The default application implementation.
 * <p>
 * An application is an executable entry point into the Equinox world. This
 * common base class provides convenient access to helpers and implements
 * lifecycle practises followed in Gyrex.
 * </p>
 * <p>
 * This class should be subclassed by clients providing an application to Gyrex.
 * However, its usage is not mandatory.
 * </p>
 * 
 * @see IApplication
 */
public abstract class BaseApplication implements IApplication {

	/**
	 * Thrown by {@link BaseApplication#doStart(Map)} to indicate a start abort
	 * which should trigger an {@link BaseApplication#EXIT_ERROR} to be
	 * returned.
	 */
	public static class StartAbortedException extends RuntimeException {
		/** serialVersionUID */
		private static final long serialVersionUID = 4550058726369912198L;

	}

	/** shutdown timeout */
	private static final long SHUTDOWN_TIMEOUT = 60000L;

	/** Exit object indicating error termination */
	public static final Integer EXIT_ERROR = new Integer(1);

	/** the singleton stop signals */
	private static final WeakHashMap<Class<? extends BaseApplication>, CountDownLatch> singletonStopSignals = new WeakHashMap<Class<? extends BaseApplication>, CountDownLatch>();

	/**
	 * Convenience method to return {@link IApplicationContext#APPLICATION_ARGS}
	 * from the specified map.
	 * 
	 * @param arguments
	 *            the map with application arguments retrieved from
	 *            {@link IApplicationContext#getArguments()}
	 * @return the extracted <code>String[]</code>
	 *         {@link IApplicationContext#APPLICATION_ARGS}
	 * @throws IllegalStateException
	 *             if the {@link IApplicationContext#APPLICATION_ARGS} are
	 *             missing or of wrong type
	 */
	protected static final String[] getApplicationArguments(final Map arguments) throws IllegalStateException {
		final Object args = arguments.get(IApplicationContext.APPLICATION_ARGS);
		if (null == args) {
			throw new IllegalStateException("application arguments missing");
		}
		if (!(args instanceof String[])) {
			throw new IllegalStateException("application arguments of wrong type");
		}
		return (String[]) args;
	}

	/** enabled or disables debug logging */
	protected boolean debug = false;

	/**
	 * Allows the application to perform necessary cleanups on shutdown and/or
	 * failed/aborted start attempts.
	 * <p>
	 * The default implementation does nothing. Subclasses may override.
	 * </p>
	 */
	protected void doCleanup() {
		// empty
	}

	/**
	 * Starts the application.
	 * <p>
	 * This method is called by {@link #start(IApplicationContext)}.
	 * Implementors should do whatever is necessary to start the application.
	 * However, they must not block when the application is running but return.
	 * </p>
	 * 
	 * @param arguments
	 *            the map with application arguments retrieved from
	 *            {@link IApplicationContext#getArguments()}
	 */
	protected abstract void doStart(Map arguments) throws Exception;

	/**
	 * Stops the application.
	 * <p>
	 * This method is called when the application has been asked to
	 * {@link #stop()}. Implementors should do whatever is necessary to stop the
	 * application and release any resources.
	 * </p>
	 * <p>
	 * Applications can return any object they like. If an Integer is returned
	 * it is treated as the program exit code if Eclipse is exiting.
	 * </p>
	 * 
	 * @return the return value of the application
	 */
	protected abstract Object doStop();

	/**
	 * Returns the logger that may be used for logging purposes.
	 * 
	 * @return the application logger
	 */
	protected abstract Logger getLogger();

	/**
	 * Returns a human readable application name.
	 * 
	 * @return the name (may not be null)
	 */
	public String getName() {
		return getClass().getSimpleName();
	}

	private CountDownLatch getStopSignal() {
		synchronized (singletonStopSignals) {
			return singletonStopSignals.get(getClass());
		}
	}

	private boolean initialzeStopSignal(final CountDownLatch stopSignal) {
		synchronized (singletonStopSignals) {
			if (singletonStopSignals.containsKey(getClass())) {
				return false;
			}
			return null == singletonStopSignals.put(getClass(), stopSignal);
		}
	}

	/**
	 * Convenience method to indicate if the application is active.
	 * 
	 * @return <code>true</code> if active, <code>false</code> otherwise
	 */
	protected boolean isActive() {
		final CountDownLatch stopSignal = getStopSignal();
		return (stopSignal != null) && (stopSignal.getCount() > 0);
	}

	/**
	 * Called during start when the application has been started successfully.
	 * <p>
	 * The default implementation does nothing. Subclasses may override.
	 * </p>
	 * <p>
	 * Note, this method is called during {@link #start(IApplicationContext)}.
	 * Implementors must not block and return in timely manner. Otherwise the
	 * application will not react to any stop requests. Typically, work is
	 * performed asynchronously (for example, using Eclipse Jobs).
	 * </p>
	 * 
	 * @param arguments
	 *            the map with application arguments retrieved from
	 *            {@link IApplicationContext#getArguments()}
	 */
	protected void onApplicationStarted(final Map arguments) {
		// empty
	}

	/**
	 * Called during start before the application will be started.
	 * <p>
	 * The default implementation does nothing. Subclasses may override.
	 * </p>
	 * <p>
	 * Note, this method is called during {@link #start(IApplicationContext)}.
	 * Implementors must not block and return in timely manner. Otherwise the
	 * application will not start. Typically, this may be used to initialize
	 * some state in the instance location or perform some bootstrapping prior
	 * to starting the application. However, be aware that the application has
	 * not been started so essential application services won't be available.
	 * </p>
	 * 
	 * @param arguments
	 *            the map with application arguments retrieved from
	 *            {@link IApplicationContext#getArguments()}
	 */
	protected void onBeforeStart(final Map arguments) {
		// empty
	}

	@Override
	public final Object start(final IApplicationContext context) throws Exception {
		if (debug) {
			getLogger().debug("{} received start request.", getName(), new Exception(String.format("%s Shutdown Call Stack", getName())));
		}

		// set stop signal to enforce singleton style
		final CountDownLatch stopSignal = new CountDownLatch(1);
		if (!initialzeStopSignal(stopSignal)) {
			throw new IllegalStateException(String.format("%s already started!", getName()));
		}

		try {
			if (debug) {
				getLogger().debug("Starting {}....", getName());
			}

			// pre-start
			onBeforeStart(context.getArguments());

			// start application
			doStart(context.getArguments());

			// signal running
			context.applicationRunning();

			// log success
			if (debug) {
				getLogger().debug("{} started.", getName());
			}

			// inform other
			onApplicationStarted(context.getArguments());

			// wait for termination
			do {
				try {
					stopSignal.await();
				} catch (final InterruptedException e) {
					// reset interrupted state
					Thread.currentThread().interrupt();
				}
			} while ((stopSignal.getCount() > 0) && Thread.interrupted());

			// stop application
			if (debug) {
				getLogger().debug("Stopping {}....", getName());
			}
			return doStop();
		} catch (final StartAbortedException aborted) {
			return EXIT_ERROR;
		} finally {
			// perform application specific cleanup
			try {
				doCleanup();
			} catch (final Exception ignored) {
				// ignored: handle exception
			} finally {
				// reset stop signal to allow restarts
				// (at this point it can only be ours)
				unsetStopSignal();

				if (debug) {
					getLogger().debug("Signaling completed shutdown of {}...", getName());
				}

				// notify shutdown complete
				synchronized (stopSignal) {
					stopSignal.notifyAll();
				}
			}
		}
	}

	@Override
	public final void stop() {
		if (debug) {
			getLogger().debug("{} received shutdown request.", getName(), new Exception(String.format("%s Shutdown Call Stack", getName())));
		}

		// check if started
		final CountDownLatch signal = unsetStopSignal();
		if (null == signal) {
			throw new IllegalStateException(String.format("%s not started!", getName()));
		}

		// shutdown and wait
		// (note, do in synchronized to not miss the shutdown notify)
		// (it's also save to rely on wait because we are the only thread having the signal)
		synchronized (signal) {
			// signal shutdown
			if (debug) {
				getLogger().debug("Signaling shutdown for {}...", getName());
			}
			signal.countDown();

			// wait for shutdown complete
			try {
				if (debug) {
					getLogger().debug("Waiting for {} to shutdown...", getName());
				}
				signal.wait(SHUTDOWN_TIMEOUT);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			if (debug) {
				getLogger().debug("{} shutdown complete.", getName());
			}
		}
	}

	private CountDownLatch unsetStopSignal() {
		synchronized (singletonStopSignals) {
			return singletonStopSignals.remove(getClass());
		}
	}
}
