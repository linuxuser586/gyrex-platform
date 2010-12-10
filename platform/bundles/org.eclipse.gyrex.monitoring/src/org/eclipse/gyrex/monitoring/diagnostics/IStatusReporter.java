/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
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

import org.eclipse.core.runtime.IStatus;

/**
 * A reporter for system status.
 * <p>
 * An essential element in Gyrex is self-monitoring. This interface allows any
 * client to report status information to Gyrex. It is made available as an OSGi
 * service and can be tracked using {@link #SERVICE_NAME this class name}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @see StatusAnalyzer
 */
public interface IStatusReporter {

	/** the OSGi service name */
	String SERVICE_NAME = IStatusReporter.class.getName();

	/**
	 * Reports a status using the specified identifier.
	 * <p>
	 * This allows to report a status to the diagnostics framework. Any status
	 * that is not ok (i.e., {@link IStatus#isOK()} returns <code>false</code>)
	 * will be reported to the system operator. The type of reporting depends on
	 * rules and filters configured externally.
	 * </p>
	 * <p>
	 * Callers are responsible to also report when the underlying issue is
	 * solved. In this case a status that is ok (i.e., {@link IStatus#isOK()}
	 * returns <code>true</code>) should be passed so that the diagnostics
	 * framework can clear any previously reported status for the specified
	 * identifier.
	 * </p>
	 * 
	 * @param id
	 *            the status identifier (may not be <code>null</code>)
	 * @param status
	 *            the status to report (may not be <code>null</code>)
	 */
	void reportStatus(String id, IStatus status);
}
