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

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.p2.internal.repositories.RepositoryDefinition;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import org.apache.commons.lang.StringUtils;

import org.kohsuke.args4j.Argument;

public final class AddRepoCmd extends BaseSwCmd {

	@Argument(index = 0, usage = "the repository id", required = true, metaVar = "ID")
	String id;

	@Argument(index = 1, usage = "the repository URI", required = true, metaVar = "URI")
	String uri;

	@Argument(index = 2, usage = "an optional node filter", required = false, metaVar = "FILTER")
	String filter;

	public AddRepoCmd() {
		super("<id> <uri> [filter] - adds a repository");
	}

	@Override
	protected void doExecute() throws Exception {
		if (!IdHelper.isValidId(id)) {
			printf("ERROR: invalid repo id");
			return;
		}

		URI location;
		try {
			location = new URI(uri);
		} catch (final URISyntaxException e) {
			printf("ERROR: invalid uri: %s", e.getMessage());
			return;
		}

		final RepositoryDefinition repo = new RepositoryDefinition();
		repo.setId(id);
		repo.setLocation(location);

		if (StringUtils.isNotBlank(filter)) {
			try {
				FrameworkUtil.createFilter(filter);
			} catch (final InvalidSyntaxException e) {
				printf("ERROR: invalid filter: %s", e.getMessage());
			}
			repo.setNodeFilter(filter);
		}

		getRepositoryManager().saveRepository(repo);
		ci.println("repository added");
	}
}