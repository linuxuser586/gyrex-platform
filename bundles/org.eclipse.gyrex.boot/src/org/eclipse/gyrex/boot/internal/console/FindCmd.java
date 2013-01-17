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
package org.eclipse.gyrex.boot.internal.console;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.gyrex.boot.internal.BootActivator;
import org.eclipse.gyrex.common.console.Command;

import org.osgi.framework.Bundle;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.kohsuke.args4j.Argument;

public class FindCmd extends Command {

	@Argument(index = 0, usage = "an optional bundle symbolic name filter string", required = false)
	String filter;

	/**
	 * Creates a new instance.
	 */
	public FindCmd() {
		super("searches bundles for possible debug options (requires .options file in bundle)");
	}

	@Override
	protected void doExecute() throws Exception {
		final Bundle[] bundles = BootActivator.getInstance().getContext().getBundles();
		for (final Bundle bundle : bundles) {
			// skip bundles that are installed or uninstalled
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) != 0) {
				continue;
			}

			// check filter
			final String symbolicName = bundle.getSymbolicName();
			if ((null != filter) && !StringUtils.containsIgnoreCase(symbolicName, filter)) {
				continue;
			}

			// find options file
			final URL optionsEntry = bundle.getEntry(".options");
			if (null == optionsEntry) {
				continue;
			}

			// print options file
			final InputStream stream = optionsEntry.openStream();
			try {
				final List<String> lines = IOUtils.readLines(stream);

				final StrBuilder string = new StrBuilder();
				final int padWidth = symbolicName.length() + 6;
				string.appendPadding(padWidth, '-').appendNewLine();
				string.append("   ").append(symbolicName).appendNewLine();
				string.appendPadding(padWidth, '-').appendNewLine();
				for (final String line : lines) {
					string.append("> ").appendln(line);
				}
				printf(string.toString());
			} finally {
				IOUtils.closeQuietly(stream);
			}
		}
	}
}