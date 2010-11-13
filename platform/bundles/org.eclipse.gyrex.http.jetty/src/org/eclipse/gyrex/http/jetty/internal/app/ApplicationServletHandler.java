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
package org.eclipse.gyrex.http.jetty.internal.app;

import javax.servlet.Servlet;

import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;

import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.LazyList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationServletHandler extends ServletHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationServletHandler.class);

	/** applicationContextHandler */
	private final ApplicationHandler applicationHandler;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationHandler
	 */
	public ApplicationServletHandler(final ApplicationHandler applicationHandler) {
		this.applicationHandler = applicationHandler;
	}

	/**
	 * Returns the applicationHandler.
	 * 
	 * @return the applicationHandler
	 */
	public ApplicationHandler getApplicationHandler() {
		return applicationHandler;
	}

	@Override
	public ServletHolder newServletHolder() {
		return new ApplicationServletHolder();
	}

	@Override
	public ServletHolder newServletHolder(final Class<? extends Servlet> servlet) {
		return new ApplicationServletHolder(servlet);
	}

	public void removeServlet(final ServletHolder holder) {
		setServlets((ServletHolder[]) LazyList.removeFromArray(getServlets(), holder));
	}

	@Override
	public void setServletMappings(final ServletMapping[] servletMappings) {
		// update
		super.setServletMappings(servletMappings);

		// log
		if (HttpJettyDebug.handlers) {
			LOG.debug("Updated servlet mappings {}", dump());
		}
	}
}