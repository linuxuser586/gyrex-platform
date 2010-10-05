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
package org.eclipse.gyrex.http.equinoxhttpservice.internal;

import javax.servlet.ServletException;

import org.eclipse.equinox.http.servlet.HttpServiceServlet;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * The HttpService application.
 */
public class HttpServiceApp extends Application {

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param context
	 */
	public HttpServiceApp(final String id, final IRuntimeContext context) {
		super(id, context);
	}

	@Override
	protected void doInit() throws CoreException {
		try {
			getApplicationServiceSupport().registerServlet("/", new HttpServiceServlet(), null);
		} catch (final ServletException e) {
			throw new CoreException(new Status(IStatus.ERROR, HttpServiceActivator.SYMBOLIC_NAME, "Error while registering the HttpService servlet. " + e.getMessage(), e));
		}
	}

}
