package com.j256.simpleschemareg;

import java.io.File;
import java.io.PrintStream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Schema registry main class.
 */
public class Main {

	private static final int DEFAILT_IDLE_TIMEOUT_MILLIS = 5000;
	private static final String SSL_KEYSTORE_LOCATION_ENV = "SSL_KEYSTORE_LOCATION";
	private static final String SSL_KEYSTORE_PASSWORD_ENV = "SSL_KEYSTORE_PASSWORD";
	private static final String SSL_KEY_PASSWORD_ENV = "SSL_KEY_PASSWORD";

	private String bindHost;
	private String rootDir = ".";
	private int httpPort;
	private int sslPort;
	private boolean handleShutdown;
	private boolean verbose;
	private String keyStorePath;
	private String keyStorePassword;
	private String keyPassword;

	public static void main(String[] args) {
		new Main().doMain(args);
	}

	private void doMain(String[] args) {

		processArgs(args);

		Server server = null;
		ServerConnector httpConnector = null;
		ServerConnector sslConnector = null;

		SchemaPersister persister = new FileSchemaPersister(new File(rootDir));

		try {
			// start a web-server for callback purposes
			server = new Server();
			OurHandler ourHandler = new OurHandler(persister, null, handleShutdown, verbose);
			server.setHandler(ourHandler);

			if (httpPort != 0) {
				httpConnector = new ServerConnector(server);
				if (bindHost != null) {
					httpConnector.setHost(bindHost);
				}
				httpConnector.setPort(httpPort);
				httpConnector.setIdleTimeout(DEFAILT_IDLE_TIMEOUT_MILLIS);
				server.addConnector(httpConnector);
			}

			if (sslPort != 0) {
				SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
				sslContextFactory.setKeyStorePath(keyStorePath);
				sslContextFactory.setKeyStorePassword(keyStorePassword);
				sslContextFactory.setKeyManagerPassword(keyPassword);

				sslConnector = new ServerConnector(server, sslContextFactory);
				if (bindHost != null) {
					sslConnector.setHost(bindHost);
				}
				sslConnector.setPort(sslPort);
				sslConnector.setIdleTimeout(DEFAILT_IDLE_TIMEOUT_MILLIS);
				server.addConnector(sslConnector);
			}

			server.start();
			// this might wait until the process is killed
			ourHandler.waitForShutdown();
			server.stop();
		} catch (Exception e) {
			System.err.println("Problem starting or stopping webserver: " + e);
			e.printStackTrace();
		} finally {
			if (httpConnector != null) {
				httpConnector.close();
			}
			if (sslConnector != null) {
				sslConnector.close();
			}
		}
	}

	private void processArgs(String[] args) {
		for (int i = 0; i < args.length;) {
			String arg = args[i];
			i++;
			if ("-b".equals(arg)) {
				if (i >= args.length) {
					usageMessageThenExit("Missing argument to " + arg, 1);
				}
				bindHost = args[i++];
			} else if ("-h".equals(arg) || "--help".equals(arg) || "--usage".equals(arg)) {
				usageMessageThenExit(null, 0);
			} else if ("-p".equals(arg)) {
				if (i >= args.length) {
					usageMessageThenExit("Missing argument to " + arg, 1);
				}
				String portStr = args[i++];
				try {
					httpPort = Integer.parseInt(portStr);
				} catch (NumberFormatException nfe) {
					usageMessageThenExit("Invalid number argument to " + arg + ": " + portStr, 1);
				}
			} else if ("-P".equals(arg)) {
				if (i >= args.length) {
					usageMessageThenExit("Missing argument to " + arg, 1);
				}
				String portStr = args[i++];
				try {
					sslPort = Integer.parseInt(portStr);
				} catch (NumberFormatException nfe) {
					usageMessageThenExit("Invalid number argument to " + arg + ": " + portStr, 1);
				}
			} else if ("-r".equals(arg)) {
				if (i >= args.length) {
					usageMessageThenExit("Missing argument to " + arg, 1);
				}
				rootDir = args[i++];
			} else if ("-s".equals(arg)) {
				handleShutdown = true;
			} else if ("-v".equals(arg)) {
				verbose = true;
			}
		}

		if (httpPort == 0 && sslPort == 0) {
			usageMessageThenExit("HTTP port (-p) or SSL port (-P) must be specified", 1);
		}
		if (sslPort != 0) {
			// verify ssl port options
			keyStorePath = System.getenv(SSL_KEYSTORE_LOCATION_ENV);
			if (keyStorePath == null) {
				usageMessageThenExit(
						"SSL port (-P) specified but keystore location env not set: " + SSL_KEYSTORE_LOCATION_ENV, 1);
			}
			keyStorePassword = System.getenv(SSL_KEYSTORE_PASSWORD_ENV);
			if (keyStorePassword == null) {
				usageMessageThenExit(
						"SSL port (-P) specified but keystore password env not set: " + SSL_KEYSTORE_PASSWORD_ENV, 1);
			}
			keyPassword = System.getenv(SSL_KEY_PASSWORD_ENV);
			if (keyPassword == null) {
				usageMessageThenExit("SSL port (-P) specified but key password env not set: " + SSL_KEY_PASSWORD_ENV,
						1);
			}
		}
	}

	/**
	 * Display an optional message then the usage messages and then exit.
	 */
	private void usageMessageThenExit(String message, int errCode) {
		PrintStream outputStream = System.err;
		if (errCode == 0) {
			outputStream = System.out;
		}
		if (message != null) {
			outputStream.println(message);
		}
		outputStream.println("Usage: simple-schema-reg [-p port | -P port] -r dir [-b bind-host] [-s] [-v] ");
		outputStream.println("       -b bind-host  name of host to bind to, if not specified then all");
		outputStream.println("       -p http-port  number of the http port to bind to");
		outputStream.println("       -P ssl-port   number of the SSL port to bind to");
		outputStream.println("       -r root-dir   root direcctory where the schema files are stored");
		outputStream.println("       -s            enable the /shutdown GET command");
		outputStream.println("       -v            verbose messages to stdout");
		System.exit(errCode);
	}
}
