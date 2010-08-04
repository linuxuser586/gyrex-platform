/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context;

import org.eclipse.gyrex.context.di.IRuntimeContextInjector;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * The context for defining the runtime environment.
 * <p>
 * In Gyrex several constraints are not static for every execution. They depend
 * on an environment. This interface defines this environment and is used in
 * Gyrex to bind runtime constraints. It does so by providing a context's client
 * with a set of pre-defined (pre-configured) services. A service in this case
 * can be anything that maps to a Java object.
 * </p>
 * <p>
 * Contexts are defined in a tree like structure with a single root context.
 * Each context has a parent. The root context has none. The parent is used for
 * inheritance.
 * </p>
 * <p>
 * A context is identified by a {@link IPath path}. The path is a
 * device-independent path. It also defines a context's hierarchy. The path of
 * the root context is equivalent to <code>{@link Path#ROOT}</code>.
 * </p>
 * <p>
 * A runtime context can be retrieved from the {@link IRuntimeContextRegistry
 * context registry}. However, only privileged code may be allowed to access any
 * context that exists. Usually the Gyrex APIs provide a specific context to
 * clients which they are allowed to use.
 * </p>
 * <p>
 * Note, for security reasons as mentioned previously a context will not allow
 * simple retrieval of its parent context. Instead always the context registry
 * has to be used to lookup a particular context. For the same reasons, a
 * context does not offer modification APIs to clients.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @see IRuntimeContextRegistry
 */
public interface IRuntimeContext extends IAdaptable {

	/**
	 * Returns a context object associated with the given type.
	 * <p>
	 * Returns <code>null</code> if no context object could be determined, or if
	 * the associated object is <code>null</code>.
	 * </p>
	 * <p>
	 * Note, clients must be aware that they run in a dynamic system. Therefore
	 * they must not hold on the object returned for a long time. The reason is
	 * that at any time a context (and its parent contexts) can be re-configured
	 * at runtime. This may change what object is returned for future calls.
	 * Depending on the object type and its provider additional API might be
	 * available to check whether an object is still valid. In this case, the
	 * provider API might be consulted for further lifecycle information.
	 * </p>
	 * 
	 * @param <T>
	 *            the expected type of the value
	 * @param type
	 *            the class of the value type to return (may not be
	 *            <code>null</code>)
	 * @return the value (maybe <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if the specified type is <code>null</code>
	 */
	<T> T get(Class<T> type) throws IllegalArgumentException;

	/**
	 * Returns the context path.
	 * <p>
	 * Each context has a path which uniquely identifies the context across
	 * Gyrex in a persistent manner. This means that the identifier must not
	 * change across subsequent invocations and sessions.
	 * </p>
	 * 
	 * @return the context path (may not be <code>null</code>)
	 */
	IPath getContextPath();

	/**
	 * Returns the {@link IRuntimeContextInjector injector} of a context.
	 * <p>
	 * The injector can be used to inject a context content into custom objects.
	 * </p>
	 * 
	 * @return the {@link IRuntimeContextInjector injector} instance of the
	 *         context
	 */
	IRuntimeContextInjector getInjector();

	/**
	 * Returns a human readable string representation of the context.
	 * <p>
	 * Note, the string returned here should only be used for debugging or error
	 * tracing purposes. It should not be exposed to users directly because it
	 * <strong>leaks</strong> the context path. The context path may contain
	 * sensitive information which is not intended for end users.
	 * </p>
	 * 
	 * @return a string representation of the context
	 * @see java.lang.Object#toString()
	 */
	@Override
	String toString();
}
