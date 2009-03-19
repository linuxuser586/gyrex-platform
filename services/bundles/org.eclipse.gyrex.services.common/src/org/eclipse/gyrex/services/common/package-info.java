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

/**
 * This package defines the Service API of Gyrex.
 * <p>
 * <strong>The Service API</strong>
 </p>
 <p>
 The service API is intended to serve as the high-level API. It - by 
 definition - can have long running operations, works fully within a 
 distributed environment, does implement a particular business context 
 and can execute across several objects. 
 </p>
 <p>
 Typically, the service API also implements transaction logic but in a 
 way that does not rely on database transactions but on data state. This 
 also enforces our distributed programming model by moving failure 
 handling into the application logic. 
 </p>
 <p>
 Let's look at an example (note, this is really just an example). Imaging 
 there is a service API for creating an order from a shopping cart. The 
 implementation would first create the order and then create order items 
 for all the items in the shopping cart and then update the order with 
 shipping and billing information etc. Now after creating several order 
 items the implementation discovers an invalid item and would like to 
 cancel the order creation. Instead of relying on database transactions 
 for this the service API is expected to call an order model manager API 
 that marks the order as invalid before returning. This leaves the 
 records in the database but it doesn't matter because they are marked 
 invalid. A job could run at low traffic times to archive all invalid 
 orders. 
 </p>
 <p>
 Another alternative implementation might be to create the order in a 
 "creating" state in the first place and to transfered it explicitly into 
 the "successfully created" state after adding all order items to 
 indicate a successful order creation to the service API clients. 
 </p>
 */

/**
 * This package defines the Service API of Gyrex.
 * <p>
 * <strong>The Service API</strong>
 * </p>
 * <p>
 * The service API is intended to serve as the high-level API. It - by
 * definition - can have long running operations, works fully within a
 * distributed environment, does implement a particular business context and can
 * execute across several objects.
 * </p>
 * <p>
 * Typically, the service API also implements transaction logic but in a way
 * that does not rely on database transactions but on data state. This also
 * better supports a distributed programming model by moving failure handling
 * into the application logic.
 * </p>
 * <p>
 * Let's look at an example (note, this is really just an example). Imaging
 * there is a service API for creating an order from a shopping cart. The
 * implementation would first create the order and then create order items for
 * all the items in the shopping cart and then update the order with shipping
 * and billing information etc. Now after creating several order items the model
 * manager fails to create another item. In this case the service implementation
 * might re-tries a few time and gives up after a few more tries. The order
 * should then be discarded. Instead of relying on database transactions for
 * this the service API simply creates the order in a "creating" state
 * initially. It transfers the order explicitly into a "successfully created"
 * state after adding all order items without problems to indicate a successful
 * order creation to service API clients. Obviously, this will leave stale
 * records in the database in case of problems. But it doesn't matter because
 * they are ignored and a job could run at low traffic times to purge (or
 * archive) all those invalid records.
 * </p>
 * <p>
 * Services are intended to be exposed to a larger audience than the underlying
 * model layer. For example, a service might be exposed as a web service via
 * HTTP(S). Clients could use JSON (or XML, binary, etc.) to talk to such a
 * service. As such, it is also important that a service neither exposes
 * transaction details to service clients nor requires a service client to deal
 * with transactional state.
 * </p>
 * <p>
 * For a more detailed read on the subject of transactions and scalable systems
 * we recommend reading <a
 * href="http://www.cidrdb.org/cidr2007/papers/cidr07p15.pdf"
 * target="blank">Life beyond Distributed Transactions: an Apostate's
 * Opinion</a>, a position paper by Pat Helland.
 * </p>
 */
package org.eclipse.gyrex.services.common;

