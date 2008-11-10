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
package org.eclipse.cloudfree.persistence;


import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.cloudfree.persistence.storage.DefaultRepositoryLookupStrategy;
import org.eclipse.cloudfree.persistence.storage.IRepositoryLookupStrategy;

/**
 * This class provides utility methods for working with the persistence API.
 * <p>
 * This class is not intended to be subclassed or instantiated. It provides
 * static methods to streamline the repository access.
 * </p>
 */
public final class PersistenceUtil {

	/**
	 * Returns the repository lookup strategy for the specified context.
	 * <p>
	 * This method basically calls {@link IContext#getAdapter(Class)} to obtain
	 * {@link IRepositoryLookupStrategy the lookup strategy} from the context.
	 * This allows a context to provide custom lookup strategies. If the context
	 * does not specify a custom lookup strategy a
	 * {@link DefaultRepositoryLookupStrategy default strategy} is returned.
	 * </p>
	 * 
	 * @param context
	 *            the context to obtain the strategy from (may not be
	 *            <code>null</code>)
	 * @return the lookup strategy
	 * @see IContext#getAdapter(Class)
	 * @see IRepositoryLookupStrategy
	 * @see DefaultRepositoryLookupStrategy
	 */
	public static IRepositoryLookupStrategy getRepositoryLookupStrategy(final IContext context) {
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}

		// ask the context first
		final IRepositoryLookupStrategy strategy = (IRepositoryLookupStrategy) context.getAdapter(IRepositoryLookupStrategy.class);
		if (null != strategy) {
			return strategy;
		}

		// fallback to default strategy
		return DefaultRepositoryLookupStrategy.getDefault();
	}

	/**
	 * Hidden constructor.
	 */
	private PersistenceUtil() {
		// empty
	}

}
