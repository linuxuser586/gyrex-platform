/*******************************************************************************
 * Copyright (c) 2009, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.storage;

import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;
import org.eclipse.gyrex.preferences.internal.util.EclipsePreferencesUtil;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.math.NumberUtils;

/**
 * Repository preferences implementation.
 */
public final class RepositoryPreferences implements IRepositoryPreferences {

	/** eclipsePreferences */
	private final IEclipsePreferences prefs;

	/**
	 * Creates a new instance.
	 * 
	 * @param prefs
	 */
	public RepositoryPreferences(final IEclipsePreferences prefs) {
		this.prefs = prefs;
	}

	@Override
	public void flush() throws BackingStoreException {
		prefs.flush();
	}

	@Override
	public String get(final String key, final String defaultValue) {
		// decode path
		final String[] decodePath = EclipsePreferencesUtil.decodePath(key);

		// get node
		// REMINDER: all paths must be interpreted relative to the repository node!!!
		// (IEclipsePreferences interprets absolute paths as relative to the ROOT)
		final String path = decodePath[0];
		final IEclipsePreferences node = (IEclipsePreferences) (path == null ? prefs : prefs.node(EclipsePreferencesUtil.makeRelative(path)));

		// lookup key
		return node.get(decodePath[1], defaultValue);
	}

	@Override
	public boolean getBoolean(final String key, final boolean defaultValue) {
		final String result = get(key, null);
		return result == null ? defaultValue : Boolean.valueOf(result).booleanValue();
	}

	@Override
	public String[] getChildrenNames(final String path) throws BackingStoreException, SecurityException, IllegalStateException {
		// REMINDER: all paths must be interpreted relative to the repository node!!!
		// (IEclipsePreferences interprets absolute paths as relative to the ROOT)
		return prefs.node(EclipsePreferencesUtil.makeRelative(path)).childrenNames();
	}

	@Override
	public float getFloat(final String key, final float defaultValue) {
		final String value = get(key, null);
		return NumberUtils.toFloat(value, defaultValue);
	}

	@Override
	public int getInt(final String key, final int defaultValue) {
		final String value = get(key, null);
		return NumberUtils.toInt(value, defaultValue);
	}

	@Override
	public String[] getKeys() throws BackingStoreException, SecurityException, IllegalStateException {
		return prefs.keys();
	}

	@Override
	public void put(final String key, final String value, final boolean encrypt) throws SecurityException {
		// decode path
		final String[] decodePath = EclipsePreferencesUtil.decodePath(key);

		// get node
		// REMINDER: all paths must be interpreted relative to the repository node!!!
		// (IEclipsePreferences interprets absolute paths as relative to the ROOT)
		final String path = decodePath[0];
		final IEclipsePreferences node = (IEclipsePreferences) (path == null ? prefs : prefs.node(EclipsePreferencesUtil.makeRelative(path)));

		// put key
		node.put(decodePath[1], value);
	}

	@Override
	public void putBoolean(final String key, final boolean value, final boolean encrypt) {
		put(key, Boolean.toString(value), encrypt);
	}

	@Override
	public void putFloat(final String key, final float value, final boolean encrypt) {
		put(key, Float.toString(value), encrypt);
	}

	@Override
	public void putInt(final String key, final int value, final boolean encrypt) throws SecurityException {
		put(key, Integer.toString(value), encrypt);
	}

	@Override
	public void remove(final String key) {
		// decode path
		final String[] decodePath = EclipsePreferencesUtil.decodePath(key);

		// get node
		// REMINDER: all paths must be interpreted relative to the repository node!!!
		// (IEclipsePreferences interprets absolute paths as relative to the ROOT)
		final String path = decodePath[0];
		final IEclipsePreferences node = (IEclipsePreferences) (path == null ? prefs : prefs.node(EclipsePreferencesUtil.makeRelative(path)));

		// remove key
		node.remove(key);
	}

	@Override
	public void sync() throws BackingStoreException {
		prefs.sync();
	}

}