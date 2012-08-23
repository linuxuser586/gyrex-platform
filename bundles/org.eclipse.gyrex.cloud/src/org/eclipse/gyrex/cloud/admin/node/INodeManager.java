/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.admin.node;

import org.eclipse.gyrex.cloud.admin.ICloudManager;

/**
 * Interface for managing a specific node at runtime.
 * <p>
 * A node manager can be obtained from the {@link ICloudManager cluster manager}
 * . It establishes a connection to a remote node and allows to monitor and
 * manage a remote node from another node in the system. This requires a network
 * setup which allows direct communication between nodes. The connection and
 * communication to the remote node is typically stateless. Clients must be
 * aware that the connection can be interrupted at any time.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface INodeManager {

}
