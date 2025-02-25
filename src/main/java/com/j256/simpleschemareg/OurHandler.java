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
import com.j256.simpleschemareg.SchemaPersister.SchemaDetails;
import com.j256.simpleschemareg.entities.ErrorResponse;
import com.j256.simpleschemareg.entities.IdResponse;
import com.j256.simpleschemareg.entities.SchemaInfo;
import com.j256.simpleschemareg.entities.SubjectVersionIdSchemaResponse;

/**
 * Little Jetty handler to process the redirect after the jetty request is confirmed that records the generated
 * verification code.
 */
public class OurHandler extends AbstractHandler {

	// GET /subjects/(string: subject)/versions/(versionId: version)/schema
	private static final String GET_SHUTDOWN = "/shutdown";
	private static final String GET_SUBJECTS = "/subjects";
	private static final Pattern GET_SCHEMCA_ID = Pattern.compile("/schemas/ids/(\\d+)");
	private static final Pattern GET_SCHEMCA_ID_SCHEMA = Pattern.compile("/schemas/ids/(\\d+)/schema");
	private static final Pattern GET_SUBJECT_VERSION = Pattern.compile("/subjects/([^/]+)/versions/(\\d+)");
	private static final Pattern GET_SUBJECT_VERSIONS = Pattern.compile("/subjects/([^/]+)/versions");
	private static final Pattern GET_SUBJECT_VERSION_SCHEMA =
			Pattern.compile("/subjects/([^/]+)/versions/(\\d+)/schema");
	private static final Pattern POST_SUBJECT_PATTERN = Pattern.compile("/subjects/([^/]+)/versions");
	private static final Pattern POST_SUBJECT_CHECK_PATTERN = Pattern.compile("/subjects/([^/]+)");
	private static final Pattern DELETE_SUBJECT_PATTERN = Pattern.compile("/subjects/([^/]+)");

	private final SchemaPersister persister;
	private final boolean handleShutdown;
	private final boolean verbose;

	private volatile boolean shuttingDown;

