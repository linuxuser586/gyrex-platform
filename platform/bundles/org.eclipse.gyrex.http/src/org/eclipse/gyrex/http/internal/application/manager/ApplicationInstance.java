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
package org.eclipse.cloudfree.http.internal.application.manager;

import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;

import org.eclipse.cloudfree.http.application.Application;

/**
 * A concrete application instance.
 */
public class ApplicationInstance {

	private final AtomicReference<Application> application;
	private final AtomicReference<ServletContext> adaptedServletContext;

	/**
	 * Creates a new instance.
	 * 
	 * @param application
	 * @param adaptedServletContext
	 */
	public ApplicationInstance(final Application application, final ServletContext adaptedServletContext) {
		this.application = new AtomicReference<Application>(application);
		this.adaptedServletContext = new AtomicReference<ServletContext>(adaptedServletContext);
	}

	/**
	 * Destroys an application instance.
	 */
	void destroy() {
		final Application application = getApplication();
		if (this.application.compareAndSet(application, null)) {
			adaptedServletContext.set(null);
			try {
				application.destroy();
			} catch (final Exception e) {
				// TODO consider logging this
			}
		}
	}

	/**
	 * Returns the adaptedServletContext.
	 * 
	 * @return the adaptedServletContext
	 */
	public ServletContext getAdaptedServletContext() {
		return adaptedServletContext.get();
	}

	/**
	 * Returns the application object.
	 * 
	 * @return the application object
	 */
	public Application getApplication() {
		return application.get();
	}

}
