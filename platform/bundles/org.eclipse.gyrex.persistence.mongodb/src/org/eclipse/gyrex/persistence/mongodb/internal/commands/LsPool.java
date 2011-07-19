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

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.persistence.mongodb.internal.MongoDbRegistry;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

public class LsPool extends Command {

	/**
	 * Creates a new instance.
	 */
	public LsPool() {
		super("list configured pools");
	}

	@Override
	protected void doExecute() throws Exception {
		final Preferences node = MongoDbRegistry.getPoolsNode();
		final SortedSet<String> poolIds = new TreeSet<String>(Arrays.asList(node.childrenNames()));
		for (final String poolId : poolIds) {
			printf("%s (%s)", node.node(poolId).get(MongoDbRegistry.PREF_KEY_URI, StringUtils.EMPTY));
		}
	}

}
