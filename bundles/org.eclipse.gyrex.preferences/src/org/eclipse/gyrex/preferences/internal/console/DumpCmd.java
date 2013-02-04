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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.kohsuke.args4j.Argument;

/**
 * Dumps preferences
 */
public class DumpCmd extends Command {

	@Argument(index = 0, required = true, usage = "path to a preference node")
	protected String path;

	/**
	 * Creates a new instance.
	 */
	public DumpCmd() {
		super("[<path>] - dumps preferences");
	}

	@Override
	protected void doExecute() throws Exception {
		final IPreferencesService preferencesService = EclipsePreferencesUtil.getPreferencesService();

		final IEclipsePreferences node = preferencesService.getRootNode();

		final String[] decodedPath = EclipsePreferencesUtil.decodePath(StringUtils.trimToEmpty(path));
		final Preferences preferencesNode = node.node(StringUtils.trimToEmpty(decodedPath[0]));
		dumpTree(0, preferencesNode);
	}

	private void dumpTree(final int indent, final Preferences node) throws Exception {
		printNodeInfo(indent, node);
		final String[] children = node.childrenNames();
		for (final String child : children) {
			dumpTree(indent + 1, node.node(child));
		}
	}

	private void printNodeInfo(final int indent, final Preferences node) throws Exception {
		final StrBuilder spaces = new StrBuilder();
		for (int i = 0; i < indent; i++) {
			spaces.append(" ");
		}
		printf(spaces.append(node.name()).append(" (").append(node.toString()).append(")").toString());
	}
}
