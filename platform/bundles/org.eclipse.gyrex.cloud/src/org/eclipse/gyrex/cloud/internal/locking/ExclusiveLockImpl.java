/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.locking;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperBasedService;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exclusive lock implementation.
 * <p>
 * This implementation is based on ZooKeeper globally synchronous lock recipe.
 * This recipe also ensures that only one thread can hold a lock at any point in
 * time. The protocol is as follows.
 * <ol>
 * <li>Call create( ) with a pathname of "_locknode_/lock-" and the sequence and
 * ephemeral flags set.</li>
 * <li>Call getChildren( ) on the lock node without setting the watch flag (this
 * is important to avoid the herd effect).</li>
 * <li>If the pathname created in step 1 has the lowest sequence number suffix,
 * the client has the lock and the client exits the protocol.</li>
 * <li>The client calls exists( ) with the watch flag set on the path in the
 * lock directory with the next lowest sequence number.</li>
 * <li>if exists( ) returns false, go to step 2. Otherwise, wait for a
 * notification for the pathname from the previous step before going to step 2.</li>
 * </ol>
 * <p>
 * The unlock protocol is very simple: clients wishing to release a lock simply
 * delete the node they created in step 1.
 * </p>
 * <p>
 * Here are a few things to notice:
 * <ul>
 * <li>The removal of a node will only cause one client to wake up since each
 * node is watched by exactly one client. In this way, you avoid the herd
 * effect.</li>
 * <li>There is no polling or timeouts.</li>
 * <li>Because of the way you implement locking, it is easy to see the amount of
 * lock contention, break locks, debug locking problems, etc.</li>
 * </ul>
 * </p>
 */
public class ExclusiveLockImpl extends ZooKeeperBasedService implements IExclusiveLock {

	private static class WaitForDeletionMonitor extends ZooKeeperMonitor {

		private static CountDownLatch deletionHappend = new CountDownLatch(1);

		public boolean await(final long timeout) throws InterruptedException {
			if (timeout > 0) {
				return deletionHappend.await(timeout, TimeUnit.MILLISECONDS);
			} else {
				deletionHappend.await();
				return true;
			}
		}

		@Override
		protected void pathDeleted(final String path) {
			deletionHappend.countDown();
		};

	}

	private static final String LOCK_NAME_PREFIX = "lock-";
	private static final Logger LOG = LoggerFactory.getLogger(ExclusiveLockImpl.class);

	private static int getSequenceNumber(final String nodeName) {
		return NumberUtils.toInt(StringUtils.removeStart(nodeName, LOCK_NAME_PREFIX), -1);
	};

	private final String lockId;
	private final IPath lockNodePath;
	private final ILockMonitor<IExclusiveLock> lockMonitor;
	private final ExecutorService lockMonitorExecutor = Executors.newSingleThreadExecutor();

	private volatile String myLockName;
	private volatile String activeLockName;

	public ExclusiveLockImpl(final String lockId, final ILockMonitor<IExclusiveLock> lockMonitor) {
		super(200l, 5);
		if (!IdHelper.isValidId(lockId)) {
			throw new IllegalArgumentException("invalid lock id; please see IdHelper#isValidId");
		}
		this.lockId = lockId;
		lockNodePath = IZooKeeperLayout.PATH_LOCKS_EXCLUSIVE.append(lockId);
		this.lockMonitor = lockMonitor;
	}

	public ExclusiveLockImpl acquire(final long timeout) throws InterruptedException, TimeoutException {
		// define a logical abort condition
		final long abortTime = System.currentTimeMillis() + timeout;

		try {
			// 1. Call create( ) with a pathname of "_locknode_/lock-" and the sequence and ephemeral flags set.
			execute(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					// note, we rely on any previously given lock name as locks are session-only locks and
					// typically may not be re-acquired
					if (!isClosed() && (null == myLockName)) {
						myLockName = ZooKeeperGate.get().createPath(lockNodePath.append(LOCK_NAME_PREFIX), CreateMode.EPHEMERAL_SEQUENTIAL).lastSegment();
						if (CloudDebug.lockService) {
							LOG.debug("Created lock node {} for lock {}", myLockName, lockNodePath);
						}
					}
					return true;
				}
			});

