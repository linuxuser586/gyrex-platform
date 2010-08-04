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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.di.IRuntimeContextInjector;

import org.osgi.framework.BundleContext;

/**
 * Base class for a provider of contextual objects to the Gyrex contextual
 * runtime.
 * <p>
 * A context object provider allows to contribute context specific objects to
 * the contextual runtime in Gyrex. It is essentially a factory for objects
 * which are to be made available in a context. Those context objects have a
 * defined lifecycle which is bound to the context they were retrieved for. See
 * {@link #getObject(Class, IRuntimeContext)} for details.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute contextual
 * objects. It is part of the contextual runtime API and should never be used
 * directly by clients. Context object providers must be made available as OSGi
 * services using type {@link RuntimeContextObjectProvider} (also known as
 * whiteboard pattern).
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
	 * The OSGi service name for a runtime context object provider service. This
	 * name can be used to obtain instances of the service.
	 * 
	 * @see BundleContext#getServiceReference(String)
	 */
	public static final String SERVICE_NAME = RuntimeContextObjectProvider.class.getName();

	/** the object types configuration (assumed to be unmodifiable once set) */
	private volatile Map<Class<?>, Class<?>> objectTypesToImplementationTypesMap;

	/**
	 * the object types (assumed to be unmodifiable once set, extracted from
	 * {@link #objectTypesToImplementationTypesMap})
	 */
	private volatile Class<?>[] objectTypes;

	// note, we intentionally don't support this (yet)
//	/**
//	 * An OSGi service property used to indicate the full qualified {@link Type}
//	 * names this provider is able to supply.
//	 * <p>
//	 * This property may be supplied in the <code>properties</code>
//	 * <code>Dictionary</code> object passed to the
//	 * <code>BundleContext.registerService</code> method. The value of this
//	 * property must be of type <code>String</code>, <code>String[]</code>, or
//	 * <code>Collection</code> of <code>String</code>. A <code>String</code>
//	 * value may also contain a comma separated list of type names.
//	 * </p>
//	 *
//	 * @see BundleContext#registerService(String, Object, java.util.Dictionary)
//	 */
//	public static final String OBJECT_TYPES = "runtime.context.object.types"; //$NON-NLS-1$

	/**
	 * Configures the provided object types.
	 * <p>
	 * Subclasses may call this during initialization to configure the provided
	 * object types. This method may only be called once and must be called
	 * before the provide is made available as an OSGi service. It's not
	 * possible to reconfigure an object provide once configured. Instead, the
	 * current one should be disposed and a new one should be registered.
	 * </p>
	 * 
	 * @param objectTypesConfiguration
	 *            a map which maps object types to their implementation type;
	 *            the key is the object type and the value the implementation
	 *            type
	 * @throws IllegalStateException
	 *             if already configured
	 */
	protected final void configureObjectTypes(final Map<Class<?>, Class<?>> objectTypesConfiguration) throws IllegalStateException {
		if (null != objectTypesToImplementationTypesMap) {
			throw new IllegalStateException("already configured");
		}
		if (null != objectTypesConfiguration) {
			objectTypesToImplementationTypesMap = Collections.unmodifiableMap(objectTypesConfiguration);
			final Set<Class<?>> keySet = objectTypesToImplementationTypesMap.keySet();
			objectTypes = keySet.toArray(new Class<?>[keySet.size()]);
		} else {
			objectTypesToImplementationTypesMap = Collections.emptyMap();
			objectTypes = new Class<?>[0];
		}
	}

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
	 * However, they must not allow changes to the lifecycle of their objects
	 * through any other channels than
	 * {@link #ungetObject(Object, IRuntimeContext)}.
	 * </p>
	 * <p>
	 * The default implementation uses the {@link #configureObjectTypes(Map)
	 * configured} object type map to lookup the implementation type, creates an
	 * instance of the implementation type using the contexts
	 * {@link IRuntimeContextInjector} and returns it. Subclasses may override
	 * and customize.
	 * </p>
	 * 
	 * @param <T>
	 *            the object type
	 * @param type
	 *            the object type
	 * @param context
	 *            the context for which the object is requested
	 * @return the object which is an instance of the given class (maybe
	 *         <code>null</code>)
	 * @throws IllegalStateException
	 *             if not configured
	 */
	public <T> T getObject(final Class<T> type, final IRuntimeContext context) throws IllegalStateException {
		if (null == objectTypesToImplementationTypesMap) {
			throw new IllegalStateException("not configured");
		}
		final Class<?> implementationType = objectTypesToImplementationTypesMap.get(type);
		if (null != implementationType) {
			return makeObject(context, implementationType);
		}
		return null;
	}

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
	 * <p>
	 * The default implementation returns the object types as configured by
	 * {@link #configureObjectTypes(Map)}. Subclasses may override to customize.
	 * </p>
	 * 
	 * @return a list of object types
	 * @throws IllegalStateException
	 *             if not configured
	 */
	public Class<?>[] getObjectTypes() throws IllegalStateException {
		final Class<?>[] types = objectTypes;
		if (null == types) {
			throw new IllegalStateException("not configured");
		}
		return types;
	}

	@SuppressWarnings("unchecked")
	private <T> T makeObject(final IRuntimeContext context, final Class<?> implementationType) {
		return (T) context.getInjector().make(implementationType);
	}

	@Override
	public String toString() {
		try {
			final StringBuilder builder = new StringBuilder();
			builder.append("RuntimeContextObjectProvider [getClass()=").append(getClass()).append(", getObjectTypes()=").append(Arrays.toString(getObjectTypes())).append("]");
			return builder.toString();
		} catch (final IllegalStateException e) {
			final StringBuilder builder = new StringBuilder();
			builder.append("RuntimeContextObjectProvider [getClass()=").append(getClass()).append(", ").append(e.getMessage()).append("]");
			return builder.toString();
		}
	}

	/**
	 * Called by the platform when an object which was retrieved from this
	 * provider through a {@link #getObject(Class, IRuntimeContext)} call is no
	 * longer used by a particular context.
	 * <p>
	 * Implementors must perform any necessary cleanup on the object and release
	 * any resources and references associated with it so that it can be garbage
	 * collected.
	 * </p>
	 * <p>
	 * The default implementation uses the contexts
	 * {@link IRuntimeContextInjector} to un-inject it from the context.
	 * Subclasses may override and customize.
	 * </p>
	 * 
	 * @param <T>
	 *            the object type
	 * @param object
	 *            the object created by this provider
	 * @param context
	 *            the context for which the object was created
	 */
	public <T> void ungetObject(final T object, final IRuntimeContext context) {
		final IRuntimeContextInjector injectionFactory = context.get(IRuntimeContextInjector.class);
		if (null != injectionFactory) {
			injectionFactory.uninject(object);
		}
	}

}
