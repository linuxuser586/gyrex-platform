/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.context.preferences;

/**
 * Shared public constants.
 */
public interface IContextPreferencesRepositoryConstants {

	/** the repository provider id */
	String PROVIDER_ID = "org.eclipse.gyrex.persistence.context.preferences";

	/** the repository type name */
	String TYPE_NAME = ContextPreferencesRepository.class.getName();

	/**
	 * repository preference key of the context path setting (value
	 * <code>contextPath</code>)
	 */
	String PREF_KEY_CONTEXT_PATH = "contextPath";
}