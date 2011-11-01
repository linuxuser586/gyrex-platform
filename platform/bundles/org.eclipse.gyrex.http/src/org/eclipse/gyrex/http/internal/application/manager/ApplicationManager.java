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
package org.eclipse.gyrex.http.internal.application.manager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.http.application.manager.ApplicationRegistrationException;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.application.manager.MountConflictException;
import org.eclipse.gyrex.http.internal.HttpActivator;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * The application manager.
 * 
 * @TODO we need to support providers in multiple versions
 */
public class ApplicationManager implements IApplicationManager {

	public static final String NODE_URLS = "urls";
	public static final String NODE_APPLICATIONS = "applications";
	public static final String NODE_PROPERTIES = "properties";
	public static final String KEY_CONTEXT_PATH = "contextPath";
	public static final String KEY_PROVIDER_ID = "providerId";
	public static final String KEY_ACTIVE = "active";
	public static final boolean DEFAULT_ACTIVE = true; // *true* for backwards compatibility

	public static IEclipsePreferences getAppsNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(HttpActivator.SYMBOLIC_NAME).node(NODE_APPLICATIONS);
	}

	public static IEclipsePreferences getUrlsNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(HttpActivator.SYMBOLIC_NAME).node(NODE_URLS);
	}

	@Override
	public void activate(final String applicationId) {
		if (!IdHelper.isValidId(applicationId)) {
			throw new IllegalArgumentException("invalid application id; please use only ascii chars a-z, 0-9, ., _ and/or -");
		}

		try {
			// check if there is a registration
			final Preferences node = getAppsNode();
			if (!node.nodeExists(applicationId)) {
				throw new IllegalStateException(String.format("Application '%s' does not exist", applicationId));
			}

			// activate
			node.node(applicationId).putBoolean(KEY_ACTIVE, true);
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error activating application. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void deactivate(final String applicationId) {
		if (!IdHelper.isValidId(applicationId)) {
			throw new IllegalArgumentException("invalid application id; please use only ascii chars a-z, 0-9, ., _ and/or -");
		}

		try {
			// check if there is a registration
			final Preferences node = getAppsNode();
			if (!node.nodeExists(applicationId)) {
				throw new IllegalStateException(String.format("Application '%s' does not exist", applicationId));
			}

			// deactivate
			node.node(applicationId).putBoolean(KEY_ACTIVE, false);
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error removing application registration info from the backend data store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	/**
	 * Gets an {@link ApplicationRegistration} object for a registered
	 * applications
	 * 
	 * @param applicationId
	 * @return
	 */
	public ApplicationRegistration getApplicationRegistration(final String applicationId) {
		try {
			final Preferences appNode = getAppsNode().node(applicationId);
			if (!appNode.nodeExists("")) {
				throw new IllegalStateException(String.format("application %s does not exists", applicationId));
			}

			final String providerId = appNode.get(KEY_PROVIDER_ID, null);
			if (StringUtils.isBlank(providerId)) {
				throw new IllegalStateException(String.format("application information %s contains invalid provider id %s", applicationId, String.valueOf(providerId)));
			}

			final String contextPath = appNode.get(KEY_CONTEXT_PATH, null);
			if (StringUtils.isBlank(contextPath) || !Path.EMPTY.isValidPath(contextPath)) {
				throw new IllegalStateException(String.format("application information %s contains invalid context path %s", applicationId, String.valueOf(contextPath)));
			}

			Map<String, String> properties = null;
			if (appNode.nodeExists(NODE_PROPERTIES)) {
				final Preferences propertiesNode = appNode.node(NODE_PROPERTIES);
				final String[] keys = propertiesNode.keys();
				if (keys.length > 0) {
					properties = new HashMap<String, String>(keys.length);
					for (final String key : keys) {
						properties.put(key, propertiesNode.get(key, null));
					}
				}
			}

			final IRuntimeContext context = HttpActivator.getInstance().getService(IRuntimeContextRegistry.class).get(new Path(contextPath));
			if (context == null) {
				throw new IllegalStateException(String.format("context %s does not exists", contextPath));
			}
			return new ApplicationRegistration(applicationId, providerId, context, properties);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error reading application info for application %s. %s", applicationId, ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	public Collection<String> getMounts(final String applicationId) throws IllegalStateException {
		final IEclipsePreferences urlsNode = ApplicationManager.getUrlsNode();
		try {
			final SortedSet<String> mounts = new TreeSet<String>();
			final String[] urls = urlsNode.keys();
			for (final String url : urls) {
				final String appId = urlsNode.get(url, StringUtils.EMPTY);
				if (appId.equals(applicationId)) {
					mounts.add(url);
				}
			}

			return mounts;
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error reading application info for application %s. %s", applicationId, ExceptionUtils.getRootCauseMessage(e)), e);
		}

	}

	@Override
	public Map<String, String> getProperties(final String applicationId) throws IllegalArgumentException {
		// verify application id
		if (!IdHelper.isValidId(applicationId)) {
			throw new IllegalArgumentException("invalid application id; please use only ascii chars a-z, 0-9, ., _ and/or -");
		}

		try {
			final Preferences appNode = getAppsNode().node(applicationId);
			if (!appNode.nodeExists("")) {
				return null;
			}

			final Map<String, String> properties = new HashMap<String, String>();
			if (appNode.nodeExists(NODE_PROPERTIES)) {
				final Preferences propertiesNode = appNode.node(NODE_PROPERTIES);
				final String[] keys = propertiesNode.keys();
				for (final String key : keys) {
					properties.put(key, propertiesNode.get(key, null));
				}
			}

			return properties;
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error reading application properties for application %s. %s", applicationId, ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	public Collection<String> getRegisteredApplications() throws BackingStoreException {
		return Arrays.asList(getAppsNode().childrenNames());
	}

	public boolean isActive(final String applicationId) {
		if (!IdHelper.isValidId(applicationId)) {
			throw new IllegalArgumentException("invalid application id; please use only ascii chars a-z, 0-9, ., _ and/or -");
		}

		try {
			// check if there is a registration
			final Preferences node = getAppsNode();
			if (!node.nodeExists(applicationId)) {
				throw new IllegalStateException(String.format("Application '%s' does not exist", applicationId));
			}

			// return
			return node.node(applicationId).getBoolean(KEY_ACTIVE, DEFAULT_ACTIVE);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error activating application. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void mount(final String url, final String applicationId) throws MountConflictException, MalformedURLException {
		// parse the url
		final URL parsedUrl = parseAndVerifyUrl(url);

		// verify application id
		if (!IdHelper.isValidId(applicationId)) {
			throw new IllegalArgumentException("invalid application id; please use only ascii chars a-z, 0-9, ., _ and/or -");
		}

		try {
			// verify the application exists
			final Preferences node = getAppsNode();
			if (!node.nodeExists(applicationId)) {
				throw new IllegalStateException(String.format("Application '%s' does not exist", applicationId));
			}

			// verify the url is not registered yet
			final String externalForm = parsedUrl.toExternalForm();
			final Preferences urlsNode = getUrlsNode();
			if (null != urlsNode.get(externalForm, null)) {
				throw new MountConflictException(url);
			}

			// put url
			urlsNode.put(externalForm, applicationId);
			urlsNode.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error persisting application registration info to the backend data store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private URL parseAndVerifyUrl(final String url) throws MalformedURLException {
		if (null == url) {
			throw new IllegalArgumentException("url must not be null");
		}

		// parse the url
		final URL parsedUrl = new URL(url);

		// verify protocol
		final String protocol = parsedUrl.getProtocol();
		if (!(protocol.equals("http") || protocol.equals("https"))) {
			throw new IllegalArgumentException("url '" + url + "' must start with 'http://' or 'https://'");
		}
		return parsedUrl;
	}

	@Override
	public void register(final String applicationId, final String providerId, final IRuntimeContext context, final Map<String, String> properties) throws ApplicationRegistrationException {
		if (!IdHelper.isValidId(applicationId)) {
			throw new IllegalArgumentException("invalid application id; please use only ascii chars a-z, 0-9, ., _ and/or -");
		}
		if (!IdHelper.isValidId(providerId)) {
			throw new IllegalArgumentException("invalid provider id; please use only ascii chars a-z, 0-9, ., _ and/or -");
		}
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}

		try {
			// check if there is already a registration
			final Preferences node = getAppsNode();
			if (node.nodeExists(applicationId)) {
				throw new ApplicationRegistrationException(applicationId);
			}

			// persist registration
			final Preferences appNode = node.node(applicationId);
			appNode.put(KEY_PROVIDER_ID, providerId);
			appNode.put(KEY_CONTEXT_PATH, context.getContextPath().toString());

			if (null != properties) {
				final Preferences propertiesNode = appNode.node(NODE_PROPERTIES);
				for (final Entry<String, String> entry : properties.entrySet()) {
					propertiesNode.put(entry.getKey(), entry.getValue());
				}
			}

			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error persisting application registration info to the backend data store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void setProperties(final String applicationId, final Map<String, String> properties) throws IllegalArgumentException, IllegalStateException {
		// verify application id
		if (!IdHelper.isValidId(applicationId)) {
			throw new IllegalArgumentException("invalid application id; please use only ascii chars a-z, 0-9, ., _ and/or -");
		}

		try {
			final Preferences appNode = getAppsNode().node(applicationId);
			if (!appNode.nodeExists("")) {
				throw new IllegalStateException(String.format("application %s does not exists", applicationId));
			}

			if ((null == properties) || properties.isEmpty()) {
				if (appNode.nodeExists(NODE_PROPERTIES)) {
					appNode.node(NODE_PROPERTIES).removeNode();
					appNode.flush();
				}
			} else {
				final Preferences propertiesNode = appNode.node(NODE_PROPERTIES);
				// update all properties
				for (final String key : properties.keySet()) {
					final String value = properties.get(key);
					if (StringUtils.isNotBlank(value)) {
						propertiesNode.put(key, value);
					} else {
						propertiesNode.remove(key);
					}
				}

				// remove obsolete properties
				final String[] keys = propertiesNode.keys();
				for (final String key : keys) {
					if (!properties.containsKey(key)) {
						propertiesNode.remove(key);
					}
				}

				appNode.flush();
			}
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error reading application properties for application %s. %s", applicationId, ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	@Override
	public void unmount(final String url) throws MalformedURLException, IllegalArgumentException, IllegalStateException {
		// parse the url
		final URL parsedUrl = parseAndVerifyUrl(url);

		final String applicationId;
		try {
			// check if defined
			final String externalForm = parsedUrl.toExternalForm();
			final Preferences urlsNode = getUrlsNode();
			applicationId = urlsNode.get(externalForm, null);

			// throw IllegalStateException if nothing was removed
			if (null == applicationId) {
				throw new IllegalStateException("no application was mounted for url '" + externalForm + "' (submitted url was '" + url + "')");
			}

			// remove from persisted info
			urlsNode.remove(externalForm);
			urlsNode.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error persisting application registration info to the backend data store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void unregister(final String applicationId) {
		// remove persistent info
		try {
			// check if there is a registration
			final Preferences node = getAppsNode();
			if (!node.nodeExists(applicationId)) {
				throw new IllegalStateException(String.format("Application '%s' does not exist", applicationId));
			}

			// remove
			node.node(applicationId).removeNode();
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error removing application registration info from the backend data store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

}
