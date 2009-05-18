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

import org.eclipse.gyrex.configuration.ConfigurationMode;
import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.PreferencesUtil;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentTypeSupport;

/**
 * The default {@link IRepositoryLookupStrategy repository lookup strategy}.
 * <p>
 * The default repository lookup strategy is a configurable strategy with
 * different defaults depending on the
 * {@link PlatformConfiguration#getConfigurationMode() configuration mode}. The
 * following default behavior is available:
 * <dl>
 * <dt>{@link ConfigurationMode#DEVELOPMENT}</dt>
 * <dd>One repository is used for all model objects.</dd>
 * <dt>{@link ConfigurationMode#PRODUCTION}</dt>
 * <dd>A configured repository is used depending on the
 * {@link IRuntimeContext#getContextPath() context path}. If no repository is
 * configured for a particular context path the path is traversed upwards till a
 * repository is found.</dd>
 * </dl>
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
		// TODO: incorporate repository type from content type into lookup
		// lookup the repository id based on the context from the preferences
		final String peferenceKey = PREF_PATH_REPORITORIES.concat(contentType.getMediaType());
		return PreferencesUtil.getPreferences(context).get(PersistenceActivator.PLUGIN_ID, peferenceKey, null);
	}

}
