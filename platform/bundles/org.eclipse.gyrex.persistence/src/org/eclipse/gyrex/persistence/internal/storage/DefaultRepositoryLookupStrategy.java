/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
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

import java.text.MessageFormat;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.preferences.PreferencesUtil;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentTypeSupport;

import org.osgi.service.prefs.BackingStoreException;

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

	/**
	 * the preference key path for repositories lookup (value
	 * <code>repositories//</code>)
	 */
	private static final String PREF_PATH_REPORITORIES = "repositories//";

	/** the shared instance */
	private static DefaultRepositoryLookupStrategy sharedInstance;

	/**
	 * Composes and returns a preference key for the specified content type.
	 * 
	 * @param contentType
	 *            the content type
	 * @return the preferences keys
	 */
	private static String getContextPreferenceKey(final RepositoryContentType contentType) {
		// just the media type
		return PREF_PATH_REPORITORIES.concat(contentType.getMediaType());
	}

	/**
	 * Returns the shared instance of the default repository lookup strategy.
	 * 
	 * @return the shared instance
	 */
	public static DefaultRepositoryLookupStrategy getDefault() {
		if (null == sharedInstance) {
			return sharedInstance = new DefaultRepositoryLookupStrategy();
		}
		return sharedInstance;
	}

	/**
	 * Sets a repository for the specified content type in a given context
	 * 
	 * @param context
	 *            the context to set the repository in
	 * @param contentType
	 *            the content type to set the repository for
	 * @param repositoryId
	 *            the repository id to set (maybe <code>null</code> to unset)
	 * @throws BackingStoreException
	 *             when thrown by
	 *             {@link IRuntimeContextPreferences#flush(String)}
	 */
	public static void setRepository(final IRuntimeContext context, final RepositoryContentType contentType, final String repositoryId) throws BackingStoreException {
		final IRuntimeContextPreferences preferences = PreferencesUtil.getPreferences(context);
		if (null != repositoryId) {
			preferences.put(PersistenceActivator.PLUGIN_ID, getContextPreferenceKey(contentType), repositoryId, false);
		} else {
			preferences.remove(PersistenceActivator.PLUGIN_ID, getContextPreferenceKey(contentType));
		}
		preferences.flush(PersistenceActivator.PLUGIN_ID);
	}

	/**
	 * Hidden constructor.
	 */
	private DefaultRepositoryLookupStrategy() {
		// empty
	}

	@Override
	public Repository getRepository(final IRuntimeContext context, final RepositoryContentType contentType) throws IllegalStateException {
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}
		if (null == contentType) {
			throw new IllegalArgumentException("contentType must not be null");
		}

		// get the repository id
		final String repositoryId = getRepositoryId(context, contentType);

		// fail if we don't have an id
		if (null == repositoryId) {
			throw new IllegalStateException(MessageFormat.format("No repository available for storing content of type ''{0}'' in context ''{1}''.", contentType.getMediaType(), context.getContextPath()));
		}

		// get the repository
		final Repository repository = getRepository(repositoryId);

		// check that the repository can handle the content type
		final RepositoryContentTypeSupport contentTypeSupport = repository.getContentTypeSupport();
		if ((null != contentTypeSupport) && !contentTypeSupport.isSupported(contentType)) {
			throw new IllegalStateException(MessageFormat.format("The repository ''{0}'' in context ''{1}'' does not support content of type ''{2}''.", repository, context.getContextPath(), contentType));
		}

		// return the repository
		return repository;
	}

	private Repository getRepository(final String repositoryId) {
		return PersistenceActivator.getInstance().getRepositoriesManager().getRepository(repositoryId);
	}

	private String getRepositoryId(final IRuntimeContext context, final RepositoryContentType contentType) {
		// lookup the repository id based on the context from the preferences
		return PreferencesUtil.getPreferences(context).get(PersistenceActivator.PLUGIN_ID, getContextPreferenceKey(contentType), null);
	}
}
