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
package org.eclipse.gyrex.server.internal.roles;

public class ServerRoleDefaultStartOption implements Comparable<ServerRoleDefaultStartOption> {

	public static enum Mode {
		ANY, DEVELOPMENT, PRODUCTION
	}

	public static enum Trigger {
		ON_BOOT, ON_CLOUD_CONNECT
	}

	private final String roleId;
	private final Mode mode;
	private final Trigger trigger;
	private final int startLevel;
	private final String nodeFilter;

	ServerRoleDefaultStartOption(final String roleId, final Mode mode, final Trigger trigger, final int startLevel, final String nodeFilter) {
		this.roleId = roleId;
		this.mode = mode;
		this.trigger = trigger;
		this.startLevel = startLevel;
		this.nodeFilter = nodeFilter;
	}

	@Override
	public int compareTo(final ServerRoleDefaultStartOption o) {
		// // sort according to start level
		return getStartLevel() - o.getStartLevel();
	}

	/**
	 * Returns the nodeFilter.
	 * 
	 * @return the nodeFilter
	 */
	public String getNodeFilter() {
		// only valid on cloud connect
		return trigger == Trigger.ON_CLOUD_CONNECT ? nodeFilter : null;
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

	/**
	 * Indicates if the mode and trigger matches the node.
	 * 
	 * @param mode
	 * @param trigger
	 * @return
	 */
	public boolean matches(final Mode mode, final Trigger trigger) {
		return ((this.mode == Mode.ANY) || (this.mode == mode)) && (this.trigger == trigger);
	}

}