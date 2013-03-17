/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.schedules;

import java.util.List;
import java.util.TimeZone;

import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleManager;
import org.eclipse.gyrex.jobs.service.IJobService;

/**
 * A schedule for executing a set of jobs based on triggers.
 * <p>
 * The Gyrex Job API is based on the creation of schedules. A schedule basically
 * groups a set of jobs together which usually belong together (eg. a schedule
 * for system jobs or a schedule for all jobs related to a tenant). Thus, a
 * schedule is essentially an execution plan for jobs.
 * </p>
 * <p>
 * However, a schedule does executes jobs directly. Instead they are pushed in a
 * {@link IQueue queue}. A worker engine, which might be running on a different
 * node, is responsible for monitoring a queue and executing scheduled jobs. The
 * {@link IJobService} will be used for those tasks.
 * </p>
 * <p>
 * Schedules can be created and/or updated using the {@link IScheduleManager}.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISchedule {

	/**
	 * Returns the schedule entries.
	 * 
	 * @return an unmodifiable list of entries
	 */
	List<IScheduleEntry> getEntries();

	/**
	 * Returns the schedule identifier.
	 * 
	 * @return the schedule id
	 */
	String getId();

	/**
	 * Returns the id of the queue that will be used for scheduling jobs.
	 * <p>
	 * This defines a default queue for a schedule. It's still possible to
	 * define individual queues per schedule.
	 * </p>
	 * 
	 * @return the queue id (may be <code>null</code>)
	 */
	String getQueueId();

	/**
	 * Returns the time zone that this schedule uses.
	 * <p>
	 * The default time zone is <code>UTC</code> (also often referred as
	 * <code>GMT</code>)
	 * </p>
	 * 
	 * @return the time zone
	 */
	TimeZone getTimeZone();

	/**
	 * Indicates if the schedule is enabled.
	 * 
	 * @return <code>true</code> if enabled, <code>false</code> otherwise
	 */
	boolean isEnabled();
}
