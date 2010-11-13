/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.application;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;

import org.osgi.service.http.HttpContext;

/**
 * A Gyrex HTTP application instance.
 * <p>
 * Gyrex defines HTTP applications as a point for bundling a set of
 * functionality offered via HTTP operating in a specific
 * {@link IRuntimeContext context}. Applications are contributed to the platform
 * by {@link ApplicationProvider providers}.
 * </p>
 * <p>
 * Applications are created lazily the first time a request for an application
 * is received. They are defined through an administrative API provided by
 * {@link IApplicationManager}. Applications are identified by an
 * {@link #getId() id}. At any time there is at most one single application
 * instance for the same {@link #getId() id}.
 * </p>
 * <p>
 * Although it may be possible to compare Gyrex HTTP applications to JavaEE web
 * applications, they do not provide the full set of functionality provided by a
 * JavaEE web application. The central point of entrance of a HTTP request is
 * {@link #handleRequest(HttpServletRequest, HttpServletResponse)}. From there
 * on its the responsibility of the application to deal with the request. This
 * allows for a great flexibility but also comes with big responsibility. It's
 * the task of the application to handle security and to deal with Servlets,
 * their registrations and filters and such things.
 * </p>
 * <p>
 * Similar to the OSGi Http Service, an application may allow to register
 * servlets and resources dynamically. However, it's the decision and
 * responsibility of the application to enable and support that dynamic
 * behavior. The creation of such dynamic behavior is supported by an
 * application through {@link IApplicationContext}.
 * </p>
 * <p>
 * An application defines a scope for all servlets and resources registered with
 * it. The underlying assumption is that resources and servlets are shared
 * within an application's boundaries. Therefore this class implements methods
 * similar to those defined in the {@link HttpContext} interface which - in
 * their default implementations - are delegated to the
 * {@link IApplicationContext}. It allows for better interoperability with OSGi,
 * the underlying servlet container and the registered servlets and resources.
 * </p>
 * <p>
 * Sharing servlets and resources across application instances is not supported
 * out-of-the-box for security and proper capsulation reasons.
 * </p>
 * <p>
 * This class may be subclassed by clients providing a custom application type.
 * Only references to methods defined in this class are valid.
 * </p>
 * 
 * @see ApplicationProvider
 */
public abstract class Application extends PlatformObject {

	/** the application id */
	private final String id;

	/** the context */
	private final IRuntimeContext context;

	/** the application context */
	private final AtomicReference<IApplicationContext> applicationContext = new AtomicReference<IApplicationContext>();

	/** the application status */
	private final AtomicReference<IStatus> status = new AtomicReference<IStatus>();

	/** deferred initialization */
	private final AtomicLong initTimestamp = new AtomicLong();
	private final Lock initLock = new ReentrantLock();

	/** the destroyed state */
	private final AtomicBoolean destroyed = new AtomicBoolean();

	/**
	 * Creates a new application instance.
	 * 
	 * @param id
	 *            the application id
	 * @param context
	 *            the context
	 */
	protected Application(final String id, final IRuntimeContext context) {
		if (null == id) {
			throw new IllegalArgumentException("id must not be null");
		}
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}

