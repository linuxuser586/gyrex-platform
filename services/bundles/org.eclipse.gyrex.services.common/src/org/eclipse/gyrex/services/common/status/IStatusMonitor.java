/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.services.common.status;

import org.eclipse.core.runtime.IStatus;

/**
 * A monitor for the status of a service.
 * <p>
 * Services are an essential element in the CloudFree platform as is
 * self-monitoring. Therefore, each service is required to report information
 * when its status changes. This interface allows a service to report its status
 * information to the CloudFree platform.
 * </p>
 * <p>
 * Whenever the service status changes a service must report it using
 * {@link #publishStatus(IStatus)}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IStatusMonitor {

	/**
	 * Publishes the specified service status.
	 * <p>
	 * The CloudFree platform will evaluate the status once it is received.
	 * Based on the severity and configured rules it may trigger appropriate
	 * actions (eg. inform operators or site owners).
	 * </p>
	 * 
	 * @param status
	 *            the service status
	 * @see IStatus#isOK()
	 */
	void publishStatus(IStatus status);

}
