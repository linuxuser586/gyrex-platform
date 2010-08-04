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

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.context.internal.ContextDebug;
import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A provider registration of a {@link RuntimeContextObjectProvider}.
 */
public class ProviderRegistration {

	public static interface ProviderRegistrationReference {
		/**
		 * Called by the provider when any references to it should be flushed.
		 * <p>
		 * This may happen when the provider was updated or removed.
		 * </p>
		 * 
		 * @param provider
		 */
		void flushReference(ProviderRegistration provider);
	}

	private static final Logger LOG = LoggerFactory.getLogger(ProviderRegistration.class);

	private final Class<?> type;
	private final RuntimeContextObjectProvider provider;
	private final ServiceReference serviceReference;
	private final AtomicReference<Dictionary<String, Object>> cachedPropertiesRef = new AtomicReference<Dictionary<String, Object>>();
	private final Set<ProviderRegistrationReference> contextReferences = new HashSet<ProviderRegistrationReference>(3);
	private final Lock referencesLock = new ReentrantLock();

	/**
	 * Creates a new instance.
	 * 
	 * @param type
	 * @param provider
	 * @param serviceReference
	 */
	public ProviderRegistration(final Class<?> type, final RuntimeContextObjectProvider provider, final ServiceReference serviceReference) {
		this.type = type;
		this.provider = provider;
		this.serviceReference = serviceReference;
	}

	/**
	 * Adds a reference to this provider registration.
	 * 
	 * @param reference
	 *            the reference to add
	 */
	public void addReference(final ProviderRegistrationReference reference) {
		referencesLock.tryLock();
		try {
			contextReferences.add(reference);
		} finally {
			referencesLock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		// the instances must be the same
		final ProviderRegistration other = (ProviderRegistration) obj;
		return (other.serviceReference == serviceReference) && (other.type == type) && (other.provider == provider);
	}

	/**
	 * Flushes the provider from any context it is currently used in.
	 */
	void flushFromContexts() {
		if (ContextDebug.objectLifecycle) {
			LOG.debug("Flushing provider registration {}", this);
		}

		ProviderRegistrationReference[] references;
		referencesLock.tryLock();
		try {
			references = contextReferences.toArray(new ProviderRegistrationReference[contextReferences.size()]);
		} finally {
			referencesLock.unlock();
		}
		for (final ProviderRegistrationReference reference : references) {
			if (ContextDebug.objectLifecycle) {
				LOG.debug("Flushing provider registration {} from reference {}", this, reference);
			}
			reference.flushReference(this);
		}
	}

	/**
	 * Flushes any cached properties.
	 */
	void flushProperties() {
		cachedPropertiesRef.set(null);
	}

	/**
	 * Returns the properties which can be used to to filter for a particular
	 * provider
	 * 
	 * @return the properties.
	 */
	public Dictionary getProperties() {
		Dictionary<String, Object> properties = cachedPropertiesRef.get();
		if (null != properties) {
			return properties;
		}

		// calculate properties
		final ServiceReference serviceReference = getServiceReference();
		if (null != serviceReference) {
			final String[] propertyKeys = serviceReference.getPropertyKeys();
			properties = new Hashtable<String, Object>(2 + propertyKeys.length);
			for (final String key : propertyKeys) {
				properties.put(key, serviceReference.getProperty(key));
			}
			properties.put("bundle.name", serviceReference.getBundle().getSymbolicName());
			properties.put("bundle.version", serviceReference.getBundle().getVersion());
		} else {
			// fallback to empty properties
			properties = new Hashtable<String, Object>(1);
		}

		// cache
		cachedPropertiesRef.set(properties);

		// return
		return properties;
	}

	/**
	 * Returns the provider.
	 * 
	 * @return the provider
	 */
	public RuntimeContextObjectProvider getProvider() {
		return provider;
	}

	/**
	 * Returns the service reference.
	 * 
	 * @return the service reference
	 */
	public ServiceReference getServiceReference() {
		return serviceReference;
	}

	/**
	 * Returns the manager.
	 * 
	 * @return the manager
	 */
	public Class<?> getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serviceReference == null) ? 0 : serviceReference.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((provider == null) ? 0 : provider.hashCode());
		return result;
	}

	/**
	 * Indicates if a provider matches a particular filter.
	 * <p>
	 * Supported filters are:
	 * </p>
	 * <ul>
	 * <li><code>bundle.name</code> - the symbolic name of the bundle which
	 * provides the provider</li>
	 * <li><code>bundle.version</code> - the version of the bundle which
	 * provides the provider</li>
	 * <li>any other provider which was defined as part of the service
	 * registration of the provider (see {@link #getProperties()} implementation
	 * for more details)</li>
	 * </ul>
	 * 
	 * @param filter
	 *            the filter to match (might be <code>null</code>)
	 * @return <code>true</code> if the filter matches or if the provided filter
	 *         was <code>null</code>, <code>false</code> otherwise
	 */
	public boolean match(final Filter filter) {
		if (null == filter) {
			return true;
		}
		return filter.match(getProperties());
	}

	/**
	 * Removes a reference from this provider registration.
	 * 
	 * @param reference
	 *            the reference to remove
	 */
	public void removeReference(final ProviderRegistrationReference reference) {
		referencesLock.tryLock();
		try {
			contextReferences.remove(reference);
		} finally {
			referencesLock.unlock();
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ProviderRegistration [type=").append(type).append(", serviceReference=").append(serviceReference).append("]");
		return builder.toString();
	}

}
