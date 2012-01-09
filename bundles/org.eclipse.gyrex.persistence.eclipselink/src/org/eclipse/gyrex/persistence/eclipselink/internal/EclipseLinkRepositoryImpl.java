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
package org.eclipse.gyrex.persistence.eclipselink.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.eclipse.gyrex.common.internal.services.IServiceProxyChangeListener;
import org.eclipse.gyrex.common.internal.services.ServiceProxy;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.common.services.ServiceNotAvailableException;
import org.eclipse.gyrex.persistence.eclipselink.EclipseLinkRepository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.eclipse.persistence.config.LoggerType;
import org.eclipse.persistence.config.PersistenceUnitProperties;

import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * {@link EclipseLinkRepository} implementation
 */
@SuppressWarnings("restriction")
public class EclipseLinkRepositoryImpl extends EclipseLinkRepository {

	/**
	 * A disposer for EMFs
	 */
	private class EMFDisposer implements IServiceProxyChangeListener {

		private final WeakReference<EntityManagerFactory> entityManagerFactory;
		private final RepositoryContentType contentType;

		/**
		 * Creates a new instance.
		 * 
		 * @param entityManagerFactory
		 * @param contentType
		 */
		public EMFDisposer(final EntityManagerFactory entityManagerFactory, final RepositoryContentType contentType) {
			this.contentType = contentType;
			this.entityManagerFactory = new WeakReference<EntityManagerFactory>(entityManagerFactory);
		}

		@Override
		public boolean serviceChanged(final IServiceProxy<?> proxy) {
			// remove from cache
			emfCacheByContentType.remove(contentType);

			// just close the EMF
			final EntityManagerFactory factory = entityManagerFactory.get();
			if ((null != factory) && factory.isOpen()) {
				try {
					factory.close();
				} catch (final IllegalStateException ignored) {
					// ignored
				}
			}

			// done
			return false;
		}

	}

	/**
	 * Attempts to close a DataSource by searching for a "close" method.
	 */
	static void closeQuietly(final DataSource ds) {
		try {
			final Method closeMethod = ds.getClass().getMethod("close");
			closeMethod.invoke(ds);
		} catch (final Exception e) {
			// no close
		}
	}

	private final IRepositoryPreferences repositoryPreferences;
	private final ConcurrentMap<RepositoryContentType, WeakReference<EntityManagerFactory>> emfCacheByContentType = new ConcurrentHashMap<RepositoryContentType, WeakReference<EntityManagerFactory>>();

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 * @param repositoryProvider
	 * @param repositoryPreferences
	 */
	public EclipseLinkRepositoryImpl(final String repositoryId, final RepositoryProvider repositoryProvider, final IRepositoryPreferences repositoryPreferences) {
		super(repositoryId, repositoryProvider, new EclipseLinkRepositoryMetrics(createMetricsId(repositoryProvider, repositoryId), repositoryId, repositoryProvider, "new", "created"));
		this.repositoryPreferences = repositoryPreferences;
	}

	private DataSource createDataSource(final DataSourceFactory dataSourceFactory) throws Exception {
		final Properties dataSourceProperties = new Properties();

		// set default DataSource properties
		dataSourceProperties.put(DataSourceFactory.JDBC_DATASOURCE_NAME, getRepositoryId());
		dataSourceProperties.put(DataSourceFactory.JDBC_DESCRIPTION, String.format("DataSource for repository %s", getRepositoryId()));

		// inherit (overwrite) all properties from the preferences
		for (final String key : repositoryPreferences.getKeys("dataSourceProperties")) {
			dataSourceProperties.put(key, repositoryPreferences.get(String.format("dataSourceProperties//%s", key), null));
		}

		return dataSourceFactory.createDataSource(dataSourceProperties);
	}

