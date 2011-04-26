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
package org.eclipse.gyrex.persistence.storage.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A map like structure for retrieving repository content type assignments as
 * implemented by {@link DefaultRepositoryLookupStrategy}.
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class RepositoryContentTypeAssignments {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryContentTypeAssignments.class);

	private final String repositoryId;

	/** assignments (key: context path string, value: media type/subtype) */
	private final Map<String, Set<String>> assignments = new HashMap<String, Set<String>>(3);

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 *            the repository id
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	RepositoryContentTypeAssignments(final String repositoryId) {
		this.repositoryId = repositoryId;
	}

	/**
	 * Adds an assignment.
	 * 
	 * @param contextPath
	 *            the context path
	 * @param mediaTypeType
	 *            the media type type
	 * @param mediaTypeSubtype
	 *            the media sub type
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void add(final String contextPath, final String mediaTypeType, final String mediaTypeSubtype) {
		if (!assignments.containsKey(contextPath)) {
			assignments.put(contextPath, new HashSet<String>(2));
		}
		assignments.get(contextPath).add(String.format("%s/%s", mediaTypeType, mediaTypeSubtype));
	}

	/**
	 * Returns a list of content types assigned to a repository.
	 * 
	 * @param failIfMissing
	 *            <code>true</code> if an {@link IllegalStateException} should
	 *            be throw in case an assigned content type could not be found
	 *            in the OSGi service registry, <code>false</code> otherwise
	 * @return an unmodifiable collection of content type
	 * @throws IllegalStateException
	 *             if the repository has a content type assigned which could not
	 *             be found in the OSGi service registry and
	 *             <code>failIfMissing</code> is <code>true</code> (the message
	 *             will contain a complete list of content types which could not
	 *             be resolved)
	 */
	public Collection<RepositoryContentType> getContentTypes(final boolean failIfMissing) throws IllegalStateException {
		final Set<String> unresolvedContentTypes = new HashSet<String>(1);
		final List<RepositoryContentType> contentTypes = new ArrayList<RepositoryContentType>();
		for (final Set<String> mediaTypesSet : assignments.values()) {
			for (final String assignedMediaType : mediaTypesSet) {
				final Collection<RepositoryContentType> types = getContentTypes(assignedMediaType);
				if (types.isEmpty()) {
					unresolvedContentTypes.add(assignedMediaType);
					continue;
				}
				for (final RepositoryContentType contentType : types) {
					if (!contentTypes.contains(contentType)) {
						contentTypes.add(contentType);
					}
				}
			}
		}

		if (!unresolvedContentTypes.isEmpty()) {
			final String unresolvedTypesString = StringUtils.join(unresolvedContentTypes, ", ");
			if (failIfMissing) {
				throw new IllegalStateException(String.format("Unable to resolve content types assigned to repository '%s': %s", repositoryId, unresolvedTypesString));
			} else {
				LOG.warn("Unable to resolve content types assigned to repository '{}': {}", repositoryId, unresolvedTypesString);
			}
		}

		return contentTypes;
	}

	private Collection<RepositoryContentType> getContentTypes(final String mediaType) {
		return PersistenceActivator.getInstance().getContentTypeTracker().getContentTypes(mediaType);
	}

	/**
	 * Returns the repository identifier.
	 * 
	 * @return the repositoryId
	 */
	public String getRepositoryId() {
		return repositoryId;
	}

}
