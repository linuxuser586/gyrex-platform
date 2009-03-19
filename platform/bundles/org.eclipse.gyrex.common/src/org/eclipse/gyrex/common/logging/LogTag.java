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
package org.eclipse.gyrex.common.logging;

/**
 * A tag for classifying log entries.
 * <p>
 * This interface may be implemented by clients to provide additional log tags.
 * However, the prefered use of the shared log tags defined by
 * {@link LogAudience}, {@link LogImportance} as well as {@link LogSource} is
 * recommended.
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
