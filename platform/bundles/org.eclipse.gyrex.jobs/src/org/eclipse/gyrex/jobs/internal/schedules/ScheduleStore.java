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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.schedules;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Store which stores schedules in cloud preferences.
 */
public class ScheduleStore {

	public static ScheduleImpl create(final String storageId, final String scheduleId, final IRuntimeContext context) throws BackingStoreException {
		final IEclipsePreferences schedulesNode = ScheduleStore.getSchedulesNode();

		if (schedulesNode.nodeExists(storageId)) {
			throw new IllegalStateException(String.format("schedule '%s' already exists", scheduleId));
		}

		final ScheduleImpl scheduleImpl = new ScheduleImpl(scheduleId, schedulesNode.node(storageId));
		scheduleImpl.setContextPath(context.getContextPath());
		scheduleImpl.save();

		return scheduleImpl;
	}

	public static void flush(final String storageId, final ScheduleImpl scheduleImpl) throws BackingStoreException {
		final Preferences schedulesNode = getSchedulesNode();
		if (!schedulesNode.nodeExists(storageId)) {
			throw new IllegalStateException(String.format("schedule node '%s' does not exist", scheduleImpl.getId()));
		}

		scheduleImpl.save();
	}

	public static String[] getSchedules() throws BackingStoreException {
		return getSchedulesNode().childrenNames();
	}

	public static IEclipsePreferences getSchedulesNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(JobsActivator.SYMBOLIC_NAME).node("schedules");
	}

	public static ScheduleImpl load(final String storageId, final String scheduleId, final boolean failIfNotExist) throws BackingStoreException {
		final Preferences schedulesNode = ScheduleStore.getSchedulesNode();
		if (!schedulesNode.nodeExists(storageId)) {
			if (failIfNotExist) {
				throw new IllegalStateException(String.format("schedule '%s' does not exist", scheduleId));
			}
			return null;
		}

		return new ScheduleImpl(scheduleId, schedulesNode.node(storageId)).load();
	}

	public static void remove(final String storageId, final String scheduleId) throws BackingStoreException {
		final Preferences schedulesNode = getSchedulesNode();
		if (!schedulesNode.nodeExists(storageId)) {
			throw new IllegalStateException(String.format("schedule node '%s' does not exist", scheduleId));
		}

		schedulesNode.node(storageId).removeNode();
		schedulesNode.flush();
	}

}
