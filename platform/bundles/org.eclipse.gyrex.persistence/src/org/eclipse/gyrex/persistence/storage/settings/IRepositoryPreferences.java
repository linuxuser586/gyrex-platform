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
package org.eclipse.cloudfree.persistence.storage.settings;

import org.eclipse.equinox.security.storage.StorageException;

/**
 * Repository preferences for storing repository configuration settings (such as
 * connection information).
 * <p>
 * Repository preferences allow to encrypt settings for storing sensitive
 * information such as passwords.
 * </p>
 * <p>
 * Please note that repository preferences are only intended to store relatively
 * small size data, such as passwords. If you need to securely store large
 * objects, consider encrypting such objects in a symmetric way using randomly
 * generated password and use repository preferences to store the password.
 * </p>
 * <p>
 * If preferences were modified, the platform will automatically save them.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRepositoryPreferences {

	/**
	 * Retrieves a value associated with the key in this node. If the value was
	 * encrypted, it is decrypted.
	 * 
	 * @param key
	 *            key with this the value is associated
	 * @param def
	 *            default value to return if the key is not associated with any
	 *            value
	 * @return value associated the key. If value was stored in an encrypted
	 *         form, it will be decrypted
	 * @throws IllegalStateException
	 *             if exception occurred during decryption
	 */
	public boolean get(String key, boolean def) throws IllegalStateException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was
	 * encrypted, it is decrypted.
	 * 
	 * @param key
	 *            key with this the value is associated
	 * @param def
	 *            default value to return if the key is not associated with any
	 *            value
	 * @return value associated the key. If value was stored in an encrypted
	 *         form, it will be decrypted
	 * @throws IllegalStateException
	 *             if exception occurred during decryption
	 */
	public int get(String key, int def) throws IllegalStateException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was
	 * encrypted, it is decrypted.
	 * 
	 * @param key
	 *            key with this the value is associated
	 * @param def
	 *            default value to return if the key is not associated with any
	 *            value
	 * @return value associated the key. If value was stored in an encrypted
	 *         form, it will be decrypted
	 * @throws IllegalStateException
	 *             if exception occurred during decryption
	 */
	public String get(String key, String def) throws StorageException;

	/**
	 * Returns keys that have associated values.
	 * 
	 * @return keys that have associated values
	 */
	public String[] keys();

	/**
	 * Stores a value associated with the key in this node.
	 * 
	 * @param key
	 *            key with which the value is going to be associated
	 * @param value
	 *            value to store
	 * @param encrypt
	 *            <code>true</code> if value is to be encrypted,
	 *            <code>false</code> value does not need to be encrypted
	 * @throws StorageException
	 *             if exception occurred during encryption
	 * @throws IllegalStateException
	 *             if this node (or an ancestor) has been removed with the
	 *             {@link #removeNode()} method.
	 * @throws NullPointerException
	 *             if <code>key</code> is <code>null</code>.
	 */
	public void put(String key, boolean value, boolean encrypt) throws IllegalStateException;

	/**
	 * Stores a value associated with the key in this node.
	 * 
	 * @param key
	 *            key with which the value is going to be associated
	 * @param value
	 *            value to store
	 * @param encrypt
	 *            <code>true</code> if value is to be encrypted,
	 *            <code>false</code> value does not need to be encrypted
	 * @throws StorageException
	 *             if exception occurred during encryption
	 * @throws IllegalStateException
	 *             if this node (or an ancestor) has been removed with the
	 *             {@link #removeNode()} method.
	 * @throws NullPointerException
	 *             if <code>key</code> is <code>null</code>.
	 */
	public void put(String key, int value, boolean encrypt) throws IllegalStateException;

	/**
	 * Stores a value associated with the key in the preferences.
	 * 
	 * @param key
	 *            key with which the value is going to be associated
	 * @param value
	 *            value to store
	 * @param encrypt
	 *            <code>true</code> if value is to be encrypted,
	 *            <code>false</code> value does not need to be encrypted
	 * @throws IllegalStateException
	 *             if an exception occurred while storing the preferences
	 */
	public void put(String key, String value, boolean encrypt) throws IllegalStateException;

	/**
	 * Removes the value associated with the key.
	 * 
	 * @param key
	 *            key with which a value is associated
	 */
	public void remove(String key);

}
