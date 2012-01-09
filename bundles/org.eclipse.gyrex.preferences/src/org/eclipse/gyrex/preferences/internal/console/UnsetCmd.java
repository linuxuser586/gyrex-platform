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

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.preferences.internal.util.EclipsePreferencesUtil;

import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;

/**
 * Unsets a preferences key
 */
public class UnsetCmd extends Command {

	@Argument(index = 0, required = true, metaVar = "PATH", usage = "path to a preference key")
	String path;

	/**
	 * Creates a new instance.
	 */
	public UnsetCmd() {
		super("<path> - unsets a preference key");
	}

	@Override
	protected void doExecute() throws Exception {
		final IPreferencesService preferencesService = EclipsePreferencesUtil.getPreferencesService();
		final String[] decodedPath = EclipsePreferencesUtil.decodePath(StringUtils.trimToEmpty(path));
		final Preferences node = preferencesService.getRootNode().node(StringUtils.trimToEmpty(decodedPath[0]));
		node.remove(decodedPath[1]);
		node.flush();
		printf("Unset property '%s' at '%s'", StringUtils.trimToEmpty(decodedPath[1]), StringUtils.trimToEmpty(decodedPath[0]));
	}

}
