/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.content;

import java.util.Collection;

/**
 * A provider for {@link RepositoryContentType}s to the system.
 * <p>
 * Any content type available in a system must be made available by registering
 * an OSGi service using {@link #SERVICE_NAME} which implements this interface.
 * </p>
 */
public interface IRepositoryContentTypeProvider {

	/** the service name */
	public static final String SERVICE_NAME = IRepositoryContentTypeProvider.class.getName();

	/**
	 * Returns a list of provided content types.
	 * <p>
	 * The collection returned must not change between subsequent invocations.
	 * </p>
	 * 
	 * @return an unmodifiable, immutable list of provided content types
	 */
	Collection<RepositoryContentType> getContentTypes();

}
