/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
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

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
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
 * 
 * @see #getMediaType()
 * @see #getVersion()
 * @see #getRepositoryTypeName()
 * @see http://en.wikipedia.org/wiki/Internet_media_type
 */
public final class RepositoryContentType extends PlatformObject {

	private static final String checkMediaTypeSubType(final String mediaTypeSubType) throws IllegalArgumentException {
		final char[] cs = mediaTypeSubType.toCharArray();
		for (final char c : cs) {
			if (isControlChar(c)) {
				throw new IllegalArgumentException("invalid media type subtype; control characters not allowed, see RFC 2045 section 5.1");
			} else if (isWhitespace(c)) {
				throw new IllegalArgumentException("invalid media type subtype; whitespace not allowed, see RFC 2045 section 5.1");
			} else if (isSpecialChar(c)) {
				throw new IllegalArgumentException("invalid media type subtype; character '" + c + "' not allowed, see RFC 2045 section 5.1");
			}
		}
		return mediaTypeSubType;
	}

	private static final String checkMediaTypeType(final String mediaTypeType) throws IllegalArgumentException {
		final char[] cs = mediaTypeType.toCharArray();
		for (final char c : cs) {
			if (isControlChar(c)) {
				throw new IllegalArgumentException("invalid media type type; control characters not allowed, see RFC 2045 section 5.1");
			} else if (isWhitespace(c)) {
				throw new IllegalArgumentException("invalid media type type; whitespace not allowed, see RFC 2045 section 5.1");
			} else if (isSpecialChar(c)) {
				throw new IllegalArgumentException("invalid media type type; character '" + c + "' not allowed, see RFC 2045 section 5.1");
			}
		}
		return mediaTypeType;
	}

	private static final String checkRepositoryTypeId(final String repositoryTypeId) {
		if (!Repository.isValidId(repositoryTypeId)) {
			throw new IllegalArgumentException(MessageFormat.format("repository type name \"{0}\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", repositoryTypeId));
		}
		return repositoryTypeId;
	}

	/**
	 * US-ASCII control characters
	 * 
	 * @param c
	 *            the char
	 * @return <code>true</code> if control character, <code>false</code>
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
	 * US-ASCII white space.
	 * 
	 * @param c
	 *            the char
	 * @return <code>true</code> if whitespace, <code>false</code> otherwise
	 */
	private static final boolean isWhitespace(final char c) {
		return c == 32;
	}

	private final String mediaType;
	private final String repositoryTypeName;
	private final Version version;

	private int cachedHashCode;

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
		if (null == repositoryTypeName) {
			throw new IllegalArgumentException("repository type name must not be null");
		}
		if (null == mediaTypeType) {
			throw new IllegalArgumentException("media type type must not be null");
		}
		if (null == mediaTypeSubType) {
			throw new IllegalArgumentException("media type sub type must not be null");
		}

		mediaType = checkMediaTypeType(mediaTypeType) + "/" + checkMediaTypeSubType(mediaTypeSubType);
		this.repositoryTypeName = checkRepositoryTypeId(repositoryTypeName);
		this.version = Version.parseVersion(version);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		if (mediaType == null) {
			if (other.mediaType != null) {
				return false;
			}
		} else if (!mediaType.equals(other.mediaType)) {
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
		return mediaType;
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
	 * @see Repository#isValidId(String)
	 */
	public final String getRepositoryTypeName() {
		return repositoryTypeName;
	}

	/**
	 * Returns the version of a content type.
	 * <p>
	 * Note, this is a required parameter. The version follows the OSGi version
	 * numbers scheme which are composed of four segments - three integers and a
	 * string respectively named: <code>'major.minor.service.qualifier'</code>. But it does not necessarily reflect a
	 * bundle or package version. Content types may be versioned independently.
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		// content type is immutable so use a cached hash code
		if (cachedHashCode != 0) {
			return cachedHashCode;
		}

		final int prime = 31;
		int result = 1;
		result = prime * result + ((mediaType == null) ? 0 : mediaType.hashCode());
		result = prime * result + ((repositoryTypeName == null) ? 0 : repositoryTypeName.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		return contentType.toString();
	}
}
