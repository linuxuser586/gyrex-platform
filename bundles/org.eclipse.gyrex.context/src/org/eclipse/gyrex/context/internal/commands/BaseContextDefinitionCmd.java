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

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.context.definitions.ContextDefinition;
import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;

import org.eclipse.core.runtime.Path;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;

/**
 * Base class for context commands
 */
public abstract class BaseContextDefinitionCmd extends Command {

	@Argument(index = 0, usage = "the context path", required = true, metaVar = "PATH")
	String pathStr;

	/**
	 * Creates a new instance.
	 */
	public BaseContextDefinitionCmd(final String description) {
		super(description);
	}

	@Override
	protected void doExecute() throws Exception {
		if (StringUtils.isBlank(pathStr) || !Path.EMPTY.isValidPath(pathStr)) {
			printf("ERROR: invalid path");
			return;
		}

		final ContextDefinition contextDefinition = getRegistry().getDefinition(new Path(pathStr));
		if (null == contextDefinition) {
			printf("ERROR: context does not exist");
			return;
		}

		doExecute(contextDefinition);
	}

	protected abstract void doExecute(ContextDefinition contextDefinition) throws Exception;

	protected ContextRegistryImpl getRegistry() {
		return ContextActivator.getInstance().getContextRegistryImpl();
	}

}
