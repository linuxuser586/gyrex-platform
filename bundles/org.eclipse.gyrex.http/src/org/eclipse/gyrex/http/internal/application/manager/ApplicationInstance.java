/*******************************************************************************
 * Copyright (c) 2008, 2012 Gunnar Wagenknecht and others.
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
import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.internal.HttpDebug;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A concrete application instance.
 */
public class ApplicationInstance {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationInstance.class);

	private final AtomicReference<Application> applicationRef;
	private final AtomicReference<IApplicationContext> applicationContextRef;
	private final String applicationId;

	/**
	 * Creates a new instance.
	 * 
	 * @param application
	 * @param applicationContext
	 */
	public ApplicationInstance(final Application application, final IApplicationContext applicationContext) {
		applicationRef = new AtomicReference<Application>(application);
		applicationContextRef = new AtomicReference<IApplicationContext>(applicationContext);
		applicationId = application.getId();
	}

	/**
	 * Destroys an application instance.
	 */
	void destroy() {
		final Application application = getApplication();
		if (applicationRef.compareAndSet(application, null)) {
			try {
				application.destroy();
			} catch (final Exception e) {
				if (HttpDebug.applicationLifecycle) {
					LOG.debug("Error destorying application '{}'.", applicationId, e);
				}
			} finally {
				applicationContextRef.set(null);
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

	/**
	 * Convenience method for initializing an application.
	 */
	public void initialize() {
		final Application application = getApplication();
		final IApplicationContext applicationContext = applicationContextRef.get();
		if ((null == application) || (null == applicationContext)) {
			throw new IllegalStateException(String.format("Application '%s' already destroyed.", applicationId));
		}

		// initialize the application
		try {
			application.initialize(applicationContext);
		} catch (final Exception e) {
			// error while initializing application
			LOG.error("Error initializing application '{}'. {}", new Object[] { applicationId, ExceptionUtils.getRootCauseMessage(e), e });
			throw new ApplicationException(500, "Initialization Error", e);
		}
	}
}
