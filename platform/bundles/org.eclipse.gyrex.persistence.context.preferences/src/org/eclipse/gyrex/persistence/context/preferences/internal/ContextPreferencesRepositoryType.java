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
package org.eclipse.gyrex.persistence.context.preferences.internal;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.persistence.context.preferences.ContextPreferencesRepository;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

/**
 * A repository type which stores objects in a context preferences.
 */
public class ContextPreferencesRepositoryType extends RepositoryProvider {

	/** the repository type id */
	public static final String ID = "org.eclipse.gyrex.persistence.context.preferences";

	private IRuntimeContextRegistry contextRegistry;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 */
	public ContextPreferencesRepositoryType() {
		super(ID, ContextPreferencesRepository.class);
	}

	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		final String contextPathStr = repositoryPreferences.getPreferences().get("contextPath", null);
		if (contextPathStr == null) {
			throw new IllegalStateException(NLS.bind("No context path configured for repository {0}.", repositoryId));
		}

		final IPath path = new Path(contextPathStr);
		final IRuntimeContext context = getContextRegistry().get(path);
		if (context == null) {
			throw new IllegalStateException(NLS.bind("No context accessible with context path {0} configured for repository {1}.", path.toString(), repositoryId));
		}

		return new ContextPreferencesRepositoryImpl(repositoryId, this, context);
	}

	/**
	 * Returns the contextRegistry.
	 * 
	 * @return the contextRegistry
	 */
	public IRuntimeContextRegistry getContextRegistry() {
		final IRuntimeContextRegistry registry = contextRegistry;
		if (registry == null) {
			throw new IllegalStateException("Context registry not available!");
		}
		return registry;
	}

	/**
	 * Sets the contextRegistry.
	 * 
	 * @param contextRegistry
	 *            the contextRegistry to set
	 */
	public void setContextRegistry(final IRuntimeContextRegistry contextRegistry) {
		this.contextRegistry = contextRegistry;
	}

}
