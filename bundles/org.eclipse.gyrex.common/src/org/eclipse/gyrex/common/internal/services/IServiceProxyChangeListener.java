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
package org.eclipse.gyrex.common.internal.services;

import org.eclipse.gyrex.common.services.IServiceProxy;

import org.osgi.framework.ServiceEvent;

/**
 * A listener that gets notified when a service tracked by a
 * {@link IServiceProxy} changed.
 */
public interface IServiceProxyChangeListener {

	/**
	 * Called by {@link IServiceProxy} when an underlying {@link ServiceEvent}
	 * triggered a change to the proxy.
	 * <p>
	 * As a result of the change, the proxy may track additional services, no
	 * longer track removed services or the ordering changed due to modified
	 * service rankings. In any case, implementors should take proper actions to
	 * refresh a service use.
	 * </p>
	 * <p>
	 * Implementors must return <code>true</code> if they want to continue to
	 * receive notifications about service changes. Otherwise, the proxy will
	 * remove the listener from its change listener list.
	 * </p>
	 * 
	 * @param proxy
	 *            the proxy which tracked services changed
	 * @return <code>true</code> if the listener is still interested in future
	 *         change notifications, <code>false</code> if not
	 */
	boolean serviceChanged(IServiceProxy<?> proxy);

}
