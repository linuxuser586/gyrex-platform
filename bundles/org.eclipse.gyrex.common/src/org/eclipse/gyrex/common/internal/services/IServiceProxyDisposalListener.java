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

/**
 * A listener that gets notified when a {@link IServiceProxy} is disposed.
 */
public interface IServiceProxyDisposalListener {

	void disposed(IServiceProxy<?> proxy);

}
