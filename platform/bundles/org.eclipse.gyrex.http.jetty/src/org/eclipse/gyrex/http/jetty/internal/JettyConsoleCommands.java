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
package org.eclipse.gyrex.http.jetty.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.gyrex.http.jetty.admin.ChannelDescriptor;
import org.eclipse.gyrex.http.jetty.admin.ICertificate;
import org.eclipse.gyrex.http.jetty.admin.IJettyManager;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * Console commands
 */
public class JettyConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(IJettyManager jettyManager, CommandInterpreter ci) throws Exception;

		public String getHelp() {
			return help;
		}

		protected void printInvalidArgs(final CommandInterpreter ci) {
			ci.println("ERROR: invalid arguments");
			ci.println("\t" + getHelp());
		}
	}

	static final class ImportCertificateCommand extends Command {
		/**
		 * Creates a new instance.
		 * 
		 * @param help
		 */
		private ImportCertificateCommand(final String help) {
			super(help);
		}

		@Override
		public void execute(final IJettyManager jettyManager, final CommandInterpreter ci) throws Exception {
			final String certificateId = ci.nextArgument();
			if (certificateId == null) {
				printInvalidArgs(ci);
				return;
			}

			final String keystorePath = ci.nextArgument();
			if (keystorePath == null) {
				printInvalidArgs(ci);
				ci.println("Missing keystorePath");
				return;
			}

			final String keystoreType = ci.nextArgument();
			if (keystoreType == null) {
				printInvalidArgs(ci);
				ci.println("Missing keystoreType");
				return;
			}
			if (!keystoreType.equalsIgnoreCase("pkcs12") && !keystoreType.equalsIgnoreCase("jks")) {
				printInvalidArgs(ci);
				ci.println("Unsupported keystoreType: " + keystoreType);
				ci.println("Supported keystore types are: PKCS12, JKS");
				return;
			}

			final String keystorePassword = ci.nextArgument();
			final String keyPassword = ci.nextArgument();

			InputStream keystoreInputStream = null;
			try {
				// import key store
				final KeyStore tempKs = KeyStore.getInstance(keystoreType);
				keystoreInputStream = new BufferedInputStream(FileUtils.openInputStream(new File(keystorePath)));
				tempKs.load(keystoreInputStream, null != keystorePassword ? keystorePassword.toCharArray() : null);

				// initialize new JKS store
				final KeyStore ks = KeyStore.getInstance("JKS");
				ks.load(null);

				// generate passwords for new keystore
				final char[] generatedKeystorePassword = UUID.randomUUID().toString().toCharArray();
				final char[] generatedKeyPassword = UUID.randomUUID().toString().toCharArray();

				// verify and copy into new store
				final Enumeration aliases = tempKs.aliases();
				while (aliases.hasMoreElements()) {
					final String alias = (String) aliases.nextElement();
					ci.println("Processing entry: " + alias);
					if (tempKs.isKeyEntry(alias)) {
						ci.println("Loading key for entry: " + alias);
						final Key key = tempKs.getKey(alias, null != keyPassword ? keyPassword.toCharArray() : null != keystorePassword ? keystorePassword.toCharArray() : null);
						ci.println("Loading certificate chain for entry: " + alias);
						Certificate[] chain = tempKs.getCertificateChain(alias);
						if (null == chain) {
							final Certificate certificate = tempKs.getCertificate(alias);
							if (null == certificate) {
								ci.println("CertificateDefinition chain missing for key '" + alias + "'!");
								ci.println("Please import the complete certificate change into the keystore");
								return;
							}
							chain = new Certificate[] { certificate };
						}
						for (final Certificate certificate : chain) {
							ci.println("Found certificate:");
							ci.println(certificate);
						}
						ks.setKeyEntry("jetty", key, generatedKeyPassword, chain);
						break;
					} else {
						ci.println("No key available for entry: " + alias);
					}
				}

				// write into bytes
				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				ks.store(out, generatedKeystorePassword);

				// import
				jettyManager.addCertificate(certificateId, out.toByteArray(), generatedKeystorePassword, generatedKeyPassword);
				ci.println("Imported certificate " + certificateId + "!");
			} finally {
				IOUtils.closeQuietly(keystoreInputStream);
			}
		}
	}

	static final Map<String, Command> commands = new TreeMap<String, Command>();
	static {
		commands.put("ls", new Command(" connectors|certificates [filterString] \t - list all connectors") {
			@Override
			public void execute(final IJettyManager jettyManager, final CommandInterpreter ci) throws Exception {
				final String what = ci.nextArgument();
				if (what == null) {
					printInvalidArgs(ci);
					return;
				}

				final String filterString = ci.nextArgument();
				if (StringUtils.startsWithIgnoreCase("connectors", what)) {
					final Collection<ChannelDescriptor> channels = jettyManager.getChannels();
					for (final ChannelDescriptor descriptor : channels) {
						if ((null == filterString) || StringUtils.contains(descriptor.getId(), filterString)) {
							ci.println(String.format("%s [%s]", descriptor.getId(), descriptor.toString()));
						}
					}
				} else if (StringUtils.startsWithIgnoreCase("certificates", what)) {
					final Collection<ICertificate> certificates = jettyManager.getCertificates();
					for (final ICertificate certificate : certificates) {
						if ((null == filterString) || StringUtils.contains(certificate.getId(), filterString)) {
							ci.println(String.format("%s [%s]", certificate.getId(), certificate.getInfo()));
						}
					}
				} else {
					printInvalidArgs(ci);
					ci.println("Don't know what to list. Connectors? Certificates?");
					return;
				}

			}
		});

		commands.put("addConnector", new Command("<connectorId> <port> [<secure> <certificateId>]\t - adds a connector") {
			@Override
			public void execute(final IJettyManager jettyManager, final CommandInterpreter ci) throws Exception {
				final String channelId = ci.nextArgument();
				if (channelId == null) {
					printInvalidArgs(ci);
					return;
				}

				ChannelDescriptor channelDescriptor = jettyManager.getChannel(channelId);
				if (channelDescriptor == null) {
					channelDescriptor = new ChannelDescriptor();
				}
				channelDescriptor.setId(channelId);

				channelDescriptor.setPort(NumberUtils.toInt(ci.nextArgument()));
				channelDescriptor.setSecure(BooleanUtils.toBoolean(ci.nextArgument()));

				if (channelDescriptor.isSecure()) {
					final String certificateId = ci.nextArgument();
					if (certificateId == null) {
						ci.println("secure connectors require a certificate id");
						return;
					}
					channelDescriptor.setCertificateId(certificateId);
				}

				jettyManager.saveChannel(channelDescriptor);
			}
		});

		commands.put("removeConnector", new Command("<connectorId>\t - removes a connector") {
			@Override
			public void execute(final IJettyManager jettyManager, final CommandInterpreter ci) throws Exception {
				final String channelId = ci.nextArgument();
				if (channelId == null) {
					printInvalidArgs(ci);
					return;
				}

				jettyManager.removeChannel(channelId);
			}
		});

		commands.put("importCertificate", new ImportCertificateCommand("<certificateId> <keystorePath> <keystoreType> [<keystorePassword> [<keyPassword>]]\t - imports a certificate"));

		commands.put("removeCertificate", new Command("<certificateId>\t - removes a certificate") {
			@Override
			public void execute(final IJettyManager jettyManager, final CommandInterpreter ci) throws Exception {
				final String certificateId = ci.nextArgument();
				if (certificateId == null) {
					printInvalidArgs(ci);
					return;
				}

				jettyManager.removeCertificate(certificateId);
			}
		});
	}

	static void _zkHelp(final CommandInterpreter ci) {
		ci.println("jetty <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			ci.println("\t" + cmd + " " + commands.get(cmd).getHelp());
		}
	}

	public void _jetty(final CommandInterpreter ci) throws Exception {
		final String command = ci.nextArgument();
		if (command == null) {
			_zkHelp(ci);
			return;
		}

		final Command cmd = commands.get(command);
		if (cmd == null) {
			ci.println("ERROR: unknown Jetty command " + command);
			_zkHelp(ci);
			return;
		}

		IJettyManager manager = null;
		try {
			manager = HttpJettyActivator.getInstance().getJettyManager();
		} catch (final IllegalStateException e) {
			ci.println("Jetty manager not available!");
			return;
		}
		try {
			cmd.execute(manager, ci);
		} catch (final Exception e) {
			if (JettyDebug.debug) {
				ci.printStackTrace(e);
			} else {
				ci.println(ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---Jetty Commands---");
		help.appendln("\tjetty <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			help.appendln("\t\t" + cmd + " " + commands.get(cmd).getHelp());
		}
		return help.toString();
	}

}
