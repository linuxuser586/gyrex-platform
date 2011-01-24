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
package org.eclipse.gyrex.persistence.solr.internal;

import org.eclipse.gyrex.persistence.solr.ISolrRepositoryConstants;
import org.eclipse.gyrex.persistence.solr.config.ISolrRepositoryConfigurer;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;

import org.eclipse.core.runtime.IAdapterFactory;

/**
 * Adapter factory.
 */
public class SolrRepositoryAdapterFactory implements IAdapterFactory {

	private static final Class[] ADAPTERS = new Class[] { ISolrRepositoryConfigurer.class };

	private Object adapt(final IRepositoryDefinition repositoryDefinition, final Class adapterType) {
		if (ISolrRepositoryConfigurer.class.equals(adapterType) && ISolrRepositoryConstants.PROVIDER_ID.equals(repositoryDefinition.getProviderId())) {
			return new SolrRepositoryConfigurer(repositoryDefinition);
		}
		return null;
	}

	@Override
	public Object getAdapter(final Object adaptableObject, final Class adapterType) {
		if (adaptableObject instanceof IRepositoryDefinition) {
			return adapt((IRepositoryDefinition) adaptableObject, adapterType);
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return ADAPTERS;
	}

}
