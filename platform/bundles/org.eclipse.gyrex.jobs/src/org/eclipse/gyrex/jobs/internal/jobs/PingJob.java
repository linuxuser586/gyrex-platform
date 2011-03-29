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
package org.eclipse.gyrex.jobs.internal.jobs;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PingJob extends Job {

	private static final Logger LOG = LoggerFactory.getLogger(PingJob.class);
	private final Map<String, String> jobParameter;

	/**
	 * Creates a new instance.
	 * 
	 * @param jobParameter
	 * @param name
	 */
	public PingJob(final Map<String, String> jobParameter) {
		super("ping");
		this.jobParameter = jobParameter;
		setSystem(true);
		setPriority(SHORT);
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		LOG.info("PING {}", null != jobParameter ? jobParameter.get("ping") : "null");
		return Status.OK_STATUS;
	}

}
