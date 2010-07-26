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
package org.eclipse.gyrex.http.application.provider;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;

import org.eclipse.core.runtime.CoreException;

/**
 * A provider for {@link Application application instances}.
 * <p>
 * Application providers act like factories. Whenever a new application instance
 * has to be created the provider will be ask to create one.
 * </p>
 * <p>
 * This class may be implemented by clients contributing a custom application
 * type and must be made available to Gyrex as an OSGi service.
 * </p>
 */
public abstract class ApplicationProvider {

	private final String id;

	/**
	 * Creates a new provider instance.
	 * 
	 * @param id
	 *            the provider id
	 */
	protected ApplicationProvider(final String id) {
		if (null == id) {
			throw new IllegalArgumentException("id must not be null");
		}
		this.id = id.intern();
	}

	/**
	 * Creates a new application instance.
	 * <p>
	 * Gyrex calls this method to ask the provider for a new application which
	 * is intended to handle HTTP requests for the specified context.
	 * </p>
	 * <p>
	 * Implementors <strong>must</strong> return a new object every time this
	 * method is invoked. The application will be initialized by the platform
	 * when necessary by calling
	 * {@link Application#initialize(org.eclipse.gyrex.http.application.service.IApplicationServiceSupport)}
	 * on the returned application object.
	 * </p>
	 * 
	 * @param applicationId
	 *            the application id
	 * @param context
	 *            the context the application will operate in
	 * @return a new application instance
	 */
	public abstract Application createApplication(String applicationId, IRuntimeContext context) throws CoreException;

	/**
	 * Returns the provider id.
	 * 
	 * @return the id
	 */
	public final String getId() {
		return id;
	}
}
