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
package org.eclipse.gyrex.context.internal.commands;

import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;
import org.eclipse.gyrex.context.internal.registry.ContextDefinition;

import org.apache.commons.lang.text.StrBuilder;

/**
 * Dumps a context
 */
public class DumpContextCmd extends BaseContextDefinitionCmd {

	/**
	 * Creates a new instance.
	 */
	public DumpContextCmd() {
		super("<path> - dumps a context");
	}

	@Override
	protected void doExecute(final ContextDefinition contextDefinition) throws Exception {
		// get real context
		if (!ContextActivator.getInstance().getContextRegistryImpl().hasRealContext(contextDefinition.getPath())) {
			printf("Context not instantiated!");
			return;
		}

		final GyrexContextImpl context = ContextActivator.getInstance().getContextRegistryImpl().getRealContext(contextDefinition.getPath());
		final StrBuilder dump = new StrBuilder();
		context.dump(dump);
		printf(dump.toString());
	}

}
