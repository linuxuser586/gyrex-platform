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

import org.eclipse.gyrex.common.console.BaseCommandProvider;

/**
 *
 */
public class MongoDbCommands extends BaseCommandProvider {

	/**
	 * Creates a new instance.
	 */
	public MongoDbCommands() {
		registerCommand(PoolCommands.class);
	}

	@Override
	protected String getCommandName() {
		return "mongodb";
	}

}
