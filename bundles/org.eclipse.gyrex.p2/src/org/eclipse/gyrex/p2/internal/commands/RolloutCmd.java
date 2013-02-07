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

import org.kohsuke.args4j.Argument;

public final class RolloutCmd extends BaseSwCmd {

	@Argument(index = 0, usage = "the package id", required = true, metaVar = "ID")
	String id;

	public RolloutCmd() {
		super("<ID> - marks a package for rollout");
	}

	@Override
	protected void doExecute() throws Exception {
		if (!IdHelper.isValidId(id)) {
			printf("ERROR: invalid package id");
			return;
		}

		final PackageDefinition packageDefinition = getPackageManager().getPackage(id);
		if (null == packageDefinition) {
			printf("ERROR: package not found");
			return;
		}

		getPackageManager().markedForInstall(packageDefinition);
		printf("package marked for rollout");
	}
}