/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.common.logging;

/**
 * A tag for classifying log entries.
 * <p>
 * This interface may be implemented by clients to provide additional log tags.
 * </p>
 * <p>
 * Gyrex also provides a set of shared log tags defined by {@link LogAudience},
 * {@link LogImportance} as well as {@link LogSource} which is recommended to
 * developers as well.
 * </p>
 * 
 * @see LogAudience
 * @see LogImportance
 * @see LogSource
 */
public interface LogTag {

	/**
	 * Returns the string representation of this tag.
	 * 
	 * @return the tag string representation
	 */
	String toString();
}
