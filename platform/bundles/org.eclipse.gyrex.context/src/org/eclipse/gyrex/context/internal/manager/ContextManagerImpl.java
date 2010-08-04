/**
 * Copyright (c) 2009, 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.internal.manager;

import org.eclipse.gyrex.common.lifecycle.IShutdownParticipant;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.configuration.ContextConfiguration;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.manager.IRuntimeContextManager;

import org.osgi.framework.Filter;

/**
 * {@link IRuntimeContextManager} implementation.
 */
// TODO: this should be a ServiceFactory which knows about the bundle requesting the manager for context access permission checks
public class ContextManagerImpl implements IRuntimeContextManager, IShutdownParticipant {

	private final ContextRegistryImpl contextRegistry;

	/**
	 * Creates a new instance.
	 * 
	 * @param contextRegistry
	 */
	public ContextManagerImpl(final ContextRegistryImpl contextRegistry) {
		this.contextRegistry = contextRegistry;
	}

	@Override
	public void set(final IRuntimeContext context, final Class<?> type, final Filter filter) {
		ContextConfiguration.setFilter(context, type.getName(), filter);

		// for now, we can only flush the contexts
		// TODO investigate individual value flushing
		contextRegistry.flushContextHierarchy(context);
	}

	@Override
	public void shutdown() throws Exception {
		// nothing to do
	}

}
