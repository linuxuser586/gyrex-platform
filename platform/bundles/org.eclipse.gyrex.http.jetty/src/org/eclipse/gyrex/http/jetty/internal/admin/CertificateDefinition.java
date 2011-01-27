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
package org.eclipse.gyrex.http.jetty.internal.admin;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.http.jetty.admin.ICertificate;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * {@link ICertificate} implementation using a JKS keystore
 */
public class CertificateDefinition implements ICertificate {

	static final DateFormat TO_STRING_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	private String id;
	private byte[] keystoreBytes;
	private char[] keyPassword;
	private char[] keystorePassword;

	@Override
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.http.jetty.admin.ICertificate#getInfo()
	 */
	@Override
	public String getInfo() {
		try {
			final StrBuilder certInfo = new StrBuilder();
			final KeyStore ks = getKeyStore();
			final Enumeration aliases = ks.aliases();
			while (aliases.hasMoreElements()) {
				final String alias = (String) aliases.nextElement();
				if (!certInfo.isEmpty()) {
					certInfo.append(", ");
				}
//				certInfo.append(alias).append(": ");
				if (ks.isKeyEntry(alias)) {
					Certificate[] chain = ks.getCertificateChain(alias);
					if (null == chain) {
						final Certificate certificate = ks.getCertificate(alias);
						chain = new Certificate[] { certificate };
					}
					for (int i = 0; i < chain.length; i++) {
						if (i > 0) {
							certInfo.append(" ");
						}
						final Certificate certificate = chain[i];
						if (certificate instanceof X509Certificate) {
							final X509Certificate x509 = (X509Certificate) certificate;
							final X500PrincipalHelper helper = new X500PrincipalHelper(x509.getSubjectX500Principal());
							certInfo.append(helper.getCN());
							certInfo.append(", valid till ").append(TO_STRING_FORMAT.format(x509.getNotAfter()));
						} else {
							certInfo.append("INVALID");
						}
					}
				} else {
					certInfo.append("IGNORED");
				}
			}
			return StringUtils.trim(certInfo.toString());
		} catch (final Exception e) {
			return ExceptionUtils.getRootCauseMessage(e);
		}
	}

	@Override
	public char[] getKeyPassword() {
		return keyPassword;
	}

	@Override
	public KeyStore getKeyStore() {
		if (keystoreBytes == null) {
			throw new IllegalStateException("keystore not available");
		}
		try {
			final KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(new ByteArrayInputStream(keystoreBytes), keystorePassword);
			return keystore;
		} catch (final Exception e) {
			throw new IllegalStateException("Error loading keystore. " + e.getMessage(), e);
		}
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the id to set
	 */
	public void setId(final String id) {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id");
		}
		this.id = id;
	}

	/**
	 * Sets the keyPassword.
	 * 
	 * @param keyPassword
	 *            the keyPassword to set
	 */
	public void setKeyPassword(final char[] keyPassword) {
		this.keyPassword = keyPassword;
	}

	/**
	 * Sets the keystoreBytes.
	 * 
	 * @param keystoreBytes
	 *            the keystoreBytes to set
	 */
	public void setKeystoreBytes(final byte[] keystoreBytes) {
		this.keystoreBytes = keystoreBytes;
	}

	/**
	 * Sets the keystorePassword.
	 * 
	 * @param keystorePassword
	 *            the keystorePassword to set
	 */
	public void setKeystorePassword(final char[] keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	@Override
	public String toString() {
		final ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
		builder.append("id", id);
		builder.append("keystoreBytes", null != keystoreBytes ? "(available)" : null);
		builder.append("keystorePassword", null != keystorePassword ? "(set)" : null);
		builder.append("keyPassword", null != keyPassword ? "(set)" : null);
		return builder.toString();
	}
}
