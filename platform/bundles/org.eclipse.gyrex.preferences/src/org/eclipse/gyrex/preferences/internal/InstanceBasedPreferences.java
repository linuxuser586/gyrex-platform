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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Simple preferences implementation which stores the preferences using
 * {@link InstanceScope}.
 */
public class InstanceBasedPreferences implements IEclipsePreferences {

	private final class NodeChangeListenerWrapper implements INodeChangeListener {
		private final INodeChangeListener listener;

		private NodeChangeListenerWrapper(final INodeChangeListener listener) {
			this.listener = listener;
		}

		@Override
		public void added(final NodeChangeEvent event) {
			if (event.getParent().equals(instanceNode)) {
				final IEclipsePreferences child = new InstanceBasedPreferences(InstanceBasedPreferences.this, event.getChild().name(), false);
				listener.added(new NodeChangeEvent(InstanceBasedPreferences.this, child));
			}
		}

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
			final NodeChangeListenerWrapper other = (NodeChangeListenerWrapper) obj;
			if (listener == null) {
				if (other.listener != null) {
					return false;
				}
			} else if (!listener.equals(other.listener)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((listener == null) ? 0 : listener.hashCode());
			return result;
		}

		@Override
		public void removed(final NodeChangeEvent event) {
			if (event.getParent().equals(instanceNode)) {
				final IEclipsePreferences child = new InstanceBasedPreferences(InstanceBasedPreferences.this, event.getChild().name(), true);
				listener.removed(new NodeChangeEvent(InstanceBasedPreferences.this, child));
			}
		}
	}

	private static final IEclipsePreferences[] EMPTY_NODE_ARRAY = new IEclipsePreferences[0];
	private static final String PATH_SEPARATOR = String.valueOf(IPath.SEPARATOR);
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private final IEclipsePreferences instanceNode;
	private String cachedPath;
	private final String name;
	private final IEclipsePreferences parent;
	private boolean removed;

	public InstanceBasedPreferences(final IEclipsePreferences parent, final String name) {
		this(parent, name, false);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param parent
	 * @param name
	 */
	InstanceBasedPreferences(final IEclipsePreferences parent, final String name, final boolean removed) {
		this.parent = parent;
		this.name = name;
		this.removed = removed;
		if ((null != parent) && (null != name)) {
			if (parent instanceof InstanceBasedPreferences) {
				instanceNode = (IEclipsePreferences) ((InstanceBasedPreferences) parent).instanceNode.node(name);
			} else {
				// the parent is the ROOT and the name should be "platform"
				if (!parent.absolutePath().equals(PATH_SEPARATOR) || !name.equals(PlatformScope.NAME)) {
					throw new IllegalArgumentException(NLS.bind("name {0} and parent {1} not allowed at this point", name, parent));
				}
				instanceNode = (IEclipsePreferences) PreferencesActivator.getInstance().getPreferencesService().getRootNode().node(InstanceScope.SCOPE).node(PreferencesActivator.SYMBOLIC_NAME).node(name);
			}
		} else {
			instanceNode = null;
		}
	}

	@Override
	public String absolutePath() {
		// don't check removed state here, this method can be called at any time
		if (cachedPath == null) {
			if (parent == null) {
				cachedPath = PATH_SEPARATOR;
			} else {
				final String parentPath = parent.absolutePath();
				// if the parent is the root then we don't have to add a separator
				// between the parent path and our path
				if (parentPath.length() == 1) {
					cachedPath = parentPath + name();
				} else {
					cachedPath = parentPath + PATH_SEPARATOR + name();
				}
			}
		}
		return cachedPath;
	}

	@Override
	public void accept(final IPreferenceNodeVisitor visitor) throws BackingStoreException {
		// illegal state if the node has been removed
		checkRemoved();
		if (!visitor.visit(this)) {
			return;
		}
		final IEclipsePreferences[] toVisit = getChildren(true);
		for (int i = 0; i < toVisit.length; i++) {
			toVisit[i].accept(visitor);
		}
	}

	@Override
	public void addNodeChangeListener(final INodeChangeListener listener) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.addNodeChangeListener(new NodeChangeListenerWrapper(listener));
	}

