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
package org.eclipse.gyrex.p2.internal.installer;

import java.util.Set;

import org.eclipse.gyrex.p2.packages.PackageDefinition;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PackageInstallerJob extends Job {

	static class MutexRule implements ISchedulingRule {

		private final Object object;

		public MutexRule(final Object object) {
			this.object = object;
		}

		public boolean contains(final ISchedulingRule rule) {
			return rule == this;
		}

		public boolean isConflicting(final ISchedulingRule rule) {
			if (rule instanceof MutexRule) {
				return object.equals(((MutexRule) rule).object);
			}
			return false;
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(PackageInstallerJob.class);

	/**
	 * Creates a new instance.
	 * 
	 * @param packagesToRemove
	 * @param packagesToInstall
	 */
	PackageInstallerJob(final Set<PackageDefinition> packagesToInstall, final Set<PackageDefinition> packagesToRemove) {
		super("Software Package Installer");
		setSystem(true);
		setPriority(LONG);
		setRule(new MutexRule(PackageInstallerJob.class));
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		// report progress in an installation log in the install area

		// acquire global installation lock

		// perform installation

		// apply configuration

		// release global installation lock

		return Status.OK_STATUS;
	}

}
