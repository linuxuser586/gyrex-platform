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
package org.eclipse.cloudfree.model.common.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.cloudfree.model.common.IModelManager;
import org.eclipse.cloudfree.model.common.ModelUtil;
import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.cloudfree.persistence.storage.content.ContentType;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;

/**
 * A model provider base class which provides {@link IModelManager model
 * manager} instances to the CloudFree Platform.
 * <p>
 * A {@link ModelProvider} provides {@link IModelManager} objects. These model
 * manager objects may be obtained from a {@link IContext context} using the the
 * standard {@link IAdaptable#getAdapter(Class) Eclipse adapter mechanise}. In
 * the background, a model provider registry maintains registrations with the
 * the Eclipse {@link IAdapterManager} because it will not always be possible to
 * register adapters directly (eg., because of security constraints enforced by
 * the platform). Thus, a registration of model providers using this class is
 * enforced.
 * </p>
 * <p>
 * All model managers created by a provider share the same
 * {@link #getContentType() content type}. This assumption ensures that model
 * objects from the managers are supported by a repository. It also allows the
 * assumption that model managers created by the same provider for the same
 * context have the same repository. This simplifies the life of model manager
 * implementors because it allows the assumption that their model objects are
 * stored logically together in the same repository if the above assumption is
 * met. Note, physically coexistence cannot be guaranteed because a repository
 * might implement partitioning techniques.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute
 * {@link IModelManager} implementations. It is part of a model provider API and
 * should never be used directly by clients. Model providers must be made
 * available as OSGi services using type {@link ModelProvider}.
 * </p>
 * 
 * @see ModelUtil#getManager(Class, IContext)
 */
public abstract class ModelProvider {

	/** the list of model managers provided by the factory */
	private final List<Class<?>> providedManagers;

	/** the required content type */
	private final ContentType contentType;

	/**
	 * Creates a new factory.
	 * <p>
	 * This constructor is typical called by subclasses with the content type
	 * and a list of model managers the factory provides.
	 * </p>
	 * <p>
	 * Although not enforced in this constructor, the list should specify the
	 * public interface a model manager implements not the actual manager
	 * implementation. Each interface for one wishes to return a model manager
	 * in {@link ModelUtil#getManager(Class, IContext)} should be specified.
	 * </p>
	 * <p>
	 * If a class in the list does not extend the {@link IModelManager}
	 * interface an {@link IllegalArgumentException} will be thrown.
	 * </p>
	 * <p>
	 * Note, at least one model manager must be provided. It is possible to
	 * register additional managers through a single provider.
	 * </p>
	 * 
	 * @param contentType
	 *            the required content type for the model managers
	 * @param providedManager
	 *            a model manager interfaces the factory will provide (must
	 *            extend {@link IModelManager})
	 * @param providedManagers
	 *            the list of model manager interfaces the factory will provide
	 * @throws IllegalArgumentException
	 *             is any of the provided arguments are invalid
	 */
	protected ModelProvider(final ContentType contentType, final Class providedManager, final Class... providedManagers) throws IllegalArgumentException {
		if (null == contentType) {
			throw new IllegalArgumentException("contentType must not be null");
		}
		if (null == providedManager) {
			throw new IllegalArgumentException("providedManager must not be null");
		}
		if (!IModelManager.class.isAssignableFrom(providedManager)) {
			throw new IllegalArgumentException("manager '" + providedManager.getName() + "' is not assignable to '" + IModelManager.class.getName() + "'");
		}
		final List<Class<?>> managers = new ArrayList<Class<?>>(providedManagers.length);
		managers.add(providedManager);
		if (null != providedManagers) {
			for (final Class<?> manager : providedManagers) {
				if (null == manager) {
					throw new IllegalArgumentException("providedManagers list contains NULL entries which is not supported");
				}
				if (!IModelManager.class.isAssignableFrom(manager)) {
					throw new IllegalArgumentException("manager '" + manager.getName() + "' is not assignable to '" + IModelManager.class.getName() + "'");
				}
				managers.add(manager);
			}
		}
		this.providedManagers = Collections.unmodifiableList(managers);
		this.contentType = contentType;
	}

	/**
	 * Called by the CloudFree Platform to create a new model manager instance
	 * of the specified model manager type.
	 * <p>
	 * Subclasses must implement this method and return a manager instance which
	 * is initialized completely for using the specified repository and context.
	 * </p>
	 * 
	 * @param modelManagerType
	 *            the model manager type
	 * @param repository
	 *            the repository
	 * @param context
	 *            the context
	 * @return the model manager instance or <code>null</code>
	 * @noreference This method is not intended to be referenced by clients
	 *              directly.
	 */
	public abstract BaseModelManager createModelManagerInstance(Class modelManagerType, Repository repository, IContext context);

	/**
	 * Returns the content type required by the model managers provided by the
	 * factory.
	 * <p>
	 * The content type will be used to lookup a particular repository from a
	 * context. The repository will be capable of handling all model objects
	 * used by the managers created by this factory instance. This allows some
	 * further assumptions that simplify the life for model manager
	 * implementors.
	 * </p>
	 * 
	 * @return the content type
	 */
	public final ContentType getContentType() {
		return contentType;
	}

	/**
	 * Returns the collection of manager types contributed by this provider.
	 * <p>
	 * This method is generally used by the platform to discover which manager
	 * types are supported, in advance of dispatching any actual
	 * {@link #createModelManagerInstance(Class, Repository, IContext)}
	 * requests.
	 * </p>
	 * 
	 * @return the collection of adapter types
	 */
	public final Iterable<Class<?>> getProvidedManagers() {
		return providedManagers;
	}
}
