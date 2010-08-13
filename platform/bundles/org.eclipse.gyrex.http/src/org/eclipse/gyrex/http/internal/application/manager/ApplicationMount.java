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
package org.eclipse.gyrex.http.internal.application.manager;

import java.net.URL;

/**
 * A mounted application
 */
public class ApplicationMount {

	private final URL url;
	private final String applicationId;

	/**
	 * Creates a new instance.
	 * 
	 * @param url
	 *            the mount point
	 * @param applicationId
	 *            the application id
	 */
	public ApplicationMount(final URL url, final String applicationId) {
		this.url = url;
		this.applicationId = applicationId.intern();
	}

	/**
	 * Returns the application id.
	 * 
	 * @return the application id
	 */
	public String getApplicationId() {
		return applicationId;
	}

	/**
	 * Returns the mount point.
	 * 
	 * @return the mount point
	 */
	public URL getMountPoint() {
		return url;
	}

	@Override
	public String toString() {
		final StringBuilder toString = new StringBuilder();
		toString.append("ApplicationMount[ ");
		toString.append(url.toExternalForm());
		toString.append(" -> ");
		toString.append(applicationId);
		toString.append(" ]");
		return toString.toString();
	}
}
