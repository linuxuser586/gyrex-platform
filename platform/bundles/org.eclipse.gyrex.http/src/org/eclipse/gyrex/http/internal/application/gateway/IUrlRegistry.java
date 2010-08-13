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
package org.eclipse.gyrex.http.internal.application.gateway;

import java.net.URL;

/**
 * The URL registry is responsible for mapping URLs to applications.
 */
public interface IUrlRegistry {

	/**
	 * Registers the specified URL with the specified application id if (and
	 * only if) the URL is not already registered.
	 * 
	 * @param url
	 *            the url
	 * @param applicationId
	 *            the application id
	 * @return the id of the existing application if the URL is already
	 *         registered, <code>null</code> if the url was not previously
	 *         registered and is now registered with the application
	 */
	String registerIfAbsent(URL url, String applicationId);

	/**
	 * Unregisters the specified URL.
	 * 
	 * @param url
	 *            the url
	 * @return the id of the existing application the URL was registered with,
	 *         <code>null</code> if the url was not previously registered
	 */
	String unregister(URL url);

}
