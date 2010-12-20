/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The role manager is responsible for managing the individual roles assigned to
 * a node.
 */
public class LocalRolesManager {

	private static final Logger LOG = LoggerFactory.getLogger(LocalRolesManager.class);
	private static final Map<String, ServerRole> activeRoles = new LinkedHashMap<String, ServerRole>();

	/**
	 * @param roleId
	 * @throws ActivationException
	 */
	private static boolean activate(final String roleId) throws ActivationException {
		if (activeRoles.containsKey(roleId)) {
			return true;
		}

		final ServerRole role = ServerRolesRegistry.getDefault().getRole(roleId);
		if (role == null) {
			return false;
		}

		activeRoles.put(roleId, role);

		role.activate();

		return true;
	}

	/**
	 * Activates roles.
	 * 
	 * @param roleIds
	 */
	public static synchronized void activateRoles(final Collection<String> roleIds) {
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
	public static synchronized void activateRoles(final Collection<String> roleIds, final boolean failOnError) throws Exception {
		for (final String roleId : roleIds) {
			if (!activate(roleId)) {
				throw new IllegalArgumentException("Role " + roleId + " not found!");
			}
		}
	}

	/**
	 *
	 */
	public static synchronized void deactivateAllRoles() {
		final List<String> roleIds = new ArrayList<String>(activeRoles.keySet());
		Collections.reverse(roleIds);
		for (final String roleId : roleIds) {
			dectivate(roleId);
		}
	}

	/**
	 * Deactivates roles.
	 * 
	 * @param roleIds
	 */
	public static synchronized void deactivateRoles(final Collection<String> roleIds) {
		for (final String roleId : roleIds) {
			dectivate(roleId);
		}
	}

	private static void dectivate(final String roleId) {
		final ServerRole role = activeRoles.remove(roleId);
		if (role == null) {
			return;
		}

		role.deactivate();
	}

	/**
	 * Refreshes the roles.
	 * 
	 * @param roles
	 */
	public static synchronized void refreshRoles(final Collection<String> roles) {
		// TODO Auto-generated method stub

	}

}
