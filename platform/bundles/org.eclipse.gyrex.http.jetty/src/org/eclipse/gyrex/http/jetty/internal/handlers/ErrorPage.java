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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.log.Log;

/**
 * A Gyrex error page.
 */
public class ErrorPage {

	public static final String NEWLINE = "\n";

	/** the generator string */
	private static final String GENERATOR = "Gyrex Error Handler";

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	public static final void writeEscaped(final Writer writer, final String string) throws IOException {
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

	private final boolean showDebugInfo = Platform.inDebugMode() || Platform.inDevelopmentMode();

	private int code;

	private String officialMessage;

	private String internalMessage;

	/**
	 * Returns the admin server URL
	 * 
	 * @return the admin server URL
	 */
	private String getAdminServerURL(final HttpServletRequest request) {
		// TODO: admin server scheme should be HTTPS (not implemented yet)
		// TODO: lookup the admin server port from the preferences
		return "http://".concat(request.getServerName()).concat(":3110/");
	}

	private Throwable getException(final HttpServletRequest request) {
		return (Throwable) request.getAttribute("javax.servlet.error.exception");
	}

	private String getOverallStatusImage(final IStatus status) {
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

	private String getStatusImage(final IStatus status) {
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

	public void render(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		response.setContentType(MimeTypes.TEXT_HTML_8859_1);

		final ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(8192);
		writeErrorPage(request, writer);

		response.setContentLength(writer.size());

		final OutputStream out = response.getOutputStream();
		writer.writeTo(out);
		out.close();
	}

	public void setCode(final int code) {
		this.code = code;
	}

	public void setInternalMessage(final String internalMessage) {
		this.internalMessage = internalMessage;
	}

	public void setOfficialMessage(final String officialMessage) {
		this.officialMessage = officialMessage;
	}

	protected void writeDebugInfo(final HttpServletRequest request, final Writer writer) throws IOException {
		final Throwable exception = getException(request);
		if (null != exception) {
			writer.write("<div class=\"dev_note\">");
			writer.write("<div><img src=\"" + DefaultErrorHandlerResourcesHandler.URI_ERROR_IMAGE + "\" style=\"float:left;padding-right:1em;\">The server throw an exception while processing the request. <span id=\"showstack\"><small><a href=\"#stack-trace\" onclick=\"javascript:document.getElementById('stack').style.display='block';document.getElementById('showstack').style.display='none';return false;\">Show stack.</a></small></span></div>");
			writer.write("<div id=\"stack\" style=\"display:none;\">");
			writer.write("<div style=\"clear:both;\"></div>");
			writer.write("<a name=\"stack-trace\" />");
			writer.write("<pre>");
			writeException(exception, writer);
			writer.write("</pre>");
			writer.write("</div>");
			writer.write("</div>");
		}
	}

	protected void writeErrorPage(final HttpServletRequest request, final Writer writer) throws IOException, UnsupportedEncodingException {
		writer.write("<html>");
		writer.write(NEWLINE);
		writer.write("<head>");
		writer.write(NEWLINE);
		writer.write("<title>Error ");
		writer.write(Integer.toString(code));
		writer.write(" - ");
		writeEscaped(writer, officialMessage);
		writer.write("</title>");
		writer.write(NEWLINE);
		writer.write("<meta name=\"generator\" content=\"" + GENERATOR + "\">");
		writer.write(NEWLINE);
		writer.write("<link rel=\"stylesheet\" href=\"" + DefaultErrorHandlerResourcesHandler.URI_ERROR_CSS + "\" type=\"text/css\">");
		writer.write(NEWLINE);
		writer.write("</head>");
		writer.write(NEWLINE);
		writer.write("<body>");
		writer.write(NEWLINE);
		writer.write("<h2>Error ");
		writer.write(Integer.toString(code));
		writer.write(" - ");
		writeEscaped(writer, officialMessage);
		writer.write("</h2>");
		writer.write(NEWLINE);

		if (showDebugInfo) {
			if (internalMessage != null) {
				writer.write("<p>");
				writeFormattedMessage(writer, internalMessage);
				writer.write("</p>");
				writer.write(NEWLINE);
			}
			writeDebugInfo(request, writer);
			writePlatformStatus(request, writer);
		} else {
			writer.write("<p class=\"list-desc\">If you think you\'ve reached this page in error:</p><ul><li>Make sure the URL you\'re trying to reach is correct.</li></ul><p class=\"list-desc\">Otherwise, you can:</p><ul><li>Go <a href=\"javascript:history.back()\">back to the previous page</a></li></ul>");
		}

		writer.write("<p align=\"right\"><em>Brought to you by Gyrex. Powered by Jetty and Equinox.</em></p>");

		// IE issue workaround
		for (int i = 0; i < 20; i++) {
			writer.write(NEWLINE);
			writer.write("                                                ");
		}

		writer.write("</body>");
		writer.write(NEWLINE);
		writer.write("</html>");
		writer.write(NEWLINE);
	}

	protected void writeException(final Throwable exception, final Writer writer) {
		exception.printStackTrace(new PrintWriter(writer));
	}

	protected void writeFormattedMessage(final Writer writer, final String internalMessage) throws IOException {
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
				writeEscaped(writer, c + "");
			}
		}
		if (inQuote) {
			writer.write("</code>'");
		}
	}

	protected void writePlatformStatus(final HttpServletRequest request, final Writer writer) throws IOException, UnsupportedEncodingException {
		final IStatus platformStatus = PlatformConfiguration.getPlatformStatus();
		if (!platformStatus.isOK()) {
			writer.write("<div class=\"dev_note\">");
			writer.write("<div><img src=\"" + getOverallStatusImage(platformStatus) + "\" style=\"float:left;padding-right:1em;\">" + getOverallStatusMessage(platformStatus) + "<br><em>You might want to check the <a href=\"" + getAdminServerURL(request) + "\">server configuration</a>.</em></div>");
			writer.write("<div style=\"clear:both;\"></div>");
			writer.write("<p>Issues detected on <code>");
			writer.write(getServerName(request));
			writer.write("</code>:");
			writeStatus(platformStatus, writer);
			writer.write("</p>");
			writer.write("</div>");
		} else {
			writer.write("<div class=\"dev_note\">");
			writer.write("<div><img src=\"" + DefaultErrorHandlerResourcesHandler.URI_INFORMATION_IMAGE + "\" style=\"float:left;padding-right:1em;\">A note to developers, this server seems to be configured properly.<br><em>At least, no issues were detected.</em></div>");
			writer.write("<div style=\"clear:both;\"></div>");
			writer.write("</div>");
		}
	}

	protected void writeStackTrace(final Writer writer, final Throwable t) throws IOException {
		if (null != t) {
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.flush();
			writeEscaped(writer, sw.getBuffer().toString());
		}
	}

	private void writeStatus(final IStatus status, final Writer writer) throws IOException, UnsupportedEncodingException {
		// ignore OK status
		if (status.isOK()) {
			return;
		}

		// start list
		writer.write("<ul class=\"status\">");

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
		writer.write("</ul>");
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
		writeEscaped(writer, status.getMessage());
		writer.write("<br><small><code>(");
		writeEscaped(writer, status.getPlugin());
		writer.write(", code ");
		writeEscaped(writer, String.valueOf(status.getCode()));
		writer.write(")</code></small>");
		final Throwable statusException = status.getException();
		if (null != statusException) {
			writer.write("<br>");
			writer.write(ident);
			writer.write("<pre>");
			writeStackTrace(writer, statusException);
			writer.write(ident);
			writer.write("</pre></small>");
		}
		writer.write("</span>");

		if (status.isMultiStatus()) {
			writer.write("<br>");
			writer.write(ident);
			writer.write("<ul>");
			final IStatus[] children = status.getChildren();
			for (final IStatus child : children) {
				writeStatusItem(child, writer, identSize + 4);
			}
			writer.write(ident);
			writer.write("</ul>");
		}

		writer.write(ident);
		writer.write("</li>");
	}

}
