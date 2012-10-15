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
package org.eclipse.gyrex.persistence.internal.storage;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.provider.di.ExtendedObjectResolver;
import org.eclipse.gyrex.persistence.PersistenceUtil;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.di.RequiredContentType;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resolver for {@link Repository} objects to a context which can be injected
 * into other objects using {@link RequiredContentType} qualifier annotation.
 */
@SuppressWarnings("restriction")
public class RepositoryObjectResolverByContentTypeComponent extends ExtendedObjectResolver {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryObjectResolverByContentTypeComponent.class);

	@Override
	public Object get(final Class<?> type, final IRuntimeContext context, final Annotation annotation) {
		// find content type
		final RepositoryContentType contentType = getContentType(annotation);
		if (contentType == null) {
			// don't fail here; log error and let injection fail later if not optional
			LOG.error("No content type found for annotation ({}). Injection might fail!", annotation);
			return null;
		}

		Repository repository = null;
		try {
			repository = PersistenceUtil.getRepository(context, contentType);
		} catch (final IllegalStateException e) {
			// don't fail here; log error and let injection fail later if not optional
			LOG.error("{} Injection might fail!", e.getMessage());
			return null;
		}

		// TODO: when a repository is closed, the injection should be updated

		// ensure cast is actually possible
		try {
			return type.cast(repository);
		} catch (final ClassCastException e) {
			// don't fail here; log error and let injection fail later if not optional
			LOG.error("Repository ({}) discovered for injection based on annotation ({}) does not match the expected type. {}", new Object[] { repository, annotation, e.getMessage() });
			return null;
		}
	}

	private RepositoryContentType getContentType(final Annotation annotation) {
		final Collection<RepositoryContentType> contentTypes = PersistenceActivator.getInstance().getContentTypeTracker().getContentTypes(((RequiredContentType) annotation).value());
		if (contentTypes.isEmpty())
			return null;

		final VersionRange versionRange = new VersionRange(((RequiredContentType) annotation).version());
		for (final RepositoryContentType contentType : contentTypes) {
			if (versionRange.includes(new Version(contentType.getVersion())))
				return contentType;
		}

		return null;
	}

}
