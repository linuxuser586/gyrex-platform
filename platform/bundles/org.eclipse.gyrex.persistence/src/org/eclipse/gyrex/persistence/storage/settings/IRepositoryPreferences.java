/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.settings;

import java.io.IOException;

import org.eclipse.equinox.security.storage.ISecurePreferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Repository preferences for storing repository configuration settings (such as
 * connection information).
 * <p>
 * Repository preferences provide convenient access to the node in the Eclipse
 * preferences hierarchy which a repository should use to store and read
 * preferences. The usage of the suggested nodes is encouraged. It helps
 * operators to locate the preferences more easily if they are stored together
 * in an LDAP server for example.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRepositoryPreferences {

	/**
	 * Convenience method to flush the underlying preferences.
	 * 
	 * @throws IOException
	 *             if error occurred while saving secure preferences
	 * @throws BackingStoreException
	 *             if an error occurred while saving the Eclipse preferences
	 * @see IEclipsePreferences#flush()
	 * @see ISecurePreferences#flush()
	 */
	public void flush() throws BackingStoreException, IOException;

	/**
	 * Returns the node in the Eclipse preferences hierarchy for reading and
	 * storing non-secure preferences.
	 * 
	 * @throws IllegalStateException
	 *             if exception occurred while accessing the preferences
	 * @see IEclipsePreferences
	 */
	public IEclipsePreferences getPreferences() throws IllegalStateException;

	/**
	 * Returns the node in the Eclipse preferences hierarchy for reading and
	 * storing secure preferences.
	 * 
	 * @throws IllegalStateException
	 *             if exception occurred while accessing the preferences
	 * @see ISecurePreferences
	 */
	public ISecurePreferences getSecurePreferences() throws IllegalStateException;
}
