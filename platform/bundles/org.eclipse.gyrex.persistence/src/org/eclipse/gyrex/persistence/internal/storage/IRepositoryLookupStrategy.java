/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.storage;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;

/**
 * A strategy for the look up of {@link Repository repositories} from a context.
 * <p>
 * This interface may be implemented by clients. However, this should only be
 * necessary in rare cases. Usually, the platform provides a
 * {@link DefaultRepositoryLookupStrategy default strategy} which suites the
 * common platform usage pattern.
 * </p>
 * 
 * @see DefaultRepositoryLookupStrategy
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRepositoryLookupStrategy {

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
	Repository getRepository(IRuntimeContext context, RepositoryContentType repositoryContentType) throws IllegalStateException;
}
