/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/

/**
 * This package defines API for a administrating the Jetty web engine.
 * <p>
 * It's exposed as API in order to allow external management of cloud nodes.
 * However, some limitations apply. This package represents an administration
 * API which is tightly coupled to an internal technology. As such, it may
 * evolve quicker than usual APIs and may not follow the <a
 * href="http://wiki.eclipse.org/Version_Numbering" target="_blank">Eclipse
 * version guidelines</a>.
 * </p>
 * <p>
 * Clients using this API should inform the Gyrex development team through it's
 * preferred channels (eg. development mailing). They should also define a more
 * strict package version range (eg. <code>[1.0.0,1.1.0)</code>) when importing
 * this package (or any other sub-package).
 * </p>
 */
package org.eclipse.gyrex.http.jetty.admin;