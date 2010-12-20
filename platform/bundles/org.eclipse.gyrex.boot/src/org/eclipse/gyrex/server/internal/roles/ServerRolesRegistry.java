/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.server.internal.roles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry that keeps track of registered server roles.
 */
public class ServerRolesRegistry {

	static enum Mode {
		ANY, DEVELOPMENT, PRODUCTION
	}

	static class RoleDefaultStart {

		private final String roleId;
		private final Mode mode;
		private final Trigger trigger;
		private final int startLevel;

		/**
		 * Creates a new instance.
		 * 
		 * @param roleId
		 * @param mode
		 * @param trigger
		 * @param startLevel
		 */
		public RoleDefaultStart(final String roleId, final Mode mode, final Trigger trigger, final int startLevel) {
			this.roleId = roleId;
			this.mode = mode;
			this.trigger = trigger;
			this.startLevel = startLevel;
		}

		/**
		 * Returns the roleId.
		 * 
		 * @return the roleId
		 */
		public String getRoleId() {
			return roleId;
		}

		/**
		 * Returns the startLevel.
		 * 
		 * @return the startLevel
		 */
		public int getStartLevel() {
			return startLevel;
		}

		public boolean matches(final Mode mode, final Trigger trigger) {
			return ((this.mode == Mode.ANY) || (this.mode == mode)) && (this.trigger == trigger);
		}

	}

	public static enum Trigger {
		ON_BOOT, ON_CLOUD_CONNECT
	}

	private static final Logger LOG = LoggerFactory.getLogger(ServerRolesRegistry.class);

	private static final String EP_SERVERROLES = "org.eclipse.gyrex.server.roles";
	private static final String ELEM_REQUIRE_BUNDLE = "requireBundle";
	private static final String ELEM_REQUIRE_APPLICATION = "requireApplication";
	private static final String ELEM_DEFAULT_START = "defaultStart";
	private static final String ELEM_ROLE = "role";
	private static final String ATTR_ID = "id";
	private static final String ATTR_START_LEVEL = "startLevel";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_APPLICATION_ID = "applicationId";
	private static final String ATTR_SYMBOLIC_NAME = "symbolicName";
	private static final String ATTR_TRIGGER = "trigger";
	private static final String ATTR_MODE = "mode";
	private static final String ATTR_ROLE_ID = "roleId";

	private static ServerRolesRegistry instance = new ServerRolesRegistry();

	public static ServerRolesRegistry getDefault() {
		return instance;
	}

