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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.http.jetty.admin.ChannelDescriptor;
import org.eclipse.gyrex.http.jetty.admin.ICertificate;
import org.eclipse.gyrex.http.jetty.admin.IJettyManager;
import org.eclipse.gyrex.http.jetty.internal.HttpJettyActivator;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IJettyManager} implementation.
 */
public class JettyManagerImpl implements IJettyManager {

	private static final String PREF_NODE_CHANNELS = "channels";
	private static final String PREF_NODE_CERTIFICATES = "certs";

	private static final String PREF_KEY_CERTIFICATE_ID = "certificateId";
	private static final String PREF_KEY_SECURE_CHANNEL_ID = "secureChannelId";
	private static final String PREF_KEY_SECURE = "secure";
	private static final String PREF_KEY_PORT = "port";

	private static final String PREF_KEY_KEY_PASSWORD = "keyPassword";
	private static final String PREF_KEY_KEYSTORE_PASSWORD = "keystorePassword";
	private static final String PREF_KEY_KEYSTORE_BYTES = "keystoreBytes";

	private static final Logger LOG = LoggerFactory.getLogger(JettyManagerImpl.class);

	@Override
	public void addCertificate(final String certificateId, final byte[] keystoreBytes, final char[] keystorePassword, final char[] keyPassword) {

		if (!IdHelper.isValidId(certificateId)) {
			throw new IllegalArgumentException("invalid id");
		}
		try {
			final Preferences node = getCertificateNode(certificateId);
			node.putByteArray(PREF_KEY_KEYSTORE_BYTES, keystoreBytes);
			if (null != keystorePassword) {
				node.put(PREF_KEY_KEYSTORE_PASSWORD, new String(keystorePassword));
			} else {
				node.remove(PREF_KEY_KEYSTORE_PASSWORD);
			}
			if (null != keyPassword) {
				node.put(PREF_KEY_KEY_PASSWORD, new String(keyPassword));
			} else {
				node.remove(PREF_KEY_KEY_PASSWORD);
			}
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error saving certificate to backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public ICertificate getCertificate(final String certificateId) {
		try {
			if (!IdHelper.isValidId(certificateId)) {
				throw new IllegalArgumentException("invalid id");
			}
			final IEclipsePreferences rootNode = new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_CERTIFICATES)) {
				return null;
			}
			final Preferences channelsNode = rootNode.node(PREF_NODE_CERTIFICATES);
			if (!channelsNode.nodeExists(certificateId)) {
				return null;
			}

			return readCertificate(certificateId);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading certificate from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private Preferences getCertificateNode(final String certificateId) {
		return new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME).node(PREF_NODE_CERTIFICATES).node(certificateId);
	}

	@Override
	public Collection<ICertificate> getCertificates() {
		try {
			final IEclipsePreferences rootNode = new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_CERTIFICATES)) {
				return Collections.emptyList();
			}
			final Preferences certificatesNode = rootNode.node(PREF_NODE_CERTIFICATES);
			final String[] childrenNames = certificatesNode.childrenNames();
			final List<ICertificate> certs = new ArrayList<ICertificate>();
			for (final String certId : childrenNames) {
				final ICertificate cert = readCertificate(certId);
				if (cert != null) {
					certs.add(cert);
				}
			}
			return certs;
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading certificates from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public ChannelDescriptor getChannel(final String channelId) {
		try {
			if (!IdHelper.isValidId(channelId)) {
				throw new IllegalArgumentException("invalid id");
			}
			final IEclipsePreferences rootNode = new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_CHANNELS)) {
				return null;
			}
			final Preferences channelsNode = rootNode.node(PREF_NODE_CHANNELS);
			if (!channelsNode.nodeExists(channelId)) {
				return null;
			}

			return readChannel(channelId);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading channel from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private Preferences getChannelNode(final String channelId) {
		return new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME).node(PREF_NODE_CHANNELS).node(channelId);
	}

