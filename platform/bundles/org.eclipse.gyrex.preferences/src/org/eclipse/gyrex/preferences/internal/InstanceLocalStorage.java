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
package org.eclipse.gyrex.preferences.internal;

import org.eclipse.gyrex.preferences.internal.spi.IPlatformPreferencesStorage;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Default storage implementation that maintains platform preferences locally.
 */
public class InstanceLocalStorage implements IPlatformPreferencesStorage {

	private final IEclipsePreferences storageRoot;

	/**
	 * Creates a new instance.
	 */
	public InstanceLocalStorage() {
		storageRoot = (IEclipsePreferences) PreferencesActivator.getInstance().getPreferencesService().getRootNode().node(InstanceScope.SCOPE).node(PreferencesActivator.SYMBOLIC_NAME);
	}

	@Override
	public IEclipsePreferences getRoot() {
		return storageRoot;
	}
}
