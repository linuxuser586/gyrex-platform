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
package org.eclipse.gyrex.http.internal.apps.dummy;


import org.eclipse.gyrex.common.context.IContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.servicesupport.IApplicationServiceSupport;

/**
 * 
 */
public class DummyApp extends Application {

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param context
	 */
	protected DummyApp(final String id, final IContext context) {
		super(id, context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.http.application.Application#doInit()
	 */
	@Override
	protected void doInit() {
		final IApplicationServiceSupport applicationServiceSupport = getApplicationServiceSupport();
		if (null == applicationServiceSupport) {
			return;
		}
		try {
			applicationServiceSupport.registerServlet("/", new DummyRequestHandler(), null);
			applicationServiceSupport.registerServlet("/*.debug", new DebugServlet(), null);
			applicationServiceSupport.registerServlet("/debug", new DebugServlet(), null);
		} catch (final Exception e) {
			throw new ApplicationException(e);
		}
	}
}
