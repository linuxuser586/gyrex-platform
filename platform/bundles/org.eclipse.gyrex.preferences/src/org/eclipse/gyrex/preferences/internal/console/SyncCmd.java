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

import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.preferences.internal.util.EclipsePreferencesUtil;

import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;

/**
 * Syncs preferences
 */
public class SyncCmd extends Command {

	@Argument(index = 0, required = true, metaVar = "PATH", usage = "path to a preference node")
	String path;

	/**
	 * Creates a new instance.
	 */
	public SyncCmd() {
		super("<path> - syncs a preference hierarchy");
	}

	@Override
	protected void doExecute() throws Exception {
		final IPreferencesService preferencesService = EclipsePreferencesUtil.getPreferencesService();
		final String[] decodedPath = EclipsePreferencesUtil.decodePath(StringUtils.trimToEmpty(path));
		final Preferences node = preferencesService.getRootNode().node(StringUtils.trimToEmpty(decodedPath[0]));
		final long start = System.nanoTime();
		node.sync();
		final long duration = System.nanoTime() - start;
		if (TimeUnit.NANOSECONDS.toMillis(duration) > 1000) {
			printf("Sync finished in %d seconds.", TimeUnit.NANOSECONDS.toSeconds(duration));
		} else if (TimeUnit.NANOSECONDS.toMillis(duration) > 10) {
			printf("Sync finished in %d milli seconds.", TimeUnit.NANOSECONDS.toMillis(duration));
		} else {
			printf("Sync finished in %d nano seconds.", duration);
		}
	}

}
