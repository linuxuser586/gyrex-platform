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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.CloudState;
import org.eclipse.gyrex.cloud.internal.NodeInfo;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperBasedService;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.cloud.services.locking.IDistributedLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeper lock implementation.
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
 * <li>Because of the way locking is implemented, it is easy to see the amount
 * of lock contention, break locks, debug locking problems, etc.</li>
 * <li>It's possible to kill locks directly in ZooKeeper for administrative
 * purposes.</li>
 * </ul>
 * </p>
 */
public abstract class ZooKeeperLock<T extends IDistributedLock> extends ZooKeeperBasedService implements IDistributedLock {

	/**
	 * The loop the actually acquires the lock.
	 */
	private final class AcquireLockLoop implements Callable<Boolean> {
		/** abortTime */
		private final long abortTime;
		/** timeout */
		private final long timeout;
		/** recover */
		private final boolean recover;

		/**
		 * Creates a new instance.
		 * 
		 * @param abortTime
		 * @param timeout
		 * @param recover
		 */
		private AcquireLockLoop(final long abortTime, final long timeout, final boolean recover) {
			this.abortTime = abortTime;
			this.timeout = timeout;
			this.recover = recover;
		}

		@Override
		public Boolean call() throws Exception {
			// get gate
			final ZooKeeperGate zk = ZooKeeperGate.get();

			// start acquire loop
			do {
				if (CloudDebug.zooKeeperLockService) {
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
				if (CloudDebug.zooKeeperLockService) {
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
					if (recover) {
						throw new LockAcquirationFailedException(getId(), "Impossible to recover lock. The preceding lock could not be discovered.");
					} else {
						throw new LockAcquirationFailedException(getId(), "Impossible to acquire lock. The preceding lock could not be discovered.");
					}
				}
				if (CloudDebug.zooKeeperLockService) {
					LOG.debug("Found preceding lock {} for lock {}", precedingNodeName, lockNodePath);
				}

				// 4. The client calls exists( ) with the watch flag set on the path in the lock directory with the next lowest sequence number.
				// 5. if exists( ) returns false, go to step 2. Otherwise, wait for a notification for the pathname from the previous step before going to step 2.
				final WaitForDeletionMonitor waitForDeletionMonitor = new WaitForDeletionMonitor();
				if (zk.exists(lockNodePath.append(precedingNodeName), waitForDeletionMonitor)) {
					if (CloudDebug.zooKeeperLockService) {
						LOG.debug("Waiting for preceeing lock {} to release lock {}", precedingNodeName, lockNodePath);
					}
					if (!waitForDeletionMonitor.await(timeout)) {
						if (CloudDebug.zooKeeperLockService) {
							LOG.debug("Timeout waiting for preceeing lock {} to release lock {}", precedingNodeName, lockNodePath);
						}
						// node has not been deleted
						throw new TimeoutException(String.format("Unable to acquire lock %s within the given timeout.", getId()));
					}
					if (CloudDebug.zooKeeperLockService) {
						LOG.debug("Preceeing lock {} released lock {}", precedingNodeName, lockNodePath);
					}
				}

				if (CloudDebug.zooKeeperLockService) {
					LOG.debug("End acquire lock loop for lock {}/{}", lockNodePath, myLockName);
				}
			} while ((timeout <= 0) || (abortTime > System.currentTimeMillis()));

			if (CloudDebug.zooKeeperLockService) {
				LOG.debug("Timeout retrying to acquire lock {}/{}", lockNodePath, myLockName);
			}

			// when a this point the loop
			throw new TimeoutException(String.format("Unable to acquire lock %s within the given timeout.", getId()));
		}
	}

	/**
	 * Operation for creating a new lock node.
	 */
	private final class CreateLockNode implements Callable<Boolean> {
		@Override
		public Boolean call() throws Exception {
			// note, we rely on any previously given lock name as locks are session-only locks and
			// typically may not be re-acquired
			if (!isClosed() && (null == myLockName)) {
				final ZooKeeperGate zk = ZooKeeperGate.get();

				// create node
				final IPath nodePath = zk.createPath(lockNodePath.append(LOCK_NAME_PREFIX), isEphemeral() ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.PERSISTENT_SEQUENTIAL, lockNodeContent);

				// extract lock name
				myLockName = nodePath.lastSegment();
				if (CloudDebug.zooKeeperLockService) {
					LOG.debug("Created lock node {} for lock {}", myLockName, lockNodePath);
				}

				// generate recovery key
				myRecoveryKey = createRecoveryKey(myLockName, lockNodeContent);

				// allow remote kill
				zk.readRecord(lockNodePath, killMonitor, null);
			}
			return true;
		}
	}

