/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.tests.internal.injection;

import static junit.framework.Assert.assertNotNull;

import javax.inject.Inject;

public class ConstructorInjectionWithOsgiService {
	final ISampleService service;

	@Inject
	public ConstructorInjectionWithOsgiService(final ISampleService service) {
		assertNotNull("no osgi service", service);
		this.service = service;
	}

	@Override
	public String toString() {
		return "ConstructorInjectionWithOsgiService{ " + service + " }";
	}
}