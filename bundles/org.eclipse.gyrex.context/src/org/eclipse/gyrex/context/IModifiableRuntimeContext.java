/*******************************************************************************
 * Copyright (c) 2012, 2013 AGETO Service GmbH and others.
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
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.services.IRuntimeContextServiceLocator;

import org.osgi.framework.BundleContext;

/**
 * A modifiable, transient runtime context.
 * <p>
 * Sometimes it may be necessary to create local, transient copies of a context
 * which allow modifications without causing side-effects to child contexts or
 * other applications/systems sharing a context. This class implements a
 * contract for such local, transient and modifiable contexts.
 * </p>
 * <p>
 * Any changes done in a modifiable context are transient and do not apply to
 * its original context or any other child context of the original context.
 * </p>
 * <p>
 * There is no shared state between a modifiable context and its
 * {@link #getOriginalContext() original context}. Each modifiable contexts
 * instantiates its own context objects. However, configuration data (such as
 * {@link #getPreferences() context preferences}) <strong>is not</strong>
 * separated.
 * <em>Modifications of context preferences apply to the original context
 * and all created, active working copies sharing the same {@link #getContextPath() context path}!</em>
 * </p>
 * <p>
 * Note, clients must be aware that they run in a dynamic system. Therefore they
 * must not hold on a modifiable context for a longer time. A modifiable context
 * is designed with local usage in mind.
 * </p>
 * <p>
 * Modifiable contexts <strong>must</strong> be {@link #dispose() disposed} when
 * no longer needed.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 1.2
 */
public interface IModifiableRuntimeContext extends IRuntimeContext {

	/**
	 * Fails with throwing an {@link IllegalStateException}.
	 * <p>
	 * Creation of nested modifiable contexts is not supported.
	 * </p>
	 * 
	 * @throws IllegalStateException
	 *             when called; creation of nested modifiable contexts is not
	 *             supported
	 */
	@Override
	public IModifiableRuntimeContext createWorkingCopy() throws IllegalStateException;

	/**
	 * Disposes this context.
	 * <p>
	 * As a result, any changes will be discarded and any associated resources
	 * will be released.
	 * </p>
	 */
	void dispose();

	/**
	 * Returns a context object associated with the given type.
	 * <p>
	 * Lookup is performed first in the local, transient context data set. If
	 * any modification is found, it is applied and the result will be returned.
	 * If no modification is found, the lookup will be delegated to the
	 * {@link #getOriginalContext() original context}.
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
	 * @see IRuntimeContext#get(java.lang.Class)
	 */
	@Override
	<T> T get(Class<T> type) throws IllegalArgumentException;

	/**
	 * Returns the {@link IRuntimeContextInjector injector} for this context.
	 * <p>
	 * The injector can be used to inject context content into custom objects.
	 * The context content will be the combined content from the local
	 * modifications as well as the {@link #getOriginalContext() original
	 * context}. Local modifications overwrite any data in the original context.
	 * </p>
	 * <p>
	 * Injection of local modified data is never dynamic. Thus, when a local
	 * context is modified again any injected objects will never be updated
	 * automatically. They must be re-injected manually if necessary.
	 * </p>
	 * 
	 * @return the {@link IRuntimeContextInjector injector} instance of the
	 *         context
	 * @see IRuntimeContext#getInjector()
	 */
	@Override
	IRuntimeContextInjector getInjector();

	/**
	 * Returns the original, unmodifiable context this modifiable context has
	 * been created from.
	 * 
	 * @return the original context
	 */
	IRuntimeContext getOriginalContext();

	/**
	 * Returns the context preferences of the {@link #getOriginalContext()
	 * original context}.
	 * 
	 * @return the {@link IRuntimeContextPreferences preferences} instance of
	 *         the context
	 * @see IRuntimeContext#getPreferences()
	 */
	@Override
	IRuntimeContextPreferences getPreferences();

	/**
	 * Returns the service locator of the {@link #getOriginalContext() original
	 * context}.
	 * 
	 * @param bundleContext
	 *            the bundle context the locator shall use for lookups (must not
	 *            be <code>null</code>)
	 * @return the {@link IRuntimeContextServiceLocator service locator}
	 *         instance of the original context
	 * @see IRuntimeContext#getServiceLocator(org.osgi.framework.BundleContext)
	 */
	@Override
	IRuntimeContextServiceLocator getServiceLocator(BundleContext bundleContext);

	/**
	 * Indicates if a value is set locally.
	 * 
	 * @param type
	 *            the class of the value type to check (may not be
	 *            <code>null</code>)
	 * @return <code>true</code> if the value is set locally, <code>false</code>
	 *         otherwise
	 * @throws IllegalArgumentException
	 *             if the specified type is <code>null</code>
	 */
	boolean isLocal(Class<?> type) throws IllegalArgumentException;

	/**
	 * Sets a local context object to be used in {@link #get(Class) lookups}.
	 * <p>
	 * Any locally set object takes precedence in lookups.
	 * </p>
	 * <p>
	 * The value to be set may be <code>null</code> which will force any call to
	 * {@link #get(Class)} to return <code>null</code> even in case the original
	 * context would return a non-<code>null</code> result. In order to really
	 * unset a local value {@link #unsetLocal(Class)} must be used.
	 * </p>
	 * 
	 * @param <T>
	 *            the expected type of the value
	 * @param type
	 *            the class of the value type to set (may not be
	 *            <code>null</code>)
	 * @param value
	 *            the local value to set (maybe <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if the specified type is <code>null</code>
	 */
	<T> void setLocal(Class<T> type, T value) throws IllegalArgumentException;

	/**
	 * Unsets a local context object.
	 * <p>
	 * As a result, any locally set object will be cleared so that calls to
	 * {@link #get(Class)} return the value from the
	 * {@link #getOriginalContext() original context}.
	 * </p>
	 * 
	 * @param type
	 *            the class of the value type to unset (may not be
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if the specified type is <code>null</code>
	 */
	void unsetLocal(Class<?> type) throws IllegalArgumentException;

}
