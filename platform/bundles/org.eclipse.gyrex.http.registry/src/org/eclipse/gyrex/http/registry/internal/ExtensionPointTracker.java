/*******************************************************************************
 * Copyright (c) 2008 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Cognos Incorporated, IBM Corporation - concept/implementation from
 *                                            org.eclipse.equinox.http.registry
 *     Gunnar Wagenknecht - adaption to Gyrex
 *******************************************************************************/
package org.eclipse.gyrex.http.registry.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionPointTracker {

	public interface Listener {
		public void added(IExtension extension);

		public void removed(IExtension extension);
	}

	class RegistryChangeListener implements IRegistryChangeListener {
		public void registryChanged(final IRegistryChangeEvent event) {
			final IExtensionDelta[] deltas = event.getExtensionDeltas(namespace, simpleIdentifier);
			for (int i = 0; i < deltas.length; ++i) {
				final IExtensionDelta delta = deltas[i];
				final IExtension extension = delta.getExtension();
				switch (delta.getKind()) {
					case IExtensionDelta.ADDED:
						if (addExtension(extension)) {
							if (HttpRegistryDebug.extensionRegistration) {
								LOG.debug("added extension {} contributed by {}", extension.getUniqueIdentifier(), extension.getContributor());
							}
							listener.added(extension);
						}
						break;
					case IExtensionDelta.REMOVED:
						if (removeExtension(extension)) {
							if (HttpRegistryDebug.extensionRegistration) {
								LOG.debug("removed extension {} contributed by {}", extension.getUniqueIdentifier(), extension.getContributor());
							}
							listener.removed(extension);
						}
					default:
						break;
				}
			}
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ExtensionPointTracker.class);

	private static final Listener NULL_LISTENER = new Listener() {
		public void added(final IExtension extension) {
		}

		public void removed(final IExtension extension) {
		}
	};
	private final IExtensionRegistry registry;
	private final String extensionPointId;
	final String namespace;
	final String simpleIdentifier;
	final Listener listener;

	private final RegistryChangeListener registryChangeListener = new RegistryChangeListener();
	private final Set<IExtension> extensionCache = new HashSet<IExtension>();

	private boolean closed = true;

	public ExtensionPointTracker(final IExtensionRegistry registry, final String extensionPointId, final Listener listener) {
		this.registry = registry;
		this.extensionPointId = extensionPointId;
		this.listener = (listener != null) ? listener : NULL_LISTENER;

		if ((extensionPointId == null) || (-1 == extensionPointId.indexOf('.'))) {
			throw new IllegalArgumentException("Unexpected Extension Point Identifier: " + extensionPointId); //$NON-NLS-1$
		}
		final int lastDotIndex = extensionPointId.lastIndexOf('.');
		namespace = extensionPointId.substring(0, lastDotIndex);
		simpleIdentifier = extensionPointId.substring(lastDotIndex + 1);
	}

	synchronized boolean addExtension(final IExtension extension) {
		if (closed) {
			return false;
		}
		if (HttpRegistryDebug.extensionRegistration) {
			LOG.debug("adding extension {} contributed by {}", extension.getUniqueIdentifier(), extension.getContributor());
		}
		return extensionCache.add(extension);
	}

	public void close() {
		IExtension[] extensions = null;
		synchronized (this) {
			if (closed) {
				return;
			}
			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Closing tracker for extension point {} (namespace {}).", extensionPointId, namespace);
			}
			closed = true;
			registry.removeRegistryChangeListener(registryChangeListener);
			extensions = getExtensions();
			extensionCache.clear();
		}
		for (int i = 0; i < extensions.length; ++i) {
			listener.removed(extensions[i]);
		}
	}

	public synchronized IExtension[] getExtensions() {
		return extensionCache.toArray(new IExtension[extensionCache.size()]);
	}

	public void open() {
		IExtension[] extensions = null;
		synchronized (this) {
			if (!closed) {
				return;
			}
			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Opening tracker for extension point {} (namespace {}).", extensionPointId, namespace);
			}
			registry.addRegistryChangeListener(registryChangeListener, namespace);
			try {
				final IExtensionPoint point = registry.getExtensionPoint(extensionPointId);
				if (point != null) {
					extensions = point.getExtensions();
					extensionCache.addAll(Arrays.asList(extensions));
				}
				closed = false;
			} catch (final InvalidRegistryObjectException e) {
				registry.removeRegistryChangeListener(registryChangeListener);
				throw e;
			}
		}
		if (extensions != null) {
			for (int i = 0; i < extensions.length; ++i) {
				listener.added(extensions[i]);
			}
		}
	}

	synchronized boolean removeExtension(final IExtension extension) {
		if (closed) {
			return false;
		}
		if (HttpRegistryDebug.extensionRegistration) {
			LOG.debug("removing extension {} contributed by {}", extension.getUniqueIdentifier(), extension.getContributor());
		}
		return extensionCache.remove(extension);
	}
}
