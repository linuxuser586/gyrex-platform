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
package org.eclipse.gyrex.jobs.internal.components;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleManager;
import org.eclipse.gyrex.jobs.schedules.service.IScheduleService;

/**
 *
 */
public class ScheduleServiceComponent implements IScheduleService {

	@Override
	public IScheduleManager getScheduleManager(final IRuntimeContext context) {
		return context.get(IScheduleManager.class);
	}

}
