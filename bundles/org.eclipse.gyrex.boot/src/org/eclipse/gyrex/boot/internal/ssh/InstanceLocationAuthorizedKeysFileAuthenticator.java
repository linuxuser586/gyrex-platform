/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.ssh;

import org.eclipse.gyrex.server.Platform;

import org.eclipse.equinox.console.internal.ssh.AuthorizedKeysFileAuthenticator;

/**
 * Instance location based authorized keys file authenticator.
 */
@SuppressWarnings("restriction")
public class InstanceLocationAuthorizedKeysFileAuthenticator extends AuthorizedKeysFileAuthenticator {

	@Override
	public String getAuthorizedKeysFile() {
		return Platform.getInstanceLocation().append("etc/.ssh/authorized_keys").toFile().getAbsolutePath();
	}

}
