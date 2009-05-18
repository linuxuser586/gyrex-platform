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

import org.eclipse.gyrex.context.IRuntimeContext;

/**
 * Base class for a provider of contextual objects to Gyrex contextual runtime.
 * <p>
 * A context object provider allows to contribute context specific objects to
 * the contextual runtime in Gyrex. It is essentially a factory for objects
 * which are to be made available in a context. Those context objects have a
 * defined lifecycle which is bound to the context they were retreived for. See
 * {@link #getObject(Class, IRuntimeContext)} for details.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute contextual
 * objects. It is part of the contextual runtime API and should never be used
 * directly by clients. Context object providers must be made available as OSGi
 * services using type {@link RuntimeContextObjectProvider} (also known as whiteboard
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
public abstract class RuntimeContextObjectProvider {

	/**
	 * Returns an object which is an instance of the given class associated with
	 * the given context. Returns <code>null</code> if no such object can be
	 * found.
	 * <p>
	 * This method is guaranteed to be called only once for each context an
	 * object is requested for. From this time one the object is used in the
	 * context. When the contextual runtime detects that an object is no longer
	 * needed for a context it calls
	 * {@link #ungetObject(Object, IRuntimeContext)}. This allows providers to
	 * handle the lifecycle of context objects. They may document this as part
	 * of the public contract of the provided object. They may also provide
	 * additional API for querying the lifecycle status of their objects.
	 * However, they must not allow changed to the lifecycle of their objects
	 * through any other channels than
	 * {@link #ungetObject(Object, IRuntimeContext)}.
	 * </p>
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
	 * Called by the platform when an object which was retrieved from this
	 * provider through a {@link #getObject(Class, IRuntimeContext)} call is no
	 * longer used by a particular context.
	 * <p>
	 * Implementors must perform any necessary cleanup on the object and release
	 * any resources and references associated with it so that it can be garbage
	 * collected.
	 * </p>
	 * 
	 * @param object
	 *            the object created by this provider
	 * @param context
	 *            the context for which the object was created
	 */
	public abstract void ungetObject(final Object object, IRuntimeContext context);
}
