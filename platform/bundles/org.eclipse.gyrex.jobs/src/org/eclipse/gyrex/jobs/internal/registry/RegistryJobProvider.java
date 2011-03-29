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

import java.util.Collections;
import java.util.Map;

import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Extension registry job provider.
 */
public class RegistryJobProvider extends JobProvider {

	private final IConfigurationElement element;
	private final String id;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param element
	 */
	public RegistryJobProvider(final String id, final IConfigurationElement element) {
		super(Collections.singleton(id));
		this.id = id;
		this.element = element;
	}

	@Override
	public Job newJob(final String id, final Map<String, String> jobParameter) throws CoreException {
		if (!this.id.equals(id)) {
			return null;
		}

		return (Job) element.createExecutableExtension("class");
	}
}
