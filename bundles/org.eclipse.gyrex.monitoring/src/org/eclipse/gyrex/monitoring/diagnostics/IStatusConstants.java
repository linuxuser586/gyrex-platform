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
package org.eclipse.gyrex.monitoring.diagnostics;

import org.eclipse.core.runtime.IStatus;

import org.osgi.framework.Constants;

/**
 * Constants used for reporting system status.
 * <p>
 * An essential element in Gyrex is self-monitoring. This interface defines
 * constants which allow any client to report status information to Gyrex. In
 * order to report a system status clients should make the {@link IStatus}
 * instance available as an OSGi service which can be tracked using
 * {@link #SERVICE_NAME this class name}. It's important that the status is
 * associated with a {@link #STATUS_PID} in order to allow followers detected
 * changes of a status.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IStatusConstants {

	/** the OSGi service name */
	String SERVICE_NAME = IStatus.class.getName();

	/**
	 * The status persistent identifier property (value
	 * {@link Constants#SERVICE_PID}).
	 * <p>
	 * The OSGi {@link Constants#SERVICE_PID service pid} is used to uniquely
	 * identify a system status.
	 * </p>
	 */
	String STATUS_PID = Constants.SERVICE_PID;
}
