/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.installer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Date;

import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.p2.internal.P2Activator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * A special log created in the config area when installing/removing packages.
 */
public class InstallLog {

	private static File createNewLog(final IPath logsFolder, final String baseName) throws IOException {
		File logFile;
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			logFile = logsFolder.append(baseName).addFileExtension(i + ".log").toFile();
			if (!logFile.exists()) {
				FileUtils.forceMkdir(logFile.getParentFile());
				if (logFile.createNewFile()) {
					return logFile;
				}
			}
		}
		return null;
	}

	private final String sessionId;
	private final File logFile;
	private final PrintWriter logFileWriter;
	private final Date creationDate;

	/**
	 * Creates a new instance.
	 * 
	 * @param shaHex
	 * @throws IOException
	 */
	public InstallLog(final String sessionId) throws IOException {
		this.sessionId = sessionId;
		creationDate = new Date();

		final IPath logsFolder = PackageInstallState.getBaseLocation().append("logs");
		final String baseName = "install-" + DateFormatUtils.ISO_DATE_FORMAT.format(creationDate);

		// create new log file
		final File logFile = createNewLog(logsFolder, baseName);
		if (null == logFile) {
			throw new IllegalStateException("unable to create new log file; file count limit exceeded");
		}
		this.logFile = logFile;
		logFileWriter = new PrintWriter(new FileWriterWithEncoding(logFile, CharEncoding.UTF_16));

		// write header
		writeHeader();
	}

	public void canceled() {
		logFileWriter.println();
		logFileWriter.println("Installation canceled by operator.");
	}

	public void close() {
		writeFooter();
		logFileWriter.flush();
		logFileWriter.close();
	}

	/**
	 * @param urlInUse
	 */
	public void logConfiguration(final URL urlInUse) {
		logFileWriter.println();
		logFileWriter.println("Software Configuration");
		logFileWriter.println("----------------------");
		logFileWriter.println("URL: " + urlInUse.toString());
		logFileWriter.println("<snip>");
		InputStream in = null;
		try {
			in = urlInUse.openStream();
			logFileWriter.println(IOUtils.toString(in));
		} catch (final IOException e) {
			logFileWriter.println(ExceptionUtils.getRootCauseMessage(e));
		}
		logFileWriter.println("</snip>");
	}

	public void logInstallStatus(final InstallOperation op, final IStatus result) {
		logFileWriter.println();
		logFileWriter.println("Software Installation");
		logFileWriter.println("----------------------");
		printStatus(result, "");
		logFileWriter.println(op.getResolutionDetails());

		if (result.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			logFileWriter.println();
			logFileWriter.println("Nothing to install.");
		} else if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
			logFileWriter.println();
			logFileWriter.println("Software installation not possible.");
		} else if (!result.isOK()) {
			logFileWriter.println();
			logFileWriter.println("Install operation resolved with warnings. An installation will be forced.");
		}
	}

	public void logRepositories(final IMetadataRepositoryManager metadataRepositoryManager, final IArtifactRepositoryManager artifactRepositoryManager) {
		logFileWriter.println();
		logFileWriter.println("Software Repositories");
		logFileWriter.println("---------------------");
		logFileWriter.println();
		logFileWriter.println("Metadata Repositories:");
		printRepositories(metadataRepositoryManager);
		logFileWriter.println();
		logFileWriter.println("Artifact Repositories:");
		printRepositories(metadataRepositoryManager);
	}

	public void logRepositories(final URI[] repositories) {
		logFileWriter.println();
		logFileWriter.println("Software Repositories");
		logFileWriter.println("---------------------");
		logFileWriter.println();
		for (final URI uri : repositories) {
			logFileWriter.print("  ");
			logFileWriter.println(uri.toString());
		}
	}

	public void logUninstallStatus(final UninstallOperation op, final IStatus result) {
		logFileWriter.println();
		logFileWriter.println("Software Removal");
		logFileWriter.println("----------------");
		printStatus(result, "");
		logFileWriter.println(op.getResolutionDetails());

		if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
			logFileWriter.println();
			logFileWriter.println("Software removal not possible.");
		} else if (!result.isOK()) {
			logFileWriter.println();
			logFileWriter.println("Uninstall operation resolved with warnings. An uninstallation will be forced.");
		}
	}

	public void nothingToAdd() {
		logFileWriter.println();
		logFileWriter.println("No new packages to install.");
	}

	public void nothingToRemove() {
		logFileWriter.println();
		logFileWriter.println("No revoked packages to remove.");
	}

	private void printRepositories(final IRepositoryManager repositoryManager) {
		final URI[] knownRepositories = repositoryManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (final URI uri : knownRepositories) {
			final boolean enabled = repositoryManager.isEnabled(uri);
			final String nickName = repositoryManager.getRepositoryProperty(uri, IRepository.PROP_NICKNAME);
			logFileWriter.print("  ");
			logFileWriter.print(uri.toString());
			if (null != nickName) {
				logFileWriter.print(" (");
				logFileWriter.print(nickName);
				logFileWriter.print(")");
			}
			if (!enabled) {
				logFileWriter.print(" (disabled)");
			}
			logFileWriter.println();
		}
	}

	private void printStatus(final IStatus status, final String ident) {
		if (StringUtils.isNotBlank(status.getMessage())) {
			logFileWriter.print(ident);
			logFileWriter.print(status.getMessage());
			if (null != status.getException()) {
				logFileWriter.print(" (");
				logFileWriter.print(ExceptionUtils.getRootCauseMessage(status.getException()));
				logFileWriter.print(")");
			}
			logFileWriter.println();
		}
		if (status.isMultiStatus()) {
			final String childIdent = StringUtils.isNotBlank(status.getMessage()) ? ident + "  " : ident;
			final IStatus[] children = status.getChildren();
			for (final IStatus child : children) {
				printStatus(child, childIdent);
			}
		}
	}

	public void recoveredSession(final String recoveredInstallSessionId) {
		logFileWriter.println();
		logFileWriter.println("Continuing with previous installation session");
	}

	public void restart() {
		logFileWriter.println();
		logFileWriter.println("Restarting system. Installation will continue afterwards.");
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("InstallLog [sessionId=").append(sessionId).append(", logFile=").append(logFile).append("]");
		return builder.toString();
	}

	private void writeFooter() {
		logFileWriter.println();
		logFileWriter.println();
		logFileWriter.println("<EOF>");
	}

	private void writeHeader() {
		logFileWriter.println("Installation Log");
		logFileWriter.println("================");
		logFileWriter.println();
		logFileWriter.println("Created: " + DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(creationDate));
		logFileWriter.println("   Node: " + P2Activator.getInstance().getService(INodeEnvironment.class).getNodeId());
		logFileWriter.println("Session: " + sessionId);
		logFileWriter.println("   Path: " + logFile.getPath());
		logFileWriter.println();
		logFileWriter.println();
	}

}
