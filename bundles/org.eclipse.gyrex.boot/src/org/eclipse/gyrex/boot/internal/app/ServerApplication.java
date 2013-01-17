/*******************************************************************************
 * Copyright (c) 2008, 2012 Gunnar Wagenknecht and others.
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

import org.eclipse.gyrex.boot.internal.BootActivator;
import org.eclipse.gyrex.boot.internal.BootDebug;
import org.eclipse.gyrex.boot.internal.jmx.JettyJmxConnector;
import org.eclipse.gyrex.common.internal.applications.BaseApplication;
import org.eclipse.gyrex.server.Platform;
import org.eclipse.gyrex.server.internal.roles.LocalRolesManager;
import org.eclipse.gyrex.server.internal.roles.ServerRoleDefaultStartOption;
import org.eclipse.gyrex.server.internal.roles.ServerRolesRegistry;

import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.UnhandledException;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server application which starts Gyrex.
 */
public class ServerApplication extends BaseApplication {

	private static final String BSN_EQUINOX_CONSOLE_SSH = "org.eclipse.equinox.console.ssh";
	private static final String BSN_EQUINOX_DS = "org.eclipse.equinox.ds";
	private static final Logger LOG = LoggerFactory.getLogger(ServerApplication.class);

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
	 * <p>
	 * This method returns <code>true</code> if the server finished starting and
	 * all boot roles were activated and the shutdown has not been initiated.
	 * </p>
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
			System.err.print("  ");
			System.err.println(WordUtils.wrap(cause.getMessage(), 70, String.format("%n  "), false));
			final List throwables = ExceptionUtils.getThrowableList(cause);
			if (throwables.size() > 1) {
				System.err.println();
				System.err.println("Caused by:");
				for (int i = 1; i < throwables.size(); i++) {
					System.err.print("  ");
					System.err.println(WordUtils.wrap(ExceptionUtils.getMessage((Throwable) throwables.get(i)), 70, String.format("%n  "), false));
				}
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

		// make sure that the declarative services are initialized
		startBundle(BSN_EQUINOX_DS, true);
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
			instanceLocation = BootActivator.getInstance().getInstanceLocation();
		} catch (final Exception e) {
			printError("An error occurred reading the instance location (aka. 'workspace'). Please verify that the installation is correct and all required components are available.", e);
			throw new StartAbortedException();
		}

		// check the instance location
		checkInstanceLocation(instanceLocation);

		try {
			// install shutdown hook
			Runtime.getRuntime().addShutdownHook(shutdownHook);

			// bootstrap the platform
			bootstrap();

			// enable jxm
			jmxOn();

			// start the Equinox SSH Console if available
			if (isConsoleEnabled()) {
				startConsole();
			}

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

		// shutdown JMX connector
		jmxOff();

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

	private boolean isConsoleEnabled() {
		// check framework arguments
		final String[] args = BootActivator.getEnvironmentInfo().getFrameworkArgs();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-console")) {
				if (((i + 1) < args.length) && args[i + 1].equals("none")) {
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	private void jmxOff() {
		try {
			JettyJmxConnector.stop();
		} catch (final ClassNotFoundException e) {
			LOG.debug("Jetty JMX is not available. Ignoring error during shutdown. ({})", e.getMessage());
		} catch (final LinkageError e) {
			LOG.debug("Jetty JMX is not available. Ignoring error during shutdown. ({})", e.getMessage());
		} catch (final Exception e) {
			LOG.warn("Error while stopping Jetty JMX. {}", ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private void jmxOn() {
		try {
			JettyJmxConnector.start();
		} catch (final ClassNotFoundException e) {
			LOG.warn("Jetty JMX is not available. Please configure JMX support manually. ({})", e.getMessage());
		} catch (final LinkageError e) {
			LOG.warn("Jetty JMX is not available. Please configure JMX support manually. ({})", e.getMessage());
		} catch (final Exception e) {
			throw new UnhandledException("An error occured while starting the embedded JMX server. Please verify the port/host configuration is correct and no other server is running. JMX can also be disabled by setting system property 'gyrex.jmxrmi.skip'.", e);
		}
	}

	private boolean startBundle(final String symbolicName, final boolean required) throws BundleException {
		final Bundle bundle = BootActivator.getInstance().getBundle(symbolicName);
		if (null != bundle) {
			bundle.start(Bundle.START_TRANSIENT);
			return true;
		} else if (required) {
			printError(String.format("Required bundle '%s' not available. Your system will not function properly.", symbolicName), null);
			throw new StartAbortedException();
		}
		return false;
	}

	private void startConsole() throws BundleException {
		// enable SSH console
		// TODO: might want to use ConfigAdmin?
		final EnvironmentInfo environmentInfo = BootActivator.getEnvironmentInfo();
		if (null == environmentInfo.getProperty("osgi.console.ssh")) {
			environmentInfo.setProperty("osgi.console.ssh", String.valueOf(Platform.getInstancePort(3122)));
		}
		if (startBundle(BSN_EQUINOX_CONSOLE_SSH, false)) {
			try {
				final Object authenticator = BootActivator.getInstance().getBundle().loadClass("org.eclipse.gyrex.boot.internal.ssh.InstanceLocationAuthorizedKeysFileAuthenticator").newInstance();
				BootActivator.getInstance().getServiceHelper().registerService("org.apache.sshd.server.PublickeyAuthenticator", authenticator, "Eclipse Gyrex", "Equionx SSH Console authorized_keys support for Gyrex.", null, Integer.MAX_VALUE);
			} catch (final ClassNotFoundException e) {
				// ignore
			} catch (final LinkageError e) {
				// ignore
			} catch (final Exception e) {
				// error (but do not fail)
				LOG.warn("Unable to register authorized_keys file support for Equinox SSH Console. ", e);
			}
		}
	}

}
