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
package org.eclipse.gyrex.p2.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.p2.packages.IPackageManager;
import org.eclipse.gyrex.p2.packages.PackageDefinition;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PackageManager implements IPackageManager {

	private static final String PREF_NODE_PACKAGES = "packages";

	private static final String PREF_KEY_NODE_FILTER = "nodeFilter";
	private static final String PREF_KEY_ROLLED_OUT = "rolledOut";

	private static final Logger LOG = LoggerFactory.getLogger(PackageManager.class);

	@Override
	public PackageDefinition getPackage(final String id) {
		try {
			if (!IdHelper.isValidId(id)) {
				throw new IllegalArgumentException("invalid id");
			}
			final IEclipsePreferences rootNode = CloudScope.INSTANCE.getNode(P2Activator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_PACKAGES)) {
				return null;
			}
			final Preferences node = rootNode.node(PREF_NODE_PACKAGES);
			if (!node.nodeExists(id)) {
				return null;
			}

			return readPackage(id);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading package definition from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private Preferences getPackageNode(final String packageId) {
		return CloudScope.INSTANCE.getNode(P2Activator.SYMBOLIC_NAME).node(PREF_NODE_PACKAGES).node(packageId);
	}

	@Override
	public Collection<PackageDefinition> getPackages() {
		try {
			final IEclipsePreferences rootNode = CloudScope.INSTANCE.getNode(P2Activator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_PACKAGES)) {
				return Collections.emptyList();
			}
			final Preferences channelsNode = rootNode.node(PREF_NODE_PACKAGES);
			final String[] childrenNames = channelsNode.childrenNames();
			final List<PackageDefinition> channels = new ArrayList<PackageDefinition>();
			for (final String channelId : childrenNames) {
				final PackageDefinition descriptor = readPackage(channelId);
				if (descriptor != null) {
					channels.add(descriptor);
				}
			}
			return channels;
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading package definitions from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public boolean isRolledOut(final PackageDefinition packageDefinition) {
		try {
			if (!IdHelper.isValidId(packageDefinition.getId())) {
				throw new IllegalArgumentException("invalid id");
			}
			final IEclipsePreferences rootNode = CloudScope.INSTANCE.getNode(P2Activator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_PACKAGES)) {
				throw new IllegalArgumentException("package does not exist");
			}
			final Preferences node = rootNode.node(PREF_NODE_PACKAGES);
			if (!node.nodeExists(packageDefinition.getId())) {
				throw new IllegalArgumentException("package does not exist");
			}

			return node.node(packageDefinition.getId()).getBoolean(PREF_KEY_ROLLED_OUT, false);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading package definition from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private PackageDefinition readPackage(final String id) {
		try {
			final PackageDefinition descriptor = new PackageDefinition();
			descriptor.setId(id);

			final Preferences node = getPackageNode(id);
			descriptor.setNodeFilter(node.get(PREF_KEY_NODE_FILTER, null));
			return descriptor;
		} catch (final IllegalArgumentException e) {
			LOG.warn("Unable to read package definition {}. {}", id, ExceptionUtils.getRootCauseMessage(e));
			return null;
		}
	}

	@Override
	public void removePackage(final String id) {
		try {
			if (!IdHelper.isValidId(id)) {
				throw new IllegalArgumentException("invalid id");
			}

			final IEclipsePreferences rootNode = CloudScope.INSTANCE.getNode(P2Activator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_PACKAGES)) {
				return;
			}
			final Preferences node = rootNode.node(PREF_NODE_PACKAGES);
			if (!node.nodeExists(id)) {
				return;
			}

			node.node(id).removeNode();
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error removing package definition from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void savePackage(final PackageDefinition packageDefinition) {
		try {
			final String id = packageDefinition.getId();
			if (!IdHelper.isValidId(id)) {
				throw new IllegalArgumentException("invalid repository id");
			}
			final Preferences node = getPackageNode(id);
			final String nodeFilter = packageDefinition.getNodeFilter();
			if (StringUtils.isNotBlank(nodeFilter)) {
				node.put(PREF_KEY_NODE_FILTER, nodeFilter);
			} else {
				node.remove(PREF_KEY_NODE_FILTER);
			}
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error saving package definition to backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

}
