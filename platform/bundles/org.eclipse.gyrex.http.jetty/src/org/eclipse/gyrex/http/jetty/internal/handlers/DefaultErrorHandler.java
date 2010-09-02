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
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.gyrex.configuration.PlatformConfiguration;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.log.Log;

/**
 * Some default processing for errors.
 */
public class DefaultErrorHandler extends ErrorHandler {

	/** the generator string */
	private static final String GENERATOR = "Gyrex Error Handler";

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	private static Throwable getException(final HttpServletRequest request) {
		return (Throwable) request.getAttribute("javax.servlet.error.exception");
	}

	private static String getOverallStatusImage(final IStatus status) {
		switch (status.getSeverity()) {
			case IStatus.CANCEL:
			case IStatus.ERROR:
				return DefaultErrorHandlerResourcesHandler.URI_ERROR_IMAGE;
			case IStatus.WARNING:
				return DefaultErrorHandlerResourcesHandler.URI_WARNING_IMAGE;

			case IStatus.INFO:
			default:
				return DefaultErrorHandlerResourcesHandler.URI_INFORMATION_IMAGE;
		}
	}

	private static String getOverallStatusMessage(final IStatus status) {
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

	private static String getServerName(final HttpServletRequest request) {
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

	private static String getStatusImage(final IStatus status) {
		switch (status.getSeverity()) {
			case IStatus.CANCEL:
			case IStatus.ERROR:
				return DefaultErrorHandlerResourcesHandler.URI_ERROR_IMAGE;
			case IStatus.WARNING:
				return DefaultErrorHandlerResourcesHandler.URI_WARNING_IMAGE;

			case IStatus.INFO:
			default:
				return DefaultErrorHandlerResourcesHandler.URI_INFORMATION_IMAGE;
		}
	}

	private final boolean developmentMode = PlatformConfiguration.isOperatingInDevelopmentMode();

	/**
	 * Creates a new instance.
	 */
	public DefaultErrorHandler() {
		setShowStacks(PlatformConfiguration.isOperatingInDevelopmentMode());
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

	@Override
	protected void writeErrorPage(final HttpServletRequest request, final Writer writer, final int code, String internalMessage, final boolean showStacks) throws IOException {
		// decode message
		if (internalMessage != null) {
			internalMessage = URLDecoder.decode(internalMessage, "UTF-8");
		}

		// we do not want to hand out internal details in production mode
		final String officialMessage = HttpStatus.getMessage(code);

		final String serverName = getServerName(request);

		writer.write("<html>\n\r<head>\n\r<title>Error ");
		writer.write(Integer.toString(code));
		writer.write(" - ");
		write(writer, officialMessage);
		writer.write("</title>\n\r");
		writer.write("<meta name=\"generator\" content=\"" + GENERATOR + "\">");
		writer.write("<link rel=\"stylesheet\" href=\"" + DefaultErrorHandlerResourcesHandler.URI_ERROR_CSS + "\" type=\"text/css\">\n\r");
		writer.write("</head>\n\r<body>\n\r");
//		writer.write("<div id=\"box\">\n\r");
		writer.write("<h2>Error ");
		writer.write(Integer.toString(code));
		writer.write(" - ");
		write(writer, officialMessage);
		writer.write("</h2>\n\r\n\r");
		switch (code) {
			case 404:
				writer.write(ErrorPageMessages.get404Message());
				break;

			default:
				if (developmentMode && (null != internalMessage)) {
					writer.write("<p>\n\r\n\r");
					writeFormattedMessage(writer, internalMessage);
					writer.write("</p>\n\r");
				}
				break;
		}
		writer.write("\n\r\n\r");

		// display configuration information in development mode.
		if (showStacks) {
			final Throwable exception = getException(request);
			if (null != exception) {
				writer.write("<div class=\"dev_note\">\n\r");
				writer.write("<div><img src=\"" + DefaultErrorHandlerResourcesHandler.URI_ERROR_IMAGE + "\" style=\"float:left;padding-right:1em;\">The server throw an exception while processing the request. <span id=\"showstack\"><small><a href=\"#stack-trace\" onclick=\"javascript:document.getElementById('stack').style.display='block';document.getElementById('showstack').style.display='none';return false;\">Show stack.</a></small></span></div>\n\r");
				writer.write("<div id=\"stack\" style=\"display:none;\">\n\r");
				writer.write("<div style=\"clear:both;\"></div>\n\r");
				//writer.write("<p><code>");
				//writer.write(escapeHtml(exception.toString()));
				//writer.write("</code>:\n\r");
				writer.write("<a name=\"stack-trace\" />\n\r");
				writer.write("<pre>");
				writeException(exception, writer);
				writer.write("</pre>\n\r");
				writer.write("</div>\n\r");
				writer.write("</div>\n\r\n\r\n\r");
			}

			final IStatus platformStatus = PlatformConfiguration.getPlatformStatus();
			if (!platformStatus.isOK()) {
				writer.write("<div class=\"dev_note\">\n\r");
				writer.write("<div><img src=\"" + getOverallStatusImage(platformStatus) + "\" style=\"float:left;padding-right:1em;\">" + getOverallStatusMessage(platformStatus) + "<br><em>You might want to check the <a href=\"" + getAdminServerURL(request) + "\">server configuration</a>.</em></div>\n\r");
				writer.write("<div style=\"clear:both;\"></div>\n\r");
				writer.write("<p>Issues detected on <code>");
				writer.write(serverName);
				writer.write("</code>:\n\r");
				writeStatus(platformStatus, writer);
				writer.write("</p>\n\r");
				writer.write("</div>\n\r\n\r\n\r");
			} else {
				writer.write("<div class=\"dev_note\">\n\r");
				writer.write("<div><img src=\"" + DefaultErrorHandlerResourcesHandler.URI_INFORMATION_IMAGE + "\" style=\"float:left;padding-right:1em;\">A note to developers, this server seems to be configured properly.<br><em>At least, no issues were detected.</em></div>\n\r");
				writer.write("<div style=\"clear:both;\"></div>\n\r");
				writer.write("</div>\n\r\n\r\n\r");
			}
		} else {
			writer.write("<p class=\"list-desc\">If you think you\'ve reached this page in error:</p>\n\r" + "<ul>\n\r" + "<li>Make sure the URL you\'re trying to reach is correct.</li>\n\r" + "<li>Check <a href=\"http://" + serverName + "/status/\">http://" + serverName + "/status/</a> to view our current system status.</li>\n\r" + "</ul>\n\r" + "\n\r" + "<p class=\"list-desc\">Otherwise, you can: </p>\n\r" + "<ul>\n\r" + "<li>Go <a href=\"javascript:history.back()\">back to the previous page</a></li>\n\r" + "<li>Go to the <a href=\"http://" + serverName + "/\">Gyrex Homepage</a>.</li>\n\r" + "</ul>\n\r\n\r");
		}
		writer.write("<p align=\"right\"><em>Powered by Gyrex, Jetty and Equinox.</em></p>");
		for (int i = 0; i < 20; i++) {
			writer.write("\n\r                                                ");
		}
//		writer.write("</div>\n\r");
		writer.write("</body>\n\r</html>\n\r");
	}

	private void writeException(final Throwable exception, final Writer writer) {
		exception.printStackTrace(new PrintWriter(writer));
	}

	private void writeFormattedMessage(final Writer writer, final String internalMessage) throws IOException {
		boolean inQuote = false;
		for (int i = 0; i < internalMessage.length(); i++) {
			final char c = internalMessage.charAt(i);
			if (c == '\'') {
				if (!inQuote) {
					writer.write("'<code>");
					inQuote = true;
				} else {
					inQuote = false;
					writer.write("</code>'");
				}
			} else {
				write(writer, c + "");
			}
		}
		if (inQuote) {
			writer.write("</code>'");
		}
	}

	private void writeStackTrace(final Writer writer, final Throwable t) throws IOException {
		if (null != t) {
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.flush();
			write(writer, sw.getBuffer().toString());
		}
	}

	private void writeStatus(final IStatus status, final Writer writer) throws IOException, UnsupportedEncodingException {
		// ignore OK status
		if (status.isOK()) {
			return;
		}

		// start list
		writer.write("<ul class=\"status\">\n\r");

		/*
		 * sometimes we have a multi status with no message but only children;
		 * in this case we just print out all children
		 */
		final String statusMessage = status.getMessage();
		if (status.isMultiStatus() && ((statusMessage == null) || (statusMessage.trim().length() == 0))) {
			// write only children if a multi status has no message
			final IStatus[] children = status.getChildren();
			for (final IStatus child : children) {
				writeStatusItem(child, writer, 0);
			}
		} else {
			writeStatusItem(status, writer, 0);
		}

		// end list
		writer.write("</ul>\n\r");
	}

	private void writeStatusItem(final IStatus status, final Writer writer, final int identSize) throws IOException {
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
		writer.write("<li class=\"statusitem\">");
		writer.write("<img class=\"statusimage\" src=\"" + getStatusImage(status) + "\">&nbsp;&nbsp;");
		writer.write("<span class=\"statusmessage\">");
		write(writer, status.getMessage());
		writer.write("<br><small><code>(");
		write(writer, status.getPlugin());
		writer.write(", code ");
		write(writer, String.valueOf(status.getCode()));
		writer.write(")</code></small>");
		final Throwable statusException = status.getException();
		if (null != statusException) {
			writer.write("<br>\n\r");
			writer.write(ident);
			writer.write("<pre>");
			writeStackTrace(writer, statusException);
			writer.write(ident);
			writer.write("</pre></small>\n\r");
		}
		writer.write("</span>");

		if (status.isMultiStatus()) {
			writer.write("<br>\n\r");
			writer.write(ident);
			writer.write("<ul>\n\r");
			final IStatus[] children = status.getChildren();
			for (final IStatus child : children) {
				writeStatusItem(child, writer, identSize + 4);
			}
			writer.write(ident);
			writer.write("</ul>\n\r");
		}

		writer.write(ident);
		writer.write("</li>\n\r");
	}

}
