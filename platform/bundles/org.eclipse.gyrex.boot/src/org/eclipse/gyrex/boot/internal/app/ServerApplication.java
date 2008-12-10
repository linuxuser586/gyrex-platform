/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.boot.internal.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.StringUtils;
import org.eclipse.cloudfree.common.debug.BundleDebug;
import org.eclipse.cloudfree.configuration.PlatformConfiguration;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;

/**
 * The server application which starts the CloudFree Platform.
 */
public class ServerApplication implements IApplication {

	/** the stop or restart signal */
	private static CountDownLatch stopOrRestartSignal;

	/** a flag indicating if the application should restart upon shutdown */
	private static boolean relaunch;

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = new Integer(1);

	/**
	 * Sets the restart.
	 * 
	 * @param restart
	 *            the restart to set
	 */
	public static void signalRelaunch() {
		relaunch = true;
		final CountDownLatch signal = stopOrRestartSignal;
		if (null != signal) {
			signal.countDown();
		}
	}

	private void bootstrap() throws Exception {
		if (AppDebug.debug) {
			BundleDebug.debug("Bootstrapping platform...");
		}

		// make sure that the configuration is initialized
		final Bundle csImplBundle = AppActivator.getInstance().getBundle("org.eclipse.cloudfree.configuration.impl");
		if (null == csImplBundle) {
			throw new IllegalStateException("Bundle 'org.eclipse.cloudfree.configuration.impl' is missing. Please check the installation");
		}
		csImplBundle.start(Bundle.START_TRANSIENT);

		// make sure that the declarative services are initialized
		final Bundle dsImplBundle = AppActivator.getInstance().getBundle("org.eclipse.equinox.ds");
		if (null == dsImplBundle) {
			throw new IllegalStateException("Bundle 'org.eclipse.equinox.ds' is missing. Please check the installation");
		}
		dsImplBundle.start(Bundle.START_TRANSIENT);
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
								BundleDebug.debug("Role submitted via command line: " + role);
							}
							roles.add(role);
						}
					}
				}
			}
		}

		// read roles from preferences
		if (!ignoreConfiguredRoles) {
			final String[] rolesToStart = StringUtils.split(PlatformConfiguration.getConfigurationService().getString(AppActivator.PLUGIN_ID, "rolesToStart", null, null), ',');
			if (null != rolesToStart) {
				for (final String role : rolesToStart) {
					if (StringUtils.isNotBlank(role)) {
						if (!roles.contains(role)) {
							if (AppDebug.debugRoles) {
								BundleDebug.debug("Configured role: " + role);
							}
							roles.add(role);
						}
					}
				}
			}
		} else {
			if (AppDebug.debugRoles) {
				BundleDebug.debug("Ignoring configured roles.");
			}
		}

		// add default start roles
		if (PlatformConfiguration.isOperatingInDevelopmentMode()) {
			final String[] defaultRoles = ServerRolesRegistry.getDefault().getRolesToStartByDefaultInDevelopmentMode();
			for (final String role : defaultRoles) {
				if (!roles.contains(role)) {
					if (AppDebug.debugRoles) {
						BundleDebug.debug("Default start role: " + role);
					}
					roles.add(role);
				}
			}
		}

		return roles.toArray(new String[roles.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	@Override
	public Object start(final IApplicationContext context) throws Exception {
		final Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		if (null == args) {
			throw new IllegalStateException("application arguments missing");
		}
		if (!(args instanceof String[])) {
			throw new IllegalStateException("application arguments of wrong type");
		}

		if (AppDebug.debug) {
			BundleDebug.debug("Starting platform...");
		}

		// relaunch flag
		boolean relaunch = false;

		try {
			// bootstrap the platform
			bootstrap();

			final String[] arguments = (String[]) args;

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
				BundleDebug.debug("Platform started.");
			}

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
				BundleDebug.debug("Platform closed.");
			}

		} catch (final Exception e) {
			if (AppDebug.debug) {
				BundleDebug.debug("Platform start failed!", e);
			}

			// TODO should evaluate and suggest solution to Ops
			AppActivator.getInstance().getLog().log("Error while starting server: " + e.getMessage(), e);
			return EXIT_ERROR;
		}

		return relaunch ? EXIT_RELAUNCH : EXIT_OK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	@Override
	public void stop() {
		final CountDownLatch signal = stopOrRestartSignal;
		if (null != signal) {
			signal.countDown();
		}
	}

}
