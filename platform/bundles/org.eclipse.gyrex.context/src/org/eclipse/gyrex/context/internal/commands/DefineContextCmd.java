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

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.registry.ContextDefinition;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;

import org.eclipse.core.runtime.Path;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;

/**
 *
 */
public class DefineContextCmd extends Command {

	@Argument(index = 0, usage = "the context path", required = true, metaVar = "PATH")
	String pathStr;

	@Argument(index = 1, usage = "the context name", required = true, metaVar = "NAME")
	String name;

	/**
	 * Creates a new instance.
	 */
	public DefineContextCmd() {
		super("<path> <name> - defines a context");
	}

	@Override
	protected void doExecute() throws Exception {
		if (StringUtils.isBlank(pathStr) || !Path.EMPTY.isValidPath(pathStr)) {
			ci.println("ERROR: invalid path");
			return;
		}

		if (StringUtils.isBlank(name)) {
			ci.println("ERROR: invalid name");
			return;
		}

		final ContextDefinition definition = new ContextDefinition(new Path(pathStr));
		definition.setName(name);
		getRegistry().saveDefinition(definition);
		ci.println("context defined");
	}

	protected ContextRegistryImpl getRegistry() {
		return ContextActivator.getInstance().getContextRegistryImpl();
	}
}