	private Map<String, ServerRole> registeredRoles;
	private final List<RoleDefaultStart> defaultStartRoles = new CopyOnWriteArrayList<RoleDefaultStart>();

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

	};

	/**
	 * Returns a server role of the specified name
	 * 
	 * @param id
	 * @return a server role
	 */
	public ServerRole getRole(final String id) {
		checkInitialized();
		return registeredRoles.get(id);
	};

	public Collection<String> getRolesToStartByDefault(final Trigger trigger) {
		checkInitialized();

		// collect default start settings
		final List<RoleDefaultStart> roles = new ArrayList<ServerRolesRegistry.RoleDefaultStart>();
		for (final RoleDefaultStart defaultStart : defaultStartRoles) {
			final Mode mode = Platform.inDevelopmentMode() ? Mode.DEVELOPMENT : Mode.PRODUCTION;
			if (defaultStart.matches(mode, trigger)) {
				roles.add(defaultStart);
			}
		}

		// sort according to start level
		Collections.sort(roles, new Comparator<RoleDefaultStart>() {
			@Override
			public int compare(final RoleDefaultStart r1, final RoleDefaultStart r2) {
				return r1.getStartLevel() - r2.getStartLevel();
			}
		});

		// get role ids
		final List<String> roleIds = new ArrayList<String>(roles.size());
		for (final RoleDefaultStart roleDefaultStart : roles) {
			final String roleId = roleDefaultStart.getRoleId();
			if (!roleIds.contains(roleId)) {
				roleIds.add(roleId);
			}
		}
		return roleIds;
	}

	private void readDefaultStart(final IConfigurationElement element) {
		final String roleId = element.getAttribute(ATTR_ROLE_ID);
		if (StringUtils.isBlank(roleId)) {
			LOG.warn("Invalid defaultStart extension contributed by bundle {}: missing role id", element.getContributor().getName());
			return;
		}

		final String modeStr = element.getAttribute(ATTR_MODE);
		final Mode mode;
		if ("any".equals(modeStr)) {
			mode = Mode.ANY;
		} else if ("development".equals(modeStr)) {
			mode = Mode.DEVELOPMENT;
		} else if ("production".equals(modeStr)) {
			mode = Mode.PRODUCTION;
		} else {
			LOG.warn("Invalid defaultStart extension contributed by bundle {}: missing/wrong mode", element.getContributor().getName());
			return;
		}

		final String triggerStr = element.getAttribute(ATTR_TRIGGER);
		final Trigger trigger;
		if ("onBoot".equals(triggerStr)) {
			trigger = Trigger.ON_BOOT;
		} else if ("onCloudConnect".equals(triggerStr)) {
			trigger = Trigger.ON_CLOUD_CONNECT;
		} else {
			LOG.warn("Invalid defaultStart extension contributed by bundle {}: missing/wrong trigger", element.getContributor().getName());
			return;
		}

		final int startLevel = NumberUtils.toInt(element.getAttribute(ATTR_START_LEVEL), 0);
		defaultStartRoles.add(new RoleDefaultStart(roleId, mode, trigger, startLevel));
	}

	private void readRole(final IConfigurationElement element) {
		// id
		final String id = element.getAttribute(ATTR_ID);
		if (StringUtils.isBlank(id)) {
			LOG.warn("Invalid role extension contributed by bundle {}: missing role id", element.getContributor().getName());
			return;
		}

		// name
		final String name = element.getAttribute(ATTR_NAME);

		// required bundles
		final IConfigurationElement[] requireBundleElements = element.getChildren(ELEM_REQUIRE_BUNDLE);
		final List<String> requiredBundles = new ArrayList<String>(requireBundleElements.length);
		for (final IConfigurationElement requireBundleElement : requireBundleElements) {
			final String bundleName = requireBundleElement.getAttribute(ATTR_SYMBOLIC_NAME);
			if (StringUtils.isBlank(bundleName)) {
				LOG.warn("Invalid role extension contributed by bundle {}: missing symbolic name", element.getContributor().getName());
				return;
			}
			requiredBundles.add(bundleName);
		}

		// required apps
		final IConfigurationElement[] requiredAppsElements = element.getChildren(ELEM_REQUIRE_APPLICATION);
		final List<String> requiredApps = new ArrayList<String>(requiredAppsElements.length);
		for (final IConfigurationElement requireAppElement : requiredAppsElements) {
			final String appId = requireAppElement.getAttribute(ATTR_APPLICATION_ID);
			if (StringUtils.isBlank(appId)) {
				LOG.warn("Invalid role extension contributed by bundle {}: missing application id", element.getContributor().getName());
				return;
			}
			requiredApps.add(appId);
		}

		// register role
		registeredRoles.put(id, new ServerRole(id, name, requiredBundles, requiredApps));
	}

	private void readServerRoles() {
		final IExtensionRegistry registry = RegistryFactory.getRegistry();
		if (null == registry) {
			throw new IllegalStateException("The Equinox extension registry is not available. Please check your installation. It may be corrupt.");
		}

		final IConfigurationElement[] elements = registry.getConfigurationElementsFor(EP_SERVERROLES);
		for (final IConfigurationElement element : elements) {
			if (ELEM_ROLE.equals(element.getName())) {
				readRole(element);
			} else if (ELEM_DEFAULT_START.equals(element.getName())) {
				readDefaultStart(element);
			}
		}
	}
}
