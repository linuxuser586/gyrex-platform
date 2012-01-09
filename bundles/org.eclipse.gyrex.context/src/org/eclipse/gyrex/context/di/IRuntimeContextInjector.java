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
package org.eclipse.gyrex.context.di;

import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;

/**
 * A injector is used to inject data and services from a context into a domain
 * object. It is entirely based on the {@link IInjector Eclipse dependency
 * injection system}.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @see org.eclipse.e4.core.di.IInjector Eclipse dependency injection system for
 *      details of the injection algorithm that is used.
 */
@SuppressWarnings("restriction")
public interface IRuntimeContextInjector {

	/**
	 * Injects the context into a domain object. See the class comment for
	 * details on the injection algorithm that is used.
	 * <p>
	 * <em>Please be aware!</em> The specified object will be tracked within the
	 * injector. Changes to the context will result in updates to the object. In
	 * order to <strong>release</strong> the object from the injector it must be
	 * explicitly {@link #uninject(Object) un-injected}.
	 * </p>
	 * 
	 * @param object
	 *            The object to perform injection on
	 * @throws InjectionException
	 *             if an exception occurred while performing this operation
	 */
	void inject(Object object) throws InjectionException;

	/**
	 * Obtain an instance of the specified class and inject it with the context.
	 * <p>
	 * Class'es scope dictates if a new instance of the class will be created,
	 * or existing instance will be reused.
	 * </p>
	 * <p>
	 * <em>Please be aware!</em> The specified object will be tracked within the
	 * injector. Changes to the context will result in updates to the object. In
	 * order to <strong>release</strong> the object from the injector it must be
	 * explicitly {@link #uninject(Object) un-injected}.
	 * </p>
	 * 
	 * @param clazz
	 *            The class to be instantiated
	 * @return an instance of the specified class
	 * @throws InjectionException
	 *             if an exception occurred while performing this operation
	 * @see javax.inject.Scope
	 * @see javax.inject.Singleton
	 */
	<T> T make(Class<T> clazz) throws InjectionException;

	/**
	 * Un-injects the context from the object.
	 * 
	 * @param object
	 *            The domain object previously injected with the context
	 * @throws InjectionException
	 *             if an exception occurred while performing this operation
	 */
	void uninject(Object object) throws InjectionException;
}
