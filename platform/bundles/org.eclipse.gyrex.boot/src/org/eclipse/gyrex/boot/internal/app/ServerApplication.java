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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.internal.applications.BaseApplication;
import org.eclipse.gyrex.server.internal.roles.LocalRolesManager;
import org.eclipse.gyrex.server.internal.roles.ServerRoleDefaultStartOption;
import org.eclipse.gyrex.server.internal.roles.ServerRolesRegistry;

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
public class ServerApplication extends BaseApplication {

	/** LOG */
	private static final Logger LOG = LoggerFactory.getLogger(ServerApplication.class);

	/** shutdown hook */
	private static final Thread shutdownHook = new Thread("Shutdown Hook") {
		@Override
		public void run() {
			try {
				LOG.info("Shutting down...");
				shutdown(null);
			} catch (final Exception e) {
				// ignore
			}
		};
	};

	private static final AtomicReference<ServerApplication> singletonInstance = new AtomicReference<ServerApplication>();

	/**
	 * Indicates if the server is running.
	 * 
	 * @return <code>true</code> if running, <code>false</code> otherwise
	 */
	public static boolean isRunning() {
		final ServerApplication application = singletonInstance.get();
		return (null != application) && application.running;
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
	 * Restarts a running server.
	 */
	public static void restart() {
		final ServerApplication application = singletonInstance.get();
		if (null == application) {
			throw new IllegalStateException("Platform not started.");
		}

		if (BootDebug.debug) {
			LOG.debug("Relaunch request received!");
		}

		// set restart flag
		application.restart = true;
		application.shutdownReason = null;

		// stop application
		application.stop();
	}

	/**
	 * Shutdown a running server because of an error.
	 */
	public static void shutdown(final Throwable cause) {
		final ServerApplication application = singletonInstance.get();
		if (null == application) {
			throw new IllegalStateException("Platform not started.");
		}

		// don't restart
		application.restart = false;

		// set shutdown reason
		application.shutdownReason = cause;

		// stop application
		application.stop();
	}

	/** running state */
	private volatile boolean running;

	/** a flag indicating if the application should restart upon shutdown */
	private volatile boolean restart;

	/** optional shutdown reason */
	private volatile Throwable shutdownReason;

	/** framework log service */
	private ServiceRegistration frameworkLogServiceRegistration;

	/** the instance location */
	private Location instanceLocation;

	/**
	 * Creates a new instance.
	 */
	public ServerApplication() {
		debug = BootDebug.debug;
	}

	private void bootstrap() throws Exception {
		if (BootDebug.debug) {
			LOG.debug("Bootstrapping platform.");
		}

		// make sure that the declarative services are initialized (if available)
		final Bundle dsImplBundle = AppActivator.getInstance().getBundle("org.eclipse.equinox.ds");
		if (null != dsImplBundle) {
			dsImplBundle.start(Bundle.START_TRANSIENT);
		} else {
			printError("Bundle 'org.eclipse.equinox.ds' not available but may be required by parts of the system. Your system will not function properly.", null);
			throw new StartAbortedException();
		}
	}

	private void checkInstanceLocation(final Location instanceLocation) {
		// check if a valid location is set
		// note, just checking Location#isSet isn't enough, we need to check the full URL
		// we also allow a default to apply, thus #getURL is better then just #isSet to initialize with a default
		// (we might later set our own here if we want this)
		if ((instanceLocation == null) || (null == instanceLocation.getURL()) || !instanceLocation.getURL().toExternalForm().startsWith("file:") || instanceLocation.isReadOnly()) {
			printError("Gyrex needs a valid local instance location (aka. 'workspace'). Please start with the -data option pointing to a valid, writable file system path.", null);
			throw new StartAbortedException();
		}

		// lock the location
		try {
			if (instanceLocation.lock()) {
				// great
				return;
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
		throw new StartAbortedException();
	}

	@Override
	protected void doCleanup() {
		// reset running flag
		running = false;

		// release instance location lock
		if (null != instanceLocation) {
			instanceLocation.release();
			instanceLocation = null;
		}

		// reset shutdown reason
		shutdownReason = null;

		// remove shutdown hook
		try {
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
		} catch (final Exception e) {
			// ignore
		}
	}

	@Override
	protected void doStart(final Map arguments) throws Exception {
		final String[] args = getApplicationArguments(arguments);

		if (!singletonInstance.compareAndSet(null, this)) {
			throw new IllegalStateException("server application already running");
		}

		// get instance location
		try {
			instanceLocation = AppActivator.getInstance().getInstanceLocation();
		} catch (final Exception e) {
			printError("An error occurred reading the instance location (aka. 'workspace'). Please verify that the installation is correct and all required components are available.", e);
			throw new StartAbortedException();
		}

		// check the instance location
		checkInstanceLocation(instanceLocation);

		// configure logging
		loggingOn(args);

		try {
			// install shutdown hook
			Runtime.getRuntime().addShutdownHook(shutdownHook);

			// bootstrap the platform
			bootstrap();

			// set the platform running state early to allow server roles
			// use Platform#isRunning in their activation logic
			running = true;

			// read enabled server roles from configuration
			final List<String> roles = getEnabledServerRoles(args);

			// activate server roles
			LocalRolesManager.activateRoles(roles, true);
		} catch (final Exception e) {
			if (BootDebug.debug) {
				LOG.debug("Platform start failed!", e);
			}

			// de-configure logging
			loggingOff();

			// TODO should evaluate and suggest solution to Ops
			printError("Unable to start server. Please verify the installation is correct and all required components are available.", e);
			throw new StartAbortedException();
		}
	}

	@Override
	protected Object doStop() {
		// reset running flag
		running = false;

		// deactivate all roles
		try {
			LocalRolesManager.deactivateAllRoles();
		} catch (final Exception ignore) {
			// ignore
		}

		// de-configure logging
		loggingOff();

		// print error
		final Throwable reason = shutdownReason;
		if (reason != null) {
			printError("Server shutdown forced due to error in underlying system. Please verify the installation is correct and all required components are available. ", reason);
			return EXIT_ERROR;
		}

		// return result
		return restart ? EXIT_RESTART : EXIT_OK;
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
		boolean ignoreDefaultRoles = false;

		// scan arguments for submitted roles
		final List<String> roleIds = new ArrayList<String>();
		for (int i = 0; i < arguments.length; i++) {
			final String arg = arguments[i];
			if ("-roles".equalsIgnoreCase(arg)) {
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
							if (BootDebug.roles) {
								LOG.debug("Role submitted via command line: " + role);
							}
							roleIds.add(role);
						}
					}
				}
			}
		}

		// add default start roles
		if (!ignoreDefaultRoles) {
			final Collection<String> defaultRoles = ServerRolesRegistry.getDefault().getRolesToStartByDefault(ServerRoleDefaultStartOption.Trigger.ON_BOOT);
			for (final String role : defaultRoles) {
				if (!roleIds.contains(role)) {
					if (BootDebug.roles) {
						LOG.debug("Default start boot role: " + role);
					}
					roleIds.add(role);
				}
			}
		}

		return roleIds;

	}

	@Override
	protected Logger getLogger() {
		return LOG;
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

		// hook FrameworkLog with SLF4J forwarder
		// (note, we use strings here in order to not import those classes)
		frameworkLogServiceRegistration = AppActivator.getInstance().getServiceHelper().registerService(Logger.class.getName(), LoggerFactory.getLogger("org.eclipse.equinox.logger"), "Eclipse Gyrex", "SLF4J Equinox Framework Logger", "org.slf4j.Logger-org.eclipse.equinox.logger", null);
	}

}
