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
package org.eclipse.gyrex.persistence.storage.content;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

import org.eclipse.core.runtime.PlatformObject;

import org.osgi.framework.Version;

/**
 * The type of content persisted in a specific repository.
 * <p>
 * In order to classify the data in a repository the concept of content types is
 * used. The meaning of a content type is the same as defined by <a
 * href="http://en.wikipedia.org/wiki/Internet_media_type">Internet media
 * type</a>.
 * </p>
 * <p>
 * A {@link #getMediaType() media type} is used to uniquely identify content. A
 * media type is composed of at least two parts: a <em>type</em>, a
 * <em>subtype</em>, and one or more optional parameters. All rules and
 * limitations mentioned in <a
 * href="http://en.wikipedia.org/wiki/Internet_media_type">the Wikipedia
 * article</a> and especially in the referenced RFCs apply equally to repository
 * content types.
 * </p>
 * <p>
 * In addition to the {@link #getMediaType() media type} a content type has the
 * following required parameters.
 * <ul>
 * <li>A {@link #getRepositoryTypeName() repository type name} parameter which
 * is used to identify the required repository type.</li>
 * <li>A {@link #getVersion() version} parameter which is used to support
 * evolution of content in repositories.</li>
 * </ul>
 * </p>
 * <p>
 * A typical media type may be registered in the <a
 * href="http://www.iana.org/assignments/media-types/">IANA Media Types</a>
 * directory as a sub-type of <code>application</code> using binary. But it's
 * not limited to this media type.
 * </p>
 * <p>
 * Every content type must be made available to the system using
 * {@link IRepositoryContentTypeProvider}.
 * </p>
 * 
 * @see #getMediaType()
 * @see #getVersion()
 * @see #getRepositoryTypeName()
 * @see http://en.wikipedia.org/wiki/Internet_media_type
 */
public final class RepositoryContentType extends PlatformObject {

	private static final String checkMediaTypeSubType(final String mediaTypeSubType) throws IllegalArgumentException {
		checkToken(mediaTypeSubType, "media type subtype");
		return mediaTypeSubType;
	}

	private static final String checkMediaTypeType(final String mediaTypeType) throws IllegalArgumentException {
		checkToken(mediaTypeType, "media type type");
		return mediaTypeType;
	}

	private static Map<String, String> checkParameters(final Map<String, String> parameters) {
		for (final String key : parameters.keySet()) {
			checkToken(key, String.format("parameter key '%s'", key));
			final String value = parameters.get(key);
			// note, although not required we limit it to a token
			checkToken(value, String.format("parameter value '%s'", value));
		}
		return parameters;
	}

	private static final String checkRepositoryTypeId(final String repositoryTypeId) {
		if (null == repositoryTypeId) {
			throw new IllegalArgumentException("repository type name must not be null");
		}
		if (!IdHelper.isValidId(repositoryTypeId)) {
			throw new IllegalArgumentException(MessageFormat.format("repository type name \"{0}\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", repositoryTypeId));
		}
		return repositoryTypeId;
	}

	private static void checkToken(final String token, final String description) {
		if (null == token) {
			throw new IllegalArgumentException(String.format("invalid %s; must not be null, see RFC 2045 section 5.1", description));
		}
		final char[] cs = token.toCharArray();
		for (final char c : cs) {
			if (!isUsAsciiChar(c)) {
				throw new IllegalArgumentException(String.format("invalid %s; only US-ASCII chars allowed, see RFC 2045 section 5.1", description));
			} else if (isControlChar(c)) {
				throw new IllegalArgumentException(String.format("invalid %s; control characters not allowed, see RFC 2045 section 5.1", description));
			} else if (isWhitespace(c)) {
				throw new IllegalArgumentException(String.format("invalid %s; whitespace not allowed, see RFC 2045 section 5.1", description));
			} else if (isSpecialChar(c)) {
				throw new IllegalArgumentException(String.format("invalid %s; character '%c' not allowed, see RFC 2045 section 5.1", description, c));
			}
		}
	}

	/**
	 * US-ASCII characters
	 * 
	 * @param c
	 *            the char
	 * @return <code>true</code> if US-ASCII character, <code>false</code>
	 *         otherwise
	 */
	private static final boolean isControlChar(final char c) {
		return (c < 32) || (c == 127);
	}

	/**
	 * Special chars according to RFC 2045 section 5.1
	 * 
	 * @param c
	 *            the char
	 * @return <code>true</code> if special, <code>false</code> otherwise
	 */
	private static final boolean isSpecialChar(final char c) {
		switch (c) {
			case 40: // "("
			case 41: // ")"
			case 60: // "<"
			case 62: // ">"
			case 64: // "@"
			case 44: // ","
			case 59: // ";"
			case 58: // ":"
			case 92: // "\"
			case 34: // <">
			case 47: // "/"
			case 91: // "["
			case 93: // "]"
			case 63: // "?"
			case 61: // "="
				return true;

			default:
				return false;
		}

	}

