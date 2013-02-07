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
import org.eclipse.gyrex.p2.internal.packages.PackageDefinition;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import org.apache.commons.lang.StringUtils;

import org.kohsuke.args4j.Argument;

public final class AddPkgCmd extends BaseSwCmd {

	@Argument(index = 0, usage = "the package id", required = true, metaVar = "ID")
	String id;

	@Argument(index = 1, usage = "an optional node filter", required = false, metaVar = "FILTER")
	String filter;

	public AddPkgCmd() {
		super("<id> - adds a package");
	}

	@Override
	protected void doExecute() throws Exception {
		if (!IdHelper.isValidId(id)) {
			printf("invalid package id");
			return;
		}

		final PackageDefinition packageDefinition = new PackageDefinition();
		packageDefinition.setId(id);

		if (StringUtils.isNotBlank(filter)) {
			try {
				FrameworkUtil.createFilter(filter);
			} catch (final InvalidSyntaxException e) {
				printf("ERROR: invalid filter: %s", e.getMessage());
			}
			packageDefinition.setNodeFilter(filter);
		}

		getPackageManager().savePackage(packageDefinition);
		printf("Package added. You should now add installation artifacts to it.");
	}
}