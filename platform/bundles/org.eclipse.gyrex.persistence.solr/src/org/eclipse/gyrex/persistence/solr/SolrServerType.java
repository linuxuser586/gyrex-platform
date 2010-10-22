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

import org.apache.solr.client.solrj.SolrServer;

/**
 * Supported {@link SolrServer} types.
 */
public enum SolrServerType {
	/** a Solr instance running inside the local VM */
	EMBEDDED,

	/** a Solr instance running remotely */
	REMOTE
}