	/**
	 * US-ASCII control characters
	 * 
	 * @param c
	 *            the char
	 * @return <code>true</code> if control character, <code>false</code>
	 *         otherwise
	 */
	private static final boolean isUsAsciiChar(final char c) {
		return (c >= 0) && (c <= 127);
	}

	/**
	 * US-ASCII white space.
	 * 
	 * @param c
	 *            the char
	 * @return <code>true</code> if whitespace, <code>false</code> otherwise
	 */
	private static final boolean isWhitespace(final char c) {
		return c == 32;
	}

	private final String mediaTypeType;
	private final String mediaTypeSubType;
	private final String repositoryTypeName;

	private final Version version;
	private int cachedHashCode;

	private final Map<String, String> parameters;

	/**
	 * Creates a new content type.
	 * 
	 * @param mediaTypeType
	 *            the {@link #getMediaType() media type}
	 *            <em>type</type> (according to RFC 2045)
	 * @param mediaTypeSubType
	 *            the {@link #getMediaType() media type} <em>type</type>
	 *            (according to RFC 2045)
	 * @param repositoryTypeName
	 *            the {@link #getRepositoryTypeName() repository type name}
	 * @param version
	 *            the {@link #getVersion() version}
	 * @throws IllegalArgumentException
	 *             if any of the specified parameter is invalid
	 */
	public RepositoryContentType(final String mediaTypeType, final String mediaTypeSubType, final String repositoryTypeName, final String version) throws IllegalArgumentException {
		this(mediaTypeType, mediaTypeSubType, repositoryTypeName, version, null);
	}

