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
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.components;

import java.util.HashMap;

import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;
import org.eclipse.gyrex.jobs.internal.manager.JobManagerImpl;
import org.eclipse.gyrex.jobs.internal.schedules.ScheduleManagerImpl;
import org.eclipse.gyrex.jobs.internal.storage.CloudPreferncesJobHistoryStorage;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.schedules.manager.IScheduleManager;
import org.eclipse.gyrex.jobs.spi.storage.IJobHistoryStorage;

/**
 * The Job API context objects.
 */
public class JobContextObjectsComponent extends RuntimeContextObjectProvider {

	/**
	 * Creates a new instance.
	 */
	public JobContextObjectsComponent() {
		final HashMap<Class<?>, Class<?>> typesConfiguration = new HashMap<Class<?>, Class<?>>(2);
		typesConfiguration.put(IJobManager.class, JobManagerImpl.class);
		typesConfiguration.put(IScheduleManager.class, ScheduleManagerImpl.class);
		typesConfiguration.put(IJobHistoryStorage.class, CloudPreferncesJobHistoryStorage.class);
		configureObjectTypes(typesConfiguration);
	}

}
