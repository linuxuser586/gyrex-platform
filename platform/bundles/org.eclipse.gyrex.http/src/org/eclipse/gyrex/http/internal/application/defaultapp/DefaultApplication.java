/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.application.defaultapp;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.http.servlet.HttpServiceServlet;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.internal.HttpActivator;

import org.eclipse.osgi.util.NLS;

import org.osgi.framework.Constants;

/**
 * The default application simply registers a HTTP Service.
 */
public class DefaultApplication extends Application {

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param context
	 */
	protected DefaultApplication(final String id, final IRuntimeContext context) {
		super(id, context);
	}

	@Override
	protected void doInit() {
		final IApplicationContext applicationServiceSupport = getApplicationServiceSupport();
		if (null == applicationServiceSupport) {
			return;
		}
		try {
			final Map<String, String> params = new HashMap<String, String>(4);
			params.put(Constants.SERVICE_VENDOR, "Eclipse Gyrex");
			params.put(Constants.SERVICE_DESCRIPTION, NLS.bind("Equinox OSGi HTTP service running in Gyrex (context '{0}', application '{1}')", getContext().getContextPath().toString(), getId()));
			params.put("other.info", HttpActivator.TYPE_WEB);
			applicationServiceSupport.registerServlet("/", new HttpServiceServlet(), null);
		} catch (final Exception e) {
			throw new ApplicationException(e);
		}
	}
}
