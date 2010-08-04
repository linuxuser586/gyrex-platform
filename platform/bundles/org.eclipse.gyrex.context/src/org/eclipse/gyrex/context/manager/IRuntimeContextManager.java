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
package org.eclipse.gyrex.context.manager;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.osgi.framework.Filter;

/**
 * A manager for configuring {@link IRuntimeContext contexts}.
 * <p>
 * The manager provides APIs for managing {@link IRuntimeContext contexts}. It
 * is made available to clients as an OSGi service. Security restrictions may
 * not allow all code to access the manager.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @see IRuntimeContext
 */
public interface IRuntimeContextManager {

	/**
	 * Configures the specified context with a filter for the specified type
	 * implementation.
	 * 
	 * @param context
	 *            the context to configure
	 * @param type
	 *            the type
	 * @param filter
	 *            the filter to use when retrieving types
	 */
	void set(IRuntimeContext context, Class<?> type, Filter filter);

}
