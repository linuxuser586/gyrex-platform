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
package org.eclipse.gyrex.context.tests.internal.injection;

import static junit.framework.Assert.assertNotNull;

import javax.inject.Inject;

import org.eclipse.gyrex.context.IRuntimeContext;

public class ConstructorInjection {
	final IRuntimeContext context;

	@Inject
	public ConstructorInjection(final IRuntimeContext context) {
		assertNotNull("no context", context);
		this.context = context;
	}

	@Override
	public String toString() {
		return "ConstructorInjection{ " + context + " }";
	}
}