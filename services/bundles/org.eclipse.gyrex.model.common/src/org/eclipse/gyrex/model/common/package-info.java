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
 * This package defines the Model API of the CloudFree Platform.
 * <p>
 * <strong>The Model API</strong>
 * </p>
 * <p>
 * The model API is the lowest API level and actually consists of two APIs: the
 * model object API and the model manager API. Although these are two
 * APIs they are considered as the ONE low level API and they are allowed to
 * interact with each other in both directions.
 * </p>
 * <p>
 * This basically means that the model object API is allowed to depend on the
 * model manager API and the other way around. However, there is one strong
 * requirement for dependencies from the model object API to the model manager
 * API. The model object API is not allowed to depend on model manager API that
 * directly causes repository modifications of any kind! All repository data 
 * modifications must happen explicitly through model managers and not 
 * transparently through model object API usage. 
 * </p>
 * <p>
 * The model object API defines model objects and allows low level operations on
 * them. These operations are - by definition - mostly simple, short running and
 * not distributed, do not implement a particular business context and typically
 * execute within their objects only. They also do not cause repository updates,
 * inserts or deletes of any kind!
 * </p>
 * <p>
 * The model manager API defines low level CRUD and query operations for model
 * objects. It also defines operations for writing to the repository. These
 * operations are - by definition - mostly simple, short running and not
 * distributed, do not implement a particular business context and typically
 * execute within their objects only.
 * </p>
 * <p>
 * The model API in general is a low level API and - by definition - does not
 * implement a particular business context. This also means that it does not
 * implement business logic but it is still allowed to execute business logic.
 * What does this mean to model API implementors and clients? Let's have a look
 * using a simple example (note, this is really just an example).
 * </p>
 * <blockquote>
 * <p>
 * Imagine you have an order object. This order object allows to update the
 * order totals with a simple method call on the object. However, there are
 * different ways to calculate the order totals. Some variables that influences
 * the order total calculation are tax rules and discounts. Tax rules might be
 * determined by a configuration setting whereas discounts might be dynamic
 * depending on other variables. But because of the 'do not implement a business
 * context' rule the order object is not allowed to lookup variables from a
 * configuration or do discount calculation. So what is allowed?
 * </p>
 * <p>
 * The proper way is to delegate the calculation to someone that is allowed to
 * implement the business context. This means that instead of having an
 * <code>Order#calculateTotal(aWeirdTaxRule, someDiscountRules)</code> method the order
 * object would have an <code>Order#updateOrderTotal(anOrderTotalCalculator)</code> method.
 * The order total calculator is a contract that is implemented at the caller
 * level, which is supposed to be at a higher level.
 * </p>
 * </blockquote>
 * <p>
 * <strong>Transactions</strong>
 * </p>
 * <p>
 * Because the model manager API causes repository modifications it participates
 * in transactions as defined by the underlying repository but does NOT implement 
 * transaction logic. It's also important to note that transaction support
 * is very limited in the CloudFree Platform for scalability reasons. Basically,
 * transactions - if supported by the underlying repository - are only available 
 * within a single repository. This enforces a more scalable approach by moving
 * transaction logic out of the repository into the application. Thus, when
 * using the low-level Model API directly care must be taken to not make wrong
 * assumptions about transaction boundaries. To some extend, the concept of 
 * <em>eventual consistency</em> applies to the CloudFree model.
 * </p>
 * <p>
 * Having said that, there is a simple rule to follow. Clients should not use
 * the low-level model managers directly but any higher level API available.
 * Typically implementors of the higher level API know how to deal with the
 * issues around transactions, distribution and persistence and will solve
 * them in an easy, mostly transparent way. 
 * </p>
 * <p>
 * Again, let's look at an example which should make it easier to understand.
 * (Note, this is purely an example. Similarities to future business APIs are
 * pure fortuity.) 
 * </p>
 * <blockquote>
 * <p>
 * Let's assume there is an order model manager which allows to create and
 * insert new order objects into the repository and add order items to an 
 * order. In a traditional, <em>single</em> repository environment you would
 * probably have the following logic in place.
 * <ul>
 * <li><em>Begin a transaction</em></li>
 * <li>(1) Create an order</li>
 * <li>(2) Attach order items</li>
 * <li><em>Commit the transaction</em></li>
 * </ul>
 * But now assume a more distributed world where the actual order items would
 * be in a different repository than the order itself. Suddenly you would 
 * have to deal with distributed transactions. However, there are some
 * flaws and pitfalls with distributed transactions especially when it comes
 * to scalability. One way to avoid this would be to move the transaction logic 
 * into the application logic using states as in the following example.
 * <ul>
 * <li><em>(Begin a transaction)</em></li>
 * <li>(1)Create an order (initial order state will be CREATING)</li>
 * <li><em>(Commit the transaction)</em></li>
 * <li><em>(Begin a transaction)</em></li>
 * <li>(2) Attach order items</li>
 * <li><em>(Commit the transaction)</em></li>
 * <li><em>(Begin a transaction)</em></li>
 * <li>(3) Mark order CREATED_SUCCESSFULLY</li>
 * <li><em>(Commit the transaction)</em></li>
 * </ul>
 * The obvious difference is that we have an order in the repository after 
 * step (1) finished whereas in the traditional case we only had an order in the 
 * repository after step (2) finished successfully. If the application
 * is developed right it shouldn't care. Orders in the CREATING state are simply
 * ignored. There could be a clean-up job that purges such broken orders after
 * some time.
 * </p>
 * </blockquote>
 * <p><em>
 * Note, of course it is also possible to write a model manager/service which fully 
 * supports and implements two or three phase commits. However, this style
 * of heavy design is just not promoted actively in CloudFree. 
 * </em></p>
 */
package org.eclipse.cloudfree.model.common;

