package com.j256.simpleschemareg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.gson.Gson;
import com.j256.simpleschemareg.entities.ErrorResponse;
import com.j256.simpleschemareg.entities.IdResponse;
import com.j256.simpleschemareg.entities.SchemaDetails;
import com.j256.simpleschemareg.entities.SchemaInfo;
import com.j256.simpleschemareg.entities.SubjectVersionResponse;

/**
 * Jetty handler that services all of our schema persistence and lookup calls.
 */
public class SchemaRegHandler extends AbstractHandler {

	// GET /subjects/(string: subject)/versions/(versionId: version)/schema
	private static final String GET_SHUTDOWN = "/shutdown";
	private static final String GET_SUBJECTS = "/subjects";
	private static final Pattern GET_SCHEMA_ID_PATTERN = Pattern.compile("/schemas/ids/(\\d+)");
	private static final Pattern GET_SCHEMCA_ID_SCHEMA_PATTERN = Pattern.compile("/schemas/ids/(\\d+)/schema");
	private static final Pattern GET_SUBJECT_VERSION_PATTERN = Pattern.compile("/subjects/([^/]+)/versions/(\\d+)");
	private static final Pattern GET_SUBJECT_VERSIONS_PATTERN = Pattern.compile("/subjects/([^/]+)/versions");
	private static final Pattern GET_SUBJECT_VERSION_SCHEMA_PATTERN =
			Pattern.compile("/subjects/([^/]+)/versions/(\\d+)/schema");
	private static final Pattern POST_SUBJECT_PATTERN = Pattern.compile("/subjects/([^/]+)/versions");
	private static final Pattern POST_SUBJECT_CHECK_PATTERN = Pattern.compile("/subjects/([^/]+)");
	private static final Pattern DELETE_SUBJECT_PATTERN = Pattern.compile("/subjects/([^/]+)");
	private static final Pattern DELETE_SUBJECT_VERSION_PATTERN = Pattern.compile("/subjects/([^/]+)/versions/(\\d+)");
	private static final String PERMANENT_PARAMETER = "permanent";

	private final Gson gson = new Gson();

	private final SchemaPersister persister;
	private final String pathPrefix;
	private final int pathPrefixLength;
	private final boolean handleShutdown;
	private final boolean verbose;

	private volatile boolean shuttingDown;

	public SchemaRegHandler(SchemaPersister persister, String pathPrefix, boolean handleShutdown, boolean verbose) {
		this.persister = persister;
		this.pathPrefix = pathPrefix;
		if (pathPrefix == null) {
			this.pathPrefixLength = 0;
		} else {
			this.pathPrefixLength = pathPrefix.length();
		}
		this.handleShutdown = handleShutdown;
		this.verbose = verbose;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		String methodStr = baseRequest.getMethod();
		HttpMethod method = HttpMethod.fromString(methodStr);
		if (method == null) {
			writeResponseObj(response, HttpStatus.BAD_REQUEST_400,
					new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unknown request method: " + methodStr));
			return;
		}

		if (method == HttpMethod.GET) {
			handleGet(target, baseRequest, request, response);
		} else if (method == HttpMethod.POST) {
			handlePost(target, baseRequest, request, response);
		} else if (method == HttpMethod.DELETE) {
			handleDelete(target, baseRequest, request, response);
		} else {
			writeResponseObj(response, HttpStatus.BAD_REQUEST_400,
					new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled request method: " + method));
			return;
		}
	}

	/**
	 * Wait until the handler says we should shutdown.
	 */
	public synchronized void waitForShutdown() {
		if (verbose) {
			printMessage("Waiting for shutdown");
		}
		while (!shuttingDown) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.err.println("Warning: waiting for shutdown was interruped");
				return;
			}
		}
	}