	public OurHandler(SchemaPersister persister, boolean handleShutdown, boolean verbose) {
		this.persister = persister;
		this.handleShutdown = handleShutdown;
		this.verbose = verbose;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		HttpMethod method = HttpMethod.fromString(baseRequest.getMethod());
		if (method == null) {
			writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400,
					"unknown request method: " + baseRequest.getMethod()));
			return;
		}

		if (method == HttpMethod.GET) {
			handleGet(target, baseRequest, request, response);
		} else if (method == HttpMethod.POST) {
			handlePost(target, baseRequest, request, response);
		} else if (method == HttpMethod.DELETE) {
			handleDelete(target, baseRequest, request, response);
		} else {
			writeResponse(response,
					new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled request method: " + method));
			return;
		}
	}

	/**
	 * Wait until the handler says we should shutdown.
	 */
	public void waitForShutdown() {
		synchronized (this) {
			while (!shuttingDown) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private void handleGet(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		String pathInfo = request.getPathInfo();
		if (handleShutdown && GET_SHUTDOWN.equals(pathInfo)) {
			if (verbose) {
				printMessage("Shutting down");
			}
			synchronized (this) {
				shuttingDown = true;
				this.notify();
			}
			response.getWriter().close();
			return;
		}

		if (GET_SUBJECTS.equals(pathInfo)) {
			String[] subjects = persister.lookupSubjects();
			if (verbose) {
				printMessage("Listing subjects: " + Arrays.toString(subjects));
			}
			writeResponse(response, subjects);
			return;
		}

		Matcher matcher = GET_SCHEMCA_ID.matcher(pathInfo);
		if (matcher.matches()) {
			long schemaId = convertLong(response, "schema-id", matcher.group(1));
			if (schemaId < 0) {
				return;
			}

			SchemaDetails details = persister.lookupSchemaId(schemaId);
			if (details == null) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "schema-id '" + schemaId + " not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up schmea-id '" + schemaId + " got id " + details.getId());
			}

			writeResponse(response, new SchemaInfo(details.getSchema()));
			return;
		}

		matcher = GET_SCHEMCA_ID_SCHEMA.matcher(pathInfo);
		if (matcher.matches()) {
			long schemaId = convertLong(response, "schema-id", matcher.group(1));
			if (schemaId < 0) {
				return;
			}

			SchemaDetails details = persister.lookupSchemaId(schemaId);
			if (details == null) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "schema-id '" + schemaId + " not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up schmea-id '" + schemaId + " got id " + details.getId());
			}

			writeResponse(response, details.getSchema());
			return;
		}

		matcher = GET_SUBJECT_VERSION.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long version = convertLong(response, "version", matcher.group(2));
			if (version < 0) {
				return;
			}

			SchemaDetails details = persister.lookupSubjectVersion(subject, version);
			if (details == null) {
				writeResponse(response, new ErrorResponse(HttpStatus.NOT_FOUND_404,
						"subject '" + subject + "' and version " + version + " not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up subject '" + subject + "' version " + version + " got id " + details.getId());
			}

			writeResponse(response,
					new SubjectVersionIdSchemaResponse(subject, version, details.getId(), details.getSchema()));
			return;
		}

		matcher = GET_SUBJECT_VERSIONS.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			long[] versions = persister.lookupSubjectVersions(subject);
			if (versions == null) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "subject '" + subject + "' not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up subject '" + subject + "' versions: " + Arrays.toString(versions));
			}

			writeResponse(response, versions);
			return;
		}

		matcher = GET_SUBJECT_VERSION_SCHEMA.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long version = convertLong(response, "version", matcher.group(2));
			if (version < 0) {
				return;
			}

			SchemaDetails details = persister.lookupSubjectVersion(subject, version);
			if (details == null) {
				writeResponse(response, new ErrorResponse(HttpStatus.NOT_FOUND_404,
						"subject '" + subject + "' and version " + version + " not found"));
				return;
			}

			if (verbose) {
				printMessage("Looking up subject '" + subject + "' version " + version + " schema got id "
						+ details.getId());
			}

			writeResponse(response, details.getSchema());
			return;
		}

		writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled GET request: " + pathInfo));
	}

	/**
	 * Handle post events.
	 */
	private void handlePost(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String pathInfo = request.getPathInfo();
		// POST /subjects/(string: subject)/versions
		Matcher matcher = POST_SUBJECT_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			Gson gson = new Gson();
			SchemaInfo saveSchema;
			// read in the schema
			try (BufferedReader reader = request.getReader();) {
				saveSchema = gson.fromJson(reader, SchemaInfo.class);
			}
			SchemaDetails details = persister.saveSchema(subject, saveSchema.getSchema());
			if (details == null) {
				writeResponse(response, new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
						" saving subject '" + subject + "' failed"));
				return;
			}

			if (verbose) {
				printMessage("Saved schema for subject '" + subject + "' got version " + details.getVersion()
						+ ", id " + details.getId());
			}

			writeResponse(response, new IdResponse(details.getId()));
			return;
		}

		matcher = POST_SUBJECT_CHECK_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			Gson gson = new Gson();
			SchemaInfo saveSchema;
			// read in the schema
			try (BufferedReader reader = request.getReader();) {
				saveSchema = gson.fromJson(reader, SchemaInfo.class);
			}
			SchemaDetails details = persister.lookupSchema(subject, saveSchema.getSchema());
			if (details == null) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "subject '" + subject + "' schema not found"));
				return;
			}

			if (verbose) {
				printMessage("Checking schema for subject '" + subject + "' got version " + details.getVersion()
						+ ", id " + details.getId());
			}

			writeResponse(response, new IdResponse(details.getId()));
			return;
		}

		writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled POST request: " + pathInfo));
	}

	/**
	 * Handle delete events.
	 */
	private void handleDelete(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String pathInfo = request.getPathInfo();
		// POST /subjects/(string: subject)/versions
		Matcher matcher = DELETE_SUBJECT_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long[] versions = persister.deleteSubject(subject);
			if (verbose) {
				printMessage("Deleting subject '" + subject + "' got versions: " + Arrays.toString(versions));
			}
			writeResponse(response, versions);
			return;
		}

		writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled DELETE request: " + pathInfo));
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
			writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400, "bad request " + label + ": " + str));
			return -1;
		}
	}

	private void writeResponse(HttpServletResponse response, String str) throws IOException {
		try (Writer writer = response.getWriter();) {
			writer.append(str);
		}
	}

	private void writeResponse(HttpServletResponse response, Object obj) throws IOException {
		if (verbose && obj instanceof ErrorResponse) {
			printMessage("Writing error response: " + obj);
		}
		try (Writer writer = response.getWriter();) {
			Gson gson = new Gson();
			gson.toJson(obj, writer);
		}
	}

	private void printMessage(String msg) {
		System.out.println(msg);
		System.out.flush();
	}
}
