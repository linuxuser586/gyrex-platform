/**
 * Copyright (c) 2009, 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.internal.preferences;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.ContextDebug;
import org.eclipse.gyrex.context.internal.configuration.ContextConfiguration;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IRuntimeContextPreferences} implementation.
 */
public class GyrexContextPreferencesImpl implements IRuntimeContextPreferences {

	private static final Logger LOG = LoggerFactory.getLogger(GyrexContextPreferencesImpl.class);

	private static final String EMPTY = "";
	private static final String DEFAULT = "default";
	private static final String PLATFORM = "platform";
	private static final String UTF_8 = "UTF-8";

	/**
	 * Returns the node that may be used directly for storing the specified
	 * preferences key
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param context
	 *            the context
	 * @return the preferences node for the context.
	 */
	public static Preferences getNode(final String qualifier, final String key, final IRuntimeContext context) {
		final String pathToPreferencesKey = getPathToPreferencesKey(qualifier, key);
		final Preferences node = ContextConfiguration.getRootNodeForContextPreferences();
		if (null != context) {
			final IPath contextPath = context.getContextPath();
			if (!contextPath.isEmpty() && !contextPath.isRoot()) {
				return node.node(getPreferencesPathToSettings(contextPath, pathToPreferencesKey));
			}
		}
		// fallback to root
		return node.node(getPreferencesPathToSettings(Path.ROOT, pathToPreferencesKey));
	}

	/**
	 * Calculates the nodes that should be searched for retrieving the specified
	 * preference.
	 * <p>
	 * The lookup order is as follows:
	 * </p>
	 * <ul>
	 * <li>PLATFORM Scope
	 * <ul>
	 * <li>[context path/.../...]/.settings/[key]</li>
	 * <li>[context path/...]/.settings/[key]</li>
	 * <li>[context path]/.settings/[key]</li>
	 * <li>/.settings/[key]</li>
	 * </ul>
	 * </li>
	 * <li>DEFAULT Scope
	 * <ul>
	 * <li>[context path/.../...]/.settings/[key]</li>
	 * <li>[context path/...]/.settings/[key]</li>
	 * <li>[context path]/.settings/[key]</li>
	 * <li>/.settings/[key]</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * <p>
	 * Thus, the deeper the context path, the more expensive a lookup might get.
	 * </p>
	 * 
	 * @param qualifier
	 *            a namespace qualifier for the preference (eg. typically the
	 *            symbolic name of the bundle defining the preference)
	 * @param key
	 *            the name of the preference (optionally including its path)
	 * @param context
	 *            the context
	 * @return a list of preferences nodes
	 */
	public static Preferences[] getNodes(final String qualifier, final String key, final IRuntimeContext context) {
		if (ContextDebug.preferencesLookup) {
			LOG.debug("Preferences lookup for {}/{} in context {}", new Object[] { qualifier, key, context });
		}

		final String[] order = new String[] { PLATFORM, DEFAULT };
		final IEclipsePreferences rootNode = EclipsePreferencesUtil.getRootNode();
		final String pathToPreferencesKey = getPathToPreferencesKey(qualifier, key);
		final List<Preferences> result = new ArrayList<Preferences>();
		for (int i = 0; i < order.length; i++) {
			final String scopeString = order[i];
			final Preferences node = rootNode.node(scopeString).node(ContextConfiguration.CONTEXT_PREF_ROOT.toString());
			if (null != context) {
				for (IPath contextPath = context.getContextPath(); !contextPath.isEmpty() && !contextPath.isRoot(); contextPath = contextPath.removeLastSegments(1)) {
					final String path = getPreferencesPathToSettings(contextPath, pathToPreferencesKey);
					try {
						if (node.nodeExists(path)) {
							if (ContextDebug.preferencesLookup) {
								LOG.debug("Adding node {}", node.node(path).absolutePath());
							}
							result.add(node.node(path));
						} else {
							if (ContextDebug.preferencesLookup) {
								LOG.debug("Ignoring node {}", node.absolutePath() + "/" + path);
							}
						}
					} catch (final BackingStoreException e) {
						// node has been removed
					}
				}
			}
			// append always the root preference node
			final String root = getPreferencesPathToSettings(Path.ROOT, pathToPreferencesKey);
			try {
				if (node.nodeExists(root)) {
					if (ContextDebug.preferencesLookup) {
						LOG.debug("Adding node {}", node.node(root).absolutePath());
					}
					result.add(node.node(root));
				} else {
					if (ContextDebug.preferencesLookup) {
						LOG.debug("Ignoring node {}", node.absolutePath() + "/" + root);
					}
				}
			} catch (final BackingStoreException e) {
				// node has been removed;
			}
		}
		return result.toArray(new Preferences[result.size()]);
	}

	public static String getPathToPreferencesKey(final String qualifier, final String key) {
		return EclipsePreferencesUtil.decodePath(qualifier)[1] + IPath.SEPARATOR + EclipsePreferencesUtil.makeRelative(EclipsePreferencesUtil.decodePath(key)[0]);
	}

	public static String getPreferencesPathToSettings(final IPath contextPath, final String pathToPreferencesKey) {
		if ((null != pathToPreferencesKey) && (pathToPreferencesKey.length() > 0)) {
			return contextPath.append(GyrexContextPreferencesImpl.SETTINGS).append(pathToPreferencesKey).makeRelative().toString();
		} else {
			return contextPath.append(GyrexContextPreferencesImpl.SETTINGS).makeRelative().toString();
		}
	}