	/**
	 * Create and return an EntityManagerFactory for the specified content type.
	 * <p>
	 * Implementation for
	 * {@link #getEntityManagerFactory(RepositoryContentType)}.
	 * </p>
	 * 
	 * @param contentType
	 *            the content type
	 * @return the entity manager
	 */
	protected EntityManagerFactory createEntityManagerFactory(final RepositoryContentType contentType) {
		if (!StringUtils.equals(EclipseLinkRepository.class.getName(), contentType.getRepositoryTypeName())) {
			throw new IllegalArgumentException(String.format("Incompatible content type specified. EclipseLink Repository type expected but the specified type expects %s.", contentType.getRepositoryTypeName()));
		}

		// get unit name
		String unitName = contentType.getParameter("persistenceUnitName");
		if (null == unitName) {
			unitName = contentType.getMediaTypeSubType();
		}

		// find the proper EntityManagerFactoryBuilder service
		final IServiceProxy<EntityManagerFactoryBuilder> builderServiceProxy;
		final EntityManagerFactoryBuilder builder;
		try {
			builderServiceProxy = EclipseLinkActivator.getInstance().getServiceHelper().trackService(EntityManagerFactoryBuilder.class, String.format("(&(objectClass=%s)(%s=%s))", EntityManagerFactoryBuilder.class.getName(), EntityManagerFactoryBuilder.JPA_UNIT_NAME, unitName));
			builder = builderServiceProxy.getService();
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException(String.format("Invalid content type specified. The persistence unit name (%s) is invalid. %s", unitName, e.getMessage()), e);
		} catch (final ServiceNotAvailableException e) {
			throw new IllegalStateException(String.format("No EntityManagerFactoryBuilder service available for content type (%s).", contentType), e);
		}

		// find the proper DataSourceFactory driver to use
		final String driver = repositoryPreferences.get(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, null);
		if (null == driver) {
			throw new IllegalStateException(String.format("No database driver configured for repository '%s'.", getRepositoryId()));
		}
		final IServiceProxy<DataSourceFactory> dsFactoryServiceProxy;
		final DataSourceFactory dataSourceFactory;
		try {
			dsFactoryServiceProxy = EclipseLinkActivator.getInstance().getServiceHelper().trackService(DataSourceFactory.class, String.format("(&(objectClass=%s)(osgi.jdbc.driver.class=%s))", DataSourceFactory.class.getName(), driver));
			dataSourceFactory = dsFactoryServiceProxy.getService();
		} catch (final IllegalArgumentException e) {
			throw new IllegalStateException(String.format("The configured database driver (%s) is invalid. %s", driver, e.getMessage()), e);
		} catch (final ServiceNotAvailableException e) {
			throw new IllegalStateException(String.format("No DataSourceFactory service available for database driver (%s).", driver), e);
		}

		final Map<String, Object> props = new HashMap<String, Object>();

		DataSource dataSource = null;
		try {
			// create data source
			dataSource = createDataSource(dataSourceFactory);

			// use external connection pool
			props.put(PersistenceUnitProperties.TRANSACTION_TYPE, "RESOURCE_LOCAL");
			props.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, dataSource);

			// EclipseLink multi-tenancy support
			props.put(PersistenceUnitProperties.SESSION_NAME, getRepositoryId());

			// enable logging using JUL
			// TODO: may write/contribute a SLF4J logger
			props.put(PersistenceUnitProperties.LOGGING_LOGGER, LoggerType.JavaLogger);

			// create EMF
			final EntityManagerFactory entityManagerFactory = builder.createEntityManagerFactory(props);

			// hook with tracked services to close EMF if a service goes away
			final EMFDisposer emfDisposer = new EMFDisposer(entityManagerFactory, contentType);
			((ServiceProxy<EntityManagerFactoryBuilder>) builderServiceProxy).addChangeListener(emfDisposer, builder);
			((ServiceProxy<DataSourceFactory>) dsFactoryServiceProxy).addChangeListener(emfDisposer, dataSourceFactory);

			// done
			return entityManagerFactory;
		} catch (final Exception e) {
			closeQuietly(dataSource);
			throw new IllegalStateException(String.format("Unable to create EntityManagerFactory for persistence unit name (%s) repository (%s). %s", unitName, getRepositoryId(), e.getMessage()), e);
		}

	}

	@Override
	protected void doClose() {
		super.doClose();
	}

	/**
	 * Returns an EntityManagerFactory for the specified content type.
	 * <p>
	 * If the content type contains a parameter
	 * {@link RepositoryContentType#getParameter(String)
	 * <code>persistenceUnitName</code>} its value will be used. If no such
	 * parameter is specified the
	 * {@link RepositoryContentType#getMediaTypeSubType() media type sub type}
	 * will be used.
	 * </p>
	 * 
	 * @param contentType
	 * @return the entity manager
	 */
	@Override
	public EntityManagerFactory getEntityManagerFactory(final RepositoryContentType contentType) {
		WeakReference<EntityManagerFactory> factoryRef = emfCacheByContentType.get(contentType);
		EntityManagerFactory factory = factoryRef != null ? factoryRef.get() : null;
		if ((null == factory) || !factory.isOpen()) {
			synchronized (emfCacheByContentType) {
				factoryRef = emfCacheByContentType.get(contentType);
				factory = factoryRef != null ? factoryRef.get() : null;
				if ((null == factory) || !factory.isOpen()) {
					factory = createEntityManagerFactory(contentType);
					emfCacheByContentType.put(contentType, new WeakReference<EntityManagerFactory>(factory));
				}
			}
		}
		return factory;
	}
}
