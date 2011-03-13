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
package org.eclipse.gyrex.cloud.internal.preferences;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.admin.NodeScope;
import org.eclipse.gyrex.cloud.internal.CloudDebug;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScope;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IScope} preferences factory for <code>node</code> preferences.
 */
public class NodePreferencesScopeFactory implements IScope {
	private static final Logger LOG = LoggerFactory.getLogger(NodePreferencesScopeFactory.class);
	private static final AtomicReference<NodePreferences> rootNode = new AtomicReference<NodePreferences>();

	/**
	 * Stops the factory and releases any resource.
	 */
	public static void stop() {
		// flush the preferences
		final NodePreferences node = rootNode.get();
		if (node != null) {
			try {
				node.flush();
			} catch (final Exception e) {
				LOG.warn("Failed to flush node preferences. Changes migt be lost. {}", ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	@Override
	public IEclipsePreferences create(final IEclipsePreferences parent, final String name) {
		// sanity check
		if (!NodeScope.NAME.equals(name)) {
			LOG.error("Node preference factory called with illegal node name {} for parent {}.", new Object[] { name, parent.absolutePath(), new Exception("Call Stack") });
			throw new IllegalArgumentException("invalid node name");
		}

		// check if already created
		final NodePreferences node = rootNode.get();
		if (null != node) {
			if (CloudDebug.debug) {
				LOG.debug("Node preference factory called multiple times for name {} and parent {}.", new Object[] { name, parent.absolutePath(), new Exception("Call Stack") });
			}
			return node;
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Creating ZooKeeper preferences '{}' (parent {})", name, parent);
		}

		// create
		rootNode.compareAndSet(null, new NodePreferences(parent, name));

		// done
		return rootNode.get();
	}

}