	/**
	 * Operation for deleting a lock node.
	 */
	private final class DeleteLockNode implements Callable<Boolean> {
		@Override
		public Boolean call() throws Exception {
			final String lockName = myLockName;
			if (lockName == null) {
				return false;
			}

			// delete path
			try {
				if (CloudDebug.zooKeeperLockService) {
					LOG.debug("Deleting lock node in ZooKeeper {}/{}", lockNodePath, myLockName);
				}
				ZooKeeperGate.get().deletePath(lockNodePath.append(lockName), -1);
			} catch (final NoNodeException e) {
				// node already gone
				if (CloudDebug.zooKeeperLockService) {
					LOG.debug("Lock node already gone {}/{}", lockNodePath, myLockName);
				}
			}

			// reset my lock name upon success
			myLockName = null;

			// report success
			return true;
		}
	}

	static enum KillReason {
		ZOOKEEPER_DISCONNECT, LOCK_DELETED, LOCK_STOLEN, REGULAR_RELEASE, ACQUIRE_FAILED
	}

	/**
	 * Operation for recovering an existing lock node.
	 */
	private final class RecoverLockNode implements Callable<Boolean> {

		private final String lockName;
		private final String expectedNodeContent;

		public RecoverLockNode(final String recoveryKey) {
			if (StringUtils.isBlank(recoveryKey)) {
				throw new IllegalArgumentException("recovery key must not be empty");
			}

			// extract lock name and node content from recovery key
			final String[] extractRecoveryKeyDetails = extractRecoveryKeyDetails(recoveryKey);
			lockName = extractRecoveryKeyDetails[0];
			expectedNodeContent = extractRecoveryKeyDetails[1];
		}

		@Override
		public Boolean call() throws Exception {
			// note, we rely on any previously given lock name as locks are session-only locks and
			// typically may not be re-acquired
			if (!isClosed() && (null == myLockName)) {
				final ZooKeeperGate zk = ZooKeeperGate.get();

				if (CloudDebug.zooKeeperLockService) {
					LOG.debug("Recovery attempt for lock node {} for lock {}", lockName, lockNodePath);
				}

				// build node path
				final IPath nodePath = lockNodePath.append(lockName);

				// get node content
				final Stat stat = new Stat();
				final String record = zk.readRecord(nodePath, StringUtils.EMPTY, stat);

				// check that node exists
				if (StringUtils.isBlank(record)) {
					// does not exist, so return false here which indicates the we cannot recover
					if (CloudDebug.zooKeeperLockService) {
						LOG.debug("Recovery attempt failed. Lock node {}/{} does not exists", lockNodePath, lockName);
					}
					throw new LockAcquirationFailedException(lockId, "Unable to recover lock. The lock could not be found.");
				}

				// check that content matches
				if (!StringUtils.equals(record, expectedNodeContent)) {
					if (CloudDebug.zooKeeperLockService) {
						LOG.debug("Recovery attempt failed. Recovery key does not match for lock node {}/{}", lockNodePath, lockName);
					}
					throw new LockAcquirationFailedException(lockId, "Unable to recover lock. The recovery key does not match.");
				}

				// reset lock name
				myLockName = nodePath.lastSegment();
				if (CloudDebug.zooKeeperLockService) {
					LOG.debug("Recovered lock node {} for lock {}", myLockName, lockNodePath);
				}

				// generate new recovery key
				myRecoveryKey = createRecoveryKey(myLockName, lockNodeContent);

				// write new lock name
				// note, we must pass the expected version in order to discover concurrent recovery requests
				zk.writeRecord(nodePath, myRecoveryKey, stat.getVersion());

				// allow remote kill
				zk.readRecord(lockNodePath, killMonitor, null);
			}
			return true;
		}
	}

	/**
	 * A monitor that allows to wait for deletion of a ZooKeeper node path.
	 */
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

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperLock.class);

	static String createRecoveryKey(final String lockName, final String nodeContent) {
		return lockName.concat("_").concat(nodeContent);
	}

