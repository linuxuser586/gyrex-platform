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
package org.eclipse.gyrex.http.application.provider;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;

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

	private String id;

	/**
	 * Creates a new provider instance allowing extenders to initialize the by
	 * there one.
	 * <p>
	 * When this constructor is used callers must ensure that
	 * {@link #setId(String)} is called within object creation at some point
	 * before the provider is made available as an OSGi service to the Gyrex
	 * runtime.
	 * </p>
	 */
	protected ApplicationProvider() {
	}

	/**
	 * Creates a new provider instance using the specified id.
	 * <p>
	 * Invoking this constructor initialized the id using {@link #setId(String)}
	 * with the specified id.
	 * </p>
	 * 
	 * @param id
	 *            the provider id
	 * @see #setId(String)
	 */
	protected ApplicationProvider(final String id) {
		setId(id);
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
	 * @throws Exception
	 *             in case of errors creating the application
	 */
	public abstract Application createApplication(String applicationId, IRuntimeContext context) throws Exception;

	/**
	 * Returns the provider id.
	 * 
	 * @return the id
	 */
	public final String getId() {
		if (null == id) {
			throw new IllegalStateException(String.format("provider id has not been initialized (%s)", getClass().getName()));
		}
		return id;
	}

	/**
	 * Sets the provider id.
	 * <p>
	 * The provider id must be set before it can be used. It must only be set
	 * once and cannot be changed thereafter.
	 * </p>
	 * 
	 * @param id
	 *            the provider id (will be {@link IdHelper#isValidId(String)
	 *            validated}
	 * @throws IllegalArgumentException
	 *             if the specified id is invalid
	 * @throws IllegalStateException
	 *             the the id has already been set
	 * @see IdHelper#isValidId(String)
	 */
	protected final void setId(final String id) throws IllegalArgumentException, IllegalStateException {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id; please use only ASCII chars a..z (lower and/or upper case), number 0..9 and/or dot, dash and underscore (.-_)");
		}
		this.id = id.intern();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [id=").append(id).append("]");
		return builder.toString();
	}

}