	private IRuntimeContext context;

	public static final String SETTINGS = ".settings";

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	@Inject
	public GyrexContextPreferencesImpl(final IRuntimeContext context) {
		this.context = context;
	}

	@PreDestroy
	public void dispose() {
		context = null;
	}

	@Override
	public void flush(final String qualifier) throws BackingStoreException, SecurityException {
		if (ContextDebug.preferencesModify) {
			LOG.debug("Flushing preferences of context \"{}\" for qualifier \"{}\"", context, qualifier);
		}
		getNode(qualifier, EMPTY, context).flush();
	}

	private String get(final String key, final String defaultValue, final Preferences[] nodes) {
		return EclipsePreferencesUtil.getPreferencesService().get(EclipsePreferencesUtil.decodePath(key)[1], defaultValue, nodes);
	}

	@Override
	public String get(final String qualifier, final String key, final String defaultValue) throws SecurityException {
		return get(key, defaultValue, getNodes(qualifier, key, context));
	}

	@Override
	public boolean getBoolean(final String qualifier, final String key, final boolean defaultValue) throws SecurityException {
		final String result = get(key, null, getNodes(qualifier, key, context));
		return result == null ? defaultValue : Boolean.valueOf(result).booleanValue();
	}

	@Override
	public byte[] getByteArray(final String qualifier, final String key, final byte[] defaultValue) throws SecurityException {
		final String result = get(key, null, getNodes(qualifier, key, context));
		try {
			return result == null ? defaultValue : Base64.decodeBase64(result.getBytes(UTF_8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("Gyrex requires a platform which supports UTF-8.", e);
		}
	}

	@Override
	public double getDouble(final String qualifier, final String key, final double defaultValue) throws SecurityException {
		final String value = get(key, null, getNodes(qualifier, key, context));
		return NumberUtils.toDouble(value, defaultValue);
	}

	@Override
	public float getFloat(final String qualifier, final String key, final float defaultValue) throws SecurityException {
		final String value = get(key, null, getNodes(qualifier, key, context));
		return NumberUtils.toFloat(value, defaultValue);
	}

	@Override
	public int getInt(final String qualifier, final String key, final int defaultValue) throws SecurityException {
		final String value = get(key, null, getNodes(qualifier, key, context));
		return NumberUtils.toInt(value, defaultValue);
	}

	@Override
	public long getLong(final String qualifier, final String key, final long defaultValue) throws SecurityException {
		final String value = get(key, null, getNodes(qualifier, key, context));
		return NumberUtils.toLong(value, defaultValue);
	}

	@Override
	public void put(final String qualifier, final String key, final String value, final boolean encrypt) throws SecurityException {
		if (null == value) {
			remove(qualifier, key);
		} else {
			put(qualifier, key, value, context, encrypt);
		}
	}

	private void put(final String qualifier, final String key, final String value, final IRuntimeContext context, final boolean encrypt) {
		if (ContextDebug.preferencesModify) {
			LOG.debug("Preference modification in context \"{}\" for qualifier \"{}\": {}", new Object[] { context, qualifier, key });
		}
		getNode(qualifier, key, context).put(EclipsePreferencesUtil.decodePath(key)[1], value);
	}

	@Override
	public void putBoolean(final String qualifier, final String key, final boolean value, final boolean encrypt) throws SecurityException {
		put(qualifier, key, Boolean.toString(value), context, encrypt);
	}

	@Override
	public void putByteArray(final String qualifier, final String key, final byte[] value, final boolean encrypt) throws SecurityException {
		if (null == value) {
			remove(qualifier, key);
		} else {
			try {
				put(qualifier, key, new String(Base64.encodeBase64(value), UTF_8), context, encrypt);
			} catch (final UnsupportedEncodingException e) {
				throw new IllegalStateException("Gyrex requires a platform which supports UTF-8.", e);
			}
		}
	}

	@Override
	public void putDouble(final String qualifier, final String key, final double value, final boolean encrypt) throws SecurityException {
		put(qualifier, key, Double.toString(value), context, encrypt);
	}

	@Override
	public void putFloat(final String qualifier, final String key, final float value, final boolean encrypt) throws SecurityException {
		put(qualifier, key, Float.toString(value), context, encrypt);
	}

	@Override
	public void putInt(final String qualifier, final String key, final int value, final boolean encrypt) throws SecurityException {
		put(qualifier, key, Integer.toString(value), context, encrypt);
	}

	@Override
	public void putLong(final String qualifier, final String key, final long value, final boolean encrypt) throws SecurityException {
		put(qualifier, key, Long.toString(value), context, encrypt);
	}

	@Override
	public void remove(final String qualifier, final String key) throws SecurityException {
		getNode(qualifier, key, context).remove(EclipsePreferencesUtil.decodePath(key)[1]);
	}

	@Override
	public void sync(final String qualifier) throws BackingStoreException, SecurityException {
		if (ContextDebug.preferencesModify) {
			LOG.debug("Synchronizing preferences of context \"{}\" for qualifier \"{}\"", context, qualifier);
		}
		getNode(qualifier, EMPTY, context).sync();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("GyrexContextPreferencesImpl [context=").append(context).append("]");
		return builder.toString();
	}

}
