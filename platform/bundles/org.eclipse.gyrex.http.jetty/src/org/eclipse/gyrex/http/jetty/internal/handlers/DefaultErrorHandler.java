/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.handlers;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.http.jetty.internal.app.ApplicationHandler;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.log.Log;

/**
 * Some default processing for errors.
 */
public class DefaultErrorHandler extends ErrorHandler {

	private static final String NEWLINE = "\n";

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	private static final boolean debugMode = Platform.inDebugMode();

	/** handled methods */
	private static final Set<String> handledMethods;
	static {
		final HashSet<String> methods = new HashSet<String>();
		methods.add(HttpMethods.GET);
		methods.add(HttpMethods.POST);
		methods.add(HttpMethods.PUT);
		methods.add(HttpMethods.HEAD);
		methods.add(HttpMethods.DELETE);
		handledMethods = Collections.unmodifiableSet(methods);
	}

	/**
	 * Creates a new instance.
	 */
	public DefaultErrorHandler() {
		setShowStacks(Platform.inDevelopmentMode());
	}

	private boolean acceptsHtml(final HttpServletRequest request) {
		final Enumeration acceptHeaders = request.getHeaders(HttpHeaders.ACCEPT);
		if (acceptHeaders != null) {
			// scan all Accept headers for text/html, x-html or */*
			while (acceptHeaders.hasMoreElements()) {
				final String accept = (String) acceptHeaders.nextElement();
				if ((accept.indexOf("text/html") > -1) || (accept.indexOf("application/xhtml") > -1) || (accept.indexOf("*/*") > -1)) {
					return true;
				}
			}
		}
		return false;
	}

	private void generateErrorPagePlain(final HttpServletRequest request, final HttpServletResponse response, final int code, final String internalMessage, final String officialMessage, final String serverName) throws IOException {
		response.setContentType(MimeTypes.TEXT_HTML_8859_1);
		final ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(4096);
		writeErrorPagePlain(request, writer, code, internalMessage, officialMessage, serverName);
		writer.flush();
		response.setContentLength(writer.size());
		writer.writeTo(response.getOutputStream());
		writer.destroy();
	}

	/**
	 * Returns the admin server URL
	 * 
	 * @return the admin server URL
	 */
	private String getAdminServerURL(final HttpServletRequest request) {
		// TODO: admin server scheme should be HTTPS (not implemented yet=
		// TODO: lookup the admin server port from the preferences
		return "http://".concat(request.getServerName()).concat(":3110/");
	}

	private Throwable getException(final HttpServletRequest request) {
		return (Throwable) request.getAttribute("javax.servlet.error.exception");
	}

	private String getOverallStatusMessage(final IStatus status) {
		switch (status.getSeverity()) {
			case IStatus.CANCEL:
			case IStatus.ERROR:
				return "It looks like that this server is not configured properly.";
			case IStatus.WARNING:
				return "It looks like that the platform configuration is not perfect.";

			case IStatus.INFO:
			default:
				return "The platform configuration looks okay. Some hints/notes are available, though.";
		}
	}

	private String getServerName(final HttpServletRequest request) {
		String serverName = null;

		// try the server name the connection is configured to
		final HttpConnection httpConnection = HttpConnection.getCurrentConnection();
		if (null != httpConnection) {
			serverName = httpConnection.getConnector().getHost();
		}

		// try the local machine name if bound to 0.0.0.0
		if ((null == serverName) || serverName.equals("0.0.0.0")) {
			try {
				serverName = InetAddress.getLocalHost().getHostName();
			} catch (final UnknownHostException e) {
				Log.ignore(e);

				// try the host name provided in the request
				serverName = request.getServerName();
			}
		}
		return serverName;
	}

