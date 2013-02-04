/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.preferences.internal.util.EclipsePreferencesUtil;

import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;

/**
 * Base class for path based preference operations
 */
public abstract class PathBasedCmd extends Command {

	@Argument(index = 0, required = true, metaVar = "PATH", usage = "path to a preference node")
	protected String path;

	/**
	 * Creates a new instance.
	 */
	public PathBasedCmd(final String description) {
		super("<path> " + description);
	}

	@Override
	protected void doExecute() throws Exception {
		final IPreferencesService preferencesService = EclipsePreferencesUtil.getPreferencesService();
		final String[] decodedPath = EclipsePreferencesUtil.decodePath(StringUtils.trimToEmpty(path));
		if (!preferencesService.getRootNode().nodeExists(StringUtils.trimToEmpty(decodedPath[0]) + "/" + decodedPath[1])) {
			printf("ERROR: The specified node does not exist!");
			return;
		}
		final Preferences node = preferencesService.getRootNode().node(StringUtils.trimToEmpty(decodedPath[0]) + "/" + decodedPath[1]);
		doExecute(node);
	}

	protected abstract void doExecute(final Preferences node) throws Exception;

}
