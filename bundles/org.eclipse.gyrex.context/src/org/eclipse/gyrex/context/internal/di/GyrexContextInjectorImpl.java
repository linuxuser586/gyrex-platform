/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal.di;

import org.eclipse.gyrex.context.internal.GyrexContextImpl;

public class GyrexContextInjectorImpl extends BaseContextInjector {

	public GyrexContextInjectorImpl(final GyrexContextImpl context) {
		super(new GyrexContextObjectSupplier(context));
	}

}
