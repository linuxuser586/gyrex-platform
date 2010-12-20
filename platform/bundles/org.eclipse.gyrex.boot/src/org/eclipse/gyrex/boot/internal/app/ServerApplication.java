/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.server.internal.roles.LocalRolesManager;
import org.eclipse.gyrex.server.internal.roles.ServerRolesRegistry;
import org.eclipse.gyrex.server.internal.roles.ServerRolesRegistry.Trigger;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.service.datalocation.Location;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server application which starts Gyrex.
 */
public class ServerApplication implements IApplication {

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = new Integer(1);

	/** LOG */
	private static final Logger LOG = LoggerFactory.getLogger(ServerApplication.class);

	/** running state */
	private static final AtomicBoolean running = new AtomicBoolean();

	/** the stop or restart signal */
	private static final AtomicReference<CountDownLatch> stopOrRestartSignalRef = new AtomicReference<CountDownLatch>();

	/** a flag indicating if the application should restart upon shutdown */
	private static volatile boolean relaunch;

	/** optional shutdown reason */
	private static volatile Throwable shutdownReason;

	/**
	 * Indicates if the server is running.
	 * 
	 * @return <code>true</code> if running, <code>false</code> otherwise
	 */
	public static boolean isRunning() {
		return running.get();
	}

	private static void printError(final String message, final Throwable cause) {
		System.err.println();
		System.err.println();
		System.err.println(StringUtils.leftPad("", 72, '*'));
		System.err.println(StringUtils.center(" Server Startup Error ", 72));
		System.err.println(StringUtils.leftPad("", 72, '*'));
		System.err.println();
		System.err.println(WordUtils.wrap(message, 72));
		System.err.println();
		if (cause != null) {
			System.err.println("Reported error:");
			System.err.println(WordUtils.wrap(cause.getMessage(), 72));
			final Throwable root = ExceptionUtils.getRootCause(cause);
			if (root != null) {
				System.err.println();
				System.err.println("Caused by:");
				System.err.println(WordUtils.wrap(ExceptionUtils.getMessage(root), 72));
			}
			System.err.println();
		}
		System.err.println(StringUtils.leftPad("", 72, '*'));
		System.err.println();
		System.err.println();
	}

	/**
	 * Signals a restart.
	 */
	public static void signalRelaunch() {
		final CountDownLatch signal = stopOrRestartSignalRef.get();
		if (null == signal) {
			throw new IllegalStateException("Platform not started.");
		}

		if (BootDebug.debug) {
			LOG.debug("Relaunch request received!");
		}

		// set relaunch flag
		relaunch = true;

		// signal shutdown
		signal.countDown();
	}

	/**
	 * Signals a shutdown.
	 */
	public static void signalShutdown(final Throwable cause) {
		final CountDownLatch signal = stopOrRestartSignalRef.get();
		if (null == signal) {
			throw new IllegalStateException("Platform not started.");
		}

		if (BootDebug.debug) {
			LOG.debug("Shutdown request received!");
		}

		// set shutdown flag
		relaunch = false;

		// set shutdown reason
		shutdownReason = cause;

		// signal shutdown
		signal.countDown();
	}

	private ServiceRegistration frameworkLogServiceRegistration;

	private void bootstrap() throws Exception {
		if (BootDebug.debug) {
			LOG.debug("Bootstrapping platform.");
		}

		// make sure that the configuration is initialized
		final Bundle csImplBundle = AppActivator.getInstance().getBundle("org.eclipse.gyrex.configuration.impl");
		if (null == csImplBundle) {
			throw new IllegalStateException("Bundle 'org.eclipse.gyrex.configuration.impl' is missing. Please check the installation");
		}
		csImplBundle.start(Bundle.START_TRANSIENT);

		// make sure that the declarative services are initialized (if available)
		final Bundle dsImplBundle = AppActivator.getInstance().getBundle("org.eclipse.equinox.ds");
		if (null != dsImplBundle) {
			dsImplBundle.start(Bundle.START_TRANSIENT);
		} else {
			// TODO consider failing startup
			LOG.warn("Bundle 'org.eclipse.equinox.ds' not available but may be required by parts of the system. Your system may not function properly.");
		}
	}

