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
package org.eclipse.gyrex.preferences.internal.console;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.preferences.internal.util.EclipsePreferencesUtil;

import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Lists preferences
 */
public class SetCmd extends Command {

	public static enum Type {
		STRING, INT, BOOLEAN, FLOAT, DOUBLE, LONG
	};

	@Argument(index = 0, required = true, metaVar = "PATH", usage = "path to a preference key")
	String path;

	@Argument(index = 1, required = true, metaVar = "VALUE", usage = "value to set")
	String value;

	@Option(name = "-t", aliases = "--type", required = false, usage = "how to parse the preference value (string, int, boolean, float, double, long)")
	Type type = Type.STRING;

	/**
	 * Creates a new instance.
	 */
	public SetCmd() {
		super("[-t <type>] <path> <value> - sets a preference");
	}

	@Override
	protected void doExecute() throws Exception {
		final IPreferencesService preferencesService = EclipsePreferencesUtil.getPreferencesService();
		final String[] decodedPath = EclipsePreferencesUtil.decodePath(StringUtils.trimToEmpty(path));
		final Preferences node = preferencesService.getRootNode().node(StringUtils.trimToEmpty(decodedPath[0]));
		switch (type) {
			case INT:
				node.putInt(decodedPath[1], Integer.parseInt(value));
				break;
			case LONG:
				node.putLong(decodedPath[1], Long.parseLong(value));
				break;
			case BOOLEAN:
				node.putBoolean(decodedPath[1], Boolean.parseBoolean(value));
				break;
			case DOUBLE:
				node.putDouble(decodedPath[1], Double.parseDouble(value));
				break;
			case FLOAT:
				node.putFloat(decodedPath[1], Float.parseFloat(value));
				break;
			case STRING:
				node.put(decodedPath[1], value);
				break;
			default:
				throw new IllegalStateException("unhandled type: " + type);
		}
		node.flush();
	}

}
