/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.persistence.internal.storage.DefaultRepositoryLookupStrategy;
import org.eclipse.gyrex.persistence.internal.storage.IRepositoryLookupStrategy;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;

/**
 * This class provides utility methods for working with the persistence API.
 * <p>
 * This class is not intended to be subclassed or instantiated. It provides
 * static methods to streamline the repository access.
 * </p>
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class PersistenceUtil {

	/**
	 * Returns the repository from the specified context that is capable of
	 * storing content of the specified content type.
	 * <p>
	 * Note, there is the global assumption that there will every be only one
	 * <em>"active"</em> repository per content type for the same context.
	 * </p>
	 * 
	 * @param context
	 *            the context to lookup the repository from (may not be
	 *            <code>null</code>)
	 * @param repositoryContentType
	 *            the content type that should be stored in the repository (may
	 *            not be <code>null</code>)
	 * @return the repository
	 * @throws IllegalStateException
	 *             if no suitable repository is available
	 */
	public static Repository getRepository(final IRuntimeContext context, final RepositoryContentType repositoryContentType) {
		// get the strategy
		IRepositoryLookupStrategy strategy = context.get(IRepositoryLookupStrategy.class);
		if (null == strategy) {
			// fallback to default
			strategy = DefaultRepositoryLookupStrategy.getDefault();
		}

		// get the repository
		return strategy.getRepository(context, repositoryContentType);
	}

	/**
	 * Hidden constructor.
	 */
	private PersistenceUtil() {
		// empty
	}

}
