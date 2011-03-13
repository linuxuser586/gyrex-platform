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
package org.eclipse.gyrex.p2.internal.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.packages.PackageDefinition;

import org.eclipse.core.runtime.IPath;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * Helper for working with package install state.
 */
public class PackageInstallState {

	private static final Lock installSessionIdLock = new ReentrantLock();

	public static String getActiveInstallSessionId() {
		final File sessionFile = getBaseLocation().append("sessionActive").toFile();
		installSessionIdLock.lock();
		try {
			return FileUtils.readFileToString(sessionFile, CharEncoding.UTF_8);
		} catch (final FileNotFoundException notFound) {
			return null;
		} catch (final IOException e) {
			throw new IllegalStateException("Unable to read active install session id. " + ExceptionUtils.getRootCauseMessage(e), e);
		} finally {
			installSessionIdLock.unlock();
		}
	}

	static IPath getBaseLocation() {
		return P2Activator.getInstance().getConfigLocation().append(P2Activator.SYMBOLIC_NAME);
	}

	private static IPath getPackageFile(final PackageDefinition packageDefinition) {
		return getBaseLocation().append("packages").append(packageDefinition.getId());
	}

	public static boolean isInstalled(final PackageDefinition packageDefinition) throws BackingStoreException {
		// TODO this is a hack, we need query this from the profile
		// we also cannot store this in the cloud as it might get out of sync on re-install
		return getPackageFile(packageDefinition).toFile().isFile();
	}

	public static void removeActiveInstallSessionId() {
		final File sessionFile = getBaseLocation().append("sessionActive").toFile();
		installSessionIdLock.lock();
		try {
			if (sessionFile.isFile() && !sessionFile.delete()) {
				throw new IllegalStateException("Unable to delete active install session id. " + sessionFile.getPath());
			}
		} finally {
			installSessionIdLock.unlock();
		}
	}

	public static void setActiveInstallSessionId(final String activeSessionId) {
		final File sessionFile = getBaseLocation().append("sessionActive").toFile();
		installSessionIdLock.lock();
		try {
			FileUtils.writeStringToFile(sessionFile, activeSessionId, CharEncoding.UTF_8);
		} catch (final IOException e) {
			throw new IllegalStateException("Unable to write active install session id. " + ExceptionUtils.getRootCauseMessage(e), e);
		} finally {
			installSessionIdLock.unlock();
		}
	}

	public static void setInstalled(final PackageDefinition packageDefinition) {
		try {
			final File file = getPackageFile(packageDefinition).toFile();
			FileUtils.forceMkdir(file.getParentFile());
			FileUtils.writeStringToFile(file, DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.format(new Date()), CharEncoding.UTF_8);
		} catch (final IOException e) {
			throw new IllegalStateException("Unable to mark package as installed. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	/**
	 * Creates a new instance.
	 */
	private PackageInstallState() {
		// empty
	}

}
