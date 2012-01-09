/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gyrex.boot.internal.BootDebug;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The role manager is responsible for managing the individual roles active on a
 * node.
 */
public class LocalRolesManager {

	private static final Logger LOG = LoggerFactory.getLogger(LocalRolesManager.class);
	private static final Map<String, ServerRole> activeRoles = new LinkedHashMap<String, ServerRole>();

	/**
	 * @param roleId
	 * @throws ActivationException
	 */
	private static boolean activate(final String roleId) throws ActivationException {
		if (BootDebug.roles) {
			LOG.debug("Activating role {}...", roleId);
		}

		final ServerRole role;
		synchronized (activeRoles) {
			if (activeRoles.containsKey(roleId)) {
				if (BootDebug.roles) {
					LOG.debug("Role {} already active.", roleId);
				}
				return true;
			}

			role = ServerRolesRegistry.getDefault().getRole(roleId);
			if (role == null) {
				LOG.warn("Role {} not found in registry. Please check installation that bundles contributing the role are properly installed and resolve.", roleId);
				return false;
			}

			activeRoles.put(roleId, role);
		}

		role.activate();

		return true;
	}

	/**
	 * Activates roles.
	 * 
	 * @param roleIds
	 */
	public static void activateRoles(final Collection<String> roleIds) {
		if (BootDebug.debug) {
			LOG.debug("Activating roles {}.", StringUtils.join(roleIds, ','));
		}
		for (final String roleId : roleIds) {
			try {
				activate(roleId);
			} catch (final ActivationException e) {
				LOG.error("Failed to activate role {}. {}", new Object[] { roleId, e.getMessage(), e });
			}
		}
	}

	/**
	 * Activates the specified roles
	 * 
	 * @param roleIds
	 * @param failOnError
	 */
	public static void activateRoles(final Collection<String> roleIds, final boolean failOnError) throws Exception {
		if (BootDebug.debug) {
			LOG.debug("Activating roles {}.", StringUtils.join(roleIds, ','));
		}
		for (final String roleId : roleIds) {
			if (!activate(roleId)) {
				throw new IllegalArgumentException("Role " + roleId + " not found!");
			}
		}
	}

	/**
	 * Deactivates <strong>all</strong> roles.
	 */
	public static void deactivateAllRoles() {
		final List<String> roleIds;
		synchronized (activeRoles) {
			roleIds = new ArrayList<String>(activeRoles.keySet());
		}
		Collections.reverse(roleIds);
		deactivateRoles(roleIds);
	}

	/**
	 * Deactivates roles.
	 * 
	 * @param roleIds
	 */
	public static void deactivateRoles(final Collection<String> roleIds) {
		if (BootDebug.debug) {
			LOG.debug("Deactivating roles {}.", StringUtils.join(roleIds, ','));
		}
		for (final String roleId : roleIds) {
			dectivate(roleId);
		}
	}

	private static void dectivate(final String roleId) {
		final ServerRole role;
		synchronized (activeRoles) {
			role = activeRoles.remove(roleId);
		}
		if (role == null) {
			return;
		}

		role.deactivate();
	}
}
