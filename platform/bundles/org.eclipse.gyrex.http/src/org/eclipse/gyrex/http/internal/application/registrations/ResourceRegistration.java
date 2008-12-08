/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Cognos Incorporated, IBM Corporation - concept/implementation from 
 *                                            org.eclipse.equinox.http.servlet
 *     Gunnar Wagenknecht - adaption to CloudFree
 *******************************************************************************/
package org.eclipse.cloudfree.http.internal.application.registrations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.cloudfree.http.application.servicesupport.IResourceProvider;
import org.eclipse.cloudfree.http.internal.application.helpers.ServletUtil;
import org.osgi.framework.Bundle;

/**
 * A resource registration.
 * <p>
 * This class was inherited and adapted from
 * <code>org.eclipse.equinox.http.servlet.internal.ResourceRegistration</code>.
 * </p>
 */
public class ResourceRegistration extends Registration {

	private static final String LAST_MODIFIED = "Last-Modified"; //$NON-NLS-1$
	private static final String IF_MODIFIED_SINCE = "If-Modified-Since"; //$NON-NLS-1$
	private static final String IF_NONE_MATCH = "If-None-Match"; //$NON-NLS-1$
	private static final String ETAG = "ETag"; //$NON-NLS-1$

	private final String name;

	private IResourceProvider provider;
	private AccessControlContext acc;

	private final ServletContext servletContext;

	/**
	 * Creates a new instance.
	 * 
	 * @param alias
	 * @param name
	 * @param provider
	 * @param registrationsManager
	 * @param providerBundle
	 * @param acc
	 */
	public ResourceRegistration(final String alias, final String name, final IResourceProvider provider, final ServletContext servletContext, final RegistrationsManager registrationsManager, final Bundle providerBundle, final AccessControlContext acc) {
		super(alias, registrationsManager, providerBundle);
		this.name = name;
		this.provider = provider;
		this.servletContext = servletContext;
		this.acc = acc;
	}

