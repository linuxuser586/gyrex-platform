/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.context.internal.ContextDebug;
import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object for a type registration with all its available providers
 */
public class TypeRegistration {

	public static interface TypeRegistrationReference {
		/**
		 * Called by the type registration when any references to it should be
		 * flushed.
		 * <p>
		 * This may happen when a new provider was added so that
		 * re-computation/lookups may be necessary.
		 * </p>
		 * 
		 * @param typeRegistration
		 */
		void flushReference(TypeRegistration typeRegistration);
	}

	private static final Logger LOG = LoggerFactory.getLogger(TypeRegistration.class);

	/** typeName */
	private final String typeName;

	/**
	 * the providers, sorted by reverse natural order of
	 * {@link ServiceReference} to serve providers with a higher service ranking
	 * first
	 */
	private final ConcurrentNavigableMap<ServiceReference, ProviderRegistration> providersByReference = new ConcurrentSkipListMap<ServiceReference, ProviderRegistration>(Collections.reverseOrder());
	private final Set<TypeRegistrationReference> references = new HashSet<TypeRegistrationReference>(3);
	private final Lock referencesLock = new ReentrantLock();

	/**
	 * Creates a new instance.
	 * 
	 * @param typeName
	 */
	public TypeRegistration(final String typeName) {
		this.typeName = typeName;
	}

	void add(final Class<?> type, final RuntimeContextObjectProvider provider, final ServiceReference serviceReference) {
		if (!typeName.equals(type.getName())) {
			throw new IllegalArgumentException("manager class name should be " + typeName);
		}

		final ProviderRegistration providerRegistration = providersByReference.putIfAbsent(serviceReference, new ProviderRegistration(type, provider, serviceReference));
		if (null != providerRegistration) {
			// this should not happen if everything behaves correctly ...
			// not sure why there would be two service references for us
			LOG.warn("A provider was added which was already tracked by us. This looks like a programming (concurrency?) error. (old:{}, new:{}, {})", new Object[] { providerRegistration, serviceReference, this });
		}

		if (ContextDebug.objectLifecycle) {
			LOG.debug("Adding provider {} to {}", provider, this);
		}

		// a new provider was registered
		// flush from all contexts so that they re-evaluate next time
		flushFromContexts();
	}

	public void addReference(final TypeRegistrationReference reference) {
		referencesLock.tryLock();
		try {
			references.add(reference);
		} finally {
			referencesLock.unlock();
		}
	}

	void flushFromContexts() {
		Object[] currentRefs;
		referencesLock.tryLock();
		try {
			currentRefs = references.toArray();
		} finally {
			referencesLock.unlock();
		}
		for (final Object reference : currentRefs) {
			((TypeRegistrationReference) reference).flushReference(this);
		}
	}

	/**
	 * Returns a list of providers matching the specified filter.
	 * <p>
	 * Returns all available registrations if no filter is specified.
	 * </p>
	 * 
	 * @param filter
	 *            the optional filter to match
	 * @return the matching providers (or <code>null</code> if non match)
	 * @see ProviderRegistration#match(Filter)
	 */
	public ProviderRegistration[] getMatchingProviders(final Filter filter) {
		// we rely on the sort order of the providersByReference map to contain
		// the providers with a higher service ranking first

		// return all if no filter specified
		if (null == filter) {
			return providersByReference.values().toArray(new ProviderRegistration[0]);
		}

		// perform matching
		final List<ProviderRegistration> providers = new ArrayList<ProviderRegistration>(3);
		for (final ProviderRegistration providerRegistration : providersByReference.values()) {
			if (providerRegistration.match(filter)) {
				providers.add(providerRegistration);
			}
		}

		// return matched
		return providers.isEmpty() ? null : providers.toArray(new ProviderRegistration[providers.size()]);
	}

	/**
	 * Returns the typeName.
	 * 
	 * @return the typeName
	 */
	public String getTypeName() {
		return typeName;
	}

	void remove(final Class<?> type, final RuntimeContextObjectProvider provider, final ServiceReference reference) {
		if (!typeName.equals(type.getName())) {
			throw new IllegalArgumentException("type class name should be " + typeName);
		}

		final ProviderRegistration providerRegistration = providersByReference.remove(reference);
		if (null != providerRegistration) {
			if (ContextDebug.objectLifecycle) {
				LOG.debug("Removing provider {} from {}", provider, this);
			}
			// flush from all contexts
			providerRegistration.flushFromContexts();
		} else {
			// this should not happen if everything behaves correctly ...
			// not sure why there would be no registration for a service references tracked for us
			LOG.warn("A provider was removed which wasn't tracked by us. This looks like a programming (concurrency?) error. ({}, {}, {})", new Object[] { provider, reference, this });
		}

		// note, we do not notify all listeners here
		// the reason is because removal should *only* affect all contexts a provider is actually used in
		// lookup should not be affected on removal, i.e. if something wasn't there before it won't be there after a removal
	}

	public void removeReference(final TypeRegistrationReference reference) {
		referencesLock.tryLock();
		try {
			references.remove(reference);
		} finally {
			referencesLock.unlock();
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("TypeRegistration [typeName=").append(typeName).append("]");
		return builder.toString();
	}

	void update(final Class<?> type, final RuntimeContextObjectProvider provider, final ServiceReference reference) {
		if (!typeName.equals(type.getName())) {
			throw new IllegalArgumentException("type class name should be " + typeName);
		}

		final ProviderRegistration providerRegistration = providersByReference.get(reference);
		if (null != providerRegistration) {
			// flush the properties
			providerRegistration.flushProperties();
		}

		// some properties might have changes which will result in a different lookup order
		// thus, flush from all contexts which use this type
		flushFromContexts();
	}

}
