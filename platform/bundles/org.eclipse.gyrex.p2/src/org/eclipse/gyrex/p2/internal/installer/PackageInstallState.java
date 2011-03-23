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
package org.eclipse.gyrex.p2.internal.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;

import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.packages.InstallableUnitReference;
import org.eclipse.gyrex.p2.internal.packages.PackageDefinition;

import org.eclipse.core.runtime.IPath;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Helper for working with package install state.
 */
public class PackageInstallState {

	/** iu property identifying packages */
	public static final String IU_PROP_PACKAGE = P2Activator.SYMBOLIC_NAME + ".package";

	private static final Lock installSessionIdLock = new ReentrantLock();

	public static IInstallableUnit createUnit(final PackageDefinition definition) {
		//  units.1.id=my.product.category
		//  units.1.version=1.0.0
		//  units.1.provides.1.namespace=org.eclipse.equinox.p2.iu
		//  units.1.provides.1.name=my.product.category
		//  units.1.provides.1.version=1.0.0
		//  units.1.properties.1.name=org.eclipse.equinox.p2.type.category
		//  units.1.properties.1.value=true
		//  units.1.properties.2.name=org.eclipse.equinox.p2.name
		//  units.1.properties.2.value=My Category Name
		//  requires.1.namespace=org.eclipse.equinox.p2.iu
		//  requires.1.name=my.product
		//  requires.1.range=[1.0.0,1.0.0]
		//  requires.1.greedy=true

		final InstallableUnitDescription unitDescription = new MetadataFactory.InstallableUnitDescription();

		// populate unit id
		final String packageUnitId = getPackageUnitId(definition);
		unitDescription.setId(packageUnitId);

		// populate unit version
		final Version packageVersion = getPackageUnitVersion(definition);
		unitDescription.setVersion(packageVersion);

		// packages are singletons, always replace with newer version
		unitDescription.setSingleton(true);

		// although not intended for display purposes we set some properties
		unitDescription.setProperty(IInstallableUnit.PROP_NAME, "Software Package " + definition.getId());
		unitDescription.setProperty(IU_PROP_PACKAGE, Boolean.TRUE.toString());

		// add provided capability
		unitDescription.addProvidedCapabilities(Collections.singleton(MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, unitDescription.getId(), unitDescription.getVersion())));

		// add all IUs as greedy requires
		final List<IRequirement> requires = new ArrayList<IRequirement>();
		final Collection<InstallableUnitReference> componentsToInstall = definition.getComponentsToInstall();
		for (final InstallableUnitReference unit : componentsToInstall) {
			final VersionRange unitVersionRange = (null != unit.getVersion()) && (Version.emptyVersion.compareTo(unit.getVersion()) < 0) ? new VersionRange(unit.getVersion(), true, unit.getVersion(), true) : VersionRange.emptyRange;
			requires.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(), unitVersionRange, null, false, false, true));
		}
		unitDescription.addRequirements(requires);

		return MetadataFactory.createInstallableUnit(unitDescription);
	}

	public static String getActiveInstallSessionId() {
		final File sessionFile = getBaseLocation().append("sessionActive").toFile();
		installSessionIdLock.lock();
		try {
			return FileUtils.readFileToString(sessionFile, CharEncoding.UTF_8);
		} catch (final FileNotFoundException notFound) {
			return null;
		} catch (final IOException e) {
			throw new IllegalStateException("Unable to read active install session id. " + ExceptionUtils.getRootCauseMessage(e), e);
		} finally {
			installSessionIdLock.unlock();
		}
	}

	static IPath getBaseLocation() {
		return P2Activator.getInstance().getConfigLocation().append(P2Activator.SYMBOLIC_NAME);
	}

	private static String getPackageUnitId(final PackageDefinition definition) {
		return definition.getId().concat(".package");
	}

	private static Version getPackageUnitVersion(final PackageDefinition definition) {
		// TODO: need serial or timestamp to capture package versions
		// for now we just work with non-updatable packages
		return Version.createOSGi(1, 0, 0);
	}

	public static boolean isInstalled(final IProvisioningAgent agent, final PackageDefinition packageDefinition) throws BackingStoreException {
		// check if the package IU is available in the current profile
		final IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (null == profileRegistry) {
			throw new IllegalStateException("The current system has not been provisioned using p2. Unable to acquire profile registry.");
		}
		final IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
		if (null == profile) {
			throw new IllegalStateException("The current system has not been provisioned using p2. Unable to get profile.");
		}

		final IQueryResult<IInstallableUnit> result = profile.available(QueryUtil.createIUQuery(getPackageUnitId(packageDefinition), getPackageUnitVersion(packageDefinition)), null);
		return !result.isEmpty();
	}

	public static void removeActiveInstallSessionId() {
		final File sessionFile = getBaseLocation().append("sessionActive").toFile();
		installSessionIdLock.lock();
		try {
			if (sessionFile.isFile() && !sessionFile.delete()) {
				throw new IllegalStateException("Unable to delete active install session id. " + sessionFile.getPath());
			}
		} finally {
			installSessionIdLock.unlock();
		}
	}

	public static void setActiveInstallSessionId(final String activeSessionId) {
		final File sessionFile = getBaseLocation().append("sessionActive").toFile();
		installSessionIdLock.lock();
		try {
			FileUtils.writeStringToFile(sessionFile, activeSessionId, CharEncoding.UTF_8);
		} catch (final IOException e) {
			throw new IllegalStateException("Unable to write active install session id. " + ExceptionUtils.getRootCauseMessage(e), e);
		} finally {
			installSessionIdLock.unlock();
		}
	}

	/**
	 * Creates a new instance.
	 */
	private PackageInstallState() {
		// empty
	}

}
