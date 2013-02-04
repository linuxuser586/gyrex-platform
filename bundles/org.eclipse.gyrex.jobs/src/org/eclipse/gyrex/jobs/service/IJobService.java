/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Mike Tschierschke - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.service;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.manager.IJobManager;

/**
 * A service for managing {@link IJob distributed jobs} in Gyrex.
 * <p>
 * This interface is made available as an OSGi service using
 * {@link #SERVICE_NAME} and provides access to a {@link IJobManager}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @see IJobManager
 */
public interface IJobService {

	/** the OSGi service name */
	String SERVICE_NAME = IJobService.class.getName();

	/**
	 * Returns the job manager for the specified context.
	 * 
	 * @param context
	 *            a runtime context
	 * @return the job manager
	 */
	IJobManager getJobManager(IRuntimeContext context);

}
