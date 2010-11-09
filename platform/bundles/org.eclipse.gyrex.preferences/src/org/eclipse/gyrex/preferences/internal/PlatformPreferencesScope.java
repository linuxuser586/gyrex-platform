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
package org.eclipse.gyrex.preferences.internal;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScope;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IScope} implementation for the "platform" preferences.
 */
public class PlatformPreferencesScope implements IScope {

	private static final Logger LOG = LoggerFactory.getLogger(PlatformPreferencesScope.class);

	private static final AtomicBoolean migrated = new AtomicBoolean();

	private void copy(final IEclipsePreferences source, final IEclipsePreferences target) throws BackingStoreException {
		if (!source.nodeExists("")) {
			return;
		}

		// copy keys
		final String[] keys = source.keys();
		for (final String key : keys) {
			final String value = source.get(key, null);
			if (value != null) {
				target.put(key, value);
			}
		}

		// recursively copy children
		final String[] childrenNames = source.childrenNames();
		for (final String childName : childrenNames) {
			final IEclipsePreferences child = (IEclipsePreferences) source.node(childName);
			copy(child, (IEclipsePreferences) target.node(childName));
		}
	}

	@Override
	public IEclipsePreferences create(final IEclipsePreferences parent, final String name) {
		// allow explicit fallback to instance based preferences
		if (Platform.inDevelopmentMode() && Boolean.getBoolean("eclipse.gyrex.preferences.platform.instancebased")) {
			LOG.info("Using instance based preferences as specified via system property!");
			return new InstanceBasedPreferences(parent, name);
		}

		if (PreferencesDebug.debug) {
			LOG.debug("Creating ZooKeeper preferences '{}' (parent {})", name, parent);
		}

		final ZooKeeperBasedPreferences node = new ZooKeeperBasedPreferences(parent, name);

		// check if we need to migrate from old preferences
		if (!migrated.get() && migrated.compareAndSet(false, true)) {
			final Job job = new Job("Migrate preferences for " + parent.absolutePath() + name) {

				@Override
				protected IStatus run(final IProgressMonitor monitor) {

					try {
						final Preferences instanceScopeNode = PreferencesActivator.getInstance().getPreferencesService().getRootNode().node(InstanceScope.SCOPE);
						if (instanceScopeNode.nodeExists(PreferencesActivator.SYMBOLIC_NAME)) {
							final InstanceBasedPreferences oldNode = new InstanceBasedPreferences(parent, name);
							copy(oldNode, node);
							node.flush();

							// remove old preferences
							final Preferences oldInstanceBasedPrefNode = instanceScopeNode.node(PreferencesActivator.SYMBOLIC_NAME);
							oldInstanceBasedPrefNode.removeNode();

							// physically remove the file
							try {
								final Method method = oldInstanceBasedPrefNode.getClass().getDeclaredMethod("getLocation", (Class<?>[]) null);
								if (!method.isAccessible()) {
									method.setAccessible(true);
								}
								final IPath location = (IPath) method.invoke(oldInstanceBasedPrefNode, (Object[]) null);
								if (location != null) {
									if (!location.toFile().delete()) {
										LOG.warn("Unable to delete underlying preference file. Please remove manually. {}", location.toString());
									}
								}
							} catch (final Exception e) {
								LOG.warn("Unable to delete underlying preference file. Please remove manually. {}", e.toString());
							}
						}
					} catch (final IllegalStateException e) {
						if (PreferencesDebug.debug) {
							LOG.debug("ZooKeeper Gate not available. Will retry later. {}", e.getMessage());
						}
						// retry in a few seconds
						schedule(500);
					} catch (final Exception e) {
						LOG.warn("Error migrating old instance based preferences to cloud preferences. {}", e.getMessage(), e);
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.setPriority(Job.LONG);
			job.schedule();
		}

		return node;
	}
}
