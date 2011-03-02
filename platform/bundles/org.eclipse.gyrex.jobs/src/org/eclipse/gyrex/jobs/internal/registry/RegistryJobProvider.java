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
package org.eclipse.gyrex.jobs.internal.registry;

import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Extension registry job provider.
 */
public class RegistryJobProvider extends JobProvider {

	private final IConfigurationElement element;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param element
	 */
	public RegistryJobProvider(final String id, final IConfigurationElement element) {
		super(new String[] { id });
		this.element = element;
	}

}
