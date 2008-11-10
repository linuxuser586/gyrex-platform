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
package org.eclipse.cloudfree.common.context;

import org.eclipse.core.runtime.PlatformObject;

/**
 * An abstract superclass implementing the <code>IContext</code> interface.
 * <code>getAdapter</code> invocations are directed to the platform's adapter
 * manager.
 * <p>
 * Note: In situations where it would be awkward to subclass this class, the
 * same affect can be achieved simply by implementing the <code>IContext</code>
 * interface and explicitly forwarding the <code>getAdapter</code> request to
 * the platform's adapter manager. The method would look like:
 * 
 * <pre>
 * public Object getAdapter(Class adapter) {
 * 	return Platform.getAdapterManager().getAdapter(this, adapter);
 * }
 * </pre>
 * 
 * </p>
 * <p>
 * Clients may subclass.
 * </p>
 * 
 * @see IContext
 */
public abstract class AbstractContext extends PlatformObject implements IContext {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cloudfree.common.context.IContext#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Class adapter) {
		return super.getAdapter(adapter);
	}

}
