package org.eclipse.gyrex.http.internal.httpservice;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class DefaultHttpContext implements HttpContext {

	private final Bundle bundle;

	public DefaultHttpContext(final Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public String getMimeType(final String name) {
		return null;
	}

	@Override
	public URL getResource(final String name) {
		return bundle.getResource(name);
	}

	@Override
	public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		// default behaviour assumes the container has already performed authentication
		return true;
	}
}
