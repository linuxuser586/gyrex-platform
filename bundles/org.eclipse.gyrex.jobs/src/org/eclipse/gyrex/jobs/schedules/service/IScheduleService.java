/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Mike Tschierschke - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.schedules.service;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.schedules.ISchedule;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleManager;

/**
 * A service for managing {@link ISchedule schedules} in Gyrex.
 * <p>
 * This interface is made available as an OSGi service using
 * {@link #SERVICE_NAME} and provides access to a {@link IScheduleManager}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @see IScheduleManager
 */
public interface IScheduleService {

	/** the OSGi service name */
	String SERVICE_NAME = IScheduleService.class.getName();

	/**
	 * Returns the schedule manager for the specified context.
	 * 
	 * @param context
	 *            a runtime context
	 * @return the schedule manager
	 */
	IScheduleManager getScheduleManager(IRuntimeContext context);

}
