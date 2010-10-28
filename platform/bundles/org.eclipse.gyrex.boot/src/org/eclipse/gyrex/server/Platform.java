/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.server;

import org.eclipse.gyrex.boot.internal.app.AppActivator;
import org.eclipse.gyrex.boot.internal.app.ServerApplication;

import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

/**
 * Central gate of the Gyrex Server Runtime.
 * <p>
 * This class is a convenience class. All functionality is provided by static
 * methods.
 * </p>
 * <p>
 * Most users don't have to worry about server's lifecycle. However, if your
 * code can call methods of this class when the server is not running, it
 * becomes necessary to check {@link #isRunning()} before making the call. A
 * runtime exception might be thrown or incorrect result might be returned if a
 * method from this class is called while Platform is not running.
 * </p>
 */
public class Platform {

	/**
	 * Returns a path to a data area for the specified path within
	 * {@link #getInstanceLocationPath() the instance location}.
	 * <p>
	 * The path may not exist yet. It is the responsibility of the client to
	 * create the content of the data area returned if it does not exist.
	 * </p>
	 * <p>
	 * This method can be used to obtain a private area within the instance
	 * location. For example use the symbolic name of a bundle to obtain a data
	 * area specific to that bundle.
	 * </p>
	 * 
	 * @param path
	 *            the name of the path to get the data area for
	 * @return the path to the instance data area with the specified path
	 * @throws IllegalStateException
	 *             if the server is not running properly or running without an
	 *             instance location
	 */
	public static IPath getInstanceDataAreaPath(final String path) throws IllegalStateException {
		return AppActivator.getInstance().getInstanceDataAreaPath(path);
	}

	/**
	 * Returns the path to location of the server's working directory (also
	 * known as the instance data area).
	 * <p>
	 * This method is equivalent to acquiring the
	 * <code>org.eclipse.osgi.service.datalocation.Location</code> service using
	 * filter {@link Location#INSTANCE_FILTER}, verifying that
	 * {@link Location#getURL() the location url} points to a path within the
	 * local file system and converting the url to a {@link IPath file system
	 * path}.
	 * </p>
	 * 
	 * @return path to the location of the servers's instance data area
	 * @see Location#INSTANCE_FILTER
	 * @throws IllegalStateException
	 *             if the server is not running properly or running without an
	 *             instance location
	 */
	public static IPath getInstanceLocationPath() throws IllegalStateException {
		return AppActivator.getInstance().getInstanceLocationPath();
	}

	/**
	 * Indicates if the server is currently running in debug mode.
	 * <p>
	 * This relies on the underlying debug capabilities of the OSGi framework.
	 * Typically, the server is put in debug mode using the "-debug" command
	 * line argument.
	 * </p>
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and
	 * query it to see if they are in debug mode.
	 * </p>
	 * 
	 * @return <code>true</code> if the server is running in debug mode,
	 *         <code>false</code> otherwise
	 */
	public static boolean inDebugMode() {
		return AppActivator.isDebugMode();
	}

	/**
	 * Indicates if the server is currently running in development mode.
	 * <p>
	 * This relies on the underlying debug capabilities of the OSGi framework.
	 * Typically, the server is put in development mode using the "-dev" command
	 * line argument.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and
	 * query it to see if they are in development mode.
	 * </p>
	 * 
	 * @return <code>true</code> if the server is running in development mode,
	 *         <code>false</code> otherwise
	 */
	public static boolean inDevelopmentMode() {
		return AppActivator.isDevMode();
	}

	/**
	 * Indicates if the server is running.
	 * 
	 * @return <code>true</code> if the server is running, and
	 *         <code>false</code> otherwise
	 */
	public static boolean isRunning() {
		return ServerApplication.isRunning();
	}
}
