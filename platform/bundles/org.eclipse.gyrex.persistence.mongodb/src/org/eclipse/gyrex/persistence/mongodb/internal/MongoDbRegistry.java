/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.persistence.mongodb.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.persistence.storage.exceptions.ResourceFailureException;
import org.eclipse.gyrex.preferences.CloudScope;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.Mongo;
import com.mongodb.MongoURI;

/**
 * A simple registry for sharing {@link Mongo} connections.
 */
public class MongoDbRegistry {

	static class MongoPool {
		Mongo mongo;
		final AtomicLong usageCount = new AtomicLong(0);
	}

	public static final String PREF_KEY_URI = "uri";

	public static void configurePool(final String poolId, final String uri) throws BackingStoreException {
		if (!IdHelper.isValidId(poolId)) {
			throw new IllegalArgumentException("invalid pool id; " + poolId);
		}
		final Preferences node = getPoolsNode().node(poolId);
		node.put(PREF_KEY_URI, uri);
		node.flush();
	}

	public static Preferences getPoolsNode() {
		return CloudScope.INSTANCE.getNode(MongoDbActivator.SYMBOLIC_NAME).node("pools");
	}

	public static void removePool(final String poolId) throws BackingStoreException {
		if (!IdHelper.isValidId(poolId)) {
			throw new IllegalArgumentException("invalid pool id; " + poolId);
		}
		final Preferences node = getPoolsNode();
		if (node.nodeExists(poolId)) {
			node.node(poolId).removeNode();
			node.flush();
		}
	}

	private final ConcurrentMap<String, MongoPool> mongoById = new ConcurrentHashMap<String, MongoPool>();
	private boolean stopped;

	private static final Logger LOG = LoggerFactory.getLogger(MongoDbRegistry.class);

	private MongoPool createPool(final String poolId) {
		try {
			final Preferences node = getPoolsNode();
			if (!node.nodeExists(poolId)) {
				return null;
			}

			final String uri = node.node(poolId).get(PREF_KEY_URI, null);
			if (null == uri) {
				throw new ResourceFailureException(String.format("No Mongo URI configured for pool '%s'.", poolId));
			}

			final MongoPool mongoPool = new MongoPool();
			try {
				mongoPool.mongo = new Mongo(new MongoURI(uri));
			} catch (final Exception e) {
				throw new ResourceFailureException("Error loading pool. " + e.getMessage(), e);
			}

			if (null != mongoById.putIfAbsent(poolId, mongoPool)) {
				// already in pool
				mongoPool.mongo.close();
			}

			return mongoById.get(poolId);
		} catch (final BackingStoreException e) {
			throw new ResourceFailureException("Error loading pool. " + e.getMessage(), e);
		}
	}

	public Mongo get(final String poolId) {
		if (!IdHelper.isValidId(poolId)) {
			throw new IllegalArgumentException("invalid pool id; " + poolId);
		}
		if (stopped) {
			throw new IllegalStateException("inactive");
		}

		MongoPool pool = mongoById.get(poolId);
		if (null == pool) {
			// try to create the pool
			pool = createPool(poolId);
			if (null == pool) {
				return null;
			}
		}

		pool.usageCount.incrementAndGet();
		return pool.mongo;
	}

	public void stop() {
		stopped = true;

		for (final MongoPool mongo : mongoById.values()) {
			if (mongo.usageCount.get() > 0) {
				LOG.warn("MongoDB pool '{}' still in use while closing.");
			}
			mongo.mongo.close();
		}

		mongoById.clear();
	}

	public void unget(final String poolId) {
		if (!IdHelper.isValidId(poolId)) {
			throw new IllegalArgumentException("invalid pool id; " + poolId);
		}
		if (stopped) {
			throw new IllegalStateException("inactive");
		}

		final MongoPool pool = mongoById.get(poolId);
		if (null != pool) {
			if (0 == pool.usageCount.decrementAndGet()) {
				// no longer used
				if (mongoById.remove(poolId, pool)) {
					pool.mongo.close();
				}
			}
		}
	}

}
