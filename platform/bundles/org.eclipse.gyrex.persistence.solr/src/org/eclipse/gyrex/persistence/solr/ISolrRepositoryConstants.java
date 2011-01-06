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
package org.eclipse.gyrex.persistence.solr;

import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

/**
 * Interface with shared constants.
 */
public interface ISolrRepositoryConstants {

	/** the {@link RepositoryProvider#getProviderId() repository provider id} */
	String PROVIDER_ID = "org.eclipse.gyrex.persistence.solr";

}
