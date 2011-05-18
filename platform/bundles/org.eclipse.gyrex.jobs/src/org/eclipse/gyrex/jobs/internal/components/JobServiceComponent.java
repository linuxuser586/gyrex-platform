/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.components;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.jobs.manager.IJobManager;
import org.eclipse.gyrex.jobs.service.IJobService;

/**
 *
 */
public class JobServiceComponent implements IJobService {

	@Override
	public IJobManager getJobManager(final IRuntimeContext context) {
		return context.get(IJobManager.class);
	}

}
