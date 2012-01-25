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
package org.eclipse.gyrex.persistence.storage;

import java.util.Collection;

import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Simple contract for storing metadata (eg. schema version information) of
 * {@link Repository a repository}.
 * <p>
 * The metadata may be used for small objects only. It does not offer any
 * transaction or other rich persistence capabilities.
 * </p>
 * <p>
 * This class must be subclassed by clients that contribute a repository
 * implementation to Gyrex. As such it is considered part of a service provider
 * API which may evolve faster than the general API. Please get in touch with
 * the development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public abstract class RepositoryMetadata {

	/**
	 * Indicates if the metadata exists.
	 * 
	 * @return <code>true</code> if the metadata exists, <code>false</code>
	 *         otherwise
	 */
	public abstract boolean exists();

	/**
	 * Forces any changes in the contents of this metadata to the persistent
	 * store.
	 * <p>
	 * Once this method returns successfully, it is safe to assume that all
	 * changes made prior to the method invocation have become permanent.
	 * </p>
	 * <p>
	 * Implementations are free to flush changes into the persistent store at
	 * any time. They do not need to wait for this method to be called.
	 * </p>
	 * 
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 * @throws IllegalStateException
	 *             if this node (or an ancestor) has been removed with the
	 *             {@link #removeNode()} method.
	 * @see #sync()
	 */
	public abstract void flush() throws BackingStoreException;

	/**
	 * Returns the stored data for the specified {@code key}.
	 * 
	 * @param key
	 *            the lookup key of the data (must validate using
	 *            {@link IdHelper#isValidId(String)})
	 * @return the stored data (maybe <code>null</code> if no data is stored for
	 *         the specified {@code key})
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 */
	public abstract byte[] get(String key) throws IllegalArgumentException;

	/**
	 * Returns the metadata identifier.
	 * 
	 * @return the metadata identifier
	 */
	public abstract String getId();

	/**
	 * Returns a collection of all known keys.
	 * 
	 * @return an unmodifiable collection of all keys that have data in the
	 *         repository
	 * @throws BackingStoreException
	 *             if an exception occurred accessing the underlying data store
	 */
	public abstract Collection<String> getKeys() throws BackingStoreException;

	/**
	 * Associates the specified {@code data} with the specified {@code key}.
	 * <p>
	 * If the key already exists its data will be overwritten.
	 * </p>
	 * 
	 * @param key
	 *            the lookup key of the data (must validate using
	 *            {@link IdHelper#isValidId(String)})
	 * @param data
	 *            the data to store (may not be <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 */
	public abstract void put(String key, byte[] data) throws IllegalArgumentException;

	/**
	 * Removes this metadata, invalidating any properties contained in the
	 * removed metadata.
	 * <p>
	 * Once a metadata has been removed, attempting any method other than
	 * {@link #getId()}, of {@link #exists()} on the corresponding
	 * {@code SolrServerRepositoryMetadata} instance will fail with an
	 * {@code IllegalStateException}.
	 * <p>
	 * 
	 * @throws IllegalStateException
	 *             if this metadata (or an ancestor) has already been removed
	 *             with the {@link #remove()} method.
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 */
	public abstract void remove() throws BackingStoreException;

	/**
	 * Removes any data associated with the specified {@code key}.
	 * 
	 * @param key
	 *            the lookup key of the data (must validate using
	 *            {@link IdHelper#isValidId(String)})
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 */
	public abstract void remove(String key) throws IllegalArgumentException;

	/**
	 * Ensures that future reads from this metadata reflect any changes that
	 * were committed to the persistent store (from any VM) prior to the
	 * {@code sync} invocation.
	 * <p>
	 * As a side-effect, forces any changes in the contents of this node and its
	 * descendants to the persistent store, as if the {@code flush} method had
	 * been invoked on this node.
	 * </p>
	 * 
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 * @throws IllegalStateException
	 *             if this node (or an ancestor) has been removed with the
	 *             {@link #removeNode()} method.
	 * @see #flush()
	 */
	public abstract void sync() throws BackingStoreException;

}
