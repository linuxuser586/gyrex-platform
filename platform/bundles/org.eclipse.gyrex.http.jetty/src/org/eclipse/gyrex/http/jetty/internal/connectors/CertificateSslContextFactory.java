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
package org.eclipse.gyrex.http.jetty.internal.connectors;

import java.security.KeyStore;

import org.eclipse.gyrex.http.jetty.admin.ICertificate;

import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Specialized {@link SslContextFactory} that uses {@link ICertificate} for
 * configuring SSL.
 */
public class CertificateSslContextFactory extends SslContextFactory {

	private final ICertificate certificate;

	/**
	 * Creates a new instance.
	 * 
	 * @param certificate
	 */
	public CertificateSslContextFactory(final ICertificate certificate) {
		if (certificate == null) {
			throw new IllegalArgumentException("certificate must not be null");
		}
		this.certificate = certificate;

		setKeyManagerPassword(new String(certificate.getKeyPassword()));
	}

	@Override
	protected KeyStore loadKeyStore() throws Exception {
		return certificate.getKeyStore();
	}

	@Override
	protected KeyStore loadTrustStore() throws Exception {
		return certificate.getKeyStore();
	}

}
