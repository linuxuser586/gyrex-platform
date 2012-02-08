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
package org.eclipse.gyrex.persistence.internal.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.gyrex.persistence.storage.content.IRepositoryContentTypeProvider;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.lang.StringUtils;

/**
 *
 */
public class ContentTypeTracker extends ServiceTracker<IRepositoryContentTypeProvider, IRepositoryContentTypeProvider> {

	public ContentTypeTracker(final BundleContext context) {
		super(context, IRepositoryContentTypeProvider.class, null);
	}

	public Collection<RepositoryContentType> getContentTypes(final String mediaType) {
		final List<RepositoryContentType> types = new ArrayList<RepositoryContentType>(2);
		final Object[] providers = getServices();
		if (null != providers) {
			for (int i = 0; i < providers.length; i++) {
				final IRepositoryContentTypeProvider provider = (IRepositoryContentTypeProvider) providers[i];
				final Collection<RepositoryContentType> contentTypes = provider.getContentTypes();
				for (final RepositoryContentType type : contentTypes) {
					if ((null == mediaType) || StringUtils.equals(type.getMediaType(), mediaType)) {
						types.add(type);
					}
				}
			}
		}
		return types;
	}

	public Collection<RepositoryContentType> getContentTypes(final String mediaTypeType, final String mediaTypeSubtype) {
		final List<RepositoryContentType> types = new ArrayList<RepositoryContentType>(2);
		final Object[] providers = getServices();
		for (int i = 0; i < providers.length; i++) {
			final IRepositoryContentTypeProvider provider = (IRepositoryContentTypeProvider) providers[i];
			final Collection<RepositoryContentType> contentTypes = provider.getContentTypes();
			for (final RepositoryContentType type : contentTypes) {
				if ((null == mediaTypeType) || StringUtils.equals(type.getMediaTypeType(), mediaTypeType)) {
					if ((null == mediaTypeSubtype) || StringUtils.equals(type.getMediaTypeSubType(), mediaTypeSubtype)) {
						types.add(type);
					}
				}
			}
		}
		return types;
	}
}
