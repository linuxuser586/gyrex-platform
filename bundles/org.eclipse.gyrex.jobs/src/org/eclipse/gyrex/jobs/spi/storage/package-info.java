/*******************************************************************************
 * Copyright (c) 2013 AGETO and others.
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
 * This package defines the job storage provider integration API.
 * <p>
 * In order to allow job data (such as execution history and configuration)
 * to be stored on various systems using different technologies the concept of storage
 * abstraction is used in the Jobs API. The data stores can be - for example - in 
 * relational databases, object oriented databased, LDAP databases, other 
 * file based stores, etc. There will be no limitation on the repository 
 * type. 
 * </p>
 * <p>
 * This packages defines interfaces and classes which may be implemented or subclassed by 
 * clients that contribute a storage provider implementation to Gyrex. As such it is 
 * considered part of a service provider API which may evolve faster than the general API.
 * Please get in touch with the development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
package org.eclipse.gyrex.jobs.spi.storage;

