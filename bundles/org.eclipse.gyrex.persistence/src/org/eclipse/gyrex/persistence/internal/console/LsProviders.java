/*******************************************************************************
 * Copyright (c) 2012 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.console;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryProviderRegistry;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryProviderRegistry.RepositoryProviderRegistration;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 *
 */
public class LsProviders extends Command {

	@Option(name = "-v", aliases = { "--verbose" }, usage = "prints detailed configuration information")
	protected boolean verbose = false;

	@Argument(index = 0, metaVar = "ID-SUB-STRING", usage = "provider sub-string filter")
	protected String filter;

	/**
	 * Creates a new instance.
	 * 
	 * @param description
	 */
	public LsProviders() {
		super("- lists available repositories");
	}

	@Override
	protected void doExecute() throws Exception {
		final RepositoryProviderRegistry providerRegistry = PersistenceActivator.getInstance().getRepositoryProviderRegistry();
		final List<RepositoryProviderRegistration> registrations = providerRegistry.getAllProviderRegistrations();
		if (registrations.isEmpty()) {
			printf("No providers registered!");
			return;
		}

		Collections.sort(registrations, new Comparator<RepositoryProviderRegistration>() {
			@Override
			public int compare(final RepositoryProviderRegistration o1, final RepositoryProviderRegistration o2) {
				return o1.getProviderId().compareTo(o2.getProviderId());
			}
		});

		for (final RepositoryProviderRegistration providerRegistration : registrations) {
			printf("%s [%s]", providerRegistration.getProviderId(), StringUtils.trimToEmpty(providerRegistration.getProviderInfo()));
			if (verbose) {
				// TODO
			}
		}
	}

}
