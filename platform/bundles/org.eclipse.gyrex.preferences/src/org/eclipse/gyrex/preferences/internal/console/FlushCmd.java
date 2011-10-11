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
 * Flushs preferences
 */
public class FlushCmd extends Command {

	@Argument(index = 0, required = true, metaVar = "PATH", usage = "path to a preference node")
	String path;

	/**
	 * Creates a new instance.
	 */
	public FlushCmd() {
		super("<path> - flushes a preference hierarchy");
	}

	@Override
	protected void doExecute() throws Exception {
		final IPreferencesService preferencesService = EclipsePreferencesUtil.getPreferencesService();
		final String[] decodedPath = EclipsePreferencesUtil.decodePath(StringUtils.trimToEmpty(path));
		if (!preferencesService.getRootNode().nodeExists(decodedPath[0] + "/" + decodedPath[1])) {
			printf("ERROR: The specified node does not exist!");
			return;
		}
		final Preferences node = preferencesService.getRootNode().node(decodedPath[0] + "/" + decodedPath[1]);
		final long start = System.nanoTime();
		node.flush();
		final long duration = System.nanoTime() - start;
		if (TimeUnit.NANOSECONDS.toMillis(duration) > 1000) {
			printf("Flush finished in %d seconds.", TimeUnit.NANOSECONDS.toSeconds(duration));
		} else if (TimeUnit.NANOSECONDS.toMillis(duration) > 10) {
			printf("Flush finished in %d milli seconds.", TimeUnit.NANOSECONDS.toMillis(duration));
		} else {
			printf("Flush finished in %d nano seconds.", duration);
		}
	}

}
