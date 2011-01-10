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

/**
 * This package defines the base exceptions for the Repository API.
 * <p>
 * The Repository API defines a basic set of exceptions as a foundation of
 * structured error handling. The base class is {@link RepositoryException}
 * which extends {@link RuntimeException}. Repository implementors should
 * re-use as many exceptions as possible. This allows client code
 * to handle a basic set of the common exceptions without depending on
 * implementation specific code (eg. JPA, JDO, JDBC, etc.).
 * </p>
 * <p>
 * Implementors are encourage to contribute additional exceptions to the
 * Repository API package. Please get in touch with the development team.
 * </p
 */
package org.eclipse.gyrex.persistence.storage.exceptions;

