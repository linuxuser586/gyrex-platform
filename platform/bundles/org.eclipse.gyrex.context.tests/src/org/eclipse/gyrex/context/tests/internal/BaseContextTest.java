/*******************************************************************************
 * Copyright (c) 2010 AGETO and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.tests.internal;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.manager.IRuntimeContextManager;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;

import org.eclipse.core.runtime.IPath;

import org.junit.After;
import org.junit.Before;

/**
 * Base class for test cases that deal with contexts.
 */
public abstract class BaseContextTest {

	private IRuntimeContext context;

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public IRuntimeContext getContext() {
		return context;
	}

	/**
	 * Returns the contextManager.
	 * 
	 * @return the contextManager
	 */
	public IRuntimeContextManager getContextManager() {
		return Activator.getActivator().getContextManager();
	}

	/**
	 * Returns the contextRegistry.
	 * 
	 * @return the contextRegistry
	 */
	public IRuntimeContextRegistry getContextRegistry() {
		return Activator.getActivator().getContextRegistry();
	}

	/**
	 * Returns the context path that should be used for {@link #getContext()}.
	 * 
	 * @return the context path
	 */
	protected abstract IPath getPrimaryTestContextPath();

	/**
	 * Called during setup to initialize the context.
	 * <p>
	 * Default is empty. Subclasses may override.
	 * </p>
	 * 
	 * @throws Exception
	 */
	protected void initContext() throws Exception {
		// empty
	}

	/**
	 * Sets the context.
	 * 
	 * @param context
	 *            the context to set
	 */
	public void setContext(final IRuntimeContext context) {
		this.context = context;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		final IPath path = getPrimaryTestContextPath();
		if (path != null) {
			setContext(getContextRegistry().get(path));
			initContext();
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

}
