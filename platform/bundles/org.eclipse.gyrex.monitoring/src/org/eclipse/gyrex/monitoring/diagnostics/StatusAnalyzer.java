/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.diagnostics;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * An analyzer for determining a system status.
 * <p>
 * An essential element in Gyrex is self-monitoring. This class allows clients
 * to implement and contributed analyzers which compute system status
 * information. Implementations should be registered as OSGi services using
 * {@link #SERVICE_NAME this class}. The platform will query all registered
 * analyzers dynamically to gather information about the system status.
 * </p>
 * <p>
 * The query interval is influenced by specific events happening in the
 * platform. If clients prefer a more deterministic way of reporting a status
 * they should use {@link IStatusReporter} instead.
 * </p>
 * <p>
 * This class is intended to be subclassed by clients that wish to add status
 * analyzer to the platform.
 * </p>
 * 
 * @see IStatusReporter
 */
public abstract class StatusAnalyzer {

	/** the OSGi service name */
	public static final String SERVICE_NAME = StatusAnalyzer.class.getName();

	/**
	 * Called by the diagnostics framework to compute a system status.
	 * <p>
	 * {@link Status#CANCEL_STATUS} should be returned to indicate that a
	 * computation is currently not possible and should be re-tried later.
	 * </p>
	 * <p>
	 * Any status that is not ok (i.e., {@link IStatus#isOK()} returns
	 * <code>false</code>) will be reported to the system operator. The type of
	 * reporting depends on rules and filters configured externally.
	 * </p>
	 * 
	 * @param progressMonitor
	 *            a progress monitor for reporting progress feedback and
	 *            checking cancelation for long running operations
	 * @return a system status (may not be <code>null</code>)
	 */
	public abstract IStatus computeStatus(IProgressMonitor progressMonitor);
}