	static String[] extractRecoveryKeyDetails(final String recoveryKey) {
		final String[] keySegments = StringUtils.split(recoveryKey, '_');
		if (keySegments.length < 2) {
			throw new IllegalArgumentException("invalid recovery key format");
		}
		final String lockName = keySegments[0];
		final String nodeContent = StringUtils.removeStart(recoveryKey, lockName.concat("_"));

		if (StringUtils.isBlank(lockName) || StringUtils.isBlank(nodeContent)) {
			throw new IllegalArgumentException("invalid recovery key format");
		}
		return new String[] { lockName, nodeContent };
	};

	private static int getSequenceNumber(final String nodeName) {
		return NumberUtils.toInt(StringUtils.removeStart(nodeName, LOCK_NAME_PREFIX), -1);
	}

	final ZooKeeperMonitor killMonitor = new ZooKeeperMonitor() {
		@Override
		protected void closing(final org.apache.zookeeper.KeeperException.Code reason) {
			// the connection has been lost or the session expired
			LOG.warn("Lock {} has been lost due to disconnect (reason {})!", getId(), reason);
			killLock(KillReason.ZOOKEEPER_DISCONNECT);
		};

		@Override
		protected void pathDeleted(final String path) {
			LOG.warn("Lock {} has been deleted on the lock server!", getId());
			killLock(KillReason.LOCK_DELETED);
		};

		@Override
		protected void recordChanged(final String path) {
			// the lock record has been changed remotely
			// this means the lock was stolen
			LOG.warn("Lock {} has been stolen!", getId());
			killLock(KillReason.LOCK_STOLEN);
		};
	};

	final String lockId;
	final IPath lockNodePath;
	final String lockNodeContent;
	final boolean ephemeral;
	final boolean recovarable;

	private final ILockMonitor<T> lockMonitor;

	volatile String myLockName;
	volatile String myRecoveryKey;
	volatile String activeLockName;

	/**
	 * Creates a new lock instance.
	 * 
	 * @param lockId
	 *            the lock id
	 * @param lockMonitor
	 *            the lock monitor
	 * @param lockNodeParentPath
	 *            the lock node parent path
	 * @param ephemeral
	 *            <code>true</code> if an ephemeral node should be created,
	 *            <code>false</code> otherwise
	 * @param recovarable
	 *            <code>true</code> if the lock is recoverable,
	 *            <code>false</code> otherwise
	 */
	public ZooKeeperLock(final String lockId, final ILockMonitor<T> lockMonitor, final IPath lockNodeParentPath, final boolean ephemeral, final boolean recovarable) {
		super(200l, 5);
		if (!IdHelper.isValidId(lockId)) {
			throw new IllegalArgumentException("invalid lock id; please see IdHelper#isValidId");
		}
		this.lockId = lockId;
		lockNodePath = lockNodeParentPath.append(lockId);
		this.lockMonitor = lockMonitor;
		this.ephemeral = ephemeral;
		this.recovarable = recovarable;

		// pre-generate lock node content info
		NodeInfo nodeInfo = CloudState.getNodeInfo();
		if (null == nodeInfo) {
			nodeInfo = new NodeInfo();
		}
		lockNodeContent = nodeInfo.getNodeId() + "-" + nodeInfo.getLocation() + "-" + DigestUtils.shaHex(UUID.randomUUID().toString());

		// check implementation
		try {
			asLockType();
		} catch (final ClassCastException e) {
			throw new ClassCastException(String.format("Cannot cast the lock implementation %s to the generic lock type. Please make sure that the implementation implements the interface. %s", getClass().getName(), e.getMessage()));
		}
	}

