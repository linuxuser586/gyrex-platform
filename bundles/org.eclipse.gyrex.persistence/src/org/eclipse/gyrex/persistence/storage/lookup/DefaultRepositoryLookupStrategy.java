/*******************************************************************************
 * Copyright (c) 2008, 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.lookup;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentTypeSupport;
import org.eclipse.gyrex.persistence.storage.exceptions.ResourceFailureException;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.IPath;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

/**
 * The default {@link IRepositoryLookupStrategy repository lookup strategy}.
 * <p>
 * The default repository lookup strategy is a configurable strategy which looks
 * up the repository from the context preferences. A configured repository is
 * used depending on the {@link IRuntimeContext#getContextPath() context path}.
 * If no repository is configured for a particular context path the path is
 * traversed upwards till a repository is found.
 * </p>
 * <p>
 * This class is not intended to be subclassed or instantiated. It provides a
 * {@link #getDefault() default instance} that should be used instead.
 * </p>
 * 
 * @see IRepositoryLookupStrategy
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class DefaultRepositoryLookupStrategy implements IRepositoryLookupStrategy {

	/** the shared instance */
	private static DefaultRepositoryLookupStrategy sharedInstance;

	/**
	 * Returns the shared instance of the default repository lookup strategy.
	 * 
	 * @return the shared instance
	 */
	public static DefaultRepositoryLookupStrategy getDefault() {
		if (null == sharedInstance)
			return sharedInstance = new DefaultRepositoryLookupStrategy();
		return sharedInstance;
	}

	/**
	 * Hidden constructor.
	 */
	private DefaultRepositoryLookupStrategy() {
		// empty
	}

	private Preferences getAssignmentsNode() {
		return CloudScope.INSTANCE.getNode(PersistenceActivator.SYMBOLIC_NAME).node("assignments");
	}

	/**
	 * Returns a map of content types assignments by context.
	 * 
	 * @param repositoryId
	 *            the repository id to retrieve the assignments for
	 * @return a map with runtime context path as key and assigned content types
	 *         as value (modifications will not be reflected into assignment
	 *         changes)
	 * @throws Exception
	 *             if an error occurred accessing the configuration data store.
	 */
	public RepositoryContentTypeAssignments getContentTypeAssignments(final String repositoryId) throws Exception {
		if (!IdHelper.isValidId(repositoryId))
			throw new IllegalArgumentException("repositoryId is invalid");

		/*
		 * this lookup is really expensive, we need to walk through all assignments
		 */
		final RepositoryContentTypeAssignments assignments = new RepositoryContentTypeAssignments(repositoryId);

		final Preferences assignmentsNode = getAssignmentsNode();
		for (final String mediaTypeType : assignmentsNode.childrenNames()) {
			final Preferences mediaTypeNode = assignmentsNode.node(mediaTypeType);
			for (final String mediaTypeSubtype : mediaTypeNode.childrenNames()) {
				final Preferences contentTypeNode = mediaTypeNode.node(mediaTypeSubtype);
				for (final String contextPath : contentTypeNode.keys()) {
					final String assignedRepoId = contentTypeNode.get(contextPath, null);
					if (StringUtils.equals(repositoryId, assignedRepoId)) {
						assignments.add(contextPath, mediaTypeType, mediaTypeSubtype);
					}
				}
			}
		}
		return assignments;
	}

	@Override
	public Repository getRepository(final IRuntimeContext context, final RepositoryContentType contentType) throws IllegalStateException {
		if (null == context)
			throw new IllegalArgumentException("context must not be null");
		if (null == contentType)
			throw new IllegalArgumentException("contentType must not be null");

		// get the repository id
		final String repositoryId;
		try {
			repositoryId = getRepositoryId(context, contentType);
		} catch (final Exception e) {
			throw new ResourceFailureException(String.format("Error reading repository content type assignments for content type '%s' in context '%s'. %s", contentType.getMediaType(), context.getContextPath(), e.getMessage()), e);
		}

		// fail if we don't have an id
		if (null == repositoryId)
			throw new IllegalStateException(String.format("No repository available for storing content of type '%s' in context '%s'.", contentType.getMediaType(), context.getContextPath()));

		// get the repository
		final Repository repository = getRepository(repositoryId);
		if (null == repository)
			throw new IllegalStateException(String.format("Failed creating repository '%s' in context '%s' for content of type '%s'.", repositoryId, contentType.getMediaType(), context.getContextPath()));

		// check that the repository can handle the content type
		final RepositoryContentTypeSupport contentTypeSupport = repository.getContentTypeSupport();
		if ((null == contentTypeSupport) || !contentTypeSupport.isSupported(contentType))
			throw new IllegalStateException(String.format("The repository '%s' in context '%s' of type '%s' does not support content of type '%s'.", repositoryId, context.getContextPath(), repository.getRepositoryProvider().getRepositoryTypeName(), contentType));

		// return the repository
		return repository;
	}

	private Repository getRepository(final String repositoryId) {
		return PersistenceActivator.getInstance().getRepositoriesManager().getRepository(repositoryId);
	}

	/**
	 * Returns the configured repository id for the specified context and
	 * content type.
	 * <p>
	 * If a repository is configured directly for the content type in the
	 * specified context it will be returned. Otherwise all parent contexts will
	 * be checked up to the root context. If no assignment is available,
	 * <code>null</code> will be returned.
	 * </p>
	 * 
	 * @param context
	 *            the context to lookup the repository from (may not be
	 *            <code>null</code>)
	 * @param contentType
	 *            the content type that should be stored in the repository (may
	 *            not be <code>null</code>)
	 * @return the repository id (may be <code>null</code>)
	 * @throws Exception
	 *             if an error occurred accessing the configuration data store.
	 */
	public String getRepositoryId(final IRuntimeContext context, final RepositoryContentType contentType) throws Exception {
		/*
		 * the lookup is simple, we simply walk up the context path until a matching key is found
		 */
		final Preferences assignmentsNode = getAssignmentsNode();
		if (!assignmentsNode.nodeExists(contentType.getMediaType()))
			return null;
		final Preferences contentTypeAssignments = assignmentsNode.node(contentType.getMediaTypeType()).node(contentType.getMediaTypeSubType());

		// TODO: we may be able to support multi-version assignments here; however, that's not supported right now

		// check assignment for specified context path
		IPath path = context.getContextPath();
		String repositoryId = contentTypeAssignments.get(path.toString(), null);

		// walk up the path (while no assignment is available)
		while ((repositoryId == null) && (path.segmentCount() > 0)) {
			path = path.removeLastSegments(1);
			repositoryId = contentTypeAssignments.get(path.toString(), null);
		}

		// return what we have
		return repositoryId;
	}

	/**
	 * Sets or unsets a repository for the specified content type in a given
	 * context
	 * 
	 * @param context
	 *            the context to set or unset the repository in
	 * @param contentType
	 *            the content type to set or unset the repository for
	 * @param repositoryId
	 *            the repository id to set (maybe <code>null</code> to unset)
	 * @throws Exception
	 *             if an error occurred accessing the configuration data store.
	 */
	public void setRepository(final IRuntimeContext context, final RepositoryContentType contentType, final String repositoryId) throws Exception {
		if (context == null)
			throw new IllegalArgumentException("context must not be null");
		if (contentType == null)
			throw new IllegalArgumentException("contentType must not be null");
		if ((null != repositoryId) && !IdHelper.isValidId(repositoryId))
			throw new IllegalArgumentException("repositoryId is invalid");

		/*
		 * the assignments are stored in preferences using the following structure
		 *
		 * node: /cloud/org.eclipse.gyrex.persistence/assignments/<contentTypeType>/<contentTypeSubType>
		 * key: full context path
		 * value: repository id
		 */
		final Preferences assignmentsNode = getAssignmentsNode();
		final String contextPreferenceKey = context.getContextPath().toString();
		final Preferences contentTypeAssignments = assignmentsNode.node(contentType.getMediaTypeType()).node(contentType.getMediaTypeSubType());

		// refresh assignments
		contentTypeAssignments.sync();

		// update assignment
		if (null != repositoryId) {
			contentTypeAssignments.put(contextPreferenceKey, repositoryId);
		} else {
			contentTypeAssignments.remove(contextPreferenceKey);
			if (contentTypeAssignments.keys().length == 0) {
				// remove empty sub type node
				final Preferences contentTypeAssignmentsParent = contentTypeAssignments.parent();
				contentTypeAssignments.removeNode();
				if (contentTypeAssignmentsParent.childrenNames().length == 0) {
					// remove empty type node
					contentTypeAssignmentsParent.removeNode();
				}
			}
		}

		// flush globally
		assignmentsNode.flush();
	}
}
