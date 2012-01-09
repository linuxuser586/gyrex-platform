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
package org.eclipse.gyrex.context.internal;

import org.eclipse.gyrex.context.IRuntimeContext;

/**
 * Listener that will be informed about context disposals.
 */
public interface IContextDisposalListener {

	void contextDisposed(IRuntimeContext runtimeContext);

}
