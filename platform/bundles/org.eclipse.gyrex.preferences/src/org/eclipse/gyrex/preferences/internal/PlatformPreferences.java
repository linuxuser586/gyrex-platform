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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.IScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PlatformPreferences implements IScope, IEclipsePreferences {

	private final class NodeChangeListenerWrapper implements INodeChangeListener {
		private final INodeChangeListener listener;

		private NodeChangeListenerWrapper(final INodeChangeListener listener) {
			this.listener = listener;
		}

		@Override
		public void added(final NodeChangeEvent event) {
			if (event.getParent().equals(instanceNode)) {
				final IEclipsePreferences child = new PlatformPreferences(PlatformPreferences.this, event.getChild().name(), false);
				listener.removed(new NodeChangeEvent(PlatformPreferences.this, child));
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
				final IEclipsePreferences child = new PlatformPreferences(PlatformPreferences.this, event.getChild().name(), true);
				listener.added(new NodeChangeEvent(PlatformPreferences.this, child));
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

	/**
	 * Creates a new instance.
	 */
	public PlatformPreferences() {
		this(null, null, true);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param parent
	 * @param name
	 */
	protected PlatformPreferences(final IEclipsePreferences parent, final String name, final boolean removed) {
		this.parent = parent;
		this.name = name;
		this.removed = removed;
		if ((null != parent) && (null != name)) {
			if (parent instanceof PlatformPreferences) {
				instanceNode = (IEclipsePreferences) ((PlatformPreferences) parent).instanceNode.node(name);
			} else {
				// the parent is the ROOT and the name should be "platform"
				instanceNode = (IEclipsePreferences) PreferencesActivator.getInstance().getPreferencesService().getRootNode().node(InstanceScope.SCOPE).node(PreferencesActivator.SYMBOLIC_NAME).node(name);
			}
		} else {
			instanceNode = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#absolutePath()
	 */
	@Override
	public String absolutePath() {
		checkRemoved();
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

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences#accept(org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor)
	 */
	@Override
	public void accept(final IPreferenceNodeVisitor visitor) throws BackingStoreException {
		checkRemoved();
		if (!visitor.visit(this)) {
			return;
		}
		final IEclipsePreferences[] toVisit = getChildren(true);
		for (int i = 0; i < toVisit.length; i++) {
			toVisit[i].accept(visitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences#addNodeChangeListener(org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener)
	 */
	@Override
	public void addNodeChangeListener(final INodeChangeListener listener) {
		checkRemoved();
		instanceNode.addNodeChangeListener(new NodeChangeListenerWrapper(listener));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences#addPreferenceChangeListener(org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener)
	 */
	@Override
	public void addPreferenceChangeListener(final IPreferenceChangeListener listener) {
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

	private void checkRemoved() {
		if (removed) {
			throw new IllegalStateException(String.format("Node '%s' has been removed.", name));
		}
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#childrenNames()
	 */
	@Override
	public String[] childrenNames() throws BackingStoreException {
		checkRemoved();
		return instanceNode.childrenNames();
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#clear()
	 */
	@Override
	public void clear() throws BackingStoreException {
		checkRemoved();
		instanceNode.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IScope#create(org.eclipse.core.runtime.preferences.IEclipsePreferences, java.lang.String)
	 */
	public IEclipsePreferences create(final IEclipsePreferences parent, final String name) {
		return new PlatformPreferences(parent, name, false);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#flush()
	 */
	@Override
	public void flush() throws BackingStoreException {
		checkRemoved();
		instanceNode.flush();
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#get(java.lang.String, java.lang.String)
	 */
	@Override
	public String get(final String key, final String def) {
		checkRemoved();
		return instanceNode.get(key, def);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#getBoolean(java.lang.String, boolean)
	 */
	@Override
	public boolean getBoolean(final String key, final boolean def) {
		checkRemoved();
		return instanceNode.getBoolean(key, def);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#getByteArray(java.lang.String, byte[])
	 */
	@Override
	public byte[] getByteArray(final String key, final byte[] def) {
		checkRemoved();
		return instanceNode.getByteArray(key, def);
	}

	/**
	 * Thread safe way to obtain all children of this node. Never returns null.
	 * 
	 * @throws BackingStoreException
	 */
	protected IEclipsePreferences[] getChildren(final boolean create) throws BackingStoreException {
		final List<IEclipsePreferences> result = new ArrayList<IEclipsePreferences>();

		final String[] names = childrenNames();
		for (int i = 0; i < names.length; i++) {
			if (create || instanceNode.nodeExists(name)) {
				result.add(new PlatformPreferences(this, name, false));
			}
		}
		return result.toArray(EMPTY_NODE_ARRAY);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#getDouble(java.lang.String, double)
	 */
	@Override
	public double getDouble(final String key, final double def) {
		checkRemoved();
		return instanceNode.getDouble(key, def);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#getFloat(java.lang.String, float)
	 */
	@Override
	public float getFloat(final String key, final float def) {
		checkRemoved();
		return instanceNode.getFloat(key, def);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#getInt(java.lang.String, int)
	 */
	@Override
	public int getInt(final String key, final int def) {
		checkRemoved();
		return instanceNode.getInt(key, def);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#getLong(java.lang.String, long)
	 */
	@Override
	public long getLong(final String key, final long def) {
		checkRemoved();
		return instanceNode.getLong(key, def);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#keys()
	 */
	@Override
	public String[] keys() throws BackingStoreException {
		checkRemoved();
		return instanceNode.keys();
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#name()
	 */
	@Override
	public String name() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences#node(java.lang.String)
	 */
	@Override
	public Preferences node(final String path) {
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
		final IEclipsePreferences child = new PlatformPreferences(this, key, false);
		return child.node(index == -1 ? EMPTY_STRING : path.substring(index + 1));
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#nodeExists(java.lang.String)
	 */
	@Override
	public boolean nodeExists(final String pathName) throws BackingStoreException {
		checkRemoved();
		return instanceNode.nodeExists(pathName);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#parent()
	 */
	@Override
	public Preferences parent() {
		checkRemoved();
		return parent;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#put(java.lang.String, java.lang.String)
	 */
	@Override
	public void put(final String key, final String value) {
		checkRemoved();
		instanceNode.put(key, value);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#putBoolean(java.lang.String, boolean)
	 */
	@Override
	public void putBoolean(final String key, final boolean value) {
		checkRemoved();
		instanceNode.putBoolean(key, value);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#putByteArray(java.lang.String, byte[])
	 */
	@Override
	public void putByteArray(final String key, final byte[] value) {
		checkRemoved();
		instanceNode.putByteArray(key, value);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#putDouble(java.lang.String, double)
	 */
	@Override
	public void putDouble(final String key, final double value) {
		checkRemoved();
		instanceNode.putDouble(key, value);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#putFloat(java.lang.String, float)
	 */
	@Override
	public void putFloat(final String key, final float value) {
		checkRemoved();
		instanceNode.putFloat(key, value);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#putInt(java.lang.String, int)
	 */
	@Override
	public void putInt(final String key, final int value) {
		checkRemoved();
		instanceNode.putInt(key, value);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#putLong(java.lang.String, long)
	 */
	@Override
	public void putLong(final String key, final long value) {
		checkRemoved();
		instanceNode.putLong(key, value);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#remove(java.lang.String)
	 */
	@Override
	public void remove(final String key) {
		checkRemoved();
		instanceNode.remove(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences#removeNode()
	 */
	@Override
	public void removeNode() throws BackingStoreException {
		removed = true;
		instanceNode.removeNode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences#removeNodeChangeListener(org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener)
	 */
	@Override
	public void removeNodeChangeListener(final INodeChangeListener listener) {
		checkRemoved();
		instanceNode.removeNodeChangeListener(new NodeChangeListenerWrapper(listener));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences#removePreferenceChangeListener(org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener)
	 */
	@Override
	public void removePreferenceChangeListener(final IPreferenceChangeListener listener) {
		checkRemoved();
		instanceNode.removePreferenceChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.prefs.Preferences#sync()
	 */
	@Override
	public void sync() throws BackingStoreException {
		checkRemoved();
		instanceNode.sync();
	}

}
