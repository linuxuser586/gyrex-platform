/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.connectors;

import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.gyrex.http.jetty.admin.ICertificate;

import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;

/**
 *
 */
public class CertificateSslConnector extends SslSelectChannelConnector {

	private final ICertificate certificate;

	/**
	 * Creates a new instance.
	 * 
	 * @param certificate
	 */
	public CertificateSslConnector(final ICertificate certificate) {
		super();
		if (certificate == null) {
			throw new IllegalArgumentException("certificate must not be null");
		}
		this.certificate = certificate;
	}

	@Override
	protected KeyManager[] getKeyManagers() throws Exception {
		final KeyStore keyStore = certificate.getKeyStore();
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(DEFAULT_KEYSTORE_ALGORITHM);
		keyManagerFactory.init(keyStore, certificate.getKeyPassword());
		return keyManagerFactory.getKeyManagers();
	}

	@Override
	protected KeyStore getKeyStore(final String keystorePath, final String keystoreType, final String keystorePassword) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected TrustManager[] getTrustManagers() throws Exception {
		final KeyStore trustStore = certificate.getKeyStore();

		final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(DEFAULT_TRUSTSTORE_ALGORITHM);
		trustManagerFactory.init(trustStore);
		return trustManagerFactory.getTrustManagers();
	}
}
