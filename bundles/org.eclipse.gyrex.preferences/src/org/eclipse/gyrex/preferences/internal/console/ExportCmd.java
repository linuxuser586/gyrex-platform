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

import java.io.File;
import java.io.FileOutputStream;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.preferences.internal.util.EclipsePreferencesUtil;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;

/**
 * Lists preferences
 */
public class ExportCmd extends Command {

	private static final String FILE_EXT_EPF = ".epf";

	@Argument(index = 0, required = true, usage = "file to export to")
	File file;

	@Argument(index = 1, required = false, usage = "path to a preference node")
	String path;

	/**
	 * Creates a new instance.
	 */
	public ExportCmd() {
		super("<file> [<path>] - export preferences to file");
	}

	@Override
	protected void doExecute() throws Exception {
		final IPreferencesService preferencesService = EclipsePreferencesUtil.getPreferencesService();

		if (!StringUtils.endsWithIgnoreCase(file.getName(), FILE_EXT_EPF)) {
			file = new File(file.getParentFile(), file.getName().concat(FILE_EXT_EPF));
		}

		IEclipsePreferences node = preferencesService.getRootNode();

		if (StringUtils.isNotBlank(path)) {
			node = (IEclipsePreferences) node.node(path);
		}

		final FileOutputStream output = FileUtils.openOutputStream(file);
		try {
			preferencesService.exportPreferences(node, output, null);
			printf("Successfully exported %s to %s.", node.absolutePath(), file.getAbsolutePath());
		} finally {
			IOUtils.closeQuietly(output);
		}
	}

}