	private boolean checkInstanceLocation(final Location instanceLocation) {
		// check if a valid location is set
		// note, just checking Location#isSet isn't enough, we need to check the full URL
		// we also allow a default to apply, thus #getURL is better then just #isSet to initialize with a default
		// (we might later set our own here if we want this)
		if ((instanceLocation == null) || (null == instanceLocation.getURL()) || !instanceLocation.getURL().toExternalForm().startsWith("file:") || instanceLocation.isReadOnly()) {
			printError("Gyrex needs a valid local instance location (aka. 'workspace'). Please start with the -data option pointing to a valid, writable file system path.", null);
			return false;
		}

		// lock the location
		try {
			if (instanceLocation.lock()) {
				// great
				return true;
			}

			// we failed to create the directory.
			// Two possibilities:
			// 1. directory is already in use
			// 2. directory could not be created
			final File workspaceDirectory = new File(instanceLocation.getURL().getFile());
			if (workspaceDirectory.exists()) {
				printError("Could not launch the server because the associated workspace '" + workspaceDirectory.getAbsolutePath() + "' is currently in use by another Eclipse application.", null);
			} else {
				printError("Could not launch the server because the specified workspace cannot be created. The specified workspace directory '" + workspaceDirectory.getAbsolutePath() + "' is either invalid or read-only.", null);
			}
		} catch (final IOException e) {
			printError("Unable to verify the specified workspace directory " + instanceLocation.getURL().toExternalForm() + ".", e);
		}
		return false;

	}

	/**
	 * Reads the enabled server roles from the preferences as well as from the
	 * arguments passed when starting the server.
	 * <p>
	 * Possible arguments:
	 * <ul>
	 * <li><code>-roles &lt;role1,role2,...,roleN&gt;</code></li>
	 * <li><code>-ignoreConfiguredRoles</code></li>
	 * </ul>
	 * </p>
	 * 
	 * @param arguments
	 *            the arguments
	 * @return the enabled roles
	 */
	private List<String> getEnabledServerRoles(final String[] arguments) {
		boolean ignoreConfiguredRoles = false;
		boolean ignoreDefaultRoles = false;

		// scan arguments for submitted roles
		final List<String> roleIds = new ArrayList<String>();
		for (int i = 0; i < arguments.length; i++) {
			final String arg = arguments[i];
			if ("-ignoreConfiguredRoles".equalsIgnoreCase(arg)) {
				ignoreConfiguredRoles = true;
			} else if ("-roles".equalsIgnoreCase(arg)) {
				ignoreDefaultRoles = true;
				if (++i >= arguments.length) {
					throw new IllegalArgumentException("The argument '-roles' requires a following argument with the server roles to start.");
				}
				final String[] specifiedRoles = StringUtils.split(arguments[i], ',');
				if ((null == specifiedRoles) || (specifiedRoles.length == 0)) {
					throw new IllegalArgumentException("The specified server roles could not be identified. Please specify at least one role. You may specify multiple rows using a comma separated list.");
				}
				for (final String role : specifiedRoles) {
					if (StringUtils.isNotBlank(role)) {
						if (!roleIds.contains(role)) {
							if (BootDebug.debugRoles) {
								LOG.debug("Role submitted via command line: " + role);
							}
							roleIds.add(role);
						}
					}
				}
			}
		}

		// read roles from preferences
		if (!ignoreConfiguredRoles) {
			// note, we read from the instance scope here
			// it is assumed that an external entity properly
			// sets the role for this particular node
			final String[] rolesToStart = StringUtils.split(new InstanceScope().getNode(AppActivator.SYMBOLIC_NAME).get("rolesToStart", null), ',');
			if (null != rolesToStart) {
				ignoreDefaultRoles = true;
				for (final String role : rolesToStart) {
					if (StringUtils.isNotBlank(role)) {
						if (!roleIds.contains(role)) {
							if (BootDebug.debugRoles) {
								LOG.debug("Configured role: " + role);
							}
							roleIds.add(role);
						}
					}
				}
			}
		} else {
			if (BootDebug.debugRoles) {
				LOG.debug("Ignoring configured roles.");
			}
		}

		// add default start roles
		if (!ignoreDefaultRoles) {
			final Collection<String> defaultRoles = ServerRolesRegistry.getDefault().getRolesToStartByDefault(Trigger.ON_BOOT);
			for (final String role : defaultRoles) {
				if (!roleIds.contains(role)) {
					if (BootDebug.debugRoles) {
						LOG.debug("Default start role: " + role);
					}
					roleIds.add(role);
				}
			}
		}

		return roleIds;

	}

	private void loggingOff() {
		// disable framework logging
		if (frameworkLogServiceRegistration != null) {
			frameworkLogServiceRegistration.unregister();
			frameworkLogServiceRegistration = null;
		}

		// reset logback
		try {
			LogbackConfigurator.reset();
		} catch (final ClassNotFoundException e) {
			// logback not available
		} catch (final NoClassDefFoundError e) {
			// logback not available
		} catch (final Exception e) {
			// error (but do not fail)
			LOG.warn("Error while de-configuring logback. Please re-configure logging manually. ({})", e.getMessage());
		}
	}

