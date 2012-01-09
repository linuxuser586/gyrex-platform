/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.preferences.internal.console;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

/**
 * Removes a preferences node
 */
public class RemoveCmd extends PathBasedCmd {

	/**
	 * Creates a new instance.
	 */
	public RemoveCmd() {
		super("- removes a preference node");
	}

	@Override
	protected void doExecute(final Preferences node) throws Exception {
		final Preferences parent = node.parent();
		node.removeNode();
		parent.flush();
		printf("Removed node '%s'", StringUtils.trimToEmpty(path));
	}
}
