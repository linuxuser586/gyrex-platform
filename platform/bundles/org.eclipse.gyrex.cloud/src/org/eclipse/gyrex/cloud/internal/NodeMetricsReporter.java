/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal;

import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.common.lifecycle.IShutdownParticipant;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constantly writes metrics to ZooKeeper
 */
public class NodeMetricsReporter extends Job implements IShutdownParticipant {

	/** scheduling delay */
	private static final long DELAY = 60000l;
	private static final NodeMetricsReporter instance = new NodeMetricsReporter();
	private static final Logger LOG = LoggerFactory.getLogger(NodeMetricsReporter.class);

	private static String getUsingReflection(final Object bean, final String methodName) {
		try {
			final Method method = bean.getClass().getMethod(methodName, (Class[]) null);
			final Object result = method.invoke(method, (Object[]) null);
			if (result == null) {
				return StringUtils.EMPTY;
			}
			return String.valueOf(result);
		} catch (final Exception e) {
			// ignore
			return StringUtils.EMPTY;
		}
	}

	static void start() {
		if (CloudDebug.nodeMetrics) {
			LOG.debug("Starting node metrics report job.");
		}
		final CloudActivator activator = CloudActivator.getInstance();
		instance.schedule(DELAY);
		activator.addShutdownParticipant(instance);
	}

	static void stop() {
		if (CloudDebug.nodeMetrics) {
			LOG.debug("Stopping node metrics report job.");
		}
		final CloudActivator activator = CloudActivator.getInstance();
		instance.cancel();
		activator.removeShutdownParticipant(instance);
	}

	private NodeMetricsReporter() {
		super("Node Metrics Reporter");
		setSystem(true);
		setPriority(DECORATE);
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		if (!monitor.isCanceled()) {
			try {
				final Properties metrics = new Properties() {
					private static final long serialVersionUID = 1L;

					@Override
					public synchronized Enumeration<Object> keys() {
						return Collections.enumeration(keySet());
					}

					@Override
					public Set<Object> keySet() {
						return new TreeSet<Object>(super.keySet());
					}
				};
				final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
				metrics.setProperty("os.availableProcessors", String.valueOf(operatingSystemMXBean.getAvailableProcessors()));
				metrics.setProperty("os.systemLoadAverage", String.valueOf(operatingSystemMXBean.getSystemLoadAverage()));
				metrics.setProperty("os.committedVirtualMemorySize", getUsingReflection(operatingSystemMXBean, "getCommittedVirtualMemorySize"));
				metrics.setProperty("os.totalSwapSpaceSize", getUsingReflection(operatingSystemMXBean, "getTotalSwapSpaceSize"));
				metrics.setProperty("os.freeSwapSpaceSize", getUsingReflection(operatingSystemMXBean, "getFreeSwapSpaceSize"));
				metrics.setProperty("os.processCpuTime", getUsingReflection(operatingSystemMXBean, "getProcessCpuTime"));
				metrics.setProperty("os.freePhysicalMemorySize", getUsingReflection(operatingSystemMXBean, "getFreePhysicalMemorySize"));
				metrics.setProperty("os.totalPhysicalMemorySize", getUsingReflection(operatingSystemMXBean, "getTotalPhysicalMemorySize"));
				metrics.setProperty("os.openFileDescriptorCount", getUsingReflection(operatingSystemMXBean, "getOpenFileDescriptorCount"));
				metrics.setProperty("os.maxFileDescriptorCount", getUsingReflection(operatingSystemMXBean, "getMaxFileDescriptorCount"));

				final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
				final MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
				metrics.setProperty("heap.used", String.valueOf(heapMemoryUsage.getUsed()));
				metrics.setProperty("heap.committed", String.valueOf(heapMemoryUsage.getCommitted()));
				metrics.setProperty("heap.max", String.valueOf(heapMemoryUsage.getMax()));
				metrics.setProperty("heap.init", String.valueOf(heapMemoryUsage.getInit()));
				final MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
				metrics.setProperty("nonHeap.used", String.valueOf(nonHeapMemoryUsage.getUsed()));
				metrics.setProperty("nonHeap.committed", String.valueOf(nonHeapMemoryUsage.getCommitted()));
				metrics.setProperty("nonHeap.max", String.valueOf(nonHeapMemoryUsage.getMax()));
				metrics.setProperty("nonHeap.init", String.valueOf(nonHeapMemoryUsage.getInit()));

				final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
				metrics.setProperty("thread.count", String.valueOf(threadMXBean.getThreadCount()));
				metrics.setProperty("thread.peak", String.valueOf(threadMXBean.getPeakThreadCount()));
				metrics.setProperty("thread.totalStarted", String.valueOf(threadMXBean.getTotalStartedThreadCount()));

				final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
				metrics.setProperty("uptime", String.valueOf(runtimeMXBean.getUptime()));

				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				final NodeInfo nodeInfo = CloudState.getNodeInfo();
				metrics.store(out, String.valueOf(nodeInfo));
				ZooKeeperGate.get().writeRecord(IZooKeeperLayout.PATH_NODES_METRICS.append(getName()), CreateMode.PERSISTENT, out.toByteArray());
				if (CloudDebug.nodeMetrics) {
					LOG.debug("Node metrics reported successfully.{}{}", SystemUtils.LINE_SEPARATOR, new String(out.toByteArray(), CharEncoding.ISO_8859_1));
				}
			} catch (final Exception e) {
				LOG.warn("Failed to update node metrics. {}", e.getMessage());
			} finally {
				// reschedule
				schedule(DELAY);
			}
		}

		return Status.OK_STATUS;
	}

	@Override
	public void shutdown() throws Exception {
		cancel();
	}
}
