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
package org.eclipse.gyrex.p2.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.Version;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.p2.packages.IComponent;
import org.eclipse.gyrex.p2.packages.IPackageManager;
import org.eclipse.gyrex.p2.packages.PackageDefinition;
import org.eclipse.gyrex.p2.packages.components.InstallableUnit;
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
	private static final String PREF_NODE_COMPONENTS = "components";

	private static final String PREF_KEY_NODE_FILTER = "nodeFilter";
	private static final String PREF_KEY_INSTALL = "install";
	private static final String PREF_KEY_UNINSTALL = "uninstall";
	private static final String PREF_KEY_TYPE = "type";
	private static final String PREF_KEY_VERSION = "version";

	private static final String COMPONENT_TYPE_IU = "IU";

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
	public boolean isMarkedForInstall(final PackageDefinition packageDefinition) {
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

			return node.node(packageDefinition.getId()).getBoolean(PREF_KEY_INSTALL, false);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading package definition from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public boolean isMarkedForUninstall(final PackageDefinition packageDefinition) {
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

			return node.node(packageDefinition.getId()).getBoolean(PREF_KEY_UNINSTALL, false);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading package definition from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void markedForInstall(final PackageDefinition packageDefinition) {
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

			final Preferences pkgNode = node.node(packageDefinition.getId());
			pkgNode.putBoolean(PREF_KEY_INSTALL, true);
			pkgNode.putBoolean(PREF_KEY_UNINSTALL, false);
			pkgNode.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading package definition from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void markedForUninstall(final PackageDefinition packageDefinition) {
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

			final Preferences pkgNode = node.node(packageDefinition.getId());
			pkgNode.putBoolean(PREF_KEY_INSTALL, false);
			pkgNode.putBoolean(PREF_KEY_UNINSTALL, true);
			pkgNode.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading package definition from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private PackageDefinition readPackage(final String id) {
		try {
			final PackageDefinition pkgDefinition = new PackageDefinition();
			pkgDefinition.setId(id);

			final Preferences node = getPackageNode(id);
			pkgDefinition.setNodeFilter(node.get(PREF_KEY_NODE_FILTER, null));

			if (node.nodeExists(PREF_NODE_COMPONENTS)) {
				final Preferences componentsNode = node.node(PREF_NODE_COMPONENTS);
				for (final String componentId : componentsNode.childrenNames()) {
					final Preferences componentNode = componentsNode.node(componentId);
					final String type = componentNode.get(PREF_KEY_TYPE, null);
					if (StringUtils.equals(COMPONENT_TYPE_IU, type)) {
						final InstallableUnit iu = new InstallableUnit();
						iu.setId(componentId);
						final String version = componentNode.get(PREF_KEY_VERSION, null);
						if (version != null) {
							iu.setVersion(Version.create(version));
						}
						pkgDefinition.addComponentToInstall(iu);
					}
				}
			}

			return pkgDefinition;
		} catch (final Exception e) {
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

			final Collection<IComponent> componentsToInstall = packageDefinition.getComponentsToInstall();
			if (componentsToInstall.size() > 0) {
				final Preferences componentsToInstallNode = node.node(PREF_NODE_COMPONENTS);
				final Set<String> componentsWritten = new HashSet<String>();
				for (final IComponent component : componentsToInstall) {
					componentsWritten.add(component.getId());
					final Preferences componentNode = componentsToInstallNode.node(component.getId());
					if (component instanceof InstallableUnit) {
						componentNode.put(PREF_KEY_TYPE, COMPONENT_TYPE_IU);
						final InstallableUnit iu = (InstallableUnit) component;
						final Version version = iu.getVersion();
						if (null != version) {
							final StringBuffer versionString = new StringBuffer();
							version.toString(versionString);
							componentNode.put(PREF_KEY_VERSION, versionString.toString());
						} else {
							componentNode.remove(PREF_KEY_VERSION);
						}
					}
				}
				for (final String child : componentsToInstallNode.childrenNames()) {
					if (!componentsWritten.contains(child)) {
						componentsToInstallNode.node(child).removeNode();
					}
				}
			} else {
				if (node.nodeExists(PREF_NODE_COMPONENTS)) {
					node.node(PREF_NODE_COMPONENTS).removeNode();
				}
			}

			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error saving package definition to backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

}