	@Override
	public Collection<ChannelDescriptor> getChannels() {
		try {
			final IEclipsePreferences rootNode = new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_CHANNELS)) {
				return Collections.emptyList();
			}
			final Preferences channelsNode = rootNode.node(PREF_NODE_CHANNELS);
			final String[] childrenNames = channelsNode.childrenNames();
			final List<ChannelDescriptor> channels = new ArrayList<ChannelDescriptor>();
			for (final String channelId : childrenNames) {
				final ChannelDescriptor descriptor = readChannel(channelId);
				if (descriptor != null) {
					channels.add(descriptor);
				}
			}
			return channels;
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading channels from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private ICertificate readCertificate(final String certificateId) {
		try {
			final CertificateDefinition definition = new CertificateDefinition();
			definition.setId(certificateId);
			final Preferences node = getCertificateNode(certificateId);
			definition.setKeystoreBytes(node.getByteArray(PREF_KEY_KEYSTORE_BYTES, null));
			final String keystorePassword = node.get(PREF_KEY_KEYSTORE_PASSWORD, null);
			if (null != keystorePassword) {
				definition.setKeystorePassword(keystorePassword.toCharArray());
			}
			final String keyPassword = node.get(PREF_KEY_KEY_PASSWORD, null);
			if (null != keyPassword) {
				definition.setKeyPassword(keyPassword.toCharArray());
			}
			return definition;
		} catch (final IllegalArgumentException e) {
			LOG.warn("Unable to read Jetty certificate {}. {}", certificateId, e.getMessage());
			return null;
		}
	}

	private ChannelDescriptor readChannel(final String channelId) {
		try {
			final ChannelDescriptor descriptor = new ChannelDescriptor();
			descriptor.setId(channelId);

			final Preferences node = getChannelNode(channelId);
			descriptor.setPort(node.getInt(PREF_KEY_PORT, 0));
			descriptor.setSecure(node.getBoolean(PREF_KEY_SECURE, false));
			descriptor.setSecureChannelId(node.get(PREF_KEY_SECURE_CHANNEL_ID, null));
			descriptor.setCertificateId(node.get(PREF_KEY_CERTIFICATE_ID, null));
			return descriptor;
		} catch (final IllegalArgumentException e) {
			LOG.warn("Unable to read Jetty channel {}. {}", channelId, e.getMessage());
			return null;
		}
	}

	@Override
	public void removeCertificate(final String certificateId) {
		try {
			if (!IdHelper.isValidId(certificateId)) {
				throw new IllegalArgumentException("invalid id");
			}

			final IEclipsePreferences rootNode = new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_CERTIFICATES)) {
				return;
			}
			final Preferences certificatesNode = rootNode.node(PREF_NODE_CERTIFICATES);
			if (!certificatesNode.nodeExists(certificateId)) {
				return;
			}

			certificatesNode.node(certificateId).removeNode();
			certificatesNode.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error removing certificate from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void removeChannel(final String channelId) {
		try {
			if (!IdHelper.isValidId(channelId)) {
				throw new IllegalArgumentException("invalid id");
			}

			final IEclipsePreferences rootNode = new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_CHANNELS)) {
				return;
			}
			final Preferences channelsNode = rootNode.node(PREF_NODE_CHANNELS);
			if (!channelsNode.nodeExists(channelId)) {
				return;
			}

			channelsNode.node(channelId).removeNode();
			channelsNode.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error removing channel from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void saveChannel(final ChannelDescriptor channel) {
		try {
			final Preferences node = getChannelNode(channel.getId());
			node.putInt(PREF_KEY_PORT, channel.getPort());
			node.putBoolean(PREF_KEY_SECURE, channel.isSecure());
			final String secureChannelId = channel.getSecureChannelId();
			if (secureChannelId != null) {
				node.put(PREF_KEY_SECURE_CHANNEL_ID, secureChannelId);
			} else {
				node.remove(PREF_KEY_SECURE_CHANNEL_ID);
			}
			final String certificateId = channel.getCertificateId();
			if (certificateId != null) {
				node.put(PREF_KEY_CERTIFICATE_ID, certificateId);
			} else {
				node.remove(PREF_KEY_CERTIFICATE_ID);
			}
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error saving channel to backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}
}