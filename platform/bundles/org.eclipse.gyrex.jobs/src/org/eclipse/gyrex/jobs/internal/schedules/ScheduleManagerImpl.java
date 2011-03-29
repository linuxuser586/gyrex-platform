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
package org.eclipse.gyrex.jobs.internal.schedules;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleManager;
import org.eclipse.gyrex.jobs.schedules.IScheduleWorkingCopy;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * {@link IScheduleManager} which stores schedule in ZooKeeper.
 */
public class ScheduleManagerImpl implements IScheduleManager {

	public static IEclipsePreferences getSchedulesNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(JobsActivator.SYMBOLIC_NAME).node("schedules");
	}

	@Override
	public IScheduleWorkingCopy createSchedule(final String id) {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id: " + id);
		}

		final Preferences schedulesNode = getSchedulesNode();

		try {
			if (schedulesNode.nodeExists(id)) {
				throw new IllegalStateException(String.format("schedule '%s' already exists", id));
			}

			return new ScheduleImpl(schedulesNode.node(id));
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}

	}

	@Override
	public IScheduleWorkingCopy createWorkingCopy(final String id) {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id: " + id);
		}

		final Preferences schedulesNode = getSchedulesNode();

		try {
			if (!schedulesNode.nodeExists(id)) {
				throw new IllegalStateException(String.format("schedule '%s' does not exist", id));
			}

			return new ScheduleImpl(schedulesNode.node(id));
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public ISchedule getSchedule(final String id) {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id: " + id);
		}

		final Preferences schedulesNode = getSchedulesNode();

		try {
			if (!schedulesNode.nodeExists(id)) {
				return null;
			}

			return new ScheduleImpl(schedulesNode.node(id));
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public Collection<String> getSchedules() {
		try {
			return Arrays.asList(getSchedulesNode().childrenNames());
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public void removeSchedule(final String id) {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id: " + id);
		}

		final Preferences schedulesNode = getSchedulesNode();

		try {
			if (!schedulesNode.nodeExists(id)) {
				throw new IllegalStateException(String.format("schedule '%s' does not exist", id));
			}

			schedulesNode.node(id).removeNode();
			schedulesNode.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public void updateSchedule(final IScheduleWorkingCopy copy) {
		if (!(copy instanceof ScheduleImpl)) {
			throw new IllegalArgumentException("invalid working copy, must be obtained from this manager");
		}

		try {
			((ScheduleImpl) copy).save();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

}
