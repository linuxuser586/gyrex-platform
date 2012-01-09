/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.boot.internal.BootActivator;
import org.eclipse.gyrex.boot.internal.app.ServerApplication;

import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.datalocation.Location;

import org.osgi.framework.Bundle;

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
public final class Platform {

	/**
	 * Returns the location in the local file system of the server's working
	 * directory (also known as the instance data area).
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
	public static IPath getInstanceLocation() throws IllegalStateException {
		return BootActivator.getInstance().getInstanceLocationPath();
	}

	/**
	 * Returns the location in the local file system of the state area for the
	 * specified bundle.
	 * <p>
	 * Note, the bundle state area might not exist prior to this call. The
	 * bundle is responsible for creating it if necessary.
	 * </p>
	 * <p>
	 * The bundle state area is a file directory within the
	 * {@link #getInstanceLocation() server's instance location} where a bundle
	 * is free to create files. The content and structure of this area is
	 * defined by the bundle, and the particular bundle is solely responsible
	 * for any files it puts there. It is recommended for bundle preference
	 * settings and other configuration parameters.
	 * </p>
	 * <p>
	 * The returned location path is consistent across bundle versions, i.e. the
	 * same path will be returned for different bundle versions as long as their
	 * symbolic names are equal. Thus, it's the responsibility of the bundle to
	 * maintain content for different versions within that location.
	 * </p>
	 * 
	 * @param bundle
	 *            the bundle whose state location if returned
	 * @return a local file system path to the bundle's state location
	 * @throws IllegalStateException
	 *             if the server is not running properly or running without an
	 *             instance location
	 */
	public static IPath getStateLocation(final Bundle bundle) throws IllegalStateException {
		return BootActivator.getInstance().getStateLocation(bundle);
	}

	/**
	 * Indicates if the server is currently running in debug mode.
	 * <p>
	 * This relies on the underlying debug capabilities of the OSGi framework.
	 * Typically, the server is put in debug mode using the "-debug" command
	 * line argument.
	 * </p>
	 * <p>
	 * This method always returns <code>true</code> if
	 * {@link #inDevelopmentMode()} returns <code>true</code>, i.e. development
	 * mode automatically implies debug mode. In production, debug mode can be
	 * enabled individually, though.
	 * </p>
	 * 
	 * @return <code>true</code> if the server is running in debug mode,
	 *         <code>false</code> otherwise
	 */
	public static boolean inDebugMode() {
		return BootActivator.isDebugMode();
	}

	/**
	 * Indicates if the server is currently running in development mode.
	 * <p>
	 * Gyrex uses the concept of configuration modes to behave differently in a
	 * secure production environment and in a relaxed development environment.
	 * </p>
	 * <p>
	 * Note, although Gyrex is a dynamic platform, the configuration mode is
	 * static information. It is not anticipated that the platform changes its
	 * configuration mode. Once set it should be <strong>assumed for
	 * lifetime</strong>. A new installation has to be made to rebuild a system
	 * using a different configuration mode. Security and a clean environment
	 * are some of the reasons for this strict decision.
	 * </p>
	 * <p>
	 * By default a system operates in development mode. It must be configured
	 * explicitly to not operate in development mode.
	 * </p>
	 * 
	 * @return <code>true</code> if the server is running in development mode,
	 *         <code>false</code> otherwise
	 */
	public static boolean inDevelopmentMode() {
		return BootActivator.isDevMode();
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

	/**
	 * Hidden constructor.
	 */
	private Platform() {
		// empty
	}
}
