/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.zk.console;

import org.kohsuke.args4j.Option;

public abstract class RecursivePathBasedCmd extends PathBasedCmd {

	@Option(name = "-r", aliases = "--recursive")
	protected boolean recursive;

	/**
	 * Creates a new instance.
	 */
	public RecursivePathBasedCmd(final String description) {
		super("[-r] " + description);
	}

}
