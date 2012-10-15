/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.di;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Qualifier;

import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;

/**
 * String-based {@linkplain Qualifier qualifier} optimized for selection of a
 * proper {@link Repository} to inject.
 * <p>
 * Example usage:
 * 
 * <pre>
 *   public class OrderService {
 *     &#064;Inject <b>@RequiredContentType("x-jdbc/orders")</b> Repository ordersRepo;
 *     &#064;Inject <b>@RequiredContentType("x-jdbc/products")</b> Repository productsRepo;
 *     ...
 *   }
 * </pre>
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface RequiredContentType {

	/**
	 * The full media type of the repository content type without any
	 * parameters.
	 * <p>
	 * This is typically the output of
	 * {@link RepositoryContentType#getMediaType()}
	 * </p>
	 * 
	 * @return the media type (equivalent to
	 *         {@link RepositoryContentType#getMediaType()}
	 */
	String value();

	/**
	 * The version to match.
	 * 
	 * @return the version, defaults to '0.0.0' which indicates any.
	 */
	String version() default "0.0.0";
}
