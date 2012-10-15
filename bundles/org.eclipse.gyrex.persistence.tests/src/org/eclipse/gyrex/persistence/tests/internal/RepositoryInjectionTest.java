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
package org.eclipse.gyrex.persistence.tests.internal;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.persistence.storage.content.IRepositoryContentTypeProvider;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.di.RequiredContentType;
import org.eclipse.gyrex.persistence.storage.lookup.DefaultRepositoryLookupStrategy;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry;

import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.di.InjectionException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class RepositoryInjectionTest {

	public static class InjectiblePojo {
		@Inject
		@RequiredContentType("x-gyrex/test")
		DummyRepo repo;
	}

	public static final RepositoryContentType CONTENT_TYPE = new RepositoryContentType("x-gyrex", "test", DummyRepo.class.getName(), "1.0");

	private final IRepositoryContentTypeProvider provider = new IRepositoryContentTypeProvider() {
		@Override
		public Collection<RepositoryContentType> getContentTypes() {
			return Collections.singleton(CONTENT_TYPE);
		}
	};
	private ServiceRegistration<IRepositoryContentTypeProvider> contentTypeProviderServiceReg;

	private ServiceRegistration<RepositoryProvider> repoProviderServiceReg;

	private IRepositoryRegistry registry;

	private IRuntimeContext context;

	@After
	public void after() throws Exception {
		DefaultRepositoryLookupStrategy.getDefault().setRepository(context, CONTENT_TYPE, null);
		context = null;

		registry.removeRepository("dummy");
		registry = null;

		repoProviderServiceReg.unregister();
		repoProviderServiceReg = null;

		contentTypeProviderServiceReg.unregister();
		contentTypeProviderServiceReg = null;
	}

	@Before
	public void before() throws Exception {
		final BundleContext bc = PersistenceTestsActivator.getContext();

		final Dictionary<String, Object> properties = new Hashtable<>();

		contentTypeProviderServiceReg = bc.registerService(IRepositoryContentTypeProvider.class, provider, properties);
		repoProviderServiceReg = bc.registerService(RepositoryProvider.class, new DummyRepoProvider(), properties);

		registry = PersistenceTestsActivator.getInstance().getService(IRepositoryRegistry.class);
		registry.removeRepository("dummy");
		registry.createRepository("dummy", "dummy");

		context = PersistenceTestsActivator.getInstance().getService(IRuntimeContextRegistry.class).get(Path.ROOT);
		DefaultRepositoryLookupStrategy.getDefault().setRepository(context, CONTENT_TYPE, "dummy");
	}

	@Test
	public void testInjection() {

		final IRuntimeContext context = PersistenceTestsActivator.getInstance().getService(IRuntimeContextRegistry.class).get(Path.ROOT);

		try {
			final InjectiblePojo pojo = context.getInjector().make(InjectiblePojo.class);
			assertNotNull("Repository must be set!", pojo.repo);
		} catch (final InjectionException e) {
			e.printStackTrace();
			fail("Injection failed: " + e);
		}

	}
}
