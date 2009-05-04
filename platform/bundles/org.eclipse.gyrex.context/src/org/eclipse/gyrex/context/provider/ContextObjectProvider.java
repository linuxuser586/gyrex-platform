/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.provider;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.gyrex.context.IRuntimeContext;

/**
 * Base class for a provider of contextual objects to Gyrex contextual runtime.
 * <p>
 * A context object provider allows to contribute context specific objects to
 * the contextual runtime in Gyrex. It is essentially a factory for objects
 * which are to be made available in a context.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute contextual
 * objects. It is part of the contextual runtime API and should never be used
 * directly by clients. Context object providers must be made available as OSGi
 * services using type {@link ContextObjectProvider} (also known as whiteboard
 * pattern).
 * </p>
 * <p>
 * Note, this class is part of a service provider API which may evolve faster
 * than the general contextual runtime API. Please get in touch with the
 * development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public abstract class ContextObjectProvider {

	/**
	 * Returns the default context path an object should be registered
	 * automatically. Returns <code>null</code> or an empty path if the provider
	 * should not be registered automatically in the context hierarchy.
	 * <p>
	 * The default implementation returns the {@link Path#ROOT root path} which
	 * means that the provider should be registered with the root context
	 * automatically. Subclasses may overwrite and return <code>null</code> or
	 * any other path.
	 * </p>
	 * <p>
	 * Note, if the path is not defined yet, the provider will not be registered
	 * until the context path becomes available.
	 * </p>
	 * 
	 * @return the default registration path (maybe <code>null</code>)
	 */
	public IPath getDefaultRegistrationPath() {
		return Path.ROOT;
	}

	/**
	 * Returns an object which is an instance of the given class associated with
	 * the given context. Returns <code>null</code> if no such object can be
	 * found.
	 * 
	 * @param <T>
	 *            the object type parameter
	 * @param type
	 *            the object type
	 * @param context
	 *            the context for which the object is requested
	 * @return the object which is an instance of the given class (maybe
	 *         <code>null</code>)
	 */
	public abstract Object getObject(Class type, IRuntimeContext context);

	/**
	 * Returns the collection of object types contributed by this provider.
	 * <p>
	 * This method is generally used by the platform to discover which types are
	 * supported, in advance of dispatching any actual
	 * {@link #createObject(Class, IRuntimeContext)} requests.
	 * </p>
	 * <p>
	 * Usually, the types returned here are public API types. Those are suitable
	 * to be used by clients in calls to {@link IRuntimeContext#get(Class)}. The
	 * actual object may be of a specific implementation type.
	 * </p>
	 * 
	 * @return a list of object types
	 */
	public abstract Class[] getObjectTypes();

	/**
	 * Called by the platform when an object which was created by this provider
	 * is no longer used by a particular context.
	 * <p>
	 * Implementors should perform any necessary cleanup on the object and
	 * release any resources associated with it.
	 * </p>
	 * 
	 * @param object
	 *            the object created by this provider
	 * @param context
	 *            the context for which the object was created
	 */
	public abstract void ungetObject(final Object object, IRuntimeContext context);
}
