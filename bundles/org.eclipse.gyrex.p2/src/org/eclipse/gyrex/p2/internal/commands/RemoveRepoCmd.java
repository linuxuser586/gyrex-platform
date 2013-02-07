/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.commands;

import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.kohsuke.args4j.Argument;

/**
 * 
 */
public final class RemoveRepoCmd extends BaseSwCmd {

	@Argument(index = 0, usage = "the repository id", required = true, metaVar = "ID")
	String id;

	public RemoveRepoCmd() {
		super("<id> - removes a repository");
	}

	@Override
	protected void doExecute() throws Exception {
		if (!IdHelper.isValidId(id)) {
			printf("ERROR: invalid repo id");
			return;
		}

		getRepositoryManager().removeRepository(id);
		ci.println("repository removed");
	}
}