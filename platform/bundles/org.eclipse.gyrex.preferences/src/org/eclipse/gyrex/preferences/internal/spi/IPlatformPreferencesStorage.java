/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.preferences.internal.spi;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

/**
 * Abstraction for platform preferences storage.
 * <p>
 * May be used to provide the real storage implementation (eg. LDAP, database,
 * whatever).
 * </p>
 */
public interface IPlatformPreferencesStorage {

	/**
	 * OSGi service name (value
	 * <code>org.eclipse.gyrex.preferences.internal.spi.IPlatformPreferencesStorage</code>
	 * )
	 */
	String SERVICE_NAME = IPlatformPreferencesStorage.class.getName();

	/**
	 * Returns the node that must be used for storing preferences
	 * 
	 * @return
	 */
	IEclipsePreferences getRoot();

}
