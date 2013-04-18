/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *     Michael Soedel - fix for https://bugs.eclipse.org/405910
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.schedules;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.IScheduleEntry;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleManager;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleWorkingCopy;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * {@link IScheduleManager} which stores schedule in ZooKeeper.
 */
public class ScheduleManagerImpl implements IScheduleManager {

	public static final String SEPARATOR = "_";

	public static String getExternalId(final String internalId) {
		final int i = internalId.indexOf(SEPARATOR);
		if (i < 0)
			return internalId;
		return internalId.substring(i + 1);
	}

	private final IRuntimeContext context;
	private final String internalIdPrefix;

	/**
	 * Creates a new instance.
	 */
	@Inject
	public ScheduleManagerImpl(final IRuntimeContext context) {
		this.context = context;
		try {
			internalIdPrefix = DigestUtils.shaHex(context.getContextPath().toString().getBytes(CharEncoding.UTF_8)) + SEPARATOR;
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("Please use a JVM that supports UTF-8.");
		}
	}

	@Override
	public IScheduleWorkingCopy createSchedule(final String id) {
		if (!IdHelper.isValidId(id))
			throw new IllegalArgumentException("invalid id: " + id);

		final String internalId = toInternalId(id);
		try {
			return ScheduleStore.create(internalId, id, context);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}

	}

	@Override
	public IScheduleWorkingCopy createWorkingCopy(final String id) {
		if (!IdHelper.isValidId(id))
			throw new IllegalArgumentException("invalid id: " + id);

		final String internalId = toInternalId(id);
		try {
			return ScheduleStore.load(internalId, id, true);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public ISchedule getSchedule(final String id) {
		if (!IdHelper.isValidId(id))
			throw new IllegalArgumentException("invalid id: " + id);

		final String internalId = toInternalId(id);
		try {
			return ScheduleStore.load(internalId, id, false);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public Collection<String> getSchedules() {
		try {
			final String[] storageIds = ScheduleStore.getSchedules();
			final List<String> schedules = new ArrayList<String>(storageIds.length);
			for (final String internalId : storageIds) {
				if (internalId.startsWith(internalIdPrefix)) {
					schedules.add(toExternalId(internalId));
				}
			}
			return Collections.unmodifiableCollection(schedules);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public void removeSchedule(final String id) {
		if (!IdHelper.isValidId(id))
			throw new IllegalArgumentException("invalid id: " + id);

		final ISchedule schedule = getSchedule(id);
		if (schedule == null)
			throw new IllegalStateException(String.format("schedule '%s' does not exist", id));

		// remove jobs
		final IJobManager jobManager = context.get(IJobManager.class);
		for (final IScheduleEntry entry : schedule.getEntries()) {
			if (null != jobManager.getJob(entry.getJobId())) {
				jobManager.removeJob(entry.getJobId());
			}
		}

		final String internalId = toInternalId(id);
		try {
			ScheduleStore.remove(internalId, id);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	private String toExternalId(final String internalId) {
		return StringUtils.removeStart(internalId, internalIdPrefix);
	}

	private String toInternalId(final String id) {
		return internalIdPrefix.concat(id);
	}

	@Override
	public void updateSchedule(final IScheduleWorkingCopy copy) {
		if (!(copy instanceof ScheduleImpl))
			throw new IllegalArgumentException("invalid working copy, must be obtained from this manager");

		final ScheduleImpl scheduleImpl = (ScheduleImpl) copy;
		final String internalId = toInternalId(scheduleImpl.getId());
		try {
			ScheduleStore.flush(internalId, scheduleImpl);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Unable to access schedule store. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

}
