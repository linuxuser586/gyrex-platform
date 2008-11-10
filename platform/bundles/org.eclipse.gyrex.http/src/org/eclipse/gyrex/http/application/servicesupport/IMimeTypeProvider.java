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
package org.eclipse.cloudfree.http.application.servicesupport;

import javax.servlet.ServletContext;

import org.eclipse.cloudfree.http.application.Application;

/**
 * This interface defines methods that the application may call to get access to
 * registered mime types.
 * <p>
 * This interface is typically implemented by clients providing mime types to an
 * application. It can be contributed to an application using its
 * {@link IApplicationServiceSupport}.
 * </p>
 */
public interface IMimeTypeProvider {

	/**
	 * Maps a path to a MIME type.
	 * <p>
	 * Called by the application to determine the MIME type for the path. For
	 * servlet registrations, the application will call this method to support
	 * the {@link ServletContext} method
	 * {@link ServletContext#getMimeType(String)}. For resource registrations,
	 * the application will call this method to determine the MIME type for the
	 * Content-Type header in the response.
	 * </p>
	 * 
	 * @param file
	 *            determine the MIME type for this file
	 * @return MIME type (e.g. text/html) of the file or <code>null</code> to
	 *         indicate that the platform should determine the MIME type itself
	 * @see Application#getMimeType(String)
	 */
	public String getMimeType(String path);

}