	private String computeResourcePath(final String requestedPath, final String alias) {
		final int aliasLength = alias.equals("/") ? 0 : alias.length(); //$NON-NLS-1$
		return name + (null != requestedPath ? requestedPath.substring(aliasLength) : "");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.internal.application.registrations.Registration#doClose()
	 */
	@Override
	protected void doClose() {
		provider = null;
		acc = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.internal.application.registrations.Registration#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		// empty
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.internal.application.registrations.Registration#doHandleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String)
	 */
	@Override
	protected boolean doHandleRequest(final HttpServletRequest req, final HttpServletResponse resp, final String alias) throws ServletException, IOException {
		final IResourceProvider provider = this.provider;
		if (null == provider) {
			return false;
		}

		final String method = req.getMethod();
		if (method.equals("GET") || method.equals("POST") || method.equals("HEAD")) { //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

			final String pathInfo = ServletUtil.getPathInfo(req);
			final String resourcePath = computeResourcePath(pathInfo, alias);

			// check if we have a resource
			final URL resourceUrl = provider.getResource(resourcePath);
			if (resourceUrl == null) {
				return false;
			}

			// check if we have a directory
			final Set resourcePaths = provider.getResourcePaths(resourcePath);
			if (null != resourcePaths) {
				// enforce that all directories are accessed with URLs ending with a slash (like Apache)
				if ((null == pathInfo) || !pathInfo.endsWith("/")) {
					// build URL relative to servlet container root 
					resp.sendRedirect(req.getContextPath().concat(req.getServletPath()).concat(null != pathInfo ? pathInfo.concat("/") : "/"));
					return true;
				}

				// test if there is an index.html
				final String indexResourcePath = resourcePath.concat("index.html");
				if (resourcePaths.contains(indexResourcePath)) {
					// use the index file
					final URL indexResourceUrl = provider.getResource(indexResourcePath);
					if (null != indexResourceUrl) {
						return writeResource(req, resp, indexResourcePath, indexResourceUrl, provider, null != pathInfo ? pathInfo.concat("index.html") : "index.html");
					}
				}
			}

			// write resource
			return writeResource(req, resp, resourcePath, resourceUrl, provider, pathInfo);
		}
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

		return true;
	}

	/**
	 * Calls {@link IResourceProvider#getMimeType(String)} with the computed
	 * resource path.
	 * 
	 * @param path
	 *            the resource path
	 * @param alias
	 *            the matched alias
	 * @return whatever {@link IResourceProvider#getMimeType(String)} returned
	 * @throws MalformedURLException
	 * @see org.eclipse.cloudfree.http.application.servicesupport.IMimeTypeProvider#getMimeType(java.lang.String)
	 */
	public String getMimeType(final String path, final String alias) {
		final IResourceProvider provider = this.provider;
		if ((null == provider) || isClosed()) {
			return null;
		}
		return provider.getMimeType(computeResourcePath(path, alias));
	}

	/**
	 * Calls {@link IResourceProvider#getResource(String)} with the computed
	 * resource path.
	 * 
	 * @param path
	 *            the resource path
	 * @param alias
	 *            the matched alias
	 * @return whatever {@link IResourceProvider#getResource(String)} returned
	 * @throws MalformedURLException
	 * @see org.eclipse.cloudfree.http.application.servicesupport.IResourceProvider#getResource(java.lang.String)
	 */
	public URL getResource(final String path, final String alias) throws MalformedURLException {
		final IResourceProvider provider = this.provider;
		if ((null == provider) || isClosed()) {
			return null;
		}
		return provider.getResource(computeResourcePath(path, alias));
	}

	/**
	 * Calls {@link IResourceProvider#getResourcePaths(String)} with the
	 * computed resource path.
	 * 
	 * @param path
	 *            the resource path
	 * @param alias
	 *            the matched alias
	 * @return whatever {@link IResourceProvider#getResourcePaths(String)}
	 *         returned
	 * @see org.eclipse.cloudfree.http.application.servicesupport.IResourceProvider#getResourcePaths(java.lang.String)
	 */
	public Set getResourcePaths(final String path, final String alias) {
		final IResourceProvider provider = this.provider;
		if ((null == provider) || isClosed()) {
			return null;
		}

		return provider.getResourcePaths(computeResourcePath(path, alias));
	}

	void sendError(final HttpServletResponse resp, final int sc) throws IOException {

		try {
			// we need to reset headers for 302 and 403
			resp.reset();
			resp.sendError(sc);
		} catch (final IllegalStateException e) {
			// this could happen if the response has already been committed
		}
	}

	private boolean writeResource(final HttpServletRequest req, final HttpServletResponse resp, final String resourcePath, final URL resourceURL, final IResourceProvider resourceProvider, final String pathInfo) throws IOException {
		final ServletContext servletContext = this.servletContext;
		Boolean result = Boolean.TRUE;
		try {
			result = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {

				public Boolean run() throws Exception {
					final URLConnection connection = resourceURL.openConnection();
					final long lastModified = connection.getLastModified();
					final int contentLength = connection.getContentLength();

					String etag = null;
					if ((lastModified != -1) && (contentLength != -1)) {
						etag = "W/\"" + contentLength + "-" + lastModified + "\""; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					}

					// Check for cache revalidation.
					// We should prefer ETag validation as the guarantees are stronger and all HTTP 1.1 clients should be using it
					final String ifNoneMatch = req.getHeader(IF_NONE_MATCH);
					if ((ifNoneMatch != null) && (etag != null) && (ifNoneMatch.indexOf(etag) != -1)) {
						resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return Boolean.TRUE;
					}

					final long ifModifiedSince = req.getDateHeader(IF_MODIFIED_SINCE);
					// for purposes of comparison we add 999 to ifModifiedSince since the fidelity
					// of the IMS header generally doesn't include milli-seconds
					if ((ifModifiedSince > -1) && (lastModified > 0) && (lastModified <= (ifModifiedSince + 999))) {
						resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return Boolean.TRUE;
					}

					// return the full contents regularly
					if (contentLength != -1) {
						resp.setContentLength(contentLength);
					}

					String contentType = resourceProvider.getMimeType(resourcePath);
					if (contentType == null) {
						// ask container
						if ((null != servletContext) && (null != pathInfo)) {
							// using the path info because we don't want to expose our 
							// internal path to the container
							contentType = servletContext.getMimeType(pathInfo);
						}
					}

					if (contentType != null) {
						resp.setContentType(contentType);
					}

					if (lastModified > 0) {
						resp.setDateHeader(LAST_MODIFIED, lastModified);
					}

					if (etag != null) {
						resp.setHeader(ETAG, etag);
					}

					if (contentLength != 0) {
						// open the input stream
						InputStream is = null;
						try {
							is = connection.getInputStream();
							// write the resource
							try {
								final OutputStream os = resp.getOutputStream();
								final int writtenContentLength = writeResourceToOutputStream(is, os);
								if ((contentLength == -1) || (contentLength != writtenContentLength)) {
									resp.setContentLength(writtenContentLength);
								}
							} catch (final IllegalStateException e) { // can occur if the response output is already open as a Writer
								final Writer writer = resp.getWriter();
								writeResourceToWriter(is, writer);
								// Since ContentLength is a measure of the number of bytes contained in the body
								// of a message when we use a Writer we lose control of the exact byte count and
								// defer the problem to the Servlet Engine's Writer implementation.
							}
						} catch (final FileNotFoundException e) {
							// FileNotFoundException may indicate the following scenarios
							// - url is a directory
							// - url is not accessible
							sendError(resp, HttpServletResponse.SC_FORBIDDEN);
						} catch (final SecurityException e) {
							// SecurityException may indicate the following scenarios
							// - url is not accessible
							sendError(resp, HttpServletResponse.SC_FORBIDDEN);
						} finally {
							if (is != null) {
								try {
									is.close();
								} catch (final IOException e) {
									// ignore
								}
							}
						}
					} else {
						// zero content ... this might be a directory
						sendError(resp, HttpServletResponse.SC_FORBIDDEN);
					}
					return Boolean.TRUE;
				}
			}, acc);
		} catch (final PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
		return result.booleanValue();
	}

	int writeResourceToOutputStream(final InputStream is, final OutputStream os) throws IOException {
		final byte[] buffer = new byte[8192];
		int bytesRead = is.read(buffer);
		int writtenContentLength = 0;
		while (bytesRead != -1) {
			os.write(buffer, 0, bytesRead);
			writtenContentLength += bytesRead;
			bytesRead = is.read(buffer);
		}
		return writtenContentLength;
	}

	void writeResourceToWriter(final InputStream is, final Writer writer) throws IOException {
		final Reader reader = new InputStreamReader(is);
		try {
			final char[] buffer = new char[8192];
			int charsRead = reader.read(buffer);
			while (charsRead != -1) {
				writer.write(buffer, 0, charsRead);
				charsRead = reader.read(buffer);
			}
		} finally {
			if (reader != null) {
				reader.close(); // will also close input stream
			}
		}
	}
}
