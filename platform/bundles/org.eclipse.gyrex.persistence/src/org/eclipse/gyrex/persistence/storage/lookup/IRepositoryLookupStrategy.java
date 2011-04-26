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
package org.eclipse.gyrex.persistence.storage.lookup;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentTypeSupport;

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
	 * Note, this lookup is based on the assumption that there will ever be only
	 * one <em>"active"</em> repository per content type for the same context.
	 * </p>
	 * <p>
	 * It is expected that the returned repository supports the specified
	 * content type, i.e. the repository's {@link RepositoryContentTypeSupport}
	 * returns <code>true</code> when calling
	 * {@link RepositoryContentTypeSupport#isSupported(RepositoryContentType)}
	 * with the given content type. In case an upgrade is necessary it's the
	 * responsibility of an external coordinator (eg. an administrator) to
	 * schedule and perform such an upgrade (see
	 * {@link RepositoryContentTypeSupport} for details).
	 * </p>
	 * 
	 * @param context
	 *            the context to lookup the repository from (may not be
	 *            <code>null</code>)
	 * @param contentType
	 *            the content type that should be stored in the repository (may
	 *            not be <code>null</code>)
	 * @return the repository
	 * @throws IllegalStateException
	 *             if no suitable repository is available
	 * @noreference This method is not intended to be referenced by clients.
	 */
	Repository getRepository(IRuntimeContext context, RepositoryContentType contentType) throws IllegalStateException;
}
