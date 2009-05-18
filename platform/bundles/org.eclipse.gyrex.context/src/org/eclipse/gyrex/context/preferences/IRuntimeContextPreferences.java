/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.preferences;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Preferences of a runtime context.
 * <p>
 * This class defines a contract for retrieving, updating and removing context
 * specific preferences. It can be retrieved from a context by calling
 * {@link IRuntimeContext#get(Class)} using
 * <code>IRuntimeContextPreferences.class</code> as an argument.
 * </p>
 * <p>
 * For an excellent detailed description of the preferences functionality see
 * {@link org.osgi.service.prefs.Preferences}. To recap in a short form,
 * preferences provide a tree. Nodes on that tree can be used to specify
 * context.
 * </p>
 * <p>
 * Although runtime context preferences are based on OSGi and Eclipse
 * preferences the preferences nodes are not exposed directly for several
 * reasons (mostly security as well as complexity). They should therefore
 * considered a well known implementation detail but not API. Thus, any clients
 * which rely on this implementation detail and deal with preferences nodes
 * directly must be aware of breaking changes at any time which may not be
 * reflected by the version of the contextual preferences API.
 * </p>
 * <p>
 * Note, as the last fallback for all lookups the {@link DefaultScope} will be
 * consulted. This is an exception to the rule above because the regular Eclipse
 * APIs can (<em>and should</em>) be used to set default preferences when
 * necessary.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IRuntimeContextPreferences {

	/**
	 * Forces any changes in the contents of the node representing the namespace
	 * root and its descendants in the underlying context to the persistent
	 * store.
	 * <p>
	 * Once this method returns successfully, it is safe to assume that all
	 * changes made in the subtree rooted at this node prior to the method
	 * invocation have become permanent.
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
	 * @see #sync(String)
	 */
	public void flush(String qualifier) throws BackingStoreException, SecurityException;

	/**
	 * Return the value stored in the preference store for the given key. If the
	 * key is not defined then return the specified default value. Use the
	 * canonical context lookup order for finding the preference value.
	 * <p>
	 * The semantics of this method are to calculate the appropriate
	 * {@link Preferences} nodes in the preference hierarchy to use and then
	 * call the {@link IPreferencesService#get(String, String, Preferences[])}
	 * method. The order of the nodes is calculated by consulting the context
	 * lookup order as defined by the runtime context hierarchy.
	 * </p>
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
	 * <p>
	 * An example of a qualifier for a preference is the bundle symbolic name.
	 * (e.g. "org.eclipse.core.resources" for "description.autobuild")
	 * </p>
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	public String get(String qualifier, String key, String defaultValue) throws SecurityException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>boolean</code>. See {@link #get(String, String, String)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String, String)
	 */
	public boolean getBoolean(String qualifier, String key, boolean defaultValue) throws SecurityException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>byte[]</code>. See {@link #get(String, String, String)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String, String)
	 */
	public byte[] getByteArray(String qualifier, String key, byte[] defaultValue) throws SecurityException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>double</code>. See {@link #get(String, String, String)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String, String)
	 */
	public double getDouble(String qualifier, String key, double defaultValue) throws SecurityException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>float</code>. See {@link #get(String, String, String)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String, String)
	 */
	public float getFloat(String qualifier, String key, float defaultValue) throws SecurityException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>int</code>. See {@link #get(String, String, String)} for a complete
	 * description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String, String)
	 */
	public int getInt(String qualifier, String key, int defaultValue) throws SecurityException;

	/**
	 * Return the value stored in the preference store for the given key as
	 * <code>long</code>. See {@link #get(String, String, String)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @return the value of the preference or the given default value
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #get(String, String, String)
	 */
	public long getLong(String qualifier, String key, long defaultValue) throws SecurityException;

	/**
	 * Sets the value stored in the preference store for the given key. If the
	 * key is defined the existing value will be overridden with the specified
	 * value. Only the underlying context will be used for finding the
	 * preference to set the value.
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
	 * <p>
	 * Callers may specify a context object to aid in the determination of the
	 * correct node. For each entry in the lookup order, the context is
	 * consulted and if one matching the scope exists, then it is used to
	 * calculate the node. Otherwise a default calculation algorithm is used.
	 * </p>
	 * <p>
	 * An example of a qualifier for a preference is the bundle symbolic name.
	 * (e.g. "org.eclipse.core.resources" for "description.autobuild")
	 * </p>
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
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
	void put(String qualifier, String key, String value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>boolean</code> value stored in the preference store for
	 * the given key. See {@link #put(String, String, String, boolean)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
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
	void putBoolean(String qualifier, String key, boolean value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>byte[]</code> value stored in the preference store for the
	 * given key. See {@link #put(String, String, String, boolean)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
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
	void putByteArray(String qualifier, String key, byte[] value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>double</code> value stored in the preference store for the
	 * given key. See {@link #put(String, String, String, boolean)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param defaultValue
	 *            the value to use if the preference is not defined
	 * @param encrypt
	 *            <code>true</code> if value is to be encrypted,
	 *            <code>false</code> value does not need to be encrypted
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	void putDouble(String qualifier, String key, double value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>float</code> value stored in the preference store for the
	 * given key. See {@link #put(String, String, String, boolean)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
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
	void putFloat(String qualifier, String key, float value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>int</code> value stored in the preference store for the
	 * given key. See {@link #put(String, String, String, boolean)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
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
	void putInt(String qualifier, String key, int value, boolean encrypt) throws SecurityException;

	/**
	 * Sets the <code>long</code> value stored in the preference store for the
	 * given key. See {@link #put(String, String, String, boolean)} for a
	 * complete description of this method.
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
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
	void putLong(String qualifier, String key, long value, boolean encrypt) throws SecurityException;

	/**
	 * Removes the value stored in the preference store for the given key in the
	 * underlying context. If the key is not defined then nothing will be
	 * removed. Only the underlying context will be used for finding the
	 * preference to remove.
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
	 * <p>
	 * An example of a qualifier for a preference is the bundle symbolic name.
	 * (e.g. "org.eclipse.core.resources" for "description.autobuild")
	 * </p>
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 */
	void remove(String qualifier, String key) throws SecurityException;

	/**
	 * Ensures that future reads from the node representing the namespace root
	 * and its descendants in the underlying context reflect any changes that
	 * were committed to the persistent store (from any VM) prior to the
	 * <code>sync</code> invocation. As a side-effect, forces any changes in the
	 * contents of the node representing the namespace root and its descendants
	 * in the underlying context to the persistent store, as if the
	 * <code>flush</code> method had been invoked on this node.
	 * 
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 * @throws SecurityException
	 *             if the caller does not have permissions to access the
	 *             preferences in the specified namespace
	 * @see #flush(String)
	 */
	public void sync(String qualifier) throws BackingStoreException, SecurityException;
}
