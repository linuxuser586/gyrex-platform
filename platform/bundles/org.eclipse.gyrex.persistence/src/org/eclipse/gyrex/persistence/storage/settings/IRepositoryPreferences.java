/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.settings;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Repository preferences for storing repository configuration settings (such as
 * connection information).
 * <p>
 * Repository preferences provide convenient access to a node in the Eclipse
 * preferences hierarchy which a repository should use to store and read
 * preferences. The usage of the suggested node is encouraged. It helps
 * operators to locate the preferences more easily if they are stored together
 * in an LDAP server for example.
 * </p>
 * <p>
 * Intentionally the underlying node is not exposed. Clients should not make any
 * assumptions about the location.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRepositoryPreferences {

	/**
	 * Forces any changes in the contents of the repository preferences node and
	 * its descendants to the persistent store.
	 * <p>
	 * Once this method returns successfully, it is safe to assume that all
	 * changes made in the subtree rooted at the repository preferences node
	 * prior to the method invocation have become permanent.
	 * </p>
	 * <p>
	 * Implementations are free to flush changes into the persistent store at
	 * any time. They do not need to wait for this method to be called.
	 * </p>
	 * <p>
	 * When a flush occurs on a newly created node, it is made persistent, as
	 * are any ancestors (and descendants) that have yet to be made persistent.
	 * Note however that any properties value changes in ancestors are <i>not
	 * </i> guaranteed to be made persistent.
	 * </p>
	 * 
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #sync()
	 */
	public void flush() throws BackingStoreException;

	/**
	 * Return the value stored in the repository preference node for the given
	 * key. If the key is not defined then return the specified default value.
	 * Only the underlying repository preference node will be used for finding
	 * the preference to get the value.
	 * <p>
	 * The specified key may either refer to a simple key or be the
	 * concatenation of the path of a child node and key. If the key contains a
	 * slash ("/") character, then a double-slash must be used to denote the end
	 * of they child path and the beginning of the key. Otherwise it is assumed
	 * that the key is the last segment of the path. The following are some
	 * examples of keys and their meanings:
	 * <ul>
	 * <li>"a" - look for a value for the property "a"
	 * <li>"//a" - look for a value for the property "a"
	 * <li>"///a" - look for a value for the property "/a"
	 * <li>"//a//b" - look for a value for the property "a//b"
	 * <li>"a/b/c" - look in the child node "a/b" for property "c"
	 * <li>"/a/b/c" - look in the child node "a/b" for property "c"
	 * <li>"/a/b//c" - look in the child node "a/b" for the property "c"
	 * <li>"a/b//c/d" - look in the child node "a/b" for the property "c/d"
	 * <li>"/a/b//c/d" - look in the child node "a/b" for the property "c/d"
	 * <li>"/a/b//c//d" - look in the child node "a/b" for the property "c//d"
	 * </ul>
	 * </p>
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	public String get(String key, String defaultValue) throws SecurityException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>boolean</code>. See {@link #get(String, String)} for a complete
	 * description of this method.
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String)
	 */
	public boolean getBoolean(String key, boolean defaultValue) throws SecurityException;

	/**
	 * Returns the names of the children of the repository preference node at
	 * the specified path. (The returned array will be of size zero if the node
	 * has no children and not <code>null</code>!)
	 * 
	 * @param path
	 *            path to a descendants within the underlying repository
	 *            preference node (maybe <code>null</code> or an empty string
	 *            for the repository preference node itself)
	 * @return the names of the children of the node
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @throws IllegalStateException
	 *             if the repository preference node (or an ancestor) has been
	 *             removed.
	 */
	public String[] getChildrenNames(String path) throws BackingStoreException, SecurityException, IllegalStateException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>float</code>. See {@link #get(String, String)} for a complete
	 * description of this method.
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String)
	 */
	public float getFloat(String key, float defaultValue) throws SecurityException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>int</code>. See {@link #get(String, String)} for a complete
	 * description of this method.
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String)
	 */
	public int getInt(String key, int defaultValue) throws SecurityException;

	/**
	 * Returns all of the keys that have an associated value in the repository
	 * preference node. (The returned array will be of size zero if the node has
	 * no preferences and not <code>null</code>!)
	 * 
	 * @return an array of the keys that have an associated value in the
	 *         repository preference node.
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @throws IllegalStateException
	 *             if the repository preference node (or an ancestor) has been
	 *             removed.
	 */
	public String[] getKeys() throws BackingStoreException, SecurityException, IllegalStateException;

	/**
	 * Sets the value stored in the repository preference node for the given
	 * key. If the key is defined the existing value will be overridden with the
	 * specified value. Only the underlying repository preference node will be
	 * used for finding the preference to set the value.
	 * <p>
	 * The specified key may either refer to a simple key or be the
	 * concatenation of the path of a child node and key. If the key contains a
	 * slash ("/") character, then a double-slash must be used to denote the end
	 * of the child path and the beginning of the key. Otherwise it is assumed
	 * that the key is the last segment of the path. The following are some
	 * examples of keys and their meanings:
	 * <ul>
	 * <li>"a" - look for a value for the property "a"
	 * <li>"//a" - look for a value for the property "a"
	 * <li>"///a" - look for a value for the property "/a"
	 * <li>"//a//b" - look for a value for the property "a//b"
	 * <li>"a/b/c" - look in the child node "a/b" for property "c"
	 * <li>"/a/b/c" - look in the child node "a/b" for property "c"
	 * <li>"/a/b//c" - look in the child node "a/b" for the property "c"
	 * <li>"a/b//c/d" - look in the child node "a/b" for the property "c/d"
	 * <li>"/a/b//c/d" - look in the child node "a/b" for the property "c/d"
	 * <li>"/a/b//c//d" - look in the child node "a/b" for the property "c//d"
	 * </ul>
	 * </p>
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param value
	 *            the value to set, or <code>null</code> to
	 *            {@link #remove(String, String) remove} the preference if it is
	 *            defined
	 * @param encrypt
	 *            <code>true</code> if value is to be encrypted,
	 *            <code>false</code> value does not need to be encrypted
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	void put(String key, String value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>boolean</code> value stored in the preference store for
	 * the given key. See {@link #put(String, String, boolean)} for a complete
	 * description of this method.
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param value
	 *            the value to set
	 * @param encrypt
	 *            <code>true</code> if value is to be encrypted,
	 *            <code>false</code> value does not need to be encrypted
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	void putBoolean(String key, boolean value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>float</code> value stored in the preference store for the
	 * given key. See {@link #put(String, String, boolean)} for a complete
	 * description of this method.
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param value
	 *            the value to set
	 * @param encrypt
	 *            <code>true</code> if value is to be encrypted,
	 *            <code>false</code> value does not need to be encrypted
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	void putFloat(String key, float value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>int</code> value stored in the preference store for the
	 * given key. See {@link #put(String, String, boolean)} for a complete
	 * description of this method.
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param value
	 *            the value to set
	 * @param encrypt
	 *            <code>true</code> if value is to be encrypted,
	 *            <code>false</code> value does not need to be encrypted
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	void putInt(String key, int value, boolean encrypt) throws SecurityException;

	/**
	 * Removes the value stored in the repository preference node for the given
	 * key in the underlying context. If the key is not defined then nothing
	 * will be removed. Only the underlying repository preference node will be
	 * used for finding the preference to remove.
	 * <p>
	 * The specified key may either refer to a simple key or be the
	 * concatenation of the path of a child node and key. If the key contains a
	 * slash ("/") character, then a double-slash must be used to denote the end
	 * of the child path and the beginning of the key. Otherwise it is assumed
	 * that the key is the last segment of the path. The following are some
	 * examples of keys and their meanings:
	 * <ul>
	 * <li>"a" - look for a value for the property "a"
	 * <li>"//a" - look for a value for the property "a"
	 * <li>"///a" - look for a value for the property "/a"
	 * <li>"//a//b" - look for a value for the property "a//b"
	 * <li>"a/b/c" - look in the child node "a/b" for property "c"
	 * <li>"/a/b/c" - look in the child node "a/b" for property "c"
	 * <li>"/a/b//c" - look in the child node "a/b" for the property "c"
	 * <li>"a/b//c/d" - look in the child node "a/b" for the property "c/d"
	 * <li>"/a/b//c/d" - look in the child node "a/b" for the property "c/d"
	 * <li>"/a/b//c//d" - look in the child node "a/b" for the property "c//d"
	 * </ul>
	 * </p>
	 * 
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	void remove(String key) throws SecurityException;

	/**
	 * Ensures that future reads from the repository preferences node and its
	 * descendants reflect any changes that were committed to the persistent
	 * store (from any VM) prior to the <code>sync</code> invocation. As a
	 * side-effect, forces any changes in the contents of the repository
	 * preferences node and its descendants to the persistent store, as if the
	 * <code>flush</code> method had been invoked on this node.
	 * 
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #flush()
	 */
	public void sync() throws BackingStoreException, SecurityException;
}
