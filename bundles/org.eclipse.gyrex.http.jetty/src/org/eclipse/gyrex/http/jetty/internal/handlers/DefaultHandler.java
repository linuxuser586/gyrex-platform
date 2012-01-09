/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
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
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.jetty.internal.JettyDebug;
import org.eclipse.gyrex.http.jetty.internal.app.ApplicationHandler;

import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * Sends 404 (Not Found) for every request and shows a list of available
 * applications in development mode.
 */
public class DefaultHandler extends AbstractHandler {

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		// don't do anything if already processed
		if (response.isCommitted() || baseRequest.isHandled()) {
			return;
		}

		// mark handled
		baseRequest.setHandled(true);

		// 404 for all urls
		if (!request.getMethod().equals(HttpMethods.GET)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final ErrorPage errorPage = new ErrorPage() {
			@Override
			protected void writeDebugInfo(final HttpServletRequest request, final Writer writer) throws IOException {
				writer.write("<hr>");
				writer.write("<h1>Welcome to Gyrex!</h1>");
				writer.write("<img src=\"" + DefaultErrorHandlerResourcesHandler.URI_GYREX_LOGO + "\" style=\"float:right;padding-left:1em;\">");
				writer.write("<p>If you see this page you know your server is running. It didn't handle the current request, though. You can start deploying applications or read about the latest buzz at <a href=\"http://planeteclipse.org/\">Planet Eclipse</a>!</p>");
				writer.write("<p>Useful links:");
				writer.write("<ul>");
				writer.write("<li><a href=\"http://wiki.eclipse.org/Gyrex/Administrator_Guide/\">Administrator Guide</a></li>");
				writer.write("<li><a href=\"http://www.eclipse.org/gyrex/documentation/\">Documentation Hub</a></li>");
				writer.write("<li><a href=\"http://www.eclipse.org/forums/eclipse.gyrex/\">User Forum</a></li>");
				writer.write("</ul></p>");
				writer.write("<div style=\"clear:both;\"></div>");
				writer.write("<hr>");
				writer.write(NEWLINE);
				final Server server = getServer();
				final Handler[] handlers = server == null ? null : server.getChildHandlersByClass(ApplicationHandler.class);
				if (handlers.length > 0) {
					writer.write("<p>Applications known to this server are: <ul>");
					writer.write(NEWLINE);
					for (int i = 0; (handlers != null) && (i < handlers.length); i++) {
						writer.write("<li>");
						final ApplicationHandler appHandler = (ApplicationHandler) handlers[i];
						writer.write(appHandler.getApplicationId());
						writer.write(" (");
						writer.write(appHandler.getApplicationRegistration().getProviderId());
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
					writer.write("</ul></p>");
					writer.write(NEWLINE);
				} else {
					writer.write("<p>No applications known to this server!</p>");
					writer.write(NEWLINE);
				}
				if (JettyDebug.handlers) {
					writer.write("<pre>");
					writer.write(NEWLINE);
					server.dump(writer);
					writer.write("</pre>");
					writer.write(NEWLINE);
				}
			}
		};

		errorPage.setCode(HttpStatus.NOT_FOUND_404);
		errorPage.setOfficialMessage(HttpStatus.getMessage(HttpStatus.NOT_FOUND_404));
		errorPage.setInternalMessage("No applications on this server matched or handled this request.");

		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		errorPage.render(request, response);
	}
}