	protected final T acquire(final long timeout, final boolean recover, final String recoveryKey) throws InterruptedException, TimeoutException {
		// define a logical abort condition for the acquire loop
		// this must be done first in order to also count any operation preceding the acquire loop
		final long abortTime = System.currentTimeMillis() + timeout;

		if (recover && !isRecoverable()) {
			throw new IllegalStateException("lock implementation is not recoverable");
		}

		try {
			// create (or recover) lock node with a pathname of "_locknode_/lock-" and the sequence flag set
			if (recover) {
				execute(new RecoverLockNode(recoveryKey));
			} else {
				execute(new CreateLockNode());
			}

			// spin the lock acquisition loop
			execute(new AcquireLockLoop(abortTime, timeout, recover));

			// done
			return asLockType();
		} catch (final Exception e) {
			try {
				killLock(KillReason.ACQUIRE_FAILED);
			} catch (final Exception cleanUpException) {
				LOG.error("Error during cleanup of failed lock acquisition. Please check server logs and also check lock service server. The lock may now ne stalled. {}", ExceptionUtils.getRootCauseMessage(cleanUpException));
			}
			if (e instanceof InterruptedException) {
				throw (InterruptedException) e;
			} else if (e instanceof TimeoutException) {
				throw (TimeoutException) e;
			} else {
				throw new LockAcquirationFailedException(lockId, e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	T asLockType() {
		// this is ugly
		return (T) ZooKeeperLock.this;
	}

	@Override
	protected void disconnect() {
		killLock(KillReason.ZOOKEEPER_DISCONNECT);
	}

	@Override
	protected void doClose() {
		if (CloudDebug.zooKeeperLockService) {
			LOG.debug("Closing lock {}/{}", lockNodePath, myLockName);
		}

		// reset active lock name (which will make the lock invalid)
		activeLockName = null;
	}

	@Override
	public String getId() {
		return lockId;
	}

	/**
	 * This method is only exposed for testing purposes. Please do not call it.
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final String getMyLockName() {
		return myLockName;
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

	/**
	 * Returns the ephemeral.
	 * 
	 * @return the ephemeral
	 */
	protected final boolean isEphemeral() {
		return ephemeral;
	}

	/**
	 * Returns the recovarable.
	 * 
	 * @return the recovarable
	 */
	protected final boolean isRecoverable() {
		return recovarable;
	}

	@Override
	public boolean isValid() {
		final String myLockName = this.myLockName;
		final String activeLockName = this.activeLockName;
		return (myLockName != null) && (activeLockName != null) && activeLockName.equals(myLockName);
	}

	/**
	 * Kills the lock.
	 * 
	 * @param killReason
	 *            the kill reason
	 */
	void killLock(final KillReason killReason) {
		// in order to release a lock we must delete the node we created
		// however, this might not be possible if the connection is already gone
		if ((myLockName == null) || isClosed()) {
			return;
		}

		if (CloudDebug.zooKeeperLockService) {
			LOG.debug("Killing lock {}/{}", lockNodePath, myLockName);
		}

		try {
			// attempt to delete the lock (if possible)
			if (shouldDeleteOnKill(killReason)) {
				execute(new DeleteLockNode());
			}

			// sent notification
			notifyLockReleased(killReason);
		} catch (final SessionExpiredException e) {
			// session expired so assume the node was removed by ZooKeeper
			if (CloudDebug.zooKeeperLockService) {
				LOG.debug("ZooKeeper session expired. Relying on ZooKeeper server to remove lock node {}/{}", lockNodePath, myLockName);
			}
			// sent notification
			notifyLockReleased(KillReason.ZOOKEEPER_DISCONNECT);
		} catch (final Exception e) {
			LOG.warn("Unable to remove lock node {}. Please check server logs and also ZooKeeper. If node still exists and the session is not closed it might never get released. However, it should get released automatically after the session times out on the ZooKeeper server. {}", lockNodePath.append(myLockName), ExceptionUtils.getRootCauseMessage(e));
		} finally {
			// close the service
			close();
		}
	}

	void notifyLockAcquired() {
		// log info message
		LOG.info("Successfully acquired lock {}!", getId());

		if (lockMonitor != null) {
			if (!isClosed() && isValid()) {
				lockMonitor.lockAcquired(asLockType());
			}
		}
	}

	void notifyLockReleased(final KillReason reason) {
		// detect if released regularly
		final boolean released = reason == KillReason.REGULAR_RELEASE;

		// log info message
		LOG.info(released ? "Successfully released lock {}!" : "Lost lock {}!", getId());

		if (lockMonitor != null) {
			if (released) {
				lockMonitor.lockReleased(asLockType());
			} else {
				lockMonitor.lockLost(asLockType());
			}
		}
	}

	@Override
	public void release() {
		// kill the lock
		killLock(KillReason.REGULAR_RELEASE);
	}

	private boolean shouldDeleteOnKill(final KillReason killReason) {
		switch (killReason) {
			case LOCK_DELETED:
			case LOCK_STOLEN:
				// don't delete when a lock was stolen or already deleted
				return false;

			case REGULAR_RELEASE:
				// delete in any case if this is a regular release
				return true;

			case ZOOKEEPER_DISCONNECT:
				// start a delete attempt only if the node is not recoverable
				return !isRecoverable();

			case ACQUIRE_FAILED:
				// delete if we can't acquire cleanly
				return true;

			default:
				// delete in all other cases
				LOG.warn("Unhandled lock kill reason {}. Please report this issue to the developers. They should sanity check the implementation.", killReason);
				return true;
		}
	}

}
