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
package org.eclipse.gyrex.http.application.manager;

import java.net.MalformedURLException;
import java.util.Map;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

/**
 * A management interface for Gyrex HTTP applications.
 * <p>
 * Gyrex offers applications which run in a specific context (eg. a tenant
 * specific context). Thus, multiple instances of the <em>same</em> application
 * may be running. Each instance may expose differences in its behavior and
 * functionality depending on the context it is running in. The manager allows
 * to mount applications to the OSGi HTTP service(s) used by Gyrex.
 * </p>
 * <p>
 * The manager is provided as an OSGi service. A Gyrex based platform may use
 * OSGi security restrictions to limit the registration of applications.
 * </p>
 * 
 * @see Application
 * @see ApplicationProvider
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IApplicationManager {

	/** OSGi service name constant for the application manager service */
	String SERVICE_NAME = IApplicationManager.class.getName();

	/**
	 * Activates an application.
	 * <p>
	 * This method has no effect if the application is already active.
	 * </p>
	 * 
	 * @param applicationId
	 *            the id of the application which should be activated
	 * @throws IllegalArgumentException
	 *             if the specified application id is invalid
	 * @throws IllegalStateException
	 *             if no application is mounted for the specified url
	 */
	void activate(String applicationId) throws IllegalArgumentException, IllegalArgumentException;

	/**
	 * Deactivates an application.
	 * <p>
	 * If the application is currently active all its mounted URLs will be
	 * unmounted. Any active {@link Application} instance will be destroyed. As
	 * a result, it will not be available anymore for handling requests.
	 * Deactivated applications will retain any of its persisted settings and
	 * mounts, though. Those will be restored when the application is activated
	 * again.
	 * </p>
	 * <p>
	 * This method has no effect if the application is not active.
	 * </p>
	 * 
	 * @param applicationId
	 *            the id of the application which should be activated
	 * @throws IllegalArgumentException
	 *             if the specified application id is invalid
	 * @throws IllegalStateException
	 *             if no application is mounted for the specified url
	 */
	void deactivate(String applicationId) throws IllegalArgumentException, IllegalArgumentException;

	/**
	 * Returns the properties currently set for application.
	 * <p>
	 * If the specified application is unknown, <code>null</code> will be
	 * returned. The returned map can be freely modified. No changes will be
	 * reflected to the persisted properties. In order to update any properties,
	 * the updated map needs to be passed to {@link #setProperties(String, Map)}
	 * .
	 * </p>
	 * 
	 * @param applicationId
	 *            the application id
	 * @return a map of application properties which may the application further
	 *         when
	 *         {@link Application#initialize(org.eclipse.gyrex.http.application.service.IApplicationServiceSupport)}
	 *         is invoked
	 * @throws IllegalArgumentException
	 *             if the application id is invalid
	 */
	Map<String, String> getProperties(String applicationId) throws IllegalArgumentException;

	/**
	 * Mounts an application at the specified URL.
	 * <p>
	 * An URL must begin with a protocol (<code>http://</code> or
	 * <code>https://</code>) followed by a domain name with a port and a path
	 * (eg. <code>http://shop.gyrex.net/admin/</code>). The domain name, port
	 * and path may be optional. Everything below the specified URL is intended
	 * to be controlled by the application.
	 * </p>
	 * <p>
	 * HTTP requests will be mapped to applications similar to how the OSGi Http
	 * Service maps requests to servlet and resource registrations with the
	 * addition of adding protocol, domain and port matching to prefix path
	 * matching.
	 * </p>
	 * <p>
	 * The protocol must match exactly. No substring/prefix/suffix matching will
	 * be performed with the protocol. Only <code>http</code> or
	 * <code>https</code> are supported protocols at this time.
	 * </p>
	 * <p>
	 * Domain names will be matched using a suffix name matching. The longest
	 * matching domain wins. If no domain is provided the application will match
	 * to all domains after no other application matched the HTTP requests
	 * domain name.
	 * </p>
	 * <p>
	 * If no port is provided the application will match to all ports the
	 * underlying HTTP container listens to after the protocol and domain
	 * matched and no other application is mount directly to the port.
	 * </p>
	 * <p>
	 * If no path is provided the "root" path (i.e. "/") will be used. Trailing
	 * slashes will be removed. Paths will be match last using a prefix path
	 * matching. The longest matching path wins.
	 * </p>
	 * <p>
	 * If a URL is already in use a {@link MountConflictException} will be
	 * thrown.
	 * </p>
	 * 
	 * @param url
	 *            the url the application should be mounted on (must use
	 *            <code>http://</code> or <code>https://</code> protocol)
	 * @param applicationId
	 *            the application id
	 * @throws IllegalArgumentException
	 *             if any of the specified arguments are invalid (eg. an
	 *             unsupported protocol is used)
	 * @throws MountConflictException
	 *             if an application for the specified URL is already registered
	 * @throws MalformedURLException
	 *             if the specified url is invalid
	 * @throws IllegalStateException
	 *             if no application with the specified id exists
	 * @see Application
	 * @see ApplicationProvider
	 */
	void mount(String url, String applicationId) throws IllegalArgumentException, MountConflictException, MalformedURLException;

	/**
	 * Registers an application.
	 * <p>
	 * Application instances will be created using the specified provider. The
	 * provider will be looked up from the list of provides made available as
	 * OSGi service instances when an application instance is needed. Thus, the
	 * provider does not need to be available at the time this method is
	 * invoked.
	 * </p>
	 * <p>
	 * Applications are created lazily when the first request to an application
	 * is received. They are identified by an {@link Application#getId() id}
	 * specified via <code>applicationId</code>.
	 * </p>
	 * <p>
	 * If an application id is already in use a
	 * {@link ApplicationRegistrationException} will be thrown.
	 * </p>
	 * 
	 * @param applicationId
	 *            the application id
	 * @param providerId
	 *            the provider id
	 * @param context
	 *            the context the application will operate in
	 * @param properties
	 *            application properties which may configure the application
	 *            further when
	 *            {@link Application#initialize(org.eclipse.gyrex.http.application.context.IApplicationContext)}
	 *            is invoked
	 * @throws ApplicationRegistrationException
	 *             if an application with the specified id is already registered
	 */
	void register(String applicationId, String providerId, IRuntimeContext context, Map<String, String> properties) throws ApplicationRegistrationException;

	/**
	 * Sets the properties of an application.
	 * <p>
	 * Note, although the properties will be updated it won't affect any running
	 * applications. In order to re-initialize any running application they need
	 * to be {@link #deactivate(String) deactivated first} and
	 * {@link #activate(String) activated} again. The recommended workflow is to
	 * {@link #deactivate(String) deactivate}, update the properties, and
	 * {@link #activate(String) activate it again thereafter}.
	 * </p>
	 * 
	 * @param applicationId
	 *            the application id
	 * @param properties
	 *            application properties which may configure the application
	 *            further when
	 *            {@link Application#initialize(org.eclipse.gyrex.http.application.service.IApplicationServiceSupport)}
	 *            is invoked
	 * @throws IllegalArgumentException
	 *             if any of the specified arguments are invalid (eg. an
	 *             unsupported protocol is used)
	 * @throws IllegalStateException
	 *             if no application is registered for the specified id
	 */
	void setProperties(String applicationId, Map<String, String> properties) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Unmounts an application mounted at the specified URL.
	 * 
	 * @param url
	 *            the mount point
	 * @throws IllegalArgumentException
	 *             if any of the specified arguments are invalid (eg. an
	 *             unsupported protocol is used)
	 * @throws MalformedURLException
	 *             if the specified url is invalid
	 * @throws IllegalStateException
	 *             if no application is mounted for the specified url
	 */
	void unmount(String url) throws IllegalArgumentException, MalformedURLException, IllegalStateException;

	/**
	 * Unregisters an application and removes any mounts associated with it.
	 * 
	 * @param applicationId
	 *            the id of the application which should be unregistered
	 * @throws IllegalArgumentException
	 *             if the specified application id is invalid
	 * @throws IllegalStateException
	 *             if no application is mounted for the specified url
	 */
	void unregister(String applicationId) throws IllegalArgumentException, IllegalArgumentException;
}