			// spin the lock acquisition loop
			execute(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					// get gate
					final ZooKeeperGate zk = ZooKeeperGate.get();

					// start acquire loop
					do {
						if (CloudDebug.lockService) {
							LOG.debug("Starting acquire lock loop for lock {}/{}", lockNodePath, myLockName);
						}

						// 2. Call getChildren( ) on the lock node without setting the watch flag (this is important to avoid the herd effect).
						final Object[] nodeNames = zk.readChildrenNames(lockNodePath, null).toArray();

						// sanity check
						if (nodeNames.length == 0) {
							// this is bogus, we actually created a node above
							LOG.warn("Unexpected child count for ZooKeeper node {}. We just created a sequential child but it wasn't there. This may indicate an instability in the system.", lockNodePath);
							continue;
						}

						// sort based on sequence numbers
						Arrays.sort(nodeNames, new Comparator<Object>() {
							@Override
							public int compare(final Object o1, final Object o2) {
								final String n1 = (String) o1;
								final String n2 = (String) o2;
								final int sequence1 = getSequenceNumber(n1);
								final int sequence2 = getSequenceNumber(n2);
								if (sequence1 == -1) {
									return sequence2 != -1 ? 1 : n1.compareTo(n2);
								} else {
									return sequence2 == -1 ? -1 : sequence1 - sequence2;
								}
							}
						});

						// the active lock name
						activeLockName = (String) nodeNames[0];
						if (CloudDebug.lockService) {
							LOG.debug("Found active lock {} for lock {}", activeLockName, lockNodePath);
						}

						// 3. If the pathname created in step 1 has the lowest sequence number suffix, the client has the lock and the client exits the protocol.
						if (isValid()) {
							notifyLockAcquired();
							return true;
						}

						// find our preceding node
						String precedingNodeName = null;
						for (int i = 0; i < nodeNames.length; i++) {
							if (myLockName.equals(nodeNames[i])) {
								precedingNodeName = (String) nodeNames[i - 1];
							}
						}
						if (precedingNodeName == null) {
							throw new IllegalStateException("failed to discover preceding lock; impossible to acquire my lock");
						}
						if (CloudDebug.lockService) {
							LOG.debug("Found preceding lock {} for lock {}", precedingNodeName, lockNodePath);
						}

						// 4. The client calls exists( ) with the watch flag set on the path in the lock directory with the next lowest sequence number.
						// 5. if exists( ) returns false, go to step 2. Otherwise, wait for a notification for the pathname from the previous step before going to step 2.
						final WaitForDeletionMonitor waitForDeletionMonitor = new WaitForDeletionMonitor();
						if (zk.exists(lockNodePath.append(precedingNodeName), waitForDeletionMonitor)) {
							if (CloudDebug.lockService) {
								LOG.debug("Waiting for preceeing lock {} to release lock {}", precedingNodeName, lockNodePath);
							}
							if (!waitForDeletionMonitor.await(timeout)) {
								if (CloudDebug.lockService) {
									LOG.debug("Timeout waiting for preceeing lock {} to release lock {}", precedingNodeName, lockNodePath);
								}
								// node has not been deleted
								throw new TimeoutException(String.format("Unable to acquire lock %s within the given timeout.", getId()));
							}
							if (CloudDebug.lockService) {
								LOG.debug("Preceeing lock {} released lock {}", precedingNodeName, lockNodePath);
							}
						}

						if (CloudDebug.lockService) {
							LOG.debug("End acquire lock loop for lock {}/{}", lockNodePath, myLockName);
						}
					} while ((timeout <= 0) || (abortTime > System.currentTimeMillis()));

					if (CloudDebug.lockService) {
						LOG.debug("Timeout retrying to acquire lock {}/{}", lockNodePath, myLockName);
					}

					// when a this point the loop
					throw new TimeoutException(String.format("Unable to acquire lock %s within the given timeout.", getId()));
				}
			});

			// done
			return this;
		} catch (final InterruptedException e) {
			throw e;
		} catch (final TimeoutException e) {
			throw e;
		} catch (final Exception e) {
			throw new LockAcquirationFailedException(lockId, e);
		}
	}

	@Override
	protected void doClose(final boolean regular) {
		if (CloudDebug.lockService) {
			LOG.debug("Closing lock {}/{}", lockNodePath, myLockName);
		}

		// reset active lock name (which will make the lock invalid)
		activeLockName = null;

		// try to delete the record in ZooKeeper
		ensureLockIsReleased(regular);

		// shutdown executor
		lockMonitorExecutor.shutdown();
	}

	private void ensureLockIsReleased(final boolean regular) {
		// in order to release a lock we must delete the node we created
		// however, this might not be possible if the connection is already gone
		if (myLockName == null) {
			return;
		}

		if (CloudDebug.lockService) {
			LOG.debug("Releasing lock {}/{}", lockNodePath, myLockName);
		}
		try {
			final boolean removed = execute(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					final String lockName = myLockName;
					if (lockName == null) {
						return false;
					}

					// delete path
					ZooKeeperGate.get().deletePath(lockNodePath.append(lockName));

					// reset my lock name upon success
					myLockName = null;

					// report success
					return true;
				}
			});
			if (removed) {
				notifyLockReleased(regular);
			}
		} catch (final SessionExpiredException e) {
			// session expired so assume the node was removed by ZooKeeper
			if (CloudDebug.lockService) {
				LOG.debug("ZooKeeper session expired. Relying on ZooKeeper server to remove lock node {}/{}", lockNodePath, myLockName);
			}
		} catch (final NoNodeException e) {
			// node already gone
			if (CloudDebug.lockService) {
				LOG.debug("Lock node already gone {}/{}", lockNodePath, myLockName);
			}
		} catch (final Exception e) {
			LOG.warn("Unable to remove lock node {}. Please check server logs and also ZooKeeper. If node still exists and the session is not closed it might never get released. However, it should get released automatically after the session times out on the ZooKeeper server. {}", lockNodePath.append(myLockName), ExceptionUtils.getRootCauseMessage(e));
		}
	}

	@Override
	public String getId() {
		return lockId;
	}

	@Override
	protected String getToStringDetails() {
		final StringBuilder details = new StringBuilder();
		details.append("id=").append(lockId);
		if (isValid()) {
			details.append(", ACQUIRED");
		}
		details.append(", lockName=").append(myLockName);
		details.append(", activeLockName=").append(activeLockName);
		return details.toString();
	}

	@Override
	public boolean isValid() {
		final String myLockName = this.myLockName;
		final String activeLockName = this.activeLockName;
		return (myLockName != null) && (activeLockName != null) && activeLockName.equals(myLockName);
	}

	void notifyLockAcquired() {
		// log info message
		LOG.info("Successfully acquired exclusive lock {}!", getId());

		if (lockMonitor == null) {
			return;
		}
		lockMonitorExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if (!isClosed() && isValid()) {
					lockMonitor.lockAcquired(ExclusiveLockImpl.this);
				}
			}
		});
	}

	void notifyLockReleased(final boolean regular) {
		// log info message
		LOG.info(regular ? "Successfully released exclusive lock {}!" : "Lost exclusive lock {}!", getId());

		if (lockMonitor == null) {
			return;
		}
		lockMonitorExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if (regular) {
					lockMonitor.lockReleased(ExclusiveLockImpl.this);
				} else {
					lockMonitor.lockLost(ExclusiveLockImpl.this);
				}
			}
		});
	}

	@Override
	public void release() {
		close(true);
	}

}