	/**
	 * Creates a new content type.
	 * 
	 * @param mediaTypeType
	 *            the {@link #getMediaType() media type}
	 *            <em>type</type> (according to RFC 2045)
	 * @param mediaTypeSubType
	 *            the {@link #getMediaType() media type} <em>type</type>
	 *            (according to RFC 2045)
	 * @param repositoryTypeName
	 *            the {@link #getRepositoryTypeName() repository type name}
	 * @param version
	 *            the {@link #getVersion() version}
	 * @param parameters
	 *            additional content type parameters (maybe <code>null</code>,
	 *            iteration order will be maintained)
	 * @throws IllegalArgumentException
	 *             if any of the specified parameter is invalid
	 */
	public RepositoryContentType(final String mediaTypeType, final String mediaTypeSubType, final String repositoryTypeName, final String version, final Map<String, String> parameters) throws IllegalArgumentException {
		this.mediaTypeType = checkMediaTypeType(mediaTypeType);
		this.mediaTypeSubType = checkMediaTypeSubType(mediaTypeSubType);
		this.repositoryTypeName = checkRepositoryTypeId(repositoryTypeName);
		this.version = Version.parseVersion(version);
		this.parameters = null != parameters ? new LinkedHashMap<String, String>(checkParameters(parameters)) : null;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final RepositoryContentType other = (RepositoryContentType) obj;
		// note, media type and subtype matching is ALWAYS case-insensitive (RFC 2045 section 5.1)
		if (mediaTypeType == null) {
			if (other.mediaTypeType != null) {
				return false;
			}
		} else if ((null == other.mediaTypeType) || !mediaTypeType.toLowerCase(Locale.US).equals(other.mediaTypeType.toLowerCase(Locale.US))) {
			return false;
		}
		if (mediaTypeSubType == null) {
			if (other.mediaTypeSubType != null) {
				return false;
			}
		} else if ((null == other.mediaTypeSubType) || !mediaTypeSubType.toLowerCase(Locale.US).equals(other.mediaTypeSubType.toLowerCase(Locale.US))) {
			return false;
		}
		if (repositoryTypeName == null) {
			if (other.repositoryTypeName != null) {
				return false;
			}
		} else if (!repositoryTypeName.equals(other.repositoryTypeName)) {
			return false;
		}
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
			return false;
		}
		if (parameters == null) {
			if (other.parameters != null) {
				return false;
			}
		} else if ((null == other.parameters) || !parameters.equals(other.parameters)) {
			// our implementation is LinkedHashMap so #equals works
			return false;
		}
		return true;
	}

	/**
	 * Returns the <a
	 * href="http://en.wikipedia.org/wiki/Internet_media_type">Internet media
	 * type</a> identifier without any parameters.
	 * 
	 * @return the Internet media type identifier without parameters
	 * @see http://en.wikipedia.org/wiki/Internet_media_type
	 */
	public final String getMediaType() {
		return String.format("%s/%s", mediaTypeType, mediaTypeSubType);
	}

	/**
	 * Returns just the subtype part of the <a
	 * href="http://en.wikipedia.org/wiki/Internet_media_type">Internet media
	 * type</a>.
	 * 
	 * @return the subtype of the media type
	 * @see http://en.wikipedia.org/wiki/Internet_media_type
	 */
	public String getMediaTypeSubType() {
		return mediaTypeSubType;
	}

	/**
	 * Returns just the type part of the <a
	 * href="http://en.wikipedia.org/wiki/Internet_media_type">Internet media
	 * type</a>.
	 * 
	 * @return the type of the media type
	 * @see http://en.wikipedia.org/wiki/Internet_media_type
	 */
	public String getMediaTypeType() {
		return mediaTypeType;
	}

	/**
	 * Returns an addition parameter value.
	 * <p>
	 * Returns <code>null</code> if the content type does not have any
	 * additional parameter or if the specified parameter is not defined.
	 * </p>
	 * 
	 * @return the parameter value (maybe <code>null</code> if not set)
	 */
	public final String getParameter(final String name) {
		return null != parameters ? parameters.get(name) : null;
	}

	/**
	 * Returns the name of the repository type.
	 * <p>
	 * The repository type name further classifies the repository type of the
	 * content. For example, the same content can be stored in different
	 * repository types using different technologies. Having this parameter
	 * ensures that only compatible repositories are used when working with this
	 * content type.
	 * </p>
	 * <p>
	 * The name must be a valid repository API identifier.
	 * </p>
	 * <p>
	 * Note, this is a required parameter.
	 * </p>
	 * 
	 * @return the repository type name
	 * @see RepositoryProvider#getRepositoryTypeName()
	 * @see IdHelper#isValidId(String)
	 */
	public final String getRepositoryTypeName() {
		return repositoryTypeName;
	}

	/**
	 * Returns the version of a content type.
	 * <p>
	 * Note, this is a required parameter. The version follows the OSGi version
	 * numbers scheme which are composed of four segments - three integers and a
	 * string respectively named: <code>'major.minor.service.qualifier'</code>.
	 * But it does not necessarily reflect a bundle or package version. Content
	 * types may be versioned independently.
	 * </p>
	 * <p>
	 * The meaning of each segment is analogue to the OSGi meaning and can be
	 * summarized as:
	 * <ul>
	 * <li>the <strong>major</strong> segment indicates breakage in the content
	 * type (eg. an incompatible change in data structure)</li>
	 * <li>the <strong>minor</strong> segment indicates "externally visible"
	 * changes like adding new optional data structures (i.e., it is expected
	 * that clients developed with version x.0.0 are still functionally when
	 * running with version x.y.z where y &gt; 0).</li>
	 * <li>the <strong>service</strong> segment indicates bug fixes and the
	 * change of development stream</li>
	 * <li>the <strong>qualifier</strong> segment indicates a particular build</li>
	 * </ul>
	 * </p>
	 * <p>
	 * It may be a good practise to associate the version number of a content
	 * type with the version number of the bundle defining the content type.
	 * </p>
	 * 
	 * @return the content type version
	 * @see http://wiki.eclipse.org/Version_Numbering
	 * @see org.osgi.framework.Version
	 */
	public final String getVersion() {
		return version.toString();
	}

	@Override
	public final int hashCode() {
		// content type is immutable so use a cached hash code
		if (cachedHashCode != 0) {
			return cachedHashCode;
		}

		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((mediaTypeType == null) ? 0 : mediaTypeType.toLowerCase(Locale.US).hashCode());
		result = (prime * result) + ((mediaTypeSubType == null) ? 0 : mediaTypeSubType.toLowerCase(Locale.US).hashCode());
		result = (prime * result) + ((repositoryTypeName == null) ? 0 : repositoryTypeName.hashCode());
		result = (prime * result) + ((version == null) ? 0 : version.hashCode());
		return cachedHashCode = result;
	}

	/**
	 * Returns a string representation of the content type.
	 * <p>
	 * The string is similar to the syntax defined in <a
	 * href="http://tools.ietf.org/html/rfc2045#section-5.1"
	 * target="_blank">section 5.1 of RFC 2045</a>. The leading
	 * <code>"Content-Type" ":"</code> part will not be included.
	 * </p>
	 * <p>
	 * 
	 * @return the string representation of the content type
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		final StringBuilder contentType = new StringBuilder();
		contentType.append(getMediaType());
		contentType.append("; repositoryType=\"");
		contentType.append(getRepositoryTypeName());
		contentType.append("\"; version=\"");
		contentType.append(getVersion());
		contentType.append('"');
		if (null != parameters) {
			for (final Entry<String, String> parameter : parameters.entrySet()) {
				contentType.append("; ").append(parameter.getKey()).append("=\"").append(parameter.getValue()).append('"');
			}
		}
		return contentType.toString();
	}
}
