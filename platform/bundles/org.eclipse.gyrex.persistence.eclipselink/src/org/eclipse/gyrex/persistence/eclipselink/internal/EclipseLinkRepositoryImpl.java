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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.eclipse.gyrex.persistence.eclipselink.EclipseLinkRepository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.osgi.service.jpa.EntityManagerFactoryBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * {@link EclipseLinkRepository} implementation
 */
public class EclipseLinkRepositoryImpl extends EclipseLinkRepository {

	public static String JPA_JDBC_DRIVER_PROPERTY = "javax.persistence.jdbc.driver";
	public static String JPA_JDBC_URL_PROPERTY = "javax.persistence.jdbc.url";
	public static String JPA_JDBC_USER_PROPERTY = "javax.persistence.jdbc.user";
	public static String JPA_JDBC_PASSWORD_PROPERTY = "javax.persistence.jdbc.password";
	private final String driver;
	private final String url;
	private final String user;
	private final String password;

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 * @param repositoryProvider
	 * @param repositoryPreferences
	 */
	public EclipseLinkRepositoryImpl(final String repositoryId, final RepositoryProvider repositoryProvider, final IRepositoryPreferences repositoryPreferences) {
		super(repositoryId, repositoryProvider, new EclipseLinkRepositoryMetrics(createMetricsId(repositoryProvider, repositoryId), repositoryId, repositoryProvider, "new", "created"));

		driver = repositoryPreferences.get(JPA_JDBC_DRIVER_PROPERTY, null);
		url = repositoryPreferences.get(JPA_JDBC_URL_PROPERTY, null);
		user = repositoryPreferences.get(JPA_JDBC_USER_PROPERTY, null);
		password = repositoryPreferences.get(JPA_JDBC_PASSWORD_PROPERTY, null);
	}

	@Override
	protected void doClose() {
		super.doClose();
	}

	public EntityManagerFactory getEntityManagerFactory(final RepositoryContentType contentType) {
		if (!StringUtils.equals(EclipseLinkRepository.class.getName(), contentType.getRepositoryTypeName())) {
			throw new IllegalArgumentException(String.format("Incompatible content type specified. EclipseLink Repository type expected but the specified type expects %s.", contentType.getRepositoryTypeName()));
		}

		final EntityManagerFactoryBuilder builder = EclipseLinkActivator.getInstance().getService(EntityManagerFactoryBuilder.class);
		final Map<String, Object> props = new HashMap<String, Object>();
		if (null != driver) {
			props.put(JPA_JDBC_DRIVER_PROPERTY, driver);
		}
		if (null != url) {
			props.put(JPA_JDBC_URL_PROPERTY, url);
		}
		if (null != user) {
			props.put(JPA_JDBC_USER_PROPERTY, user);
		}
		if (null != password) {
			props.put(JPA_JDBC_PASSWORD_PROPERTY, password);
		}

		props.put(EntityManagerFactoryBuilder.JPA_UNIT_NAME, contentType.getParameter("name"));
		props.put(EntityManagerFactoryBuilder.JPA_UNIT_VERSION, contentType.getVersion());

		final EntityManagerFactory factory = builder.createEntityManagerFactory(props);

		return factory;
	}
}
