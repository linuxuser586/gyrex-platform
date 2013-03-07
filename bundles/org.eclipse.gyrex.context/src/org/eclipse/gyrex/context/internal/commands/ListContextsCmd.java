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
package org.eclipse.gyrex.context.internal.commands;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.context.definitions.ContextDefinition;
import org.eclipse.gyrex.context.internal.ContextActivator;

/**
 *
 */
public class ListContextsCmd extends Command {

	/**
	 * Creates a new instance.
	 */
	public ListContextsCmd() {
		super("list defined contexts");
	}

	@Override
	protected void doExecute() throws Exception {
		final Collection<ContextDefinition> contexts = ContextActivator.getInstance().getContextRegistryImpl().getDefinedContexts();

		final SortedMap<String, ContextDefinition> sortedContexts = new TreeMap<String, ContextDefinition>();
		for (final ContextDefinition contextDefinition : contexts) {
			sortedContexts.put(contextDefinition.getPath().toString(), contextDefinition);
		}

		for (final String key : sortedContexts.keySet()) {
			ci.println(sortedContexts.get(key));
		}
	}

}
