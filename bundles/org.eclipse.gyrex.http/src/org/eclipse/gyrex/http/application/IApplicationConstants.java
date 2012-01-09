/**
 * Copyright (c) 2008, 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.http.application;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

/**
 * Shared HTTP Application constants.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IApplicationConstants {

	/**
	 * A {@link HttpServletRequest servlet request} attribute which value is the
	 * {@link String} object containing the mount point (of type {@link URL}) a
	 * request was received on (constant value
	 * <code>org.eclipse.gyrex.http.application.mount</code>).
	 */
	String REQUEST_ATTRIBUTE_MOUNT_POINT = "org.eclipse.gyrex.http.application.mount";
}
