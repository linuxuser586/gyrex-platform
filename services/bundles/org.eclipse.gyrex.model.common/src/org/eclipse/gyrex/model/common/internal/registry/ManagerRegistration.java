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
package org.eclipse.gyrex.model.common.internal.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.model.common.IModelManager;
import org.eclipse.gyrex.model.common.provider.ModelProvider;
import org.eclipse.gyrex.persistence.PersistenceUtil;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.osgi.framework.Bundle;

/**
 *
 */
public class ManagerRegistration implements IAdapterFactory {

	private final List<Class> adapterList = new ArrayList<Class>(1);
	private final String managerClassName;
	private final List<ProviderRegistration> providers = new CopyOnWriteArrayList<ProviderRegistration>();

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 */
	public ManagerRegistration(final String managerClassName) {
		this.managerClassName = managerClassName;
	}

	public void add(final Class<?> manager, final ModelProvider provider, final Bundle bundle) {
		if (!managerClassName.equals(manager.getName())) {
			throw new IllegalArgumentException("manager class name should be " + managerClassName);
		}
		if (!adapterList.contains(manager)) {
			adapterList.add(manager);
		}

		providers.add(new ProviderRegistration(manager, provider, bundle));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Object adaptableObject, final Class adapterType) {
		// we only provide adapters of IRuntimeContext objects
		if (!(adaptableObject instanceof IRuntimeContext)) {
			return null;
		}

		// we only adapt to IModelManager
		if (!IModelManager.class.isAssignableFrom(adapterType)) {
			return null;
		}

		// the context
		final IRuntimeContext context = (IRuntimeContext) adaptableObject;

		// get the provider to use
		final ModelProvider provider = getProvider(context, adapterType);
		if (null == provider) {
			return null;
		}

		// get the content type
		final RepositoryContentType contentType = provider.getContentType();

		// get the repository
		final Repository repository = PersistenceUtil.getRepository(context, contentType);

		// get the model manager for the specified context and repository
		// TODO: implement caching
		return provider.createModelManagerInstance(adapterType, repository, context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class[] getAdapterList() {
		return adapterList.toArray(new Class[0]);
	}

	/**
	 * Looks up the provider to use for the specified context.
	 * 
	 * @param context
	 * @param managerType
	 * @return the provider to use
	 */
	private ModelProvider getProvider(final IRuntimeContext context, final Class<?> managerType) {
		// currently, we simply use the first matching provider
		// in the future a context might specify a preferred provider version
		for (final ProviderRegistration provider : providers) {
			if (managerType.isAssignableFrom(provider.getManager())) {
				return provider.getProvider();
			}
		}

		// no manager available
		return null;
	}

	public boolean isEmpty() {
		return providers.isEmpty();
	}

	public void remove(final Class<?> manager, final ModelProvider provider, final Bundle bundle) {
		if (!managerClassName.equals(manager.getName())) {
			throw new IllegalArgumentException("manager class name should be " + managerClassName);
		}
		providers.remove(new ProviderRegistration(manager, provider, bundle));
	}

}