	@Override
	public void addPreferenceChangeListener(final IPreferenceChangeListener listener) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.addPreferenceChangeListener(listener);
	}

	private IEclipsePreferences calculateRoot() {
		IEclipsePreferences result = this;
		while (result.parent() != null) {
			result = (IEclipsePreferences) result.parent();
		}
		return result;
	}

	private void checkRemoved() throws IllegalStateException {
		if (removed) {
			throw new IllegalStateException(String.format("Node '%s' has been removed.", name));
		}
	}

	@Override
	public String[] childrenNames() throws BackingStoreException {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.childrenNames();
	}

	@Override
	public void clear() throws BackingStoreException {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.clear();
	}

	@Override
	public void flush() throws BackingStoreException {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.flush();
	}

	@Override
	public String get(final String key, final String def) {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.get(key, def);
	}

	@Override
	public boolean getBoolean(final String key, final boolean def) {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.getBoolean(key, def);
	}

	@Override
	public byte[] getByteArray(final String key, final byte[] def) {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.getByteArray(key, def);
	}

	/**
	 * Thread safe way to obtain all children of this node. Never returns
	 * <code>null</code>.
	 * 
	 * @throws BackingStoreException
	 */
	protected IEclipsePreferences[] getChildren(final boolean create) throws BackingStoreException {
		final List<IEclipsePreferences> result = new ArrayList<IEclipsePreferences>();

		final String[] names = childrenNames();
		for (int i = 0; i < names.length; i++) {
			if (create || instanceNode.nodeExists(name)) {
				result.add(new InstanceBasedPreferences(this, name, false));
			}
		}
		return result.toArray(EMPTY_NODE_ARRAY);
	}

	@Override
	public double getDouble(final String key, final double def) {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.getDouble(key, def);
	}

	@Override
	public float getFloat(final String key, final float def) {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.getFloat(key, def);
	}

	@Override
	public int getInt(final String key, final int def) {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.getInt(key, def);
	}

	@Override
	public long getLong(final String key, final long def) {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.getLong(key, def);
	}

	@Override
	public String[] keys() throws BackingStoreException {
		// illegal state if the node has been removed
		checkRemoved();
		return instanceNode.keys();
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Preferences node(final String path) {
		// illegal state if the node has been removed
		checkRemoved();

		// short circuit this node
		if (path.length() == 0) {
			return this;
		}

		// if we have an absolute path use the root relative to
		// this node instead of the global root
		// in case we have a different hierarchy. (e.g. export)
		if (path.charAt(0) == IPath.SEPARATOR) {
			return calculateRoot().node(path.substring(1));
		}

		final int index = path.indexOf(IPath.SEPARATOR);
		final String key = index == -1 ? path : path.substring(0, index);
		final IEclipsePreferences child = new InstanceBasedPreferences(this, key, false);
		return child.node(index == -1 ? EMPTY_STRING : path.substring(index + 1));
	}

	@Override
	public boolean nodeExists(final String pathName) throws BackingStoreException {
		// don't check removed state here, this method can be called at any time
		return instanceNode.nodeExists(pathName);
	}

	@Override
	public Preferences parent() {
		// illegal state if the node has been removed
		checkRemoved();
		return parent;
	}

	@Override
	public void put(final String key, final String value) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.put(key, value);
	}

	@Override
	public void putBoolean(final String key, final boolean value) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.putBoolean(key, value);
	}

	@Override
	public void putByteArray(final String key, final byte[] value) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.putByteArray(key, value);
	}

	@Override
	public void putDouble(final String key, final double value) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.putDouble(key, value);
	}

	@Override
	public void putFloat(final String key, final float value) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.putFloat(key, value);
	}

	@Override
	public void putInt(final String key, final int value) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.putInt(key, value);
	}

	@Override
	public void putLong(final String key, final long value) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.putLong(key, value);
	}

	@Override
	public void remove(final String key) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.remove(key);
	}

	@Override
	public void removeNode() throws BackingStoreException {
		removed = true;
		instanceNode.removeNode();
	}

	@Override
	public void removeNodeChangeListener(final INodeChangeListener listener) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.removeNodeChangeListener(new NodeChangeListenerWrapper(listener));
	}

	@Override
	public void removePreferenceChangeListener(final IPreferenceChangeListener listener) {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.removePreferenceChangeListener(listener);
	}

	@Override
	public void sync() throws BackingStoreException {
		// illegal state if the node has been removed
		checkRemoved();
		instanceNode.sync();
	}

	@Override
	public String toString() {
		return absolutePath();
	}
}
