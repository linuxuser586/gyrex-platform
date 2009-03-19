/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;

/**
 * A registry that keeps track of registered server roles.
 */
public class ServerRolesRegistry {

	private static final String EP_SERVERROLES = AppActivator.PLUGIN_ID.concat(".serverroles");

	private static ServerRolesRegistry instance = new ServerRolesRegistry();

	public static ServerRolesRegistry getDefault() {
		return instance;
	}

	private Map<String, ServerRole> registeredRoles;
	private final List<String> defaultStartInDevelopmentMode = new ArrayList<String>(4);

	/**
	 * Hidden constructor
	 */
	private ServerRolesRegistry() {
		// empty
	}

	private void checkInitialized() {
		if (null != registeredRoles) {
			return;
		}
		synchronized (this) {
			if (null != registeredRoles) {
				return;
			}
			registeredRoles = new HashMap<String, ServerRole>();
			readServerRoles();
		}

	}

	/**
	 * Returns a server role of the specified name
	 * 
	 * @param name
	 * @return a server role
	 */
	public ServerRole getRole(final String name) {
		checkInitialized();
		return registeredRoles.get(name);
	}

	public String[] getRolesToStartByDefaultInDevelopmentMode() {
		checkInitialized();
		return defaultStartInDevelopmentMode.toArray(new String[defaultStartInDevelopmentMode.size()]);
	}

	private void readServerRoles() {
		final IExtensionRegistry registry = RegistryFactory.getRegistry();
		if (null == registry) {
			throw new IllegalStateException("The Equinox extension registry is not available. Please check your installation. It may be corrupt.");
		}

		final IConfigurationElement[] serverRolesContributions = registry.getConfigurationElementsFor(EP_SERVERROLES);
		for (final IConfigurationElement serverRolesElement : serverRolesContributions) {
			if ("role".equals(serverRolesElement.getName())) {
				final String id = serverRolesElement.getAttribute("id");
				if (StringUtils.isBlank(id)) {
					// TODO should log invalid roles
					continue;
				}
				final String name = serverRolesElement.getAttribute("name");
				final IConfigurationElement[] requireBundleElements = serverRolesElement.getChildren("requireBundle");
				final List<String> requiredBundles = new ArrayList<String>(requireBundleElements.length);
				for (final IConfigurationElement requireBundleElement : requireBundleElements) {
					final String bundleName = requireBundleElement.getAttribute("symbolicName");
					if (StringUtils.isBlank(bundleName)) {
						// TODO should log invalid bundle name
						continue;
					}
					requiredBundles.add(bundleName);
				}
				if ("inDevelopmentMode".equals(serverRolesElement.getAttribute("defaultStart"))) {
					defaultStartInDevelopmentMode.add(id);
				}
				registeredRoles.put(id, new ServerRole(id, name, requiredBundles.toArray(new String[requiredBundles.size()])));
			}
		}
	}
}
