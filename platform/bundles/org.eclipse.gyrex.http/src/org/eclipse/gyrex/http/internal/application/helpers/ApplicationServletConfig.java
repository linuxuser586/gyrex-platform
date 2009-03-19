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
package org.eclipse.gyrex.http.internal.application.helpers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * 
 */
public class ApplicationServletConfig implements ServletConfig {

	private final String servletName;
	private final Map<String, String> initParameters;
	private final ServletContext context;

	/**
	 * Creates a new instance.
	 * 
	 * @param servletName
	 *            the servlet name (see {@link #getServletName()})
	 * @param initParameters
	 *            the servlet initialization parameters
	 * @param context
	 *            the servlet context
	 */
	public ApplicationServletConfig(final String servletName, final Map<String, String> initParameters, final ServletContext context) {
		this.servletName = servletName;
		this.initParameters = initParameters;
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
	 */
	@Override
	public String getInitParameter(final String name) {
		if ((null == initParameters) || (null == name)) {
			return null;
		}
		return initParameters.get(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletConfig#getInitParameterNames()
	 */
	@Override
	public Enumeration getInitParameterNames() {
		if (null == initParameters) {
			return Collections.enumeration(Collections.emptyList());
		}
		return Collections.enumeration(initParameters.keySet());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletConfig#getServletContext()
	 */
	@Override
	public ServletContext getServletContext() {
		return context;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletConfig#getServletName()
	 */
	@Override
	public String getServletName() {
		return servletName;
	}

}
