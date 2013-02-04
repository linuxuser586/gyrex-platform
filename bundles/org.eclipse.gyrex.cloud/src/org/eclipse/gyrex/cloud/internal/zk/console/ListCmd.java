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
package org.eclipse.gyrex.cloud.internal.zk.console;

import java.util.List;

import org.apache.commons.lang.text.StrBuilder;
import org.apache.zookeeper.ZooKeeper;

/**
 *
 */
public class ListCmd extends RecursivePathBasedCmd {

	/**
	 * Creates a new instance.
	 */
	public ListCmd() {
		super("- lists the specified path");
	}

	@Override
	protected void doExecute(final ZooKeeper zk, final String path) throws Exception {
		if (recursive) {
			printTree(path, 0, zk);
		} else {
			final List<String> children = zk.getChildren(path, false);
			for (final String child : children) {
				ci.println(child);
			}
		}

	}

	private void printTree(final String path, final int indent, final ZooKeeper zk) throws Exception {
		final List<String> children = zk.getChildren(path, false);
		final StrBuilder spaces = new StrBuilder();
		for (int i = 0; i < indent; i++) {
			spaces.append(" ");
		}
		ci.println(spaces.append(path).append(" (").append(children.size()).append(")"));

		for (final String child : children) {
			printTree(path + (path.equals("/") ? "" : "/") + child, indent + 1, zk);
		}

	}
}
