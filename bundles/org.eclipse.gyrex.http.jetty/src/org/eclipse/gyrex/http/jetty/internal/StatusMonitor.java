/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal;

import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 */
public class StatusMonitor extends ServiceTracker<IStatus, IStatus> {

	private final CopyOnWriteArrayList<IStatus> statusList = new CopyOnWriteArrayList<IStatus>();
	private volatile IStatus overallStatus = Status.OK_STATUS;

	/**
	 * Creates a new instance.
	 */
	public StatusMonitor(final BundleContext context) {
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

	/**
	 * Returns the overallStatus.
	 * 
	 * @return the overallStatus
	 */
	public IStatus getOverallStatus() {
		final IStatus status = overallStatus;
		if (status == null) {
			return Status.OK_STATUS;
		}
		return status;
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

	void updateStatus() {
		final IStatus[] states = statusList.toArray(new IStatus[0]);

		// ok if we have no further states
		if (states.length == 0) {
			overallStatus = Status.OK_STATUS;
			return;
		}

		// create combined status
		overallStatus = new MultiStatus(HttpJettyActivator.SYMBOLIC_NAME, IStatus.OK, states, null, null);
	}

}
