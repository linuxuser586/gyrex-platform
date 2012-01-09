/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.provisioning;

import org.eclipse.gyrex.persistence.storage.Repository;

/**
 * A lightweight description of a provisioning operation.
 * <p>
 * This class must be subclassed by clients that contribute a repository
 * implementation to Gyrex. As such it is considered part of a service provider
 * API which may evolve faster than the general API. Please get in touch with
 * the development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public abstract class ProvisioningOperationDescriptor {

	private final String id;
	private final Repository repository;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 */
	protected ProvisioningOperationDescriptor(final String id, final Repository repository) {
		this.id = id;
		this.repository = repository;
	}

	/**
	 * Returns the identifier of the provisioning operation.
	 * <p>
	 * The identifier uniquely identifies the operation within the repository it
	 * is associated with.
	 * </p>
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the repository this operation must be executed in
	 * 
	 * @return the repository
	 */
	public Repository getRepository() {
		return repository;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ProvisioningOperationDescriptor [id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}
}