	private void loggingOn(final String[] arguments) {
		// configure logback
		try {
			LogbackConfigurator.configureDefaultContext(arguments);
		} catch (final ClassNotFoundException e) {
			// logback not available
			LOG.debug("Logback not available. Please configure logging manually. ({})", e.getMessage());
		} catch (final NoClassDefFoundError e) {
			// logback not available
			LOG.debug("Logback not available. Please configure logging manually. ({})", e.getMessage());
		} catch (final Exception e) {
			// error (but do not fail)
			LOG.warn("Error while configuring logback. Please configure logging manually. ({})", e);
		}

		// hook with GyrexFrameworkLog
		// (note, we use strings here in order to not import those classes)
		frameworkLogServiceRegistration = AppActivator.getInstance().getServiceHelper().registerService(Logger.class.getName(), LoggerFactory.getLogger("org.eclipse.osgi.framework.log.FrameworkLog"), "Eclipse Gyrex", "SLF4J Logger Factory", "org.slf4j.Logger.org.eclipse.osgi.framework.log.FrameworkLog", null);
	}

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		final Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		if (null == args) {
			throw new IllegalStateException("application arguments missing");
		}
		if (!(args instanceof String[])) {
			throw new IllegalStateException("application arguments of wrong type");
		}
		final String[] arguments = (String[]) args;

		if (isRunning()) {
			throw new IllegalStateException("server application already running");
		}

		Location instanceLocation = null;
		try {
			// get instance location
			try {
				instanceLocation = AppActivator.getInstance().getInstanceLocation();
			} catch (final Exception e) {
				printError("An error occurred reading the instance location (aka. 'workspace'). Please verify that the installation is correct and all required components are available.", e);
				return EXIT_ERROR;
			}

			// check the instance location
			if (!checkInstanceLocation(instanceLocation)) {
				return EXIT_ERROR;
			}

			// configure logging
			loggingOn(arguments);

			// relaunch flag
			boolean relaunch = false;

			try {
				if (BootDebug.debug) {
					LOG.debug("Starting platform...");
				}

				// note, this application is configured to run only ONCE
				// enforce early
				final CountDownLatch stopOrRestartSignal = new CountDownLatch(1);
				if (!stopOrRestartSignalRef.compareAndSet(null, stopOrRestartSignal)) {
					throw new IllegalStateException("Server application already started!");
				}

				// bootstrap the platform
				bootstrap();

				// set the platform running state early to allow server roles
				// use Platform#isRunning in their activation logic
				running.set(true);

				// read enabled server roles from configuration
				final List<String> roles = getEnabledServerRoles(arguments);

				// activate server roles
				LocalRolesManager.activateRoles(roles, true);

				// signal that we are now up and running
				context.applicationRunning();

				// log message
				if (BootDebug.debug) {
					LOG.info("Platform started.");
				}

				// wait for shutdown
				do {
					try {
						stopOrRestartSignal.await();
					} catch (final InterruptedException e) {
						// reset interrupted state
						Thread.currentThread().interrupt();
					}
				} while ((stopOrRestartSignal.getCount() > 0) && Thread.interrupted());

				// deactivate all roles
				LocalRolesManager.deactivateAllRoles();

				// get & reset relaunch flag
				relaunch = ServerApplication.relaunch;
				ServerApplication.relaunch = false;

				if (BootDebug.debug) {
					LOG.debug("Platform closed.");
				}

				// de-configure logging
				loggingOff();

				// print error
				final Throwable reason = shutdownReason;
				if (reason != null) {
					printError("Server shutdown forced due to error in underlying system. Please verify the installation is correct and all required components are available. ", reason);
					return EXIT_ERROR;
				}
			} catch (final Exception e) {
				if (BootDebug.debug) {
					LOG.debug("Platform start failed!", e);
				}

				// de-configure logging
				loggingOff();

				// TODO should evaluate and suggest solution to Ops
				printError("Unable to start server. Please verify the installation is correct and all required components are available.", e);
				return EXIT_ERROR;
			}

			return relaunch ? EXIT_RESTART : EXIT_OK;
		} finally {
			// reset running flag
			running.set(false);

			// release instance location lock
			if (null != instanceLocation) {
				instanceLocation.release();
			}

			// allow restart
			stopOrRestartSignalRef.set(null);

			// reset
			shutdownReason = null;
		}
	}

	@Override
	public void stop() {
		signalShutdown(null);
	}

}
