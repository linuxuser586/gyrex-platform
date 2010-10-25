/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
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
 * This package defines the repository storage model.
 * <p>
 * Gyrex supports a distributed architecture. This 
 * also applies to persistent data. In order to allow data to be stored on 
 * various systems using different technologies the concept of repository 
 * abstraction is introduced. It allows to partition data of various types 
 * across different data stores. The data stores can be - for example - 
 * relational databases, object oriented databased, LDAP databases, other 
 * file based stores, etc. There will be no limitation on the repository 
 * type. 
 * </p>
 */
package org.eclipse.gyrex.persistence.storage;

