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
package org.eclipse.gyrex.persistence.storage;

/**
 * Interface with shared, public repository constants.
 */
public interface IRepositoryContstants {

	/**
	 * optional OSGi service property which specifies a repository description
	 * (value {@value #SERVICE_PROPERTY_REPOSITORY_DESCRIPTION})
	 */
	String SERVICE_PROPERTY_REPOSITORY_DESCRIPTION = "gyrex.repository.description";

	/**
	 * optional OSGi service property which specifies a repository id (value
	 * {@value #SERVICE_PROPERTY_REPOSITORY_ID})
	 */
	String SERVICE_PROPERTY_REPOSITORY_ID = "gyrex.repository.id";

}
