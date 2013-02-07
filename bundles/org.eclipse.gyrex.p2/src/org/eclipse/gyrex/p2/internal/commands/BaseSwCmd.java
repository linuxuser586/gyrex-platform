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

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.packages.IPackageManager;
import org.eclipse.gyrex.p2.internal.repositories.IRepositoryDefinitionManager;

public abstract class BaseSwCmd extends Command {

	public BaseSwCmd(final String description) {
		super(description);
	}

	protected IPackageManager getPackageManager() {
		return P2Activator.getInstance().getPackageManager();
	}

	protected IRepositoryDefinitionManager getRepositoryManager() {
		return P2Activator.getInstance().getRepositoryManager();
	}

}
