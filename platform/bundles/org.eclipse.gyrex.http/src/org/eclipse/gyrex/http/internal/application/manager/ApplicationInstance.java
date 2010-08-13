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
package org.eclipse.gyrex.http.internal.application.manager;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.context.IApplicationContext;

/**
 * A concrete application instance.
 */
public class ApplicationInstance {

	private final AtomicReference<Application> applicationRef;
	private final AtomicReference<IApplicationContext> applicationContextRef;

	/**
	 * Creates a new instance.
	 * 
	 * @param application
	 * @param applicationContext
	 */
	public ApplicationInstance(final Application application, final IApplicationContext applicationContext) {
		applicationRef = new AtomicReference<Application>(application);
		applicationContextRef = new AtomicReference<IApplicationContext>(applicationContext);
	}

	/**
	 * Destroys an application instance.
	 */
	void destroy() {
		final Application application = getApplication();
		if (applicationRef.compareAndSet(application, null)) {
			applicationContextRef.set(null);
			try {
				application.destroy();
			} catch (final Exception e) {
				// TODO consider logging this
			}
		}
	}

	/**
	 * Returns the application object.
	 * 
	 * @return the application object
	 */
	public Application getApplication() {
		return applicationRef.get();
	}

	/**
	 * Returns the Application.
	 * 
	 * @return the adaptedServletContext
	 */
	public IApplicationContext getApplicationContext() {
		return applicationContextRef.get();
	}

}
