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
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;
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
					writer.write("No applications known to this server!");
					writer.write(NEWLINE);
				}
				if (HttpJettyDebug.handlers) {
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