		this.id = id.intern();
		this.context = context;
	}

	private void deferredInit() {
		try {
			if (!initLock.tryLock(2, TimeUnit.SECONDS)) {
				throw new ApplicationException(503, "Initialization In Progress");
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApplicationException(503, "Not Initialized");
		}
		try {
			if (initTimestamp.get() == 0) {
				try {
					doInit();
					initTimestamp.set(System.currentTimeMillis());
				} catch (final Exception e) {
					throw new ApplicationException(503, "Inititalization Error", e);
				}
			}
		} finally {
			initLock.unlock();
		}
	}

	/**
	 * Called by the platform to destroy the application when it's no longer
	 * needed.
	 * <p>
	 * The implementation first sets an internal flag so that it stops receiving
	 * requests. Next, it calls {@link #doDestroy()} and after that it releases
	 * the reference to the {@link IApplicationContext} if available. Subclasses
	 * may override {@link #doDestroy()} to perform necessary cleanup.
	 * </p>
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final void destroy() {
		// set status
		destroyed.set(true);

		try {
			// destroy
			doDestroy();
		} finally {
			// unset
			applicationContext.set(null);
		}
	}

	/**
	 * Called by {@link #destroy()} to release/destroy any application specific
	 * resources.
	 * <p>
	 * The default implementation does nothing. Subclasses may override.
	 * </p>
	 */
	protected void doDestroy() {
		// empty
	}

	/**
	 * Called by {@link #initialize(IApplicationContext)} to perform any
	 * application specific initialization.
	 * <p>
	 * The default implementation does nothing. Subclasses may override.
	 * </p>
	 */
	protected void doInit() throws CoreException {
		// empty

	}

	/**
	 * Returns the application context passed to
	 * {@link #initialize(IApplicationContext)}.
	 * 
	 * @return the application context (maybe <code>null</code> if not supported
	 *         by the application or the application has been destroyed)
	 */
	protected IApplicationContext getApplicationServiceSupport() {
		return applicationContext.get();
	}

	/**
	 * Returns the context the application operates in.
	 * 
	 * @return the context
	 */

	public final IRuntimeContext getContext() {
		return context;
	}

	/**
	 * Returns the application id.
	 * 
	 * @return the application id
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Returns the status of the application.
	 * <p>
	 * The status indicates if the application will operate properly. If any
	 * status with a severity other than {@link IStatus#OK} is returned the
	 * platform may reject request to this application with a
	 * {@link HttpServletResponse#SC_SERVICE_UNAVAILABLE} error and the message
	 * provided in the status object returned.
	 * </p>
	 * <p>
	 * Clients that want to modify the application status should do so in a
	 * background operation and set it via {@link #setStatus(IStatus)}.
	 * </p>
	 * <p>
	 * If the application is {@link #destroy() destroyed}, this method will
	 * always return a status of severity {@link IStatus#CANCEL} to stop
	 * receiving any further requests.
	 * </p>
	 * 
	 * @return the application status
	 */
	public final IStatus getStatus() {
		// check if destroyed
		if (destroyed.get()) {
			return Status.CANCEL_STATUS;
		}

		// get status
		final IStatus status = this.status.get();
		if (null != status) {
			return status;
		}

		// assume OK
		return Status.OK_STATUS;
	}

	/**
	 * Called by the platform to allow the application to respond to a HTTP
	 * request.
	 * <p>
	 * The default implementation first calls
	 * {@link #handleSecurity(HttpServletRequest, HttpServletResponse)} to
	 * ensure that the request is allowed to be handled. It then asks the
	 * {@link IApplicationContext} to handle the request.
	 * </p>
	 * 
	 * @param request
	 *            the <code>HttpServletRequest</code> object that contains the
	 *            client's request
	 * @param response
	 *            the <code>HttpServletResponse</code> object that contains the
	 *            servlet's response
	 * @exception ApplicationException
	 *                if an exception occurs that interferes with the
	 *                application's normal operation
	 * @exception IOException
	 *                if an input or output exception occurs
	 * @see Servlet#service(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse)
	 */
	public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ApplicationException {
		// check for deferred initialization
		if (initTimestamp.get() == 0) {
			deferredInit();
		}

		// check security
		if (!handleSecurity(request, response)) {
			return;
		}

		// get the context
		final IApplicationContext context = getApplicationServiceSupport();
		if (null == context) {
			// if there is no context this method should not be overridden
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
			return;
		}

		// let the application context handle the request
		if (context.handleRequest(request, response)) {
			return;
		}

		// return 404, no resource registered
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Handles security for the specified request.
	 * <p>
	 * The default implementation of
	 * {@link #handleRequest(HttpServletRequest, HttpServletResponse)} calls
	 * this method prior to servicing the specified request. This method
	 * controls whether the request is processed in the normal manner or an
	 * error is returned.
	 * </p>
	 * <p>
	 * The purpose of this method is similar to
	 * {@link HttpContext#handleSecurity(HttpServletRequest, HttpServletResponse)}
	 * but adapted to the {@link Application} scope. The following documentation
	 * was inherited and adapted from {@link HttpContext}.
	 * </p>
	 * <p>
	 * If the request requires authentication and the Authorization header in
	 * the request is missing or not acceptable, then this method should set the
	 * WWW-Authenticate header in the response object, set the status in the
	 * response object to Unauthorized(401) and return <code>false</code>. See
	 * also RFC 2617: <i>HTTP Authentication: Basic and Digest Access
	 * Authentication </i> (available at http://www.ietf.org/rfc/rfc2617.txt).
	 * </p>
	 * <p>
	 * If the request requires a secure connection and the
	 * <code>getScheme</code> method in the request does not return 'https' or
	 * some other acceptable secure protocol, then this method should set the
	 * status in the response object to Forbidden(403) and return
	 * <code>false</code>.
	 * </p>
	 * <p>
	 * When this method returns <code>false</code>, the application will send
	 * the response back to the client, thereby completing the request. When
	 * this method returns <code>true</code>, the Http Service will proceed with
	 * servicing the request.
	 * </p>
	 * <p>
	 * If the specified request has been authenticated, this method must set the
	 * {@link HttpContext#AUTHENTICATION_TYPE} request attribute to the type of
	 * authentication used, and the {@link HttpContext#REMOTE_USER} request
	 * attribute to the remote user (request attributes are set using the
	 * <code>setAttribute</code> method on the request). If this method does not
	 * perform any authentication, it must not set these attributes.
	 * </p>
	 * <p>
	 * If the authenticated user is also authorized to access certain resources,
	 * this method must set the {@link HttpContext#AUTHORIZATION} request
	 * attribute to the <code>Authorization</code> object obtained from the
	 * <code>org.osgi.service.useradmin.UserAdmin</code> service.
	 * </p>
	 * <p>
	 * The servlet responsible for servicing the specified request determines
	 * the authentication type and remote user by calling the
	 * <code>getAuthType</code> and <code>getRemoteUser</code> methods,
	 * respectively, on the request.
	 * </p>
	 * <p>
	 * The default implementation just returns <code>true</code>. Subclasses may
	 * overwrite.
	 * </p>
	 * 
	 * @param request
	 *            the HTTP request
	 * @param response
	 *            the HTTP response
	 * @return <code>true</code> if the request should be serviced,
	 *         <code>false</code> if the request should not be serviced and Http
	 *         Service will send the response back to the client.
	 * @throws java.io.IOException
	 *             may be thrown by this method. If this occurs, the Http
	 *             Service will terminate the request and close the socket.
	 * @see HttpContext#handleSecurity(HttpServletRequest, HttpServletResponse)
	 */
	protected boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		// TODO integrate with security from container
		return true;
	}

	/**
	 * Called by the platform to initialize the application.
	 * <p>
	 * This implementations remembers the {@link IApplicationContext} and then
	 * calls {@link #doInit()}. Subclasses may override {@link #doInit()}.
	 * </p>
	 * 
	 * @param applicationContext
	 *            the application context.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final void initialize(final IApplicationContext applicationContext) throws CoreException {
		if (null == applicationContext) {
			throw new IllegalArgumentException("application context must not be null");
		}
		this.applicationContext.set(applicationContext);

		try {
			doInit();
			initTimestamp.set(System.currentTimeMillis());
		} catch (final IllegalStateException e) {
			// deferred initialization
			initTimestamp.set(0);
		}
	}

	/**
	 * Sets or resets the application status.
	 * 
	 * @param status
	 *            the status to set, or <code>null</code> to reset
	 * @see #getStatus()
	 */
	public final void setStatus(final IStatus status) {
		this.status.set(status);
	}

	@Override
	public String toString() {
		final StringBuilder toString = new StringBuilder();
		toString.append(getClass().getSimpleName());
		toString.append("[");
		toString.append(id);
		toString.append("]");
		return toString.toString();
	}
}
