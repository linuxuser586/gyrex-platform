/**
 * Copyright (c) 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.boot.internal.ssh;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 * {@link PublickeyAuthenticator} which authenticates using a specified
 * {@link #setAuthorizedKeysFile(String) authorized_keys} file.
 */
//FIXME: remove once available in Equinox SSH bundle
public class AuthorizedKeysFileAuthenticator implements PublickeyAuthenticator {
	private String authorizedKeysFile;

	public boolean authenticate(final String username, final PublicKey key, final ServerSession session) {
		final String authorizedKeysFile = getAuthorizedKeysFile();
		if (null == authorizedKeysFile) {
			// TODO should use better logging than System.err?
			System.err.println("No authorized_keys file configured!");
			return false;
		}
		try {
			// dynamically read key file at each login attempt
			final AuthorizedKeys keys = new AuthorizedKeys(authorizedKeysFile);
			for (final PublicKey authorizedKey : keys.getKeys()) {
				if (isSameKey(authorizedKey, key)) {
					return true;
				}
			}
		} catch (final FileNotFoundException e) {
			// TODO should use better logging than System.err?
			System.err.println("Configured authorized_keys file not found! " + e.getMessage());
		} catch (final IOException e) {
			// TODO should use better logging than System.err?
			System.err.println("Please check authorized_keys file! " + e.getMessage());
		}
		return false;
	}

	public String getAuthorizedKeysFile() {
		return authorizedKeysFile;
	}

	private boolean isSameDSAKey(final DSAPublicKey k1, final DSAPublicKey k2) {
		return k1.getY().equals(k2.getY()) && k1.getParams().getG().equals(k2.getParams().getG()) && k1.getParams().getP().equals(k2.getParams().getP()) && k1.getParams().getQ().equals(k2.getParams().getQ());
	}

	private boolean isSameKey(final PublicKey k1, final PublicKey k2) throws IOException {
		if ((k1 instanceof DSAPublicKey) && (k2 instanceof DSAPublicKey)) {
			return isSameDSAKey((DSAPublicKey) k1, (DSAPublicKey) k2);
		} else if ((k1 instanceof RSAPublicKey) && (k2 instanceof RSAPublicKey)) {
			return isSameRSAKey((RSAPublicKey) k1, (RSAPublicKey) k2);
		} else {
			throw new IOException("Unsupported key types detected!");
		}
	}

	private boolean isSameRSAKey(final RSAPublicKey k1, final RSAPublicKey k2) {
		return k1.getPublicExponent().equals(k2.getPublicExponent()) && k1.getModulus().equals(k2.getModulus());
	}

	public void setAuthorizedKeysFile(final String authorizedKeysFile) {
		this.authorizedKeysFile = authorizedKeysFile;
	}
}