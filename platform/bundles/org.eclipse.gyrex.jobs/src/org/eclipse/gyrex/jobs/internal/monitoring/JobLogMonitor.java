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
package org.eclipse.gyrex.jobs.internal.monitoring;

import org.eclipse.gyrex.jobs.internal.JobsActivator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;

import org.osgi.framework.ServiceRegistration;

/**
 * Subclass of {@link IProgressMonitor monitor} that wraps an existing monitor
 * and provides the job's state via OSGi service of type {@link IJobMonitor}
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class JobLogMonitor extends ProgressMonitorWrapper implements IJobMonitor {

	int totalWork, worked;

	String currentTask;

	ServiceRegistration<IJobMonitor> serviceRegistration;

	final String jobId;

	/**
	 * Creates a new instance.
	 * 
	 * @param jobId
	 */
	public JobLogMonitor(final IProgressMonitor monitor, final String jobId) {
		super(monitor);
		this.jobId = jobId;
	}

	@Override
	public void beginTask(final String name, final int totalWork) {
		super.beginTask(name, totalWork);
		// log begin task
		this.totalWork = totalWork;
		// publish
		serviceRegistration = JobsActivator.getInstance().getServiceHelper().registerService(IJobMonitor.class, this, "Ageto Service GmbH", "Job monitor for " + jobId, null, -1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#done()
	 */
	@Override
	public void done() {
		super.done();
		// unregister service
		if (null != serviceRegistration) {
			serviceRegistration.unregister();
		}
	}

	@Override
	public String getCurrentTask() {
		return currentTask;
	}

	@Override
	public String getJobId() {
		return jobId;
	}

	@Override
	public int getTotalWork() {
		return totalWork;
	}

	@Override
	public int getWorked() {
		return worked;
	}

	@Override
	public void setTaskName(final String name) {
		super.setTaskName(name);
		// set task name
		currentTask = name;
	}

	@Override
	public void worked(final int work) {
		super.worked(work);
		// log work
		worked = Math.min(totalWork, worked + work);
	}
}
