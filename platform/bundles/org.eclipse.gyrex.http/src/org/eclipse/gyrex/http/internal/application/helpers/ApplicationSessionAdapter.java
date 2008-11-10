/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.http.internal.application.helpers;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * Adapts a {@link HttpSession} so that the correct application specific
 * {@link ServletContext} is returned.
 */
@SuppressWarnings("deprecation")
public class ApplicationSessionAdapter implements HttpSession {

	private final HttpSession session;
	private final ServletContext servletContext;

	public ApplicationSessionAdapter(final HttpSession session, final ServletContext servletContext) {
		this.session = session;
		this.servletContext = servletContext;
	}

	@Override
	public Object getAttribute(final String name) {
		return session.getAttribute(name);
	}

	@Override
	public Enumeration getAttributeNames() {
		return session.getAttributeNames();
	}

	@Override
	public long getCreationTime() {
		return session.getCreationTime();
	}

	@Override
	public String getId() {
		return session.getId();
	}

	@Override
	public long getLastAccessedTime() {
		return session.getLastAccessedTime();
	}

	@Override
	public int getMaxInactiveInterval() {
		return session.getMaxInactiveInterval();
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	@Deprecated
	public HttpSessionContext getSessionContext() {
		return session.getSessionContext();
	}

	@Override
	@Deprecated
	public Object getValue(final String name) {
		return session.getValue(name);
	}

	@Override
	@Deprecated
	public String[] getValueNames() {
		return session.getValueNames();
	}

	@Override
	public void invalidate() {
		session.invalidate();
	}

	@Override
	public boolean isNew() {
		return session.isNew();
	}

	@Override
	@Deprecated
	public void putValue(final String name, final Object value) {
		session.putValue(name, value);
	}

	@Override
	public void removeAttribute(final String name) {
		session.removeAttribute(name);
	}

	@Override
	@Deprecated
	public void removeValue(final String name) {
		session.removeValue(name);
	}

	@Override
	public void setAttribute(final String name, final Object value) {
		session.setAttribute(name, value);
	}

	@Override
	public void setMaxInactiveInterval(final int interval) {
		session.setMaxInactiveInterval(interval);
	}
}
