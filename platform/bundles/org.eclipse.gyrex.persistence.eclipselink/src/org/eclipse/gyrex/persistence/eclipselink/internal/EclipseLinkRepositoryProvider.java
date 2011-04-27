/**
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.persistence.eclipselink.internal;

import org.eclipse.gyrex.persistence.eclipselink.EclipseLinkRepository;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

/**
 * Repository provider for {@link EclipseLinkRepository}.
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class EclipseLinkRepositoryProvider extends RepositoryProvider {

	/**
	 * Creates a new instance.
	 * 
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	public EclipseLinkRepositoryProvider() {
		super("org.eclipse.gyrex.persistence.eclipselink", EclipseLinkRepository.class);
	}

	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		// TODO Auto-generated method stub
		return null;
	}
}
