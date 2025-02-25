package com.j256.simpleschemareg;

import java.io.File;
import java.io.PrintStream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * Schema registry main class.
 */
public class Main {

	private String host;
	private String rootDir;
	private int port;
	private boolean handleShutdown;
	private boolean verbose;

	public static void main(String[] args) {
		new Main().doMain(args);
	}

	private void doMain(String[] args) {

		processArgs(args);

		Server server = null;
		ServerConnector connector = null;

		SchemaPersister persister = new FileSchemaPersister(new File(rootDir));

		try {
			// start a web-server for callback purposes
			server = new Server();
			OurHandler handler = new OurHandler(persister, handleShutdown, verbose);
			server.setHandler(handler);
			connector = new ServerConnector(server);
			if (host != null) {
				connector.setHost(host);
			}
			connector.setPort(port);
			connector.setIdleTimeout(5000);
			server.addConnector(connector);
			server.start();
			// this might wait until the process is killed
			handler.waitForShutdown();
			server.stop();
		} catch (Exception e) {
			System.err.println("Problem starting or stopping webserver: " + e);
			e.printStackTrace();
		} finally {
			if (connector != null) {
				connector.close();
			}
		}
	}

	private void processArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if ("-b".equals(args[i])) {
				i++;
				if (i >= args.length) {
					usageMessageThenExit("Missing argument to -b", 1);
				}
				host = args[i];
			} else if ("-h".equals(args[i]) || "--help".equals(args[i]) || "--usage".equals(args[i])) {
				usageMessageThenExit(null, 0);
			} else if ("-p".equals(args[i])) {
				i++;
				if (i >= args.length) {
					usageMessageThenExit("Missing argument to -p", 1);
				}
				try {
					port = Integer.parseInt(args[i]);
				} catch (NumberFormatException nfe) {
					usageMessageThenExit("Invalid number argument to -p: " + args[i], 1);
				}
			} else if ("-r".equals(args[i])) {
				i++;
				if (i >= args.length) {
					usageMessageThenExit("Missing argument to -r", 1);
				}
				rootDir = args[i];
			} else if ("-s".equals(args[i])) {
				handleShutdown = true;
			} else if ("-v".equals(args[i])) {
				verbose = true;
			}
		}
		if (port == 0) {
			usageMessageThenExit("Port (-p) must be specified", 1);
		}
		if (rootDir == null) {
			usageMessageThenExit("Root-directory (-r) must be specified", 1);
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
		outputStream.println("Usage: simple-schema-reg -p port -r dir [-b bind-host] [-s] [-v] ");
		outputStream.println("       -b bind-host  name of host to bind to, if not specified then all");
		outputStream.println("       -p port       number of the port to bind to");
		outputStream.println("       -r root-dir   root direcctory where the schema files are stored");
		outputStream.println("       -s            enable the /shutdown GET command");
		outputStream.println("       -v            verbose messages to stdout");
		System.exit(errCode);
	}
}
