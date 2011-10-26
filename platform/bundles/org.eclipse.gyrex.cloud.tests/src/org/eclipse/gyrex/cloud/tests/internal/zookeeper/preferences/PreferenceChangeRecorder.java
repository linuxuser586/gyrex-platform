/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences;

import static junit.framework.Assert.assertNull;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

/**
 *
 */
public final class PreferenceChangeRecorder implements IPreferenceChangeListener {

	private final BlockingDeque<PreferenceChangeEvent> events = new LinkedBlockingDeque<IEclipsePreferences.PreferenceChangeEvent>();

	public void assertEmpty(final String message) {
		assertNull(message, peek());
	}

	public PreferenceChangeEvent peek() {
		return events.peek();
	}

	public PreferenceChangeEvent poll() {
		return events.poll();
	}

	public PreferenceChangeEvent poll(final int timeout) throws InterruptedException, TimeoutException {
		return events.poll(timeout, TimeUnit.MILLISECONDS);
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		events.add(event);
	}

	public void reset() {
		events.clear();
	}
}