	private void handleGet(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		String pathInfo = getPathInfo(request, response);
		if (pathInfo == null) {
			return;
		}

		// GET /shutdown
		if (handleShutdown && GET_SHUTDOWN.equals(pathInfo)) {
			if (verbose) {
				printMessage("Shutting down");
			}
			// need to write the response before the shutdown is notified
			writeResponseObj(response, HttpStatus.OK_200, new ErrorResponse(HttpStatus.OK_200, "shutting down"));
			synchronized (this) {
				shuttingDown = true;
				this.notify();
			}
			return;
		}

		// GET /subjects
		if (GET_SUBJECTS.equals(pathInfo)) {
			String[] subjects = persister.lookupSubjects();
			if (verbose) {
				printMessage("Listing subjects: " + Arrays.toString(subjects));
			}
			writeResponseObj(response, HttpStatus.OK_200, subjects);
			return;
		}

		// GET /schemas/ids/(id: schema-id)
		Matcher matcher = GET_SCHEMA_ID_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			long schemaId = convertLong(response, "schema-id", matcher.group(1));

			SchemaDetails details = persister.lookupSchemaId(schemaId);
			if (details == null) {
				writeResponseObj(response, HttpStatus.NOT_FOUND_404,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "schema-id " + schemaId + " not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up schmea-id '" + schemaId + " got id " + details.getId());
			}

			writeResponseObj(response, HttpStatus.OK_200, new SchemaInfo(details.getSchema()));
			return;
		}

		// GET /schemas/ids/(int: schema-id)/schema");
		matcher = GET_SCHEMCA_ID_SCHEMA_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			long schemaId = convertLong(response, "schema-id", matcher.group(1));

			SchemaDetails details = persister.lookupSchemaId(schemaId);
			if (details == null) {
				writeResponseObj(response, HttpStatus.NOT_FOUND_404,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "schema-id " + schemaId + " not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up schmea-id '" + schemaId + " got id " + details.getId());
			}

