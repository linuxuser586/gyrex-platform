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
package org.eclipse.gyrex.monitoring.internal;

import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A tracker for {@link IStatus}.
 */
public class StatusTracker extends ServiceTracker<IStatus, IStatus> {

	private static final IStatus UNINITIALIZED = new Status(IStatus.CANCEL, MonitoringActivator.SYMBOLIC_NAME, "Not Initialized.");

	private final CopyOnWriteArrayList<IStatus> statusList = new CopyOnWriteArrayList<IStatus>();
	private volatile IStatus systemStatus = UNINITIALIZED;

	public StatusTracker(final BundleContext context) {
		super(context, IStatus.class, null);
	}

	@Override
	public IStatus addingService(final ServiceReference<IStatus> reference) {
		final IStatus status = super.addingService(reference);
		if (status != null) {
			statusList.add(status);
			updateStatus();
		}
		return status;
	}

	public IStatus getSystemStatus() {
		return systemStatus;
	}

	@Override
	public void modifiedService(final ServiceReference<IStatus> reference, final IStatus service) {
		updateStatus();
	}

	@Override
	public void removedService(final ServiceReference<IStatus> reference, final IStatus service) {
		statusList.remove(service);
		updateStatus();
		super.removedService(reference, service);
	}

	protected void setSystemStatus(final MultiStatus systemStatus) {
		this.systemStatus = null != systemStatus ? systemStatus : UNINITIALIZED;
	}

	private void updateStatus() {
		// create multi status
		final MultiStatus systemStatus = new MultiStatus(MonitoringActivator.SYMBOLIC_NAME, 0, "System Status", null);
		for (final IStatus status : statusList) {
			systemStatus.add(status);
		}

		// update status
		setSystemStatus(systemStatus);
	}

}
