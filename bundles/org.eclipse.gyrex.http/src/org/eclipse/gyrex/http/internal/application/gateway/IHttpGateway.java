/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.application.gateway;


/**
 * A gateway for connecting a HTTP environment (eg. Jetty) with the application
 * manager.
 */
public interface IHttpGateway {

	/**
	 * Returns a human readable name of the gateway.
	 * 
	 * @return a human readable name of the gateway
	 */
	String getName();

	/**
	 * Returns the URL registry for the specified application manager.
	 * 
	 * @param applicationManager
	 *            the backing application manager binding (must be used for
	 *            access/creating application instances which are unique across
	 *            all gateways)
	 * @return the URL registry
	 */
	// TODO clarify if application instance uniqueness across all gateways really works
	IUrlRegistry getUrlRegistry(HttpGatewayBinding applicationManager);

}