	private String getStatusBullet(final IStatus status) {
		switch (status.getSeverity()) {
			case IStatus.CANCEL:
			case IStatus.ERROR:
				return " ! ";
			case IStatus.WARNING:
				return " ? ";

			case IStatus.INFO:
			default:
				return " * ";
		}
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final HttpConnection connection = HttpConnection.getCurrentConnection();
		if (connection == null) {
			throw new IllegalStateException("Called outside request thread! No connection available.");
		}

		// mark handled
		connection.getRequest().setHandled(true);

		// only render output for a few methods
		final String method = request.getMethod();
		if (!handledMethods.contains(method)) {
			return;
		}

		// con't cache error pages
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
		response.setHeader(HttpHeaders.PRAGMA, "no-cache");

		// get code and message
		final int code = connection.getResponse().getStatus();
		String internalMessage = connection.getResponse().getReason();

		// handle empty message
		if ((null == internalMessage) && (code == 500) && (null != getException(request))) {
			internalMessage = getException(request).toString();
		}

		// decode message
		if (internalMessage != null) {
			internalMessage = URLDecoder.decode(internalMessage, "UTF-8");
		}

		// we do not want to hand out internal details in production mode
		final String officialMessage = HttpStatus.getMessage(code);

		// support non-html response
		if (!acceptsHtml(request)) {
			generateErrorPagePlain(request, response, code, internalMessage, officialMessage, getServerName(request));
			return;
		}

		// create error page
		final ErrorPage errorPage = new ErrorPage() {
			@Override
			protected void writeDebugInfo(final HttpServletRequest request, final Writer writer) throws IOException {
				// write stack
				super.writeDebugInfo(baseRequest, writer);

				// write optional debug info if available
				if (getCode() == 404) {
					// write application stack
					final Object debugInfo = request.getAttribute(ApplicationHandler.ATTRIBUTE_DEBUG_INFO);
					if (debugInfo != null) {
						writer.write("<div class=\"dev_note\">");
						writer.write("Resources known to last request handling application are:");
						writer.write("<pre>");
						writer.write(NEWLINE);
						writeEscaped(writer, debugInfo.toString());
						writer.write("</pre>");
						writer.write(NEWLINE);
						writer.write("</div>");
					}

					// write application list
					writer.write("<div class=\"dev_note\">");
					final Server server = getServer();
					final Handler[] handlers = server == null ? null : server.getChildHandlersByClass(ApplicationHandler.class);
					if ((handlers != null) && (handlers.length > 0)) {
						writer.write("Applications known to this server are: <ul>");
						writer.write(NEWLINE);
						for (int i = 0; (handlers != null) && (i < handlers.length); i++) {
							writer.write("<li>");
							final ApplicationHandler appHandler = (ApplicationHandler) handlers[i];
							writeEscaped(writer, appHandler.getApplicationId());
							writer.write(" (");
							writeEscaped(writer, appHandler.getApplicationRegistration().getProviderId());
							writer.write('@');
							writeEscaped(writer, appHandler.getApplicationRegistration().getContext().getContextPath());
							writer.write(")");
							if (appHandler.isRunning()) {
								writer.write(" [running]");
							} else {
								if (appHandler.isFailed()) {
									writer.write(" [failed]");
								}
								if (appHandler.isStopped()) {
									writer.write(" [stopped]");
								}
							}
							final String[] urls = appHandler.getUrls();
							for (final String url : urls) {
								writer.write(String.format("<br><small> --&gt; <a href=\"%s\">%s</a></small>", url, url));
							}
							writer.write("</li>");
							writer.write(NEWLINE);
						}
						writer.write("</ul>");
						writer.write(NEWLINE);
					} else {
						writer.write("No applications known to this server!");
						writer.write(NEWLINE);
					}
					writer.write("</div>");
				}
			}
		};

		// set page details
		errorPage.setCode(code);
		errorPage.setOfficialMessage(officialMessage);
		errorPage.setInternalMessage(internalMessage);

		// render page
		errorPage.render(request, response);
	}

