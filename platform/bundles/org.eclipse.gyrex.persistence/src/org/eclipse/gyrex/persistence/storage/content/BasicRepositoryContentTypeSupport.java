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
package org.eclipse.gyrex.persistence.storage.content;

import org.eclipse.gyrex.persistence.storage.Repository;

/**
 * A basic repository content type support strategy which allows compatible
 * content types but does not support provisioning of new content types.
 * <p>
 * This class may be be subclassed by clients that contribute a repository type
 * to Gyrex instead of subclassing {@link RepositoryContentTypeSupport}.
 * </p>
 */
public class BasicRepositoryContentTypeSupport extends RepositoryContentTypeSupport {

	private final Repository repository;

	/**
	 * Creates a new instance for the specified repository.
	 * 
	 * @param repository
	 *            the repository (may not be <code>null</code>)
	 */
	public BasicRepositoryContentTypeSupport(final Repository repository) {
		this.repository = repository;
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public boolean isSupported(final RepositoryContentType contentType) {
		// check that the repository type is compatible to the content type
		return repository.getRepositoryProvider().getRepositoryTypeName().equals(contentType.getRepositoryTypeName());
	}

}
