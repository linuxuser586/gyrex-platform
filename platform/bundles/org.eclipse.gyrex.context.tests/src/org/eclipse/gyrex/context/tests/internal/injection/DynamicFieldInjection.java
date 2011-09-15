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

import org.eclipse.gyrex.context.IRuntimeContext;

public class DynamicFieldInjection {

	@Inject
	ISampleService service;
	@Inject
	IRuntimeContext context;

	@Override
	public String toString() {
		return "DynamicFieldInjection{ " + context + ", " + service + " }";
	}

	public void assertInjected() {
		assertNotNull("no context", context);
		assertNotNull("no osgi service", service);
	}
}