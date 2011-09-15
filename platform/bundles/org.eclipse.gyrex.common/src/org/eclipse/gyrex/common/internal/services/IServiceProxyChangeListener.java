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
package org.eclipse.gyrex.common.internal.services;

import org.eclipse.gyrex.common.services.IServiceProxy;

import org.osgi.util.tracker.ServiceTracker;

/**
 * A listener that gets notified when a service tracked by a
 * {@link IServiceProxy} changed.
 * <p>
 * Note, due to limitations of {@link ServiceTracker} API, add notifications are
 * asynchronous.
 * </p>
 */
public interface IServiceProxyChangeListener {

	void serviceChanged(IServiceProxy<?> proxy);

}