			writeResponseStr(response, details.getSchema());
			return;
		}

		// GET /subjects/(string: subject)/versions/(int: version)
		matcher = GET_SUBJECT_VERSION_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long version = convertLong(response, "version", matcher.group(2));

			SchemaDetails details = persister.lookupSubjectVersion(subject, version);
			if (details == null) {
				writeResponseObj(response, HttpStatus.NOT_FOUND_404, new ErrorResponse(HttpStatus.NOT_FOUND_404,
						"subject '" + subject + "' and version " + version + " not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up subject '" + subject + "' version " + version + " got id " + details.getId());
			}

			writeResponseObj(response, HttpStatus.OK_200,
					new SubjectVersionResponse(subject, version, details.getId(), details.getSchema()));
			return;
		}

		// GET /subjects/(string: subject))/versions
		matcher = GET_SUBJECT_VERSIONS_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			long[] versions = persister.lookupSubjectVersions(subject);
			if (versions == null) {
				writeResponseObj(response, HttpStatus.NOT_FOUND_404,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "subject '" + subject + "' not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up subject '" + subject + "' versions: " + Arrays.toString(versions));
			}

			writeResponseObj(response, HttpStatus.OK_200, versions);
			return;
		}

		// GET /subjects/(string: subject)/versions/(int: version)/schema
		matcher = GET_SUBJECT_VERSION_SCHEMA_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long version = convertLong(response, "version", matcher.group(2));

			SchemaDetails details = persister.lookupSubjectVersion(subject, version);
			if (details == null) {
				writeResponseObj(response, HttpStatus.NOT_FOUND_404, new ErrorResponse(HttpStatus.NOT_FOUND_404,
						"subject '" + subject + "' and version " + version + " not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up subject '" + subject + "' version " + version + " schema got id "
						+ details.getId());
			}

			writeResponseStr(response, details.getSchema());
			return;
		}

		writeResponseObj(response, HttpStatus.BAD_REQUEST_400,
				new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled GET request: " + pathInfo));
	}

	/**
	 * Handle post events.
	 */
	private void handlePost(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String pathInfo = getPathInfo(request, response);
		if (pathInfo == null) {
			return;
		}

		// POST /subjects/(string: subject)/versions
		Matcher matcher = POST_SUBJECT_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			SchemaInfo saveSchema;
			// read in the schema
			try (BufferedReader reader = request.getReader();) {
				saveSchema = gson.fromJson(reader, SchemaInfo.class);
			}
			SchemaDetails details = persister.saveSchema(subject, saveSchema.getSchema());
			if (details == null) {
				writeResponseObj(response, HttpStatus.INTERNAL_SERVER_ERROR_500, new ErrorResponse(
						HttpStatus.INTERNAL_SERVER_ERROR_500, " saving subject '" + subject + "' failed"));
				return;
			}

			if (verbose) {
				printMessage("Saved schema for subject '" + subject + "' got version " + details.getVersion() + ", id "
						+ details.getId());
			}

			writeResponseObj(response, HttpStatus.OK_200, new IdResponse(details.getId()));
			return;
		}

		// POST /subjects/(string: subject)
		matcher = POST_SUBJECT_CHECK_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			SchemaInfo saveSchema;
			// read in the schema
			try (BufferedReader reader = request.getReader();) {
				saveSchema = gson.fromJson(reader, SchemaInfo.class);
			}
			SchemaDetails details = persister.lookupSchema(subject, saveSchema.getSchema());
			if (details == null) {
				writeResponseObj(response, HttpStatus.NOT_FOUND_404,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "subject '" + subject + "' check not found"));
				return;
			}

			if (verbose) {
				printMessage("Checking schema for subject '" + subject + "' got version " + details.getVersion()
						+ ", id " + details.getId());
			}

			writeResponseObj(response, HttpStatus.OK_200,
					new SubjectVersionResponse(subject, details.getVersion(), details.getId(), details.getSchema()));
			return;
		}

		writeResponseObj(response, HttpStatus.BAD_REQUEST_400,
				new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled POST request: " + pathInfo));
	}

	/**
	 * Handle delete events.
	 */
	private void handleDelete(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String pathInfo = getPathInfo(request, response);
		if (pathInfo == null) {
			return;
		}

		// DELETE /subjects/(string: subject)
		Matcher matcher = DELETE_SUBJECT_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long[] versions = persister.deleteSubject(subject);
			if (verbose) {
				printMessage("Deleting subject '" + subject + "' got versions: " + Arrays.toString(versions));
			}
			writeResponseObj(response, HttpStatus.OK_200, versions);
			return;
		}

		// DELETE /subjects/(string: subject)/versions/(int: version)
		matcher = DELETE_SUBJECT_VERSION_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long version = convertLong(response, "version", matcher.group(2));
			boolean permanent = Boolean.parseBoolean(request.getParameter(PERMANENT_PARAMETER));
			if (verbose) {
				printMessage("Deleting subject '" + subject + "', version " + version + ", permanent " + permanent);
			}
			SchemaDetails details = persister.deleteSubjectVersion(subject, version, permanent);
			if (details == null) {
				writeResponseObj(response, HttpStatus.NOT_FOUND_404, new ErrorResponse(HttpStatus.NOT_FOUND_404,
						"subject '" + subject + "' version " + version + " not found"));
			} else {
				// response is just a version number strangely
				writeResponseObj(response, HttpStatus.OK_200, version);
			}
			return;
		}

		writeResponseObj(response, HttpStatus.BAD_REQUEST_400,
				new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled DELETE request: " + pathInfo));
	}

	private String getPathInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String pathInfo = request.getPathInfo();
		if (pathPrefix == null || pathPrefixLength == 0) {
			return pathInfo;
		}
		if (pathInfo.length() < pathPrefixLength || !pathInfo.startsWith(pathPrefix)) {
			writeResponseObj(response, HttpStatus.NOT_FOUND_404,
					new ErrorResponse(HttpStatus.NOT_FOUND_404, "unknown path: " + pathInfo));
			return null;
		}
		return pathInfo.substring(pathPrefixLength);
	}

	/**
	 * Convert and return a long argument.
	 * 
	 * @return The converted long or -1 if there was an error.
	 */
	private long convertLong(HttpServletResponse response, String label, String str) throws IOException {
		try {
			return Long.parseLong(str);
		} catch (NumberFormatException nfe) {
			// may not get here because of the regex \d+ but let's be careful out there
			writeResponseObj(response, HttpStatus.BAD_REQUEST_400,
					new ErrorResponse(HttpStatus.BAD_REQUEST_400, "bad request " + label + " number: " + str));
			return -1;
		}
	}

	private void writeResponseStr(HttpServletResponse response, String str) throws IOException {
		try (Writer writer = response.getWriter();) {
			writer.append(str);
		}
	}

	private void writeResponseObj(HttpServletResponse response, int status, Object obj) throws IOException {
		if (verbose && obj instanceof ErrorResponse) {
			printMessage("Writing error response: " + obj);
		}
		if (status != HttpStatus.OK_200) {
			response.setStatus(status);
		}
		try (Writer writer = response.getWriter();) {
			gson.toJson(obj, writer);
		}
	}

	private void printMessage(String msg) {
		System.out.println(msg);
		System.out.flush();
	}
}
