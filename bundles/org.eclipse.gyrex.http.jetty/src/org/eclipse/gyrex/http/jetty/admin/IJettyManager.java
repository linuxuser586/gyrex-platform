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
package org.eclipse.gyrex.http.jetty.admin;

import java.util.Collection;

/**
 * A service which allows configuration of the Gyrex Jetty web engine.
 * <p>
 * It's exposed as API in order to allow external management of cloud nodes.
 * However, some limitations apply. This package represents an administration
 * API which is tightly coupled to an internal technology. As such, it may
 * evolve quicker than usual APIs and may not follow the <a
 * href="http://wiki.eclipse.org/Version_Numbering" target="_blank">Eclipse
 * version guidelines</a>.
 * </p>
 * <p>
 * Clients using this API should inform the Gyrex development team through it's
 * preferred channels (eg. development mailing). They should also define a more
 * strict package version range (eg. <code>[1.0.0,1.1.0)</code>) when importing
 * this package (or any other sub-package).
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IJettyManager {

	/**
	 * @param certificateId
	 * @param keystoreBytes
	 * @param keystorePassword
	 * @param keyPassword
	 */
	void addCertificate(String certificateId, byte[] keystoreBytes, char[] keystorePassword, char[] keyPassword);

	/**
	 * @param certificateId
	 */
	ICertificate getCertificate(String certificateId);

	/**
	 * Returns all configured certificates
	 * 
	 * @return
	 */
	Collection<ICertificate> getCertificates();

	/**
	 * @param channelId
	 * @return
	 */
	ChannelDescriptor getChannel(String channelId);

	/**
	 * Returns all configured channels
	 * 
	 * @return
	 */
	Collection<ChannelDescriptor> getChannels();

	/**
	 * @param certificateId
	 * @return
	 */
	Collection<ChannelDescriptor> getChannelsUsingCertificate(String certificateId);

	/**
	 * @param certificateId
	 */
	void removeCertificate(String certificateId);

	/**
	 * @param channelId
	 */
	void removeChannel(String channelId);

	/**
	 * Sets the channels.
	 * 
	 * @param channels
	 */
	void saveChannel(ChannelDescriptor channel);
}
