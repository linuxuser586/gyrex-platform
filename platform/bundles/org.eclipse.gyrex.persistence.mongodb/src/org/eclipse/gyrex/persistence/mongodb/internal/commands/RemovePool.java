/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.persistence.mongodb.internal.commands;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.persistence.mongodb.internal.MongoDbRegistry;

import org.kohsuke.args4j.Argument;

public class RemovePool extends Command {

	@Argument(index = 0, usage = "specifies the pool identifier", metaVar = "POOLID", required = true)
	private String poolId;

	/**
	 * Creates a new instance.
	 */
	public RemovePool() {
		super("removes a pool");
	}

	@Override
	protected void doExecute() throws Exception {
		MongoDbRegistry.removePool(poolId);
		printf("Removed pool '%s'.", poolId);
	}

}
