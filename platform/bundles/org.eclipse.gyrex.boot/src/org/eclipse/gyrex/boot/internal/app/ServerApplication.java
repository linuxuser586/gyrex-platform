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
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.osgi.service.datalocation.Location;

import org.osgi.framework.Bundle;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server application which starts Gyrex.
 */
public class ServerApplication implements IApplication {

	/** the stop or restart signal */
	private static CountDownLatch stopOrRestartSignal;

	/** a flag indicating if the application should restart upon shutdown */
	private static boolean relaunch;

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = new Integer(1);

	private static final Logger LOG = LoggerFactory.getLogger(ServerApplication.class);

	/**
	 * Signals a restart.
	 */
	public static void signalRelaunch() {
		relaunch = true;
		if (AppDebug.debug) {
			LOG.debug("Relaunch request received!");
		}
		final CountDownLatch signal = stopOrRestartSignal;
		if (null != signal) {
			signal.countDown();
		}
	}

	/**
	 * Signals a shutdown.
	 */
	public static void signalShutdown() {
		relaunch = false;
		if (AppDebug.debug) {
			LOG.debug("Shutdown request received!");
		}
		final CountDownLatch signal = stopOrRestartSignal;
		if (null != signal) {
			signal.countDown();
		}
	}

	private void bootstrap() throws Exception {
		if (AppDebug.debug) {
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
		if ((instanceLocation == null) || !instanceLocation.isSet()) {
			System.err.println("Gyrex needs a valid workspace. Please start with the -data option pointing to a valid file system path.");
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
				System.err.println("Could not launch the server because the associated workspace '" + workspaceDirectory.getAbsolutePath() + "' is currently in use by another Eclipse application.");
			} else {
				System.err.println("Could not launch the server because the specified workspace cannot be created. The specified workspace directory '" + workspaceDirectory.getAbsolutePath() + "' is either invalid or read-only.");
			}
		} catch (final IOException e) {
			System.err.println("Error verifying the specified workspace directory. " + e.getMessage());
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
	private String[] getEnabledServerRoles(final String[] arguments) {
		// scan arguments for submitted roles
		boolean ignoreConfiguredRoles = false;
		final List<String> roles = new ArrayList<String>();
		for (int i = 0; i < arguments.length; i++) {
			final String arg = arguments[i];
			if ("-ignoreConfiguredRoles".equalsIgnoreCase(arg)) {
				ignoreConfiguredRoles = true;
			} else if ("-roles".equalsIgnoreCase(arg)) {
				if (++i >= arguments.length) {
					throw new IllegalArgumentException("The argument '-roles' requires a following argument with the server roles to start.");
				}
				final String[] specifiedRoles = StringUtils.split(arguments[i], ',');
				if ((null == specifiedRoles) || (specifiedRoles.length == 0)) {
					throw new IllegalArgumentException("The specified server roles could not be identified. Please specify at least one role. You may specify multiple rows using a comma separated list.");
				}
				for (final String role : specifiedRoles) {
					if (StringUtils.isNotBlank(role)) {
						if (!roles.contains(role)) {
							if (AppDebug.debugRoles) {
								LOG.debug("Role submitted via command line: " + role);
							}
							roles.add(role);
						}
					}
				}
			}
		}

		// read roles from preferences
		if (!ignoreConfiguredRoles) {
			// TODO: introduce node specific preferences
			final String[] rolesToStart = StringUtils.split(new PlatformScope().getNode(AppActivator.PLUGIN_ID).get("rolesToStart", null), ',');
			if (null != rolesToStart) {
				for (final String role : rolesToStart) {
					if (StringUtils.isNotBlank(role)) {
						if (!roles.contains(role)) {
							if (AppDebug.debugRoles) {
								LOG.debug("Configured role: " + role);
							}
							roles.add(role);
						}
					}
				}
			}
		} else {
			if (AppDebug.debugRoles) {
				LOG.debug("Ignoring configured roles.");
			}
		}

		// add default start roles
		if (PlatformConfiguration.isOperatingInDevelopmentMode()) {
			final String[] defaultRoles = ServerRolesRegistry.getDefault().getRolesToStartByDefaultInDevelopmentMode();
			for (final String role : defaultRoles) {
				if (!roles.contains(role)) {
					if (AppDebug.debugRoles) {
						LOG.debug("Default start role: " + role);
					}
					roles.add(role);
				}
			}
		}

		return roles.toArray(new String[roles.size()]);
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

		final Location instanceLocation = AppActivator.getInstance().getInstanceLocation();
		try {
			// check the instance location
			if (!checkInstanceLocation(instanceLocation)) {
				return EXIT_ERROR;
			}

			// configure logging
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

			// relaunch flag
			boolean relaunch = false;

			try {
				if (AppDebug.debug) {
					LOG.debug("Starting platform...");
				}

				// bootstrap the platform
				bootstrap();

				// read enabled server roles from configuration
				final String[] roles = getEnabledServerRoles(arguments);

				// activate server roles
				for (final String roleName : roles) {
					final ServerRole role = ServerRolesRegistry.getDefault().getRole(roleName);
					if (null == role) {
						// TODO log unknown role
						continue;
					}
					role.activate();
				}

				// signal that we are now up and running
				context.applicationRunning();

				if (AppDebug.debug) {
					LOG.info("Platform started.");
				}

				// note, this application is configured to run only ONCE
				// thus, the following code is safe
				stopOrRestartSignal = new CountDownLatch(1);
				do {
					try {
						stopOrRestartSignal.await();
					} catch (final InterruptedException e) {
						// reset interrupted state
						Thread.currentThread().interrupt();
					}
				} while ((stopOrRestartSignal.getCount() > 0) && Thread.interrupted());
				stopOrRestartSignal = null;

				// get & reset relaunch flag
				relaunch = ServerApplication.relaunch;
				ServerApplication.relaunch = false;

				if (AppDebug.debug) {
					LOG.debug("Platform closed.");
				}

			} catch (final Exception e) {
				if (AppDebug.debug) {
					LOG.debug("Platform start failed!", e);
				}

				// TODO should evaluate and suggest solution to Ops
				LOG.error("Error while starting server: " + e.getMessage(), e);
				return EXIT_ERROR;
			}

			// de-configure logging
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

			return relaunch ? EXIT_RESTART : EXIT_OK;
		} finally {
			// release instance location lock
			if (null != instanceLocation) {
				instanceLocation.release();
			}
		}
	}

	@Override
	public void stop() {
		final CountDownLatch signal = stopOrRestartSignal;
		if (null != signal) {
			signal.countDown();
		}
	}

}