	private void writeErrorPagePlain(final HttpServletRequest request, final Writer writer, final int code, final String internalMessage, final String officialMessage, final String serverName) throws IOException {
		HttpConnection.getCurrentConnection().getResponse().setContentType(MimeTypes.TEXT_PLAIN_8859_1);
		writer.write("Error ");
		writer.write(Integer.toString(code));
		writer.write(" - ");
		writePlain(writer, officialMessage);
		writer.write(NEWLINE);

		if (debugMode) {
			if (null != internalMessage) {
				writer.write(NEWLINE);
				writePlain(writer, internalMessage);
				writer.write(NEWLINE);
			}

			final Throwable exception = getException(request);
			if (null != exception) {
				writer.write(NEWLINE);
				writeException(exception, writer);
				writer.write(NEWLINE);
			}

			writer.write(NEWLINE);
			final IStatus platformStatus = PlatformConfiguration.getPlatformStatus();
			if (!platformStatus.isOK()) {
				writer.write(getOverallStatusMessage(platformStatus) + NEWLINE);
				writer.write("You might want to check the server configuration (" + getAdminServerURL(request) + ")." + NEWLINE);
				writer.write(NEWLINE);
				writer.write("Issues detected on ");
				writer.write(serverName);
				writer.write(":" + NEWLINE);
				writeStatusPlain(platformStatus, writer);
				writer.write(NEWLINE);
			} else {
				writer.write("A note to developers, this server seems to be configured properly." + NEWLINE + "At least, no issues were detected." + NEWLINE);
			}

		} else {
			writer.write("If you think you\'ve reached this page in error:" + NEWLINE + "  * Make sure the URL you\'re trying to reach is correct." + NEWLINE + "</ul>\n\r" + NEWLINE);
		}

		writer.write(NEWLINE);
		writer.write(NEWLINE);
		writer.write("-- ");
		writer.write(NEWLINE);
		writer.write("Brought to you by Gyrex. Powered by Jetty and Equinox.");
		writer.write(NEWLINE);
	}

	private void writeEscaped(final Writer writer, final String string) throws IOException {
		if (string == null) {
			return;
		}

		for (int i = 0; i < string.length(); i++) {
			final char c = string.charAt(i);

			switch (c) {
				case '&':
					writer.write("&amp;");
					break;
				case '<':
					writer.write("&lt;");
					break;
				case '>':
					writer.write("&gt;");
					break;

				default:
					if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
						writer.write('?');
					} else {
						writer.write(c);
					}
			}
		}
	}

	private void writeException(final Throwable exception, final Writer writer) {
		exception.printStackTrace(new PrintWriter(writer));
	}

	/**
	 * @param writer
	 * @param officialMessage
	 */
	private void writePlain(final Writer writer, final String string) throws IOException {
		if (string == null) {
			return;
		}

		for (int i = 0; i < string.length(); i++) {
			final char c = string.charAt(i);
			if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
				writer.write('?');
			} else {
				writer.write(c);
			}
		}
	}

	private void writeStatusItemPlain(final IStatus status, final Writer writer, final int identSize) throws IOException {
		// ignore OK status
		if (status.isOK()) {
			return;
		}

		// ident
		String ident = "";
		for (int i = 0; i < identSize; i++) {
			ident += " ";
		}

		// message
		writer.write(ident);
		writer.write(getStatusBullet(status));
		writeEscaped(writer, status.getMessage());
		writer.write(" (");
		writeEscaped(writer, status.getPlugin());
		writer.write(", code ");
		writeEscaped(writer, String.valueOf(status.getCode()));
		writer.write(")");
		final Throwable statusException = status.getException();
		if (null != statusException) {
			writer.write(NEWLINE);
			writer.write(ident);
			writer.write("   caused by: ");
			writer.write(statusException.toString());
		}
		writer.write(NEWLINE);

		if (status.isMultiStatus()) {
			final IStatus[] children = status.getChildren();
			for (final IStatus child : children) {
				writeStatusItemPlain(child, writer, identSize + 3);
			}
		}
	}

	private void writeStatusPlain(final IStatus status, final Writer writer) throws IOException {
		// ignore OK status
		if (status.isOK()) {
			return;
		}

		/*
		 * sometimes we have a multi status with no message but only children;
		 * in this case we just print out all children
		 */
		final String statusMessage = status.getMessage();
		if (status.isMultiStatus() && ((statusMessage == null) || (statusMessage.trim().length() == 0))) {
			// write only children if a multi status has no message
			final IStatus[] children = status.getChildren();
			for (final IStatus child : children) {
				writeStatusItemPlain(child, writer, 0);
			}
		} else {
			writeStatusItemPlain(status, writer, 0);
		}
	}

}
