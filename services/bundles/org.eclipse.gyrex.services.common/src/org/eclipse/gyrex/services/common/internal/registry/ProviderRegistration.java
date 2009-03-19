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
package org.eclipse.gyrex.services.common.internal.registry;


import org.eclipse.gyrex.services.common.provider.ServiceProvider;
import org.osgi.framework.Bundle;

/**
 * Handle object for a registration
 */
public class ProviderRegistration {

	private final Class<?> manager;
	private final ServiceProvider provider;
	private final Bundle bundle;

	/**
	 * Creates a new instance.
	 * 
	 * @param manager
	 * @param provider
	 * @param bundle
	 */
	public ProviderRegistration(final Class<?> manager, final ServiceProvider provider, final Bundle bundle) {
		this.manager = manager;
		this.provider = provider;
		this.bundle = bundle;
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
		return (other.bundle == bundle) && (other.manager == manager) && (other.provider == provider);
	}

	/**
	 * Returns the bundle.
	 * 
	 * @return the bundle
	 */
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * Returns the manager.
	 * 
	 * @return the manager
	 */
	public Class<?> getManager() {
		return manager;
	}

	/**
	 * Returns the provider.
	 * 
	 * @return the provider
	 */
	public ServiceProvider getProvider() {
		return provider;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
		result = prime * result + ((manager == null) ? 0 : manager.hashCode());
		result = prime * result + ((provider == null) ? 0 : provider.hashCode());
		return result;
	}

}
