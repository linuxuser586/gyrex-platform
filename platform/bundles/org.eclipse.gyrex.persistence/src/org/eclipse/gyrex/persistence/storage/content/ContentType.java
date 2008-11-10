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
package org.eclipse.cloudfree.persistence.storage.content;

import org.eclipse.core.runtime.PlatformObject;
import org.osgi.framework.Version;

/**
 * The type of content persisted in a repository.
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
 * limitations mentioned in <a href="">the Wikipedia article</a> and especially
 * in the referenced RFCs apply equally to repository content types.
 * </p>
 * <p>
 * In addition to the {@link #getMediaType() media type} a content type has a
 * required {@link #getVersion() version} parameter which is used to support
 * evolution of content in repositories.
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
 * @see http://en.wikipedia.org/wiki/Internet_media_type
 */
public final class ContentType extends PlatformObject {

	private final String mediaType;
	private final Version version;

	/**
	 * Creates a new content type.
	 * 
	 * @param mediaType
	 *            the {@link #getMediaType() media type}
	 * @param version
	 *            the {@link #getVersion() version}
	 */
	public ContentType(final String mediaType, final String version) {
		if (null == mediaType) {
			throw new IllegalArgumentException("media type must not be null");
		}
		this.mediaType = mediaType;
		this.version = Version.parseVersion(version);
	}

	/**
	 * Returns the <a
	 * href="http://en.wikipedia.org/wiki/Internet_media_type">Internet media
	 * type</a> identifier.
	 * 
	 * @return the Internet media type identifier
	 * @see http://en.wikipedia.org/wiki/Internet_media_type
	 */
	public String getMediaType() {
		return mediaType;
	}

	/**
	 * Returns the version of a content type.
	 * <p>
	 * Note, this is a required parameter. The version follows the OSGi version
	 * numbers scheme which are composed of four segments - three integers and a
	 * string respectively named: <code>'major.minor.service.qualifier'</code>.
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
	public String getVersion() {
		return version.toString();
	}
}
