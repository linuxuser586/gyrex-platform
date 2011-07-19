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

import com.mongodb.Mongo;
import com.mongodb.MongoURI;

public class ConfiguredPool extends Command {

	@Argument(index = 0, usage = "specifies the pool identifier", metaVar = "POOLID", required = true)
	private String poolId;

	@Argument(index = 1, usage = "specifies the Mongo connect URI", metaVar = "URI", required = true)
	private String uri;

	/**
	 * Creates a new instance.
	 */
	public ConfiguredPool() {
		super("<poolId> <uri> - creates/configures a pool");
	}

	@Override
	protected void doExecute() throws Exception {
		// check URI & connection
		final Mongo mongo = new MongoURI(uri).connect();
		mongo.close();

		// configure
		MongoDbRegistry.configurePool(poolId, uri);
		printf("Configured pool '%s' with '%s'.", poolId, uri);
	}

}
