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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Lists preferences
 */
public class LsCmd extends Command {

	@Option(name = "-r", aliases = "--recursive", required = false, usage = "recursively go into child nodes")
	protected boolean recursive = false;

	@Argument(index = 0, required = true, usage = "path to a preference node (use // at the end to list keys of a node)")
	protected String path;

	/**
	 * Creates a new instance.
	 */
	public LsCmd() {
		super("[-r] [<path>] - list preferences");
	}

	@Override
	protected void doExecute() throws Exception {
		final IPreferencesService preferencesService = EclipsePreferencesUtil.getPreferencesService();

		final IEclipsePreferences node = preferencesService.getRootNode();

		final String[] decodedPath = EclipsePreferencesUtil.decodePath(StringUtils.trimToEmpty(path));
		final Preferences preferencesNode = node.node(StringUtils.trimToEmpty(decodedPath[0]));
		if (recursive) {
			printTree(0, preferencesNode);
		} else {
			if (StringUtils.isNotBlank(decodedPath[1])) {
				printValue(preferencesNode, decodedPath[1]);
			} else {
				final String[] keys = preferencesNode.keys();
				for (final String key : keys) {
					printValue(preferencesNode, key);
				}
				final String[] childrenNames = preferencesNode.childrenNames();
				for (final String child : childrenNames) {
					printChildInfo(0, preferencesNode.node(child));
				}
			}
		}
	}

	private void printChildInfo(final int indent, final Preferences node) throws Exception {
		final String[] children = node.childrenNames();
		final StrBuilder spaces = new StrBuilder();
		for (int i = 0; i < indent; i++) {
			spaces.append(" ");
		}
		printf(spaces.append(node.absolutePath()).append(" (").append(children.length).append(")").toString());
	}

	private void printTree(final int indent, final Preferences node) throws Exception {
		printChildInfo(indent, node);

		final String[] children = node.childrenNames();
		for (final String child : children) {
			printTree(indent + 1, node.node(child));
		}

	}

	private void printValue(final Preferences preferencesNode, final String key) {
		final String value = preferencesNode.get(key, null);
		if (null != value) {
			printf("%s: %s=%s", preferencesNode.absolutePath(), key, value);
		} else {
			printf("%s: %s not set", preferencesNode.absolutePath(), key);
		}
	}

}